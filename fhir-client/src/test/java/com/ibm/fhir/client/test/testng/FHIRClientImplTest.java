/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.client.test.testng;

import static org.testng.Assert.assertNotNull;

import java.util.Properties;

import jakarta.ws.rs.client.WebTarget;

import org.testng.annotations.Test;

import com.ibm.fhir.client.FHIRClient;
import com.ibm.fhir.client.FHIRClientFactory;

/**
 * Tests FHIR Client configurations.
 */
public class FHIRClientImplTest {

    @Test
    public void testClientImplWithoutKeyStore() throws Exception {
        String baseURL = "http://localhost:9443/test";
        Properties properties = new Properties();
        properties.put(FHIRClient.PROPNAME_BASE_URL, baseURL);
        FHIRClient client = FHIRClientFactory.getClient(properties);
        WebTarget wt = client.getWebTarget(baseURL + "/Patient?_page=2");
        assertNotNull(wt);
    }

    @Test(enabled = false)
    public void testClientImplWithBasicAuth() throws Exception {
        /*
         * used during dev, it's handy test for basicauth, but not reliable for UT/IT.
         */
        String baseURL = "https://jigsaw.w3.org/HTTP/Basic/";
        Properties properties = new Properties();
        properties.put("fhirclient.basicauth.enabled", "true");
        properties.put("fhirclient.basicauth.username", "guest");
        properties.put("fhirclient.basicauth.password", "guest");
        properties.put("fhirclient.logging.enabled", "true");
        properties.put(FHIRClient.PROPNAME_BASE_URL, baseURL);
        FHIRClient client = FHIRClientFactory.getClient(properties);
        WebTarget wt = client.getWebTarget(baseURL);
        assertNotNull(wt.request().get());
    }
}