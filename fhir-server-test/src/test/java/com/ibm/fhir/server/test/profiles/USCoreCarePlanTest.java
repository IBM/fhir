/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test.profiles;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.client.FHIRParameters;
import com.ibm.fhir.client.FHIRResponse;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.CarePlan;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.DateTime;
import com.ibm.fhir.model.type.code.CarePlanStatus;

/**
 * Tests the US Core 3.1.1 Profile with CarePlan.
 *
 *The specification says the following parameters should work:
 * <code>status=http://hl7.org/fhir/ValueSet/request-status|active</code>
 * It's a default binding and should work without a bound system. We only extract active, and not the default system.
 */
public class USCoreCarePlanTest extends ProfilesTestBase {
    private static final String CLASSNAME = USCoreCarePlanTest.class.getName();
    private static final Logger logger = Logger.getLogger(CLASSNAME);

    public Boolean skip = Boolean.TRUE;

    private String carePlanId = null;

    @Override
    public List<String> getRequiredProfiles() {
        return Arrays.asList(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-careplan|3.1.1");
    }

    @Override
    public void setCheck(Boolean check) {
        this.skip = check;
        if (skip) {
            logger.info("Skipping Tests for 'fhir-ig-us-core - CarePlan', the profiles don't exist");
        }
    }

    @BeforeClass
    public void loadResources() throws Exception {
        if (skip) {
            return;
        }
        loadCarePlan();
    }

    public void loadCarePlan() throws Exception {
        String resource = "json/profiles/fhir-ig-us-core/CarePlan-colonoscopy.json";

        CarePlan carePlan = TestUtil.readExampleResource(resource);
        com.ibm.fhir.model.type.Period period = com.ibm.fhir.model.type.Period.builder()
                .start(DateTime.of("2019-01-01"))
                .end(DateTime.of("2020-01-01"))
                .build();

        // Note: The test uses ACTIVE as a CodeableConcept rather than a plain string.
        carePlan = carePlan.toBuilder().period(period).status(CarePlanStatus.ACTIVE).build();
        carePlanId = createResourceAndReturnTheLogicalId("CarePlan", carePlan);
    }

    @Test
    public void testSearchForPatientAndCategory() throws Exception {
        // SHALL support searching using the combination of the patient and category search parameters
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            return;
        }
        FHIRParameters parameters = new FHIRParameters();
        parameters.searchParam("patient", "Patient/example");
        parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
        FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
        assertSearchResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.getResource(Bundle.class);
        assertBaseBundleNotEmpty(bundle);
        assertContainsIds(bundle, carePlanId);
    }

    @Test
    public void testSearchForPatientAndCategoryAndDate() throws Exception {
        // SHOULD support searching using the combination of the patient and category and date search parameters
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            return;
        }
        FHIRParameters parameters = new FHIRParameters();
        parameters.searchParam("patient", "Patient/example");
        parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
        parameters.searchParam("date", "gt2018");
        parameters.searchParam("date", "le2021");
        FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
        assertSearchResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.getResource(Bundle.class);
        assertBaseBundleNotEmpty(bundle);
        assertContainsIds(bundle, carePlanId);
    }

    @Test
    public void testSearchForPatientAndCategoryAndDateGe() throws Exception {
        // SHOULD support searching using the combination of the patient and category and date search parameters
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
            parameters.searchParam("date", "ge2019");
            parameters.searchParam("date", "le2021");
            FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertBaseBundleNotEmpty(bundle);
            assertContainsIds(bundle, carePlanId);
        }
    }

    @Test
    public void testSearchForPatientAndCategoryAndStatus() throws Exception {
        // SHOULD support searching using the combination of the patient and category and status search parameters:
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
            parameters.searchParam("status", "active");
            FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertBaseBundleNotEmpty(bundle);
            assertContainsIds(bundle, carePlanId);
        }
    }

    // See the note at the top of this class as to why this test is disabled.
    @Test (enabled = false)
    public void testSearchForPatientAndCategoryAndStatusSystem() throws Exception {
        // SHOULD support searching using the combination of the patient and category and status search parameters:
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
            parameters.searchParam("status", "http://hl7.org/fhir/ValueSet/request-status|");
            FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertBaseBundleNotEmpty(bundle);
            assertContainsIds(bundle, carePlanId);
        }
    }

    @Test
    public void testSearchForPatientAndCategoryAndStatusSystemStatus() throws Exception {
        // SHOULD support searching using the combination of the patient and category and status search parameters:
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
            parameters.searchParam("status", "active");
            FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertBaseBundleNotEmpty(bundle);
            assertContainsIds(bundle, carePlanId);
        }
    }

    @Test
    public void testSearchForPatientAndCategoryAndStatusSystemStatusAndDate() throws Exception {
        // SHOULD support searching using the combination of the patient and category and status search parameters:
        // http://build.fhir.org/ig/HL7/US-Core-R4/StructureDefinition-us-core-careplan.html
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://hl7.org/fhir/us/core/CodeSystem/careplan-category|assess-plan");
            parameters.searchParam("status", "active");
            parameters.searchParam("date", "ge2019");
            parameters.searchParam("date", "le2021");
            FHIRResponse response = client.search(CarePlan.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertBaseBundleNotEmpty(bundle);
            assertContainsIds(bundle, carePlanId);
        }
    }
}