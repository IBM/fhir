/*
 * (C) Copyright IBM Corp. 2017, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.smart.test;

import static com.ibm.fhir.model.type.String.string;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import com.ibm.fhir.model.resource.Encounter;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.Reference;
import com.ibm.fhir.persistence.FHIRPersistence;
import com.ibm.fhir.persistence.FHIRPersistenceTransaction;
import com.ibm.fhir.persistence.MultiResourceResult;
import com.ibm.fhir.persistence.ResourceChangeLogRecord;
import com.ibm.fhir.persistence.ResourcePayload;
import com.ibm.fhir.persistence.SingleResourceResult;
import com.ibm.fhir.persistence.context.FHIRPersistenceContext;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.exception.FHIRPersistenceResourceDeletedException;
import com.ibm.fhir.persistence.exception.FHIRPersistenceResourceNotFoundException;

/**
 * Mock implementation of FHIRPersistence for use during testing.
 *
 */
public class MockPersistenceImpl implements FHIRPersistence {
    static final String ENCOUNTER_ID_GOOD = "good";
    static final String ENCOUNTER_ID_BAD = "bad";

    Patient patient = null;
    Observation observation = null;
    Encounter encounter_in_patient_compartment = null;
    Encounter encounter_not_in_patient_compartment = null;

    public MockPersistenceImpl(Patient patient, Observation observation) throws Exception {
        this.patient = patient;
        this.observation = observation;


        encounter_not_in_patient_compartment = TestUtil.getMinimalResource(Encounter.class);
        encounter_in_patient_compartment = encounter_not_in_patient_compartment.toBuilder()
                .id(ENCOUNTER_ID_GOOD)
                .subject(Reference.builder()
                    .reference(string("Patient/" + patient.getId()))
                    .build())
                .build();
    }

    @Override
    public <T extends Resource> SingleResourceResult<T> create(FHIRPersistenceContext context, T resource) throws FHIRPersistenceException {
        return null;
    }

    @Override
    public <T extends Resource> SingleResourceResult<T> read(FHIRPersistenceContext context, Class<T> resourceType, String logicalId)
        throws FHIRPersistenceException, FHIRPersistenceResourceDeletedException {

        if (resourceType == Patient.class) {
            return new SingleResourceResult.Builder<T>()
                    .success(true)
                    .resource((T)patient)
                    .build();
        }

        if (resourceType == Observation.class) {
            return new SingleResourceResult.Builder<T>()
                    .success(true)
                    .resource((T)observation)
                    .build();
        }

        if (resourceType == Encounter.class) {
            if (ENCOUNTER_ID_GOOD.equals(logicalId)) {
                return new SingleResourceResult.Builder<T>()
                        .success(true)
                        .resource((T)encounter_in_patient_compartment)
                        .build();
            } else if (ENCOUNTER_ID_BAD.equals(logicalId)){
                return new SingleResourceResult.Builder<T>()
                        .success(true)
                        .resource((T)encounter_not_in_patient_compartment)
                        .build();
            } else {
                throw new FHIRPersistenceResourceNotFoundException("Not found");
            }
        }

        return null;
    }

    @Override
    public <T extends Resource> SingleResourceResult<T> vread(FHIRPersistenceContext context, Class<T> resourceType, String logicalId, String versionId)
        throws FHIRPersistenceException, FHIRPersistenceResourceDeletedException {

        if (resourceType == Patient.class) {
            return new SingleResourceResult.Builder<T>()
                    .success(true)
                    .resource((T)patient)
                    .build();
        }

        if (resourceType == Observation.class) {
            return new SingleResourceResult.Builder<T>()
                    .success(true)
                    .resource((T)observation)
                    .build();
        }

        return null;
    }

    @Override
    public <T extends Resource> SingleResourceResult<T> update(FHIRPersistenceContext context, String logicalId, T resource) throws FHIRPersistenceException {
    	return null;
    }

    @Override
    public <T extends Resource> MultiResourceResult<T> history(FHIRPersistenceContext context, Class<T> resourceType, String logicalId) throws FHIRPersistenceException {
        return null;
    }

    @Override
    public MultiResourceResult<Resource> search(FHIRPersistenceContext context, Class<? extends Resource> resourceType) throws FHIRPersistenceException {
        return null;
    }

    @Override
    public boolean isTransactional() {
        return false;
    }

    @Override
    public FHIRPersistenceTransaction getTransaction() {
        return null;
    }

    @Override
    public OperationOutcome getHealth() throws FHIRPersistenceException {
        return null;
    }

    @Override
    public int reindex(FHIRPersistenceContext context, OperationOutcome.Builder oob, Instant tstamp, List<Long> indexIds,
        String resourceLogicalId) throws FHIRPersistenceException {
        return 0;
    }

    @Override
    public String generateResourceId() {
        return null;
    }

    @Override
    public ResourcePayload fetchResourcePayloads(Class<? extends Resource> resourceType, Instant fromLastModified, Instant toLastModified,
            Function<ResourcePayload, Boolean> process) throws FHIRPersistenceException {
        return null;
    }

    @Override
    public List<ResourceChangeLogRecord> changes(int resourceCount, Instant fromLastModified, Long afterResourceId, String resourceTypeName)
        throws FHIRPersistenceException {
        return null;
    }

    @Override
    public List<Long> retrieveIndex(int count, java.time.Instant notModifiedAfter, Long afterIndexId, String resourceTypeName) throws FHIRPersistenceException {
        return null;
    }
}
