/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.cos.payload;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.fhir.config.FHIRConfigHelper;
import com.ibm.fhir.config.FHIRConfiguration;
import com.ibm.fhir.config.PropertyGroup;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.persistence.FHIRPersistenceSupport;
import com.ibm.fhir.persistence.cos.client.COSPayloadClient;
import com.ibm.fhir.persistence.cos.client.CosPropertyGroupAdapter;
import com.ibm.fhir.persistence.cos.impl.COSClientManager;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.payload.FHIRPayloadPersistence;
import com.ibm.fhir.persistence.payload.PayloadPersistenceResponse;
import com.ibm.fhir.persistence.payload.PayloadPersistenceResult;
import com.ibm.fhir.persistence.payload.PayloadPersistenceResult.Status;
import com.ibm.fhir.persistence.payload.PayloadReader;
import com.ibm.fhir.persistence.payload.PayloadReaderImpl;
import com.ibm.fhir.persistence.util.InputOutputByteStream;

/**
 * Provides storage and retrieval of FHIR payload data using IBM
 * cloud object storage (Cos).
 */
public class FHIRPayloadPersistenceCosImpl implements FHIRPayloadPersistence {
    private static final Logger logger = Logger.getLogger(FHIRPayloadPersistenceCosImpl.class.getName());
    
    @Override
    public PayloadPersistenceResponse storePayload(String resourceTypeName, int resourceTypeId, String logicalId, int version, String resourcePayloadKey, Resource resource)
        throws FHIRPersistenceException {
        long start = System.nanoTime();

        COSPayloadClient cpc = COSClientManager.getClientForTenantDatasource();

        Future<PayloadPersistenceResult> result;
        final String objectName = makeObjectName(resourceTypeId, logicalId, version, resourcePayloadKey);
        try {
            // Render the object to a byte-stream but don't compress when storing in Cos
            // (although this could be made a configurable option if we want)
            InputOutputByteStream ioStream = FHIRPersistenceSupport.render(resource, false);
            cpc.write(objectName, ioStream);
            
            result = CompletableFuture.completedFuture(new PayloadPersistenceResult(Status.OK));
        } catch (Exception x) {
            result = CompletableFuture.completedFuture(new PayloadPersistenceResult(Status.FAILED));
        } finally {
            if (logger.isLoggable(Level.FINE)) {
                long elapsed = System.nanoTime() - start;
                logger.fine(String.format("Wrote resource payload to COS: '%s/%s/%d' [took %5.3f s]", resourceTypeName, logicalId, version, elapsed/1e9));
            }
        }
        PayloadPersistenceResponse response = new PayloadPersistenceResponse(resourcePayloadKey, resourceTypeName, resourceTypeId, logicalId, version, result);
        return response;
    }

    @Override
    public <T extends Resource> T readResource(Class<T> resourceType, String rowResourceTypeName, int resourceTypeId, String logicalId, int version, String resourcePayloadKey, List<String> elements) throws FHIRPersistenceException {
        final long start = System.nanoTime();
        COSPayloadClient cpc = COSClientManager.getClientForTenantDatasource();

        final CosPropertyGroupAdapter config = getConfigAdapter();
        final String objectName = makeObjectName(resourceTypeId, logicalId, version, resourcePayloadKey);
        try {
            PayloadReader payloadReader = new PayloadReaderImpl(config.isCompress(), elements);
            return cpc.read(resourceType, objectName, payloadReader);
        } finally {
            if (logger.isLoggable(Level.FINE)) {
                long elapsed = System.nanoTime() - start;
                logger.fine(String.format("Read resource payload from COS: '%s/%s/%d' [took %5.3f s]", resourceType.getSimpleName(), logicalId, version, elapsed/1e9));
            }
        }
    }

    /**
     * Generate the COS object name to use for the given set of parameters
     * @param resourceTypeId
     * @param logicalId
     * @param version
     * @param resourcePayloadKey
     * @return
     */
    private static String makeObjectName(int resourceTypeId, String logicalId, int version, String resourcePayloadKey) {
        StringBuilder objectName = new StringBuilder();
        objectName.append(Integer.toString(resourceTypeId));
        objectName.append("/");
        objectName.append(logicalId);
        objectName.append("/");
        objectName.append(Integer.toString(version));
        objectName.append("/");
        objectName.append(resourcePayloadKey);
        return objectName.toString();
    }

    @Override
    public void deletePayload(String resourceType, int resourceTypeId, String logicalId, Integer version, String resourcePayloadKey) throws FHIRPersistenceException {
        COSPayloadClient cpc = COSClientManager.getClientForTenantDatasource();

        final String objectName = makeObjectName(resourceTypeId, logicalId, version, resourcePayloadKey);
        try {
            cpc.delete(objectName);
        } catch (FHIRPersistenceException x) {
            logger.severe("Failed to delete payload for: '" + resourceTypeId + "/" + logicalId + "/" + version + "'");
            throw x;
        }
    }
    
    /**
     * Get the tenant-specific configuration for COS
     * @return
     */
    private CosPropertyGroupAdapter getConfigAdapter() {
        // get the PropertyGroup for the current tenant/datasource
        final String dsId = "default"; // only one payload datasource for COS
        String dsPropertyName = FHIRConfiguration.PROPERTY_PERSISTENCE_PAYLOAD + "/" + dsId;
        PropertyGroup dsPG = FHIRConfigHelper.getPropertyGroup(dsPropertyName);
        if (dsPG == null) {
            throw new IllegalStateException("Could not locate configuration property group: " + dsPropertyName);
        }
        return new CosPropertyGroupAdapter(dsPG);
    }
}