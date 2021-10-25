/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.operation.bulkdata.util;

import static com.ibm.fhir.model.type.String.string;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.fhir.config.FHIRConfiguration;
import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.resource.Group;
import com.ibm.fhir.model.resource.Medication;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Parameters.Parameter;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Element;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.model.type.PositiveInt;
import com.ibm.fhir.operation.bulkdata.OperationConstants;
import com.ibm.fhir.operation.bulkdata.OperationConstants.ExportType;
import com.ibm.fhir.operation.bulkdata.model.PollingLocationResponse;
import com.ibm.fhir.server.spi.operation.FHIROperationContext;
import com.ibm.fhir.server.spi.operation.FHIROperationContext.Type;

/**
 * Test Export util
 */
public class BulkDataExportUtilTest {

    @BeforeClass
    public void setup() {
        FHIRConfiguration.setConfigHome("target/test-classes");
    }

    @BeforeMethod
    public void startMethod(Method method) throws FHIRException {

        // Configure the request context for our search tests
        FHIRRequestContext context = FHIRRequestContext.get();
        if (context == null) {
            context = new FHIRRequestContext();
        }
        FHIRRequestContext.set(context);
        context.setTenantId("default");
    }

    @AfterMethod
    public void clearThreadLocal() {
        FHIRRequestContext.remove();
    }

    @Test
    public void testCheckExportTypeInstance() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        FHIROperationContext.Type type = Type.INSTANCE;

