/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.client.FHIRResponse;
import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.code.AdministrativeGender;

/**
 * This class tests the system history interactions
 * [base]/_history
 * [base]/_history?_count=10
 * [base]/_history?_count=10&_since=2021-02-21T03:00:53.878052Z"
 * [base]/_history?_count=10&_before=2021-02-21T03:00:53.878052Z"
 * 
 * Error case. Don't mix paging styles
 * [base]/_history?_count=10&_since=2021-02-21T03:00:53.878052Z&_changeIdMarker=4"
 */
public class SystemHistoryTest extends FHIRServerTestBase {
    private static final Logger logger = Logger.getLogger(SystemHistoryTest.class.getName());

    // Create some resources, update, delete and undelete
    private boolean deleteSupported = false;
    
    // the id of the test patient we create
    private String patientId;

    /**
     * Retrieve the server's conformance statement to determine the status of certain runtime options.
     *
     * @throws Exception
     */
    @BeforeClass
    public void retrieveConfig() throws Exception {
        deleteSupported = isDeleteSupported();
        System.out.println("Delete operation supported?: " + Boolean.valueOf(deleteSupported).toString());
    }

    /**
     * Generate some change log activity by creating, updating and deleting
     * some resources.
     * @throws Exception
     */
    @Test
    public void populateResourcesForHistory() throws Exception {
        if (!deleteSupported) {
            logger.info("Delete not supported. Skipping system history test");
            return; // can't do anything useful with this test
        }

        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = TestUtil.readLocalResource("patient-example-a.json");

        patient = patient.toBuilder().gender(AdministrativeGender.MALE).build();
        Entity<Patient> entity =
                Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response =
                target.path("Patient").request()
                .post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the patient's logical id value.
        this.patientId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new patient and verify it.
        response = target.path("Patient/"
                + patientId).request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        TestUtil.assertResourceEquals(patient, responsePatient);

        updateResource(responsePatient);                    // 1
        updateResource(responsePatient);                    // 2
        deleteResource("Patient", responsePatient.getId()); // 3
        undeleteResource(responsePatient);                  // 4
        updateResource(responsePatient);                    // 5

        // Create an Observation, so that we can check the type filtering works
        Observation observation =
                TestUtil.buildPatientObservation(patientId, "Observation1.json");

        Entity<Observation> observationEntity =
                Entity.entity(observation, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response observationResponse =
                target.path("Observation").request()
                        .post(observationEntity, Response.class);
        assertResponse(observationResponse, Response.Status.CREATED.getStatusCode());
    }


    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithTypePatient() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        assertNotNull(this.patientId);

        // Get the system history...which will be a bundle of changes. Without any
        // _since filter, we'll probably get back data which has been created by
        // other tests, so the only assertion we can make is we get back at least
        // the number of change records we expect.
        
        // Follow the next links until we get
        String requestPath = "Patient/_history";
        boolean found = false;
        int count;
        do {
            Response historyResponse =
                    target.path(requestPath)
                    .queryParam("_count", "2")
                    .request().get(Response.class);
            assertResponse(historyResponse, Response.Status.OK.getStatusCode());

            Bundle bundle = historyResponse.readEntity(Bundle.class);
            
            // Check the bundle to see if we found the patient
            for (Bundle.Entry be: bundle.getEntry()) {
                // simple way to see if our patient has appeared
                String fullUrl = be.getFullUrl().getValue();
                if (fullUrl.contains("Patient/" + patientId)) {
                    found = true;
                }
            }
            
            // See if there's more work to do
            count = bundle.getEntry().size();
            if (!found && count > 0) {
                // set up to follow the next link
                requestPath = getNextLink(bundle);
                assertNotNull(requestPath);
            }
        } while (count > 0 && !found);
        
        assertTrue(found, "Patient id in history");
    }

    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithTypeObservation() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        Response historyResponse =
                target.path("Observation/_history").request().get(Response.class);
        assertResponse(historyResponse, Response.Status.OK.getStatusCode());

