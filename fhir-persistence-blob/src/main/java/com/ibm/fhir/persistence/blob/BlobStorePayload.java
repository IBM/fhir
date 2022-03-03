/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
 
package com.ibm.fhir.persistence.blob;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.specialized.AppendBlobAsyncClient;
import com.ibm.fhir.model.resource.OperationOutcome.Issue;
import com.ibm.fhir.model.type.code.IssueSeverity;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.payload.PayloadPersistenceResult;
import com.ibm.fhir.persistence.util.InputOutputByteStream;

import reactor.core.publisher.Flux;

/**
 * DAO command to store the configured payload in the Azure blob
 */
public class BlobStorePayload {
    private static final Logger logger = Logger.getLogger(BlobStorePayload.class.getName());
    
    // The maximum size of a single append blob block call
    private static final int MAX_APPEND_BLOB_BLOCK_SIZE = AppendBlobAsyncClient.MAX_APPEND_BLOCK_BYTES;

    // The normalized resource type id from the RDBMS resource_types table
    final int resourceTypeId;

    // The resource id (logical identifier)
    final String logicalId;

    // The resource version as an integer
    final int version;

    // The key to make this particular resource payload record unique
    final String resourcePayloadKey;

    // The resource payload bytes to be uploaded
    final InputOutputByteStream ioStream;

    /**
     * Public constructor
     * @param resourceTypeId
     * @param logicalId
     * @param version
     * @param resourcePayloadKey
     * @param ioStream
     */
    public BlobStorePayload(int resourceTypeId, String logicalId, int version, String resourcePayloadKey, InputOutputByteStream ioStream) {
        this.resourceTypeId = resourceTypeId;
        this.logicalId = logicalId;
        this.version = version;
        this.resourcePayloadKey = resourcePayloadKey;
        this.ioStream = ioStream;
    }

    /**
     * Execute this command against the given client
     * @param client
     * @throws FHIRPersistenceException
     */
    public CompletableFuture<PayloadPersistenceResult> run(BlobManagedContainer client) throws FHIRPersistenceException {
        
        if (ioStream.size() > MAX_APPEND_BLOB_BLOCK_SIZE) {
            FHIRPersistenceException x = new FHIRPersistenceException("Resource payload size cannot exceed " + MAX_APPEND_BLOB_BLOCK_SIZE + " bytes");
            x.withIssue(Issue.builder().code(IssueType.TOO_LONG).severity(IssueSeverity.ERROR).diagnostics("Resource too large for payload offload").build());
            throw x;
        }
        final String blobPath = BlobPayloadSupport.getPayloadPath(resourceTypeId, logicalId, version, resourcePayloadKey);
        logger.fine(() -> "Payload storage path: " + blobPath);
        BlobAsyncClient bc = client.getClient().getBlobAsyncClient(blobPath);

        // Reactor pipeline to first call create then perform the upload. The create(true)
        // calls returns a Mono<AppendBlobItem>. The flatMap is basically installing a
        // callback (ahem, reaction) saying that when create call completes, then
        // perform the upload (appendBlockWithResponse). The map(...) call then says when we get
        // a response from the appendBlockWithResponse call, translate the response status into
        // a PayloadPersistenceResult. The whole pipeline is then initiated by calling toFuture()
        // which means we don't have to wait here for the response but can instead collect it
        // later when we actually need it (usually just before the RDBMS transaction commits)
        return bc.getAppendBlobAsyncClient()
                .create(true)
                .flatMap(item -> {
                    return bc.getAppendBlobAsyncClient().appendBlockWithResponse(Flux.just(ioStream.wrap()), ioStream.size(), null, null);
                })
                .map(response -> new PayloadPersistenceResult(response.getStatusCode() == 201 ? PayloadPersistenceResult.Status.OK : PayloadPersistenceResult.Status.FAILED))
                .toFuture();
    }
}