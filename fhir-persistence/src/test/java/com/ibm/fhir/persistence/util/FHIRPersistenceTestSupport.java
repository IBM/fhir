/*
 * (C) Copyright IBM Corp. 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
 
package com.ibm.fhir.persistence.util;

import java.time.ZoneOffset;

import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.persistence.FHIRPersistence;
import com.ibm.fhir.persistence.SingleResourceResult;
import com.ibm.fhir.persistence.context.FHIRPersistenceContext;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;

/**
 * Methods supporting persistence unit tests
 */
public class FHIRPersistenceTestSupport {
    /**
     * Helper function to replace the previously deprecated persistence layer method which has
     * now been removed. Injects the meta elements into the resource before calling the
     * persistence create method.
     * 
     * @param <T>
     * @param persistence
     * @param context
     * @param resource
     * @return
     * @throws FHIRPersistenceException
     */
    public static <T extends Resource> SingleResourceResult<T> create(FHIRPersistence persistence, FHIRPersistenceContext context, T resource) throws FHIRPersistenceException  {

        // Generate a new logical resource id
        final String logicalId = persistence.generateResourceId();

        // Set the resource id and meta fields.
        final int newVersionNumber = 1;
        final Instant lastUpdated = Instant.now(ZoneOffset.UTC);
        T updatedResource = FHIRPersistenceUtil.copyAndSetResourceMetaFields(resource, logicalId, newVersionNumber, lastUpdated);
        return persistence.create(context, updatedResource);
    }

    /**
     * Helper function to replace the previously deprecated persistence layer method which has
     * now been removed. Injects the meta elements into the resource before calling the
     * persistence update method.
     * 
     * @param <T>
     * @param persistence
     * @param context
     * @param logicalId
     * @param resource
     * @return
     * @throws FHIRPersistenceException
     */
    public static <T extends Resource> SingleResourceResult<T> update(FHIRPersistence persistence, FHIRPersistenceContext context, String logicalId, T resource)
            throws FHIRPersistenceException {

        final com.ibm.fhir.model.type.Instant lastUpdated = com.ibm.fhir.model.type.Instant.now(ZoneOffset.UTC);
        final int newVersionId = resource.getMeta() == null || resource.getMeta().getVersionId() == null ? 1 : Integer.parseInt(resource.getMeta().getVersionId().getValue()) + 1;
        resource = FHIRPersistenceUtil.copyAndSetResourceMetaFields(resource, logicalId, newVersionId, lastUpdated);
        return persistence.update(context, resource);
    }

    /**
     * Helper funciton to replace the previously deprecated persistence layer method which has
     * now been removed. Injects/replaces the meta elements in the resource before calling the
     * persistence delete method.
     * 
     * @param <T>
     * @param persistence
     * @param context
     * @param resource
     * @throws FHIRPersistenceException
     */
    public static <T extends Resource> T delete(FHIRPersistence persistence, FHIRPersistenceContext context, T resource) throws FHIRPersistenceException {
        // cannot delete a resource which hasn't been created - we expect the id to be present
        // and the meta.versionId current. If the version in the resource parameter isn't the latest, the
        // persistence layer will fail the delete thinking there's a concurrent update issue.
        if (resource.getId() == null) {
            throw new FHIRPersistenceException("test error - resource must have a valid id");
        }
        
        if (resource.getMeta() == null || resource.getMeta().getVersionId() == null) {
            // this is most likely an issue with the test implementation
            throw new FHIRPersistenceException("test error - resource must have a valid meta.versionId");
        }
        
        final com.ibm.fhir.model.type.Instant lastUpdated = com.ibm.fhir.model.type.Instant.now(ZoneOffset.UTC);
        final int newVersionId = Integer.parseInt(resource.getMeta().getVersionId().getValue()) + 1;
        resource = FHIRPersistenceUtil.copyAndSetResourceMetaFields(resource, resource.getId(), newVersionId, lastUpdated);
        persistence.delete(context, resource);

        // return the modified resource which may be used for subsequent tests
        return resource;
    }

    /**
     * Convenience method for unit tests to make sure a synthesized resource has valid
     * id, meta.versionId and meta.lastUpdated values
     * @param <T>
     * @param persistence the FHIRPersistence implementation
     * @param resource the resource to be copied and modified
     * @param id the resource logical id. If null, the persistence layer will be asked to generate one
     * @param version the version of the resource
     * @return
     */
    public static <T extends Resource> T setIdAndMeta(FHIRPersistence persistence, T resource, String id, int version) {
        if (id == null) {
            id = persistence.generateResourceId();
        }
        // Set the resource id and meta fields.
        final Instant lastUpdated = Instant.now(ZoneOffset.UTC);
        return FHIRPersistenceUtil.copyAndSetResourceMetaFields(resource, id, version, lastUpdated);
    }
}