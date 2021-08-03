/**
 * (C) Copyright IBM Corp. 2019, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test.examples;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.annotations.Test;

import com.ibm.fhir.examples.Index;
import com.ibm.fhir.model.spec.test.DriverMetrics;
import com.ibm.fhir.model.spec.test.R4ExamplesDriver;
import com.ibm.fhir.server.test.FHIRServerTestBase;
import com.ibm.fhir.validation.test.ValidationProcessor;

/**
 * Basic sniff test of the FHIR Server.
 */
public class R4ExampleServerTest extends FHIRServerTestBase {

    /**
     * Process all the examples in the fhir-r4-spec example library
     */
    @Test(groups = { "server-examples" })
    public void processExamples() throws Exception {
        // Process each of the examples using the provided ExampleRequestProcessor. We want to
        // validate first before we try and send to FHIR
        final R4ExamplesDriver driver = new R4ExamplesDriver();

        // Setup a Pool
        ExecutorService es = Executors.newFixedThreadPool(5);
        driver.setPool(es, 5);

        DriverMetrics dm = new DriverMetrics();
        driver.setMetrics(dm);
        driver.setValidator(new ValidationProcessor());
        driver.setProcessor(new ExampleRequestProcessor(this, "default", dm, 1));

        String index = System.getProperty(this.getClass().getName()
            + ".index", Index.MINIMAL_JSON.name());
        driver.processIndex(Index.valueOf(index));
    }
}
