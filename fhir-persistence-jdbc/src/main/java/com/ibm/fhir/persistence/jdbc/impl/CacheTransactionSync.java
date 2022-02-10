/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.impl;

import java.util.logging.Logger;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import com.ibm.fhir.persistence.jdbc.FHIRPersistenceJDBCCache;
import com.ibm.fhir.persistence.jdbc.TransactionData;


/**
 * Adapter to synchronize the thread-local caches with the shared caches when the
 * transaction commits. It's important that we don't share ids created inside a
 * transaction with other threads until the data is committed - those ids are not
 * visible to other threads/transactions (the 'I' in ACID).
 */
public class CacheTransactionSync implements Synchronization {
    private static final Logger logger = Logger.getLogger(CacheTransactionSync.class.getName());

    // The sync registry...and I have no idea why this isn't passed as context to the methods
    private final TransactionSynchronizationRegistry txSyncRegistry;
    
    // The cache delegate to call when we receive an event
    private final FHIRPersistenceJDBCCache cache;
    
    private final String transactionDataKey;

    // A callback when we hit a rollback
    private final Runnable rolledBackHandler;

    /**
     * Public constructor
     * 
     * @param txSyncRegistry
     * @param cache
     * @param transactionDataKey
     * @param rolledBackHandler
     */
    public CacheTransactionSync(TransactionSynchronizationRegistry txSyncRegistry, FHIRPersistenceJDBCCache cache, String transactionDataKey,
            Runnable rolledBackHandler) {
        this.txSyncRegistry = txSyncRegistry;
        this.cache = cache;
        this.transactionDataKey = transactionDataKey;
        this.rolledBackHandler = rolledBackHandler;
    }
    
    @Override
    public void beforeCompletion() {
        // called just before the commit process starts, so we can use this to persist any data we've
        // been holding onto. This makes it really easy to collect parameters during bundle processing
        // and then insert them efficiently using a series of batches. The big benefit here is that
        // it greatly reduces the amount of time we could be locking rows in common_token_values which
        // helps with throughput.
        Object obj = txSyncRegistry.getResource(this.transactionDataKey);
        if (obj != null && obj instanceof TransactionData) {
            // important to log this when debugging because it might not be obvious how this happens
            logger.fine("Persisting TransactionData found in the TransactionSynchronizationRegistry");
            ((TransactionData)obj).persist();
        }
    }

    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
            cache.transactionCommitted();
        } else {
            // probably a rollback, so throw away everything
            logger.info("Transaction failed - afterCompletion(status = " + status + ")");
            cache.transactionRolledBack();

            if (rolledBackHandler != null) {
                rolledBackHandler.run();
            }
        }
    }
}