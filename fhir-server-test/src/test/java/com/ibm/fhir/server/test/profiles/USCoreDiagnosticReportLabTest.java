/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test.profiles;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.client.FHIRParameters;
import com.ibm.fhir.client.FHIRResponse;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.DiagnosticReport;
import com.ibm.fhir.model.test.TestUtil;

/**
 * Tests the US Core 3.1.1 Profile with DiagnosticReport Lab.
 * https://www.hl7.org/fhir/us/core/StructureDefinition-us-core-diagnosticreport-lab.html
 */
public class USCoreDiagnosticReportLabTest extends ProfilesTestBase {

    private static final String CLASSNAME = USCoreDiagnosticReportLabTest.class.getName();
    private static final Logger logger = Logger.getLogger(CLASSNAME);

    public Boolean skip = Boolean.TRUE;
    public Boolean DEBUG = Boolean.FALSE;

    private String diagnosticReportId1 = null;
    private String diagnosticReportId2 = null;
    private String diagnosticReportId3 = null;

    @Override
    public List<String> getRequiredProfiles() {
        return Arrays.asList(
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab|3.1.1");
    }

    @Override
    public void setCheck(Boolean check) {
        this.skip = check;
        if (skip) {
            logger.info("Skipping Tests for 'fhir-ig-us-core - DiagnosticReport', the profiles don't exist");
        }
    }

    @BeforeClass
    public void loadResources() throws Exception {
        if (!skip) {
            loadDiagnosticReport1();
            loadDiagnosticReport2();
            loadDiagnosticReport3();
        }
    }

    public void loadDiagnosticReport1() throws Exception {
        String resource = "json/profiles/fhir-ig-us-core/DiagnosticReport-urinalysis.json";

        DiagnosticReport diagnosticReport = TestUtil.readExampleResource(resource);
        diagnosticReportId1 = createResourceAndReturnTheLogicalId("DiagnosticReport", diagnosticReport);
    }

    public void loadDiagnosticReport2() throws Exception {
        String resource = "json/profiles/fhir-ig-us-core/DiagnosticReport-metabolic-panel.json";
        DiagnosticReport diagnosticReport = TestUtil.readExampleResource(resource);
        diagnosticReportId2 = createResourceAndReturnTheLogicalId("DiagnosticReport", diagnosticReport);
    }

    public void loadDiagnosticReport3() throws Exception {
        String resource = "json/profiles/fhir-ig-us-core/DiagnosticReport-cbc.json";

        DiagnosticReport diagnosticReport = TestUtil.readExampleResource(resource);
        diagnosticReportId3 = createResourceAndReturnTheLogicalId("DiagnosticReport", diagnosticReport);
    }

    @Test
    public void testSearchForPatient() throws Exception {
        // SHALL support searching for all diagnostic reports for a patient using the patient search parameter:
        // GET [base]/DiagnosticReport?patient=[reference]
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertContainsIds(bundle, diagnosticReportId2);
            assertContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndCategory() throws Exception {
        // SHALL support searching using the combination of the patient and category search parameters:
        // GET [base]/DiagnosticReport?patient=[reference]&type={[system]}|[code]
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://terminology.hl7.org/CodeSystem/v2-0074|LAB");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertContainsIds(bundle, diagnosticReportId2);
            assertContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndCode() throws Exception {
        // SHALL support searching using the combination of the patient and code search parameters:
        // including optional support for composite OR search on code (e.g.code={system|}[code],{system|}[code],...)
        // GET [base]/DiagnosticReport?patient=[reference]&code={system|}[code]{,{system|}[code],...}
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("code", "http://loinc.org|24356-8");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertDoesNotContainsIds(bundle, diagnosticReportId2);
            assertDoesNotContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndCodeWithoutSystem() throws Exception {
        // SHALL support searching using the combination of the patient and code search parameters:
        // including optional support for composite OR search on code (e.g.code={system|}[code],{system|}[code],...)
        // GET [base]/DiagnosticReport?patient=[reference]&code={system|}[code]{,{system|}[code],...}
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("code", "http://loinc.org|24356-8");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertDoesNotContainsIds(bundle, diagnosticReportId2);
            assertDoesNotContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndMultipleCodes() throws Exception {
        // SHALL support searching using the combination of the patient and code search parameters:
        // including optional support for composite OR search on code (e.g.code={system|}[code],{system|}[code],...)
        // GET [base]/DiagnosticReport?patient=[reference]&code={system|}[code]{,{system|}[code],...}
        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("code", "http://loinc.org|24356-8,http://loinc.org|24323-8");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertContainsIds(bundle, diagnosticReportId2);
            assertDoesNotContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndCategoryAndDate() throws Exception {
        // SHALL support searching using the combination of the patient and category and date search parameters:
        // including support for these date comparators: gt,lt,ge,le
        // including optional support for composite AND search on date (e.g.date=[date]&date=[date]]&...)
        // GET
        // [base]/DiagnosticReport?patient=[reference]&category=http://terminology.hl7.org/CodeSystem/v2-0074|LAB&date={gt|lt|ge|le}[date]{&date={gt|lt|ge|le}[date]&...}

        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://terminology.hl7.org/CodeSystem/v2-0074|LAB");
            parameters.searchParam("date", "2005-07-05");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertDoesNotContainsIds(bundle, diagnosticReportId2);
            assertContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndCategoryAndDateLeGe() throws Exception {
        // SHALL support searching using the combination of the patient and category and date search parameters:
        // including support for these date comparators: gt,lt,ge,le
        // including optional support for composite AND search on date (e.g.date=[date]&date=[date]]&...)
        // GET
        // [base]/DiagnosticReport?patient=[reference]&category=http://terminology.hl7.org/CodeSystem/v2-0074|LAB&date={gt|lt|ge|le}[date]{&date={gt|lt|ge|le}[date]&...}

        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://terminology.hl7.org/CodeSystem/v2-0074|LAB");
            parameters.searchParam("date", "ge2005-01");
            parameters.searchParam("date", "lt2005-07-05");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertDoesNotContainsIds(bundle, diagnosticReportId1);
            assertContainsIds(bundle, diagnosticReportId2);
            assertDoesNotContainsIds(bundle, diagnosticReportId3);
        }
    }

    @Test
    public void testSearchForPatientAndCategoryAndDateAndStatus() throws Exception {
        // SHOULD support searching using the combination of the patient and status search parameters:
        // GET [base]/DiagnosticReport?patient=[reference]&status={system|}[code]{,{system|}[code],...}
        // Note: implied system, and we only pass in the token.

        if (!skip) {
            FHIRParameters parameters = new FHIRParameters();
            parameters.searchParam("patient", "Patient/example");
            parameters.searchParam("category", "http://terminology.hl7.org/CodeSystem/v2-0074|LAB");
            parameters.searchParam("date", "ge2005-01");
            parameters.searchParam("status", "final");
            FHIRResponse response = client.search(DiagnosticReport.class.getSimpleName(), parameters);
            assertSearchResponse(response, Response.Status.OK.getStatusCode());
            Bundle bundle = response.getResource(Bundle.class);
            assertNotNull(bundle);
            assertTrue(bundle.getEntry().size() >= 1);
            assertContainsIds(bundle, diagnosticReportId1);
            assertContainsIds(bundle, diagnosticReportId2);
            assertContainsIds(bundle, diagnosticReportId3);
        }
    }
}