        Class<? extends Resource> resourceType = Patient.class;
        OperationConstants.ExportType exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.INVALID);

        resourceType = Medication.class;
        exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.INVALID);

        resourceType = Group.class;
        exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.GROUP);
    }

    @Test
    public void testCheckExportTypeResourceType() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        FHIROperationContext.Type type = Type.RESOURCE_TYPE;

        Class<? extends Resource> resourceType = Patient.class;
        OperationConstants.ExportType exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.PATIENT);

        resourceType = Medication.class;
        exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.INVALID);

        resourceType = Group.class;
        exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.INVALID);
    }

    @Test
    public void testCheckExportTypeSystem() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        FHIROperationContext.Type type = Type.SYSTEM;

        Class<? extends Resource> resourceType = Patient.class;
        OperationConstants.ExportType exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.SYSTEM);

        resourceType = Medication.class;
        exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.SYSTEM);

        resourceType = Group.class;
        exportType = util.checkExportType(type, resourceType);
        assertEquals(exportType, ExportType.SYSTEM);
    }

    @Test
    public void testCheckAndConvertToMediaType() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // QueryParameters Map
        Map<String, List<String>> _mvm = new HashMap<>();
        _mvm.put("_outputFormat", Arrays.asList("application/fhir+ndjson"));

        // Default Format
        MediaType type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+ndjson");

        // No Format
        _mvm.clear();
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+ndjson");

        // Empty
        _mvm.clear();
        _mvm.put("_outputFormat", Collections.emptyList());
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+ndjson");

        // Multiple values
        _mvm.clear();
        _mvm.put("_outputFormat", Arrays.asList("application/fhir+parquet", "ndjson"));
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+parquet");

        // Parquet Format
        _mvm.clear();
        _mvm.put("_outputFormat", Arrays.asList("application/fhir+parquet"));
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+parquet");

        // Not a valid format
        try {
            _mvm.clear();
            _mvm.put("_outputFormat", Arrays.asList("application/json"));
            type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
            fail();
        } catch (FHIROperationException e) {
            assertNotNull(e);
        }

        // Liberty Encoded + to ' ' and invalid
        try {
            _mvm.clear();
            _mvm.put("_outputFormat", Arrays.asList("application/nd fred"));
            type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
            fail();
        } catch (FHIROperationException e) {
            assertNotNull(e);
        }

        // Liberty Encoded + to ' '
        _mvm.clear();
        _mvm.put("_outputFormat", Arrays.asList("application/fhir ndjson"));
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+ndjson");

        // Test the format application/ndjson
        _mvm.put("_outputFormat", Arrays.asList("application/ndjson"));
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+ndjson");

        // Test the format ndjson
        _mvm.put("_outputFormat", Arrays.asList("ndjson"));
        type = util.checkAndConvertToMediaType(generateParametersFromMap(_mvm));
        assertNotNull(type);
        assertEquals(type.getType(), "application");
        assertEquals(type.getSubtype(), "fhir+ndjson");
    }

    @Test
    public void testCheckAndExtractSinceNull() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // No Parameters
        assertNull(util.checkAndExtractSince(null));

        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_since")).value((Element)null).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        Instant inst = util.checkAndExtractSince(ps);
        assertNull(inst);
    }

    @Test
    public void testCheckAndExtractSinceEmpty() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        Instant inst = util.checkAndExtractSince(ps);
        assertNull(inst);
    }

    @Test
    public void testCheckAndExtractSinceWithString() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_since")).value(string("2018-07-01T00:00:00Z")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        Instant inst = util.checkAndExtractSince(ps);
        assertNotNull(inst);
    }

    @Test
    public void testCheckAndExtractSinceWithInvalidString() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("invalid")).value(string("2018-07-01T00:00:00Z")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        Instant inst = util.checkAndExtractSince(ps);
        assertNull(inst);
    }

    @Test
    public void testCheckAndExtractSinceWithInstant() {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_since")).value(Instant.of("2018-07-01T00:00:00Z")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        BulkDataExportUtil util = new BulkDataExportUtil();
        Instant inst = util.checkAndExtractSince(ps);
        assertNotNull(inst);
    }

    @Test
    public void testCheckAndExtractSinceWithInvalidType() {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_since")).value(PositiveInt.of(1)).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        Instant inst = util.checkAndExtractSince(ps);
        assertNull(inst);
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateTypesEmpty() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        // Value requires a string greater than 1
        parameters.add(Parameter.builder().name(string("_type")).value(string("1")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateTypes(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateTypesNull() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_type")).value((Element)null).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateTypes(ps);
        fail();
    }

    @Test
    public void testCheckAndValidateTypesPatientWithoutComma() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_type")).value(string("Patient")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> types = util.checkAndValidateTypes(ps);
        assertNotNull(types);
    }

    @Test
    public void testCheckAndValidateTypesPatientWithComma() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_type")).value(string("Patient,")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        List<String> types = util.checkAndValidateTypes(ps);
        assertNotNull(types);
    }

    @Test
    public void testCheckAndValidateTypesPatientMedicationWithComma() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_type")).value(string("Patient,Medication")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> types = util.checkAndValidateTypes(ps);
        assertNotNull(types);
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateTypesPatientMedicationWithExtraComma() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_type")).value(string("Patient,,Medication")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateTypes(ps);
        fail();
    }

    @Test
    public void testCheckAndValidateTypesWithExtraComma() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_type")).value(string(",,")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> result = util.checkAndValidateTypes(ps);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckAndValidateTypesNoParameters() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("french")).value(string("Patient,,Medication")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        List<String> result = util.checkAndValidateTypes(ps);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckAndValidateTypesEmptyParameters() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> result = util.checkAndValidateTypes(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckAndValidateTypeFilters() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> result = util.checkAndValidateTypeFilters(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckAndValidateTypeFiltersNoParameters() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("french")).value(string("Patient,,Medication")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        List<String> result = util.checkAndValidateTypeFilters(ps);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckAndValidateTypeFiltersParameters() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_typeFilter")).value(string("type1")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> result = util.checkAndValidateTypeFilters(ps);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testCheckAndValidateTypeFiltersParametersTypes() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_typeFilter")).value(string("type1,type2")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();
        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> result = util.checkAndValidateTypeFilters(ps);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateTypeFiltersParametersTypesComma() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_typeFilter")).value(string(",,2")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        BulkDataExportUtil util = new BulkDataExportUtil();
        List<String> result = util.checkAndValidateTypeFilters(ps);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateTypeFiltersParametersTypesInvalidType() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_typeFilter")).value(PositiveInt.of(2)).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();
        BulkDataExportUtil util = new BulkDataExportUtil();
        util.checkAndValidateTypeFilters(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateTypeFiltersParametersTypesNull() throws FHIROperationException {
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("_typeFilter")).value((Element)null).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();
        BulkDataExportUtil util = new BulkDataExportUtil();
        util.checkAndValidateTypeFilters(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateJobNull() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("job")).value((Element)null).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateJob(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateJobInvalidType() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("job")).value(PositiveInt.of(2)).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateJob(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateJobNoJob() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("fred")).value(PositiveInt.of(2)).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateJob(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateJobNullParameters() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        util.checkAndValidateJob(null);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateJobInvalidQuestion() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("job")).value(string("1?")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateJob(ps);
        fail();
    }

    @Test(expectedExceptions = { com.ibm.fhir.exception.FHIROperationException.class })
    public void testCheckAndValidateJobInvalidSlash() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("job")).value(string("1/")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        util.checkAndValidateJob(ps);
        fail();
    }

    @Test
    public void testCheckAndValidateJobValid() throws FHIROperationException {
        BulkDataExportUtil util = new BulkDataExportUtil();
        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name(string("job")).value(string("1234q346")).build());
        Parameters.Builder builder = Parameters.builder();
        builder.id(UUID.randomUUID().toString());
        builder.parameter(parameters);
        Parameters ps = builder.build();

        String result = util.checkAndValidateJob(ps);
        assertNotNull(result);
        assertEquals(result, "1234q346");
    }

    @Test
    public void testGetOutputParametersWithJson() throws Exception {
        BulkDataExportUtil util = new BulkDataExportUtil();
        PollingLocationResponse pollingLocationResponse = new PollingLocationResponse();
        Parameters result = util.getOutputParametersWithJson(pollingLocationResponse);
        assertNotNull(result);
        assertFalse(result.getParameter().isEmpty());
        assertFalse(result.getParameter().get(0).getValue().as(com.ibm.fhir.model.type.String.class).getValue().isEmpty());
    }

    private Parameters generateParametersFromMap(Map<String, List<String>> _mvm) {
        Parameters.Builder builder = Parameters.builder().id("BulkDataExportUtilTest");
        for (Map.Entry<String, List<String>> entry : _mvm.entrySet()) {
            for (String value : entry.getValue()) {
                builder.parameter(Parameter.builder().name(string(entry.getKey())).value(string(value)).build());
            }
        }
        return builder.build();
    }
}