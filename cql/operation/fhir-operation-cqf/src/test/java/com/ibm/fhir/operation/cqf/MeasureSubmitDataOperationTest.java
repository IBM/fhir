/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.operation.cqf;

import static com.ibm.fhir.cql.helpers.ModelHelper.coding;
import static com.ibm.fhir.cql.helpers.ModelHelper.fhirstring;
import static com.ibm.fhir.cql.helpers.ModelHelper.reference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.mockito.MockedStatic;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Encounter;
import com.ibm.fhir.model.resource.MeasureReport;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Parameters.Parameter;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.HumanName;
import com.ibm.fhir.model.type.Id;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.UnsignedInt;
import com.ibm.fhir.model.type.code.BundleType;
import com.ibm.fhir.model.type.code.EncounterStatus;
import com.ibm.fhir.registry.FHIRRegistry;
import com.ibm.fhir.search.util.SearchHelper;
import com.ibm.fhir.server.spi.operation.FHIROperationContext;
import com.ibm.fhir.server.spi.operation.FHIRResourceHelpers;

public class MeasureSubmitDataOperationTest {
    MeasureSubmitDataOperation op;
    SearchHelper searchHelper;

    @BeforeClass
    public void initializeSearchUtil() {
        searchHelper = new SearchHelper();
    }

    @BeforeMethod
    public void setup() {
        op = new MeasureSubmitDataOperation();
    }

    @Test
    public void testInstanceExecutionWithResources() throws Exception {

        MeasureReport.Builder builder = MeasureReport.builder()
                .id("measure-report-1");
        builder.setValidating(false);

        Patient p = Patient.builder()
                .id(UUID.randomUUID().toString())
                .name(HumanName.builder().family(fhirstring("Doe")).given(fhirstring("John")).build())
                .build();

        Encounter e = Encounter.builder()
                .id(UUID.randomUUID().toString())
                .meta(Meta.builder().versionId(Id.of("1")).lastUpdated(Instant.now()).build())
                .status(EncounterStatus.FINISHED)
                .subject( reference( p ) )
                .clazz( coding("wellness") )
                .build();

        Encounter e2 = Encounter.builder()
                .id(e.getId())
                .meta(Meta.builder().versionId(Id.of("2")).lastUpdated(Instant.now()).build())
                .status(EncounterStatus.FINISHED)
                .subject( reference( p ) )
                .clazz( coding("wellness") )
                .build();

        MeasureReport report = builder.build();
        List<Resource> resources = Arrays.asList(p,e,e2);

        runTest(report, resources);
    }

    @Test
    public void testInstanceExecutionWithoutResources() throws Exception {

        MeasureReport.Builder builder = MeasureReport.builder()
                .id("measure-report-1");
        builder.setValidating(false);

        MeasureReport report = builder.build();
        List<Resource> resources = Collections.emptyList();

        runTest(report, resources);
    }

    protected Parameters runTest(MeasureReport report, List<Resource> resources) throws Exception, FHIROperationException {
        Parameters inParams = createInParameters(report, resources);
        Bundle responseBundle = Bundle.builder()
                .type(BundleType.COLLECTION)
                .total(UnsignedInt.of(0))
                .build();

        try (MockedStatic<FHIRRegistry> staticRegistry = mockStatic(FHIRRegistry.class)) {
            FHIRRegistry mockRegistry = spy(FHIRRegistry.class);
            staticRegistry.when(FHIRRegistry::getInstance).thenReturn(mockRegistry);

            FHIRResourceHelpers resourceHelper = mock(FHIRResourceHelpers.class);
            when( resourceHelper.doBundle(any(Bundle.class), anyBoolean())).thenReturn(responseBundle);

            Parameters outParams = op.doInvoke(FHIROperationContext.createInstanceOperationContext("submit-data"),
                    null, null, null, inParams, resourceHelper, searchHelper);
            assertNotNull(outParams);
            return outParams;
        }
    }

    public Parameters createInParameters(MeasureReport report, List<Resource> resources) {
        Parameters.Builder inParams = Parameters.builder()
                .parameter(Parameter.builder()
                    .name(fhirstring(MeasureSubmitDataOperation.PARAM_IN_MEASURE_REPORT))
                    .resource(report)
                    .build());
        if( resources != null ) {
            resources.forEach( r -> {
                inParams.parameter(Parameter.builder()
                        .name(fhirstring(MeasureSubmitDataOperation.PARAM_IN_RESOURCE))
                        .resource(r)
                        .build());
            });
        }
        return inParams.build();
    }
}