        Bundle bundle = historyResponse.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertNotNull(bundle.getEntry());
        assertTrue(bundle.getEntry().size() >= 1);
    }
    
    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithMultipleTypes() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        Response historyResponse =
                target.path("_history")
                .queryParam("_count", "100")
                .queryParam("_type", "Patient,Observation")
                .request().get(Response.class);
        assertResponse(historyResponse, Response.Status.OK.getStatusCode());

        Bundle bundle = historyResponse.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertNotNull(bundle.getEntry());
        assertTrue(bundle.getEntry().size() >= 5);
        
        // Check that the next link was composed correctly:
        String nextLink = getNextLink(bundle);
        assertNotNull(nextLink);
        assertTrue(nextLink.contains("_type=Patient,Observation"));
    }

    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithNoType() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        Response historyResponse =
                target.path("_history").request().get(Response.class);
        assertResponse(historyResponse, Response.Status.OK.getStatusCode());

        Bundle bundle = historyResponse.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertNotNull(bundle.getEntry());
        assertTrue(bundle.getEntry().size() >= 5);
    }

    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithBadSort() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        Response historyResponse =
                target.path("_history")
                    .queryParam("_sort", "bogus")
                    .request().get(Response.class);
        assertResponse(historyResponse, Response.Status.BAD_REQUEST.getStatusCode());
    } 

    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithTypePatientAndOrderNone() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        assertNotNull(this.patientId);

        // Get the system history...which will be a bundle of changes. Without any
        // _since filter, we'll probably get back data which has been created by
        // other tests, so the only assertion we can make is we get back at least
        // the number of change records we expect.
        
        // Follow the next links until we 
        String requestPath = "Patient/_history";
        boolean found = false;
        int count;
        long lastResourceId = -1;
        String nextPath = null;
        do {
            final Response historyResponse;
            if (nextPath == null) {
                historyResponse = target.path(requestPath)
                    .queryParam("_count", "50")
                    .queryParam("_sort", "none")
                    .request().get(Response.class);
            } else {
                historyResponse = target.path(nextPath)
                    .request().get(Response.class);
            }
            assertResponse(historyResponse, Response.Status.OK.getStatusCode());

            Bundle bundle = historyResponse.readEntity(Bundle.class);
            
            // Check the bundle to see if we found the patient
            for (Bundle.Entry be: bundle.getEntry()) {
                // simple way to see if our patient has appeared
                String fullUrl = be.getFullUrl().getValue();
                if (fullUrl.contains("Patient/" + patientId)) {
                    found = true;
                }
                
                // Check that the resourceId value for the resource is always increasing. For
                // whole system history interactions, this id goes in the bundle.entry.id field
                long resourceId = Long.parseLong(be.getId());
                assertTrue(resourceId > lastResourceId, "check resource ordering");
                lastResourceId = resourceId;
            }

            // See if there's more work to do
            count = bundle.getEntry().size();
            if (count > 0) {
                // set up to follow the next link
                nextPath = getNextLink(bundle);
                assertNotNull(nextPath);
            }
        } while (count > 0);
        
        assertTrue(found, "Patient id in history");
    }

    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithTypePatientAndOrderASC() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        assertNotNull(this.patientId);

        // Get the system history...which will be a bundle of changes. Without any
        // _since filter, we'll probably get back data which has been created by
        // other tests, so the only assertion we can make is we get back at least
        // the number of change records we expect.

        // When sorting by _lastUpdated, there could be multiple records
        // sharing the same timestamp so we have to include equals in the
        // filter. This means that for the final request, we may get the
        // same data again and again, so terminating the loop requires a
        // bit more finesse.
        String requestPath = "Patient/_history";
        boolean found = false;
        int count;
        Instant prevLastUpdated = null;
        String nextPath = null;
        String prevDigest = null; // to help see if the response changed 
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant before = Instant.now().plus(1, ChronoUnit.HOURS);
        do {
            final Response historyResponse;
            if (nextPath == null) {
                // first time around the loop
                historyResponse = target.path(requestPath)
                    .queryParam("_count", "50")
                    .queryParam("_sort", "_lastUpdated")
                    .queryParam("_since", since)
                    .queryParam("_before", before)
                    .request()
                    .header("Prefer", "return=minimal")
                    .get(Response.class);
            } else {
                // loop repeat, using the next link from the previous response
                historyResponse = target.path(nextPath)
                    .request()
                    .header("Prefer", "return=minimal")
                    .get(Response.class);
            }
            assertResponse(historyResponse, Response.Status.OK.getStatusCode());

            Bundle bundle = historyResponse.readEntity(Bundle.class);
            
            // Check the bundle to see if we found the patient
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Bundle.Entry be: bundle.getEntry()) {
                // simple way to see if our patient has appeared
                String fullUrl = be.getFullUrl().getValue();
                if (fullUrl.contains("Patient/" + patientId)) {
                    found = true;
                }

                // Check that resources are ordered by last updated time
                assertNotNull(be.getResponse().getLastModified());
                assertNotNull(be.getResponse().getLastModified().getValue());
                Instant lastUpdated = be.getResponse().getLastModified().getValue().toInstant();
                if (prevLastUpdated != null) {
                    // also allow equals, hence the inversion of the test expression
                    assertTrue(!lastUpdated.isBefore(prevLastUpdated), "record _lastUpdated ordering");
                }
                prevLastUpdated = lastUpdated;

                // hash together the ids so that we can easily detect duplicate responses
                digest.update(be.getId().getBytes(StandardCharsets.UTF_8));
            }

            // See if there's more work to do
            count = bundle.getEntry().size();
            if (count > 0) {
                String currentDigest = Base64.getEncoder().encodeToString(digest.digest());
                if (prevDigest != null && prevDigest.equals(currentDigest)) {
                    count = 0; // same response so we terminate
                } else {
                    prevDigest = currentDigest;
                }
                // set up to follow the next link
                nextPath = getNextLink(bundle);
                assertNotNull(nextPath);
            }
        } while (count > 0);
        
        assertTrue(found, "Patient id in history");
    }

    @Test(dependsOnMethods = {"populateResourcesForHistory"})
    public void testSystemHistoryWithTypePatientAndOrderDESC() throws Exception {
        if (!deleteSupported) {
            return;
        }
        WebTarget target = getWebTarget();

        assertNotNull(this.patientId);

        // Get the system history...which will be a bundle of changes. Without any
        // _since filter, we'll probably get back data which has been created by
        // other tests, so the only assertion we can make is we get back at least
        // the number of change records we expect.
        
        // Follow the next links until we 
        String requestPath = "Patient/_history";
        boolean found = false;
        int count;
        Instant prevLastUpdated = null;
        String nextPath = null;
        String prevDigest = null; // to help see if the response changed 
        do {
            final Response historyResponse;
            if (nextPath == null) {
                historyResponse = target.path(requestPath)
                    .queryParam("_count", "50")
                    .queryParam("_sort", "-_lastUpdated")
                    .request().get(Response.class);
            } else {
                historyResponse = target.path(nextPath)
                    .request().get(Response.class);
            }
            assertResponse(historyResponse, Response.Status.OK.getStatusCode());

            Bundle bundle = historyResponse.readEntity(Bundle.class);
            
            // Check the bundle to see if we found the patient
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Bundle.Entry be: bundle.getEntry()) {
                // simple way to see if our patient has appeared
                String fullUrl = be.getFullUrl().getValue();
                if (fullUrl.contains("Patient/" + patientId)) {
                    found = true;
                }

                // Check that resources are ordered by last updated time
                assertNotNull(be.getResponse().getLastModified());
                assertNotNull(be.getResponse().getLastModified().getValue());
                Instant lastUpdated = be.getResponse().getLastModified().getValue().toInstant();
                if (prevLastUpdated != null) {
                    // also allow equals, hence the inversion of the test expression
                    assertTrue(!lastUpdated.isAfter(prevLastUpdated), "record -_lastUpdated ordering");
                }
                prevLastUpdated = lastUpdated;

                // hash together the ids so that we can easily detect duplicate responses
                digest.update(be.getId().getBytes(StandardCharsets.UTF_8));
            }

            // See if there's more work to do
            count = bundle.getEntry().size();
            if (count > 0) {
                String currentDigest = Base64.getEncoder().encodeToString(digest.digest());
                if (prevDigest != null && prevDigest.equals(currentDigest)) {
                    count = 0; // same response so we terminate
                } else {
                    prevDigest = currentDigest;
                }
                // set up to follow the next link
                nextPath = getNextLink(bundle);
                assertNotNull(nextPath);
            }
        } while (count > 0);
        
        assertTrue(found, "Patient id in history");
    }

    /**
     * Update the resource
     * @param resourceToUpdate
     * @throws Exception
     */
    public void updateResource(Resource resourceToUpdate) throws Exception {
        FHIRResponse response = client.update(resourceToUpdate);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
    }

    /**
     * Undelete the resource - which means just do an update. Checks the response.
     * @param resourceToUndelete
     * @throws Exception
     */
    public void undeleteResource(Resource resourceToUndelete) throws Exception {
        if (!deleteSupported) {
            return;
        }

        // FHIR server responds with 201 CREATED
        FHIRResponse response = client.update(resourceToUndelete);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
    }

    /**
     * Delete the resource identified by resourceTypeName/logicalId and check the response
     * @param resourceTypeName
     * @param logicalId
     * @throws Exception
     */
    public void deleteResource(String resourceTypeName, String logicalId) throws Exception {
        if (!deleteSupported) {
            return;
        }

        FHIRResponse response = client.delete(resourceTypeName, logicalId);
        assertNotNull(response);
        assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
        assertNotNull(response.getETag());
    }
}