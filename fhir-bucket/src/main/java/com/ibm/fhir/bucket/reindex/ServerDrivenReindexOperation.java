/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.bucket.reindex;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;

import com.ibm.fhir.bucket.client.FHIRBucketClient;
import com.ibm.fhir.bucket.client.FHIRBucketClientUtil;
import com.ibm.fhir.bucket.client.FhirServerResponse;
import com.ibm.fhir.database.utils.api.DataAccessException;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.OperationOutcome.Issue;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Parameters.Parameter;
import com.ibm.fhir.model.resource.Resource;

/**
 * Drives the $reindex custom operation in parallel. Each thread keeps running
 * until the OperationOutcome indicates that no work remains to be processed.
 */
public class ServerDrivenReindexOperation extends DriveReindexOperation {
    private static final Logger logger = Logger.getLogger(ServerDrivenReindexOperation.class.getName());

    // the maximum number of requests we permit
    private final int maxConcurrentRequests;

    // flag to indicate if we should be running
    private volatile boolean running = true;

    private volatile boolean active = false;

    // count of how many threads are currently running
    private AtomicInteger currentlyRunning = new AtomicInteger();

    // thread pool for processing requests
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private final FHIRBucketClient fhirClient;

    private final String url = "$reindex";

    // The serialized Parameters resource sent with each POST
    private final String requestBody;

    private Thread monitorThread;

    /**
     * Public constructor
     * @param client the FHIR client
     * @param maxConcurrentRequests the number of threads to spin up
     */
    public ServerDrivenReindexOperation(FHIRBucketClient fhirClient, int maxConcurrentRequests, String tstampParam, int resourceCountParam) {
        this.fhirClient = fhirClient;
        this.maxConcurrentRequests = maxConcurrentRequests;

        Parameters parameters = Parameters.builder()
                .parameter(Parameter.builder().name(str("tstamp")).value(str(tstampParam)).build())
                .parameter(Parameter.builder().name(str("resourceCount")).value(intValue(resourceCountParam)).build())
                .build();

        // Serialize into the requestBody string used by all the threads
        this.requestBody = FHIRBucketClientUtil.resourceToString(parameters);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Reindex request parameters: " + requestBody);
        }
    }

    /**
     * Start the main loop
     */
    @Override
    public void init() {
        if (!running) {
            throw new IllegalStateException("Already shutdown");
        }

        // Initiate the monitorThread. This will fill the pool
        // with worker threads, and monitor for completion or failure
        logger.info("Starting monitor thread");
        this.monitorThread = new Thread(() -> monitorLoop());
        this.monitorThread.start();
    }

    /**
     * The main monitor loop.
     */
    public void monitorLoop() {
        while (this.running) {
            if (!this.active) {
                // See if we can make one successful request before filling the pool
                // with hundreds of parallel requests
                int currentThreadCount = this.currentlyRunning.get();
                if (currentThreadCount == 0) {
                    // Nothing currently running, so make one test call to verify things are working
                    logger.info("monitor probe - checking reindex operation");
                    if (callOnce() && this.running) {
                        // should be OK now to fill the pool with workers
                        logger.info("Test probe successful - filling worker pool");
                        this.active = true;

                        for (int i=0; i<this.maxConcurrentRequests && this.running && this.active; i++) {
                            this.currentlyRunning.addAndGet(1);
                            pool.execute(() -> callReindexOperation());

                            // Slow down the ramp-up so we don't hit a new server with
                            // hundreds of requests in one go
                            safeSleep(1000);
                        }
                    } else if (this.running) {
                        // call failed, so wait a bit before we try again
                        safeSleep(5000);
                    }
                } else {
                    // need to wait for all the existing threads to die off before we try to restart. This
                    // could take a while because we have a long tx timeout in Liberty.
                    logger.info("Waiting for current threads to complete before restart: " + currentThreadCount);
                    safeSleep(5000);
                }

            } else { // active
                // worker threads are active, so sleep for a bit before we check again
                safeSleep(5000);
            }
        }
    }

    /**
     * Sleep for the given number of ms, or until interrupted
     * @param ms
     */
    @Override
    protected void safeSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException x) {
            // NOP
        }
    }

    /**
     * Program is stopping, so tell the threads they can stop too
     */
    @Override
    public void signalStop() {
        this.running = false;

        // make sure the pool doesn't start new work
        pool.shutdown();
    }

    /**
     * Wait until things are stopped
     */
    @Override
    public void waitForStop() {
        if (this.running) {
            signalStop();
        }

        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException x) {
            logger.warning("Wait for pool shutdown interrupted");
        }

        try {
            // break any sleep inside the monitorThread
            this.monitorThread.interrupt();
            this.monitorThread.join();
        } catch (InterruptedException x) {
            logger.warning("Interrupted waiting for monitorThread completion");
        }
    }

    /**
     * Thread to repeatedly call the $reindex operation until the response
     * indicates all the work is complete.
     */
    private void callReindexOperation() {
        while (this.running && this.active) {
            boolean ok = false;
            try {
                ok = callOnce();
            } catch (DataAccessException x) {
                // allow active be set to false.  This will notify monitorLoop something is wrong.
                // Probably all threads will encounter the same exception and monitorLoop will
                // try to refill the pool if all threads exit.
                logger.severe("DataAccessException caught when contacting FHIR server. FHIR client thread will exit." + x.toString() );
            } catch (IllegalStateException x) {
                // Fail for this exception too. fhir-bucket fhir client suggests this exception results from config error.
                // So probably this will be caught first time monitorLoop calls callOnce and not here.
                logger.severe("IllegalStateException caught. FHIR client thread will exit." + x.toString() );
            }
            if (!ok) {
                // stop everything on the first failure
                this.active = false;
            }
        }

        this.currentlyRunning.decrementAndGet();
    }

    /**
     * Make one call to the FHIR server $reindex operation
     * @return true if the call was successful (200 OK)
     */
    private boolean callOnce() {
        boolean result = false;

        // tell the FHIR Server to reindex a number of resources
        long start = System.nanoTime();
        FhirServerResponse response = fhirClient.post(url, requestBody);
        long end = System.nanoTime();

        double elapsed = (end - start) / 1e9;
        logger.info(String.format("called $reindex: %d %s [took %5.3f s]", response.getStatusCode(), response.getStatusMessage(), elapsed));

        if (response.getStatusCode() == HttpStatus.SC_OK) {
            Resource resource = response.getResource();
            if (resource != null) {
                if (resource.is(OperationOutcome.class)) {
                    // check the result to see if we should stop running
                    checkResult((OperationOutcome)resource);
                    result = true;
                } else {
                    logger.severe("FHIR Server reindex response is not an OperationOutcome: " + response.getStatusCode() + " " + response.getStatusMessage());
                    logger.severe("Actual response: " + FHIRBucketClientUtil.resourceToString(resource));
                }
            } else {
                // this would be a bit weird
                logger.severe("FHIR Server reindex operation returned no OperationOutcome: " + response.getStatusCode() + " " + response.getStatusMessage());
            }
        } else {
            // Stop as soon as we hit an error
            logger.severe("FHIR Server reindex operation returned an error: " + response.getStatusCode() + " " + response.getStatusMessage());
        }

        return result;
    }

    /**
     * Check the result to see if the server is telling us it's done
     * @param result
     */
    private void checkResult(OperationOutcome result) {
        List<Issue> issues = result.getIssue();
        if (issues.size() == 1) {
            Issue one = issues.get(0);
            if ("Reindex complete".equals(one.getDiagnostics().getValue())) {
                logger.info("Reindex - all done");

                // tell all the running threads they can stop now
                this.running = false;
            }
        }
    }
}
