/*
 * (C) Copyright IBM Corp. 2017, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.operation.spi;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ws.rs.core.MultivaluedMap;

import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.patch.FHIRPatch;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.resource.OperationOutcome.Issue;
import com.ibm.fhir.persistence.FHIRPersistenceTransaction;
import com.ibm.fhir.persistence.ResourceEraseRecord;
import com.ibm.fhir.persistence.SingleResourceResult;
import com.ibm.fhir.persistence.erase.EraseDTO;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.interceptor.FHIRPersistenceEvent;
import com.ibm.fhir.persistence.payload.PayloadKey;
import com.ibm.fhir.search.context.FHIRSearchContext;
import com.ibm.fhir.server.util.FHIRRestHelper.Interaction;

/**
 * This interface describes the set of helper methods from the FHIR REST layer that are used by custom operation
 * implementations.
 */
public interface FHIRResourceHelpers {
    // Constant for indicating the need to validate a resource
    public static final boolean DO_VALIDATION = true;
    // Constant for indicating whether an update can be skipped when the requested update resource matches the existing one
    public static final boolean SKIPPABLE_UPDATE = true;

    /**
     * Validate an interaction for a specified resource type.
     *
     * @param interaction
     *            the interaction to be performed
     * @param resourceType
     *            the resource type against which the interaction is to be performed
     * @throws FHIROperationException
     */
    void validateInteraction(Interaction interaction, String resourceType) throws FHIROperationException;

    /**
     * Performs the heavy lifting associated with a 'create' interaction.
     *
     * @param type
     *            the resource type specified as part of the request URL
     * @param resource
     *            the Resource to be stored.
     * @param ifNoneExist
     *            whether to create the resource if none exists
     * @param doValidation
     *            if true, validate the resource; if false, assume the resource has already been validated
     * @return a FHIRRestOperationResponse object containing the results of the operation
     * @throws Exception
     */
    FHIRRestOperationResponse doCreate(String type, Resource resource, String ifNoneExist, boolean doValidation) throws Exception;
    
    /**
     * Performs the heavy lifting associated with a 'create' interaction. Validates the resource.
     *
     * @param type
     *            the resource type specified as part of the request URL
     * @param resource
     *            the Resource to be stored.
     * @param ifNoneExist
     *            whether to create the resource if none exists
     * @return a FHIRRestOperationResponse object containing the results of the operation
     * @throws Exception
     */
    default FHIRRestOperationResponse doCreate(String type, Resource resource, String ifNoneExist) throws Exception {
        return doCreate(type, resource, ifNoneExist, DO_VALIDATION);
    }

    /**
     * 1st phase of CREATE. Perform the search for conditional create (ifNoneExist) interactions.
     * Any validation (if required) should be performed before this call.
     * @param event
     * @param warnings
     * @param type
     * @param resource
     * @param ifNoneExist
     * @return
     * @throws Exception
     */
    FHIRRestOperationResponse doCreateMeta(FHIRPersistenceEvent event, List<Issue> warnings, String type, Resource resource, 
        String ifNoneExist) throws Exception;

    /**
     * 3rd phase of resource create. Persist the resource using the configured persistence layer. Does not modify
     * the resource.
     * @param event
     * @param warnings
     * @param resource
     * @return
     * @throws Exception
     */
    FHIRRestOperationResponse doCreatePersist(FHIRPersistenceEvent event, List<Issue> warnings, Resource resource) throws Exception;

    /**
     * 1st phase of update interaction. 
     * @param event
     * @param type
     * @param id
     * @param patch
     * @param newResource
     * @param ifMatchValue
     * @param searchQueryString
     * @param skippableUpdate
     * @param doValidation
     * @param warnings
     * @return
     * @throws Exception
     */
    FHIRRestOperationResponse doUpdateMeta(FHIRPersistenceEvent event, String type, String id, FHIRPatch patch, Resource newResource, String ifMatchValue,
        String searchQueryString, boolean skippableUpdate, boolean doValidation, List<Issue> warnings) throws Exception;

    /**
     * Persist the newResource value for patch or update interactions
     * @param event
     * @param type
     * @param id
     * @param isPatch
     * @param newResource
     * @param prevResource
     * @param warnings
     * @param isDeleted
     * @return
     * @throws Exception
     */
    public FHIRRestOperationResponse doPatchOrUpdatePersist(FHIRPersistenceEvent event, String type, String id, boolean isPatch,
        Resource newResource, Resource prevResource, List<Issue> warnings, boolean isDeleted) throws Exception;
    
    /**
     * Builds a collection of properties that will be passed to the persistence interceptors.
     *
     * @param type
     *            the resource type
     * @param id
     *            the resource logical ID
     * @param version
     *            the resource version
     * @param searchContext
     *            the request search context
     * @return a map of persistence event properties
     * @throws FHIRPersistenceException
     */
    Map<String, Object> buildPersistenceEventProperties(String type, String id,
        String version, FHIRSearchContext searchContext) throws FHIRPersistenceException;
    
    /**
     * Performs an update operation (a new version of the Resource will be stored). Validates the resource.
     *
     * @param type
     *            the type of the resource to be updated
     * @param id
     *            the id of the Resource being updated
     * @param newResource
     *            the new resource to be stored
     * @param ifMatchValue
     *            an optional "If-Match" header value to request a version-aware update
     * @param searchQueryString
     *            an optional search query string to request a conditional update
     * @param skippableUpdate
     *            if true, and the resource content in the update matches the existing resource on the server, then skip the update;
     *            if false, then always attempt the update
     * @return a FHIRRestOperationResponse that contains the results of the operation
     * @throws Exception
     */
    default FHIRRestOperationResponse doUpdate(String type, String id, Resource newResource, String ifMatchValue,
            String searchQueryString, boolean skippableUpdate) throws Exception {
        return doUpdate(type, id, newResource, ifMatchValue, searchQueryString, skippableUpdate, DO_VALIDATION);
    }

    /**
     * Performs an update operation (a new version of the Resource will be stored).
     *
     * @param type
     *            the type of the resource to be updated
     * @param id
     *            the id of the Resource being updated
     * @param newResource
     *            the new resource to be stored
     * @param ifMatchValue
     *            an optional "If-Match" header value to request a version-aware update
     * @param searchQueryString
     *            an optional search query string to request a conditional update
     * @param skippableUpdate
     *            if true, and the resource content in the update matches the existing resource on the server, then skip the update;
     *            if false, then always attempt the update
     * @param doValidation
     *            if true, validate the resource; if false, assume the resource has already been validated
     * @return a FHIRRestOperationResponse that contains the results of the operation
     * @throws Exception
     */
    FHIRRestOperationResponse doUpdate(String type, String id, Resource newResource, String ifMatchValue,
            String searchQueryString, boolean skippableUpdate, boolean doValidation) throws Exception;

    /**
     * Performs a patch operation (a new version of the Resource will be stored).
     *
     * @param type
     *            the type of the resource to be updated
     * @param id
     *            the id of the Resource being updated
     * @param patch
     *            the patch to apply
     * @param ifMatchValue
     *            an optional "If-Match" header value to request a version-aware update
     * @param searchQueryString
     *            an optional search query string to request a conditional update
     * @param skippableUpdate
     *            if true, and the result of the patch matches the existing resource on the server, then skip the update;
     *            if false, then always attempt the update
     * @return a FHIRRestOperationResponse that contains the results of the operation
     * @throws Exception
     */
    FHIRRestOperationResponse doPatch(String type, String id, FHIRPatch patch, String ifMatchValue,
            String searchQueryString, boolean skippableUpdate) throws Exception;


    /**
     * Performs a 'delete' operation on the specified resource.
     *
     * @param type
     *            the resource type associated with the Resource to be deleted
     * @param id
     *            the id of the Resource to be deleted
     * @return a FHIRRestOperationResponse that contains the results of the operation
     * @throws Exception
     */
    FHIRRestOperationResponse doDelete(String type, String id, String searchQueryString) throws Exception;

    /**
     * Performs a 'read' operation to retrieve a Resource.
     *
     * @param type
     *            the resource type associated with the Resource to be retrieved
     * @param id
     *            the id of the Resource to be retrieved
     * @param throwExcOnNull
     *            whether to throw an exception on null
     * @param includeDeleted
     *            allow the read, even if the resource has been deleted
     * @param contextResource
     *            the resource
     * @param queryParameters
     *            for supporting _elements and _summary for resource read
     * @return a SingleResourceResult wrapping the resource and including its deletion status
     * @throws Exception
     */
    default SingleResourceResult<? extends Resource> doRead(String type, String id, boolean throwExcOnNull, boolean includeDeleted,
            Resource contextResource) throws Exception {
        return doRead(type, id, throwExcOnNull, includeDeleted, contextResource, null);
    }

    /**
     * Performs a 'read' operation to retrieve a Resource with select query parameters.
     *
     * @param type
     *            the resource type associated with the Resource to be retrieved
     * @param id
     *            the id of the Resource to be retrieved
     * @param throwExcOnNull
     *            whether to throw an exception on null
     * @param includeDeleted
     *            allow the read, even if the resource has been deleted
     * @param contextResource
     *            the resource
     * @param queryParameters
     *            for supporting _elements and _summary for resource read
     * @return a SingleResourceResult wrapping the resource and including its deletion status
     * @throws Exception
     */
    SingleResourceResult<? extends Resource> doRead(String type, String id, boolean throwExcOnNull, boolean includeDeleted,
            Resource contextResource, MultivaluedMap<String, String> queryParameters) throws Exception;

    /**
     * Performs a 'vread' operation by retrieving the specified version of a Resource with no query parameters
     *
     * @param type
     *            the resource type associated with the Resource to be retrieved
     * @param id
     *            the id of the Resource to be retrieved
     * @param versionId
     *            the version id of the Resource to be retrieved
     * @return the Resource
     * @throws Exception
     */
    default Resource doVRead(String type, String id, String versionId) throws Exception {
        return doVRead(type, id, versionId, null);
    }

    /**
     * Performs a 'vread' operation by retrieving the specified version of a Resource.
     *
     * @param type
     *            the resource type associated with the Resource to be retrieved
     * @param id
     *            the id of the Resource to be retrieved
     * @param versionId
     *            the version id of the Resource to be retrieved
     * @param queryParameters
     *            for supporting _elements and _summary for resource vread
     * @return the Resource
     * @throws Exception
     */
    Resource doVRead(String type, String id, String versionId, MultivaluedMap<String, String> queryParameters) throws Exception;

    /**
     * Performs the work of retrieving versions of a Resource.
     *
     * @param type
     *            the resource type associated with the Resource to be retrieved
     * @param id
     *            the id of the Resource to be retrieved
     * @param queryParameters
     *            a Map containing the query parameters from the request URL
     * @param requestUri
     * @return a Bundle containing the history of the specified Resource
     * @throws Exception
     */
    Bundle doHistory(String type, String id, MultivaluedMap<String, String> queryParameters, String requestUri) throws Exception;

    /**
     * Implement the system level history operation to obtain a list of changes to resources
     *
     * @param queryParameters
     *            a Map containing the query parameters from the request URL
     * @param requestUri
     *            the request URI
     * @return a Bundle containing the history of the specified Resource
     * @throws Exception
     */
    Bundle doHistory(MultivaluedMap<String, String> queryParameters, String requestUri) throws Exception;

    /**
     * Performs heavy lifting associated with a 'search' operation.
     *
     * @param type
     *            the resource type associated with the search
     * @param compartment
     *            the compartment type to search in, or null if not a compartment search
     * @param compartmentId
     *            the specific compartment to search in, or null if not a compartment search
     * @param queryParameters
     *            a Map containing the query parameters from the request URL
     * @param requestUri
     *            the request URI
     * @param contextResource
     *            the resource context
     * @return a Bundle containing the search result set
     * @throws Exception
     */
    Bundle doSearch(String type, String compartment, String compartmentId, MultivaluedMap<String, String> queryParameters,
            String requestUri, Resource contextResource) throws Exception;

    /**
     * Performs heavy lifting associated with a 'search' operation.
     *
     * @param type
     *            the resource type associated with the search
     * @param compartment
     *            the compartment type to search in, or null if not a compartment search
     * @param compartmentId
     *            the specific compartment to search in, or null if not a compartment search
     * @param queryParameters
     *            a Map containing the query parameters from the request URL
     * @param requestUri
     *            the request URI
     * @param contextResource
     *            the resource context
     * @param checkIfInteractionAllowed
     *            if true, check that the search interaction is permitted
     * @return a Bundle containing the search result set
     * @throws Exception
     */
    Bundle doSearch(String type, String compartment, String compartmentId, MultivaluedMap<String, String> queryParameters,
        String requestUri, Resource contextResource, boolean checkIfInteractionAllowed) throws Exception;

    /**
     * Helper method which invokes a custom operation.
     *
     * @param operationContext
     *            the FHIROperationContext associated with the request
     * @param resourceTypeName
     *            the resource type associated with the request
     * @param logicalId
     *            the resource logical id associated with the request
     * @param versionId
     *            the resource version id associated with the request
     * @param operationName
     *            the name of the custom operation to be invoked
     * @param resource
     *            the input resource associated with the custom operation to be invoked
     * @param queryParameters
     *            query parameters may be passed instead of a Parameters resource for certain custom operations invoked
     *            via GET
     * @return a Resource that represents the response to the custom operation
     * @throws Exception
     */
    Resource doInvoke(FHIROperationContext operationContext, String resourceTypeName, String logicalId, String versionId, String operationName,
            Resource resource, MultivaluedMap<String, String> queryParameters) throws Exception;

    /**
     * Processes a bundled request (batch or transaction type).
     *
     * @param bundle
     *            the request Bundle
     * @param skippableUpdates
     *            if true, and the bundle contains an update for which the resource content in the update matches the existing
     *            resource on the server, then skip the update; if false, then always attempt the updates specified in the bundle
     * @return the response Bundle
     */
    Bundle doBundle(Bundle bundle, boolean skippableUpdates) throws Exception;

    FHIRPersistenceTransaction getTransaction() throws Exception;

    /**
     * Invoke the FHIR persistence reindex operation for either a specified list of indexIds,
     * or a randomly chosen resource, last reindexed before the given timestamp.
     * @param operationContext the operation context
     * @param operationOutcomeResult accumulate issues in this {@link OperationOutcome.Builder}
     * @param tstamp only reindex resources with a reindex_tstamp less than this
     * @param indexIds list of index IDs of resources to reindex, or null
     * @param resourceLogicalId resourceType (e.g. "Patient"), or resourceType/logicalId a specific resource (e.g. "Patient/abc123"), to reindex, or null;
     * this parameter is ignored if the indexIds parameter value is non-null
     * @return count of the number of resources reindexed by this call
     * @throws Exception
     */
    int doReindex(FHIROperationContext operationContext, OperationOutcome.Builder operationOutcomeResult, Instant tstamp, List<Long> indexIds,
        String resourceLogicalId) throws Exception;

    /**
     * Invoke the FHIR Persistence erase operation for a specific instance of the erase.
     * @param operationContext
     * @param eraseDto
     * @return
     * @throws Exception
     */
    default ResourceEraseRecord doErase(FHIROperationContext operationContext, EraseDTO eraseDto) throws FHIROperationException {
        /*
         * @implNote, to keep from breaking other implementations, we are marking it as Unsupported and throwing an exception.
         */
        throw new FHIROperationException("Unsupported for the given platform");
    }

    /**
     * Invoke the FHIR persistence retrieve index operation to retrieve a list of indexIds available for reindexing.
     * @param operationContext the operation context
     * @param resourceTypeName the resource type of index IDs to return, or null
     * @param count the maximum nuber of index IDs to retrieve
     * @param notModifiedAfter only retrieve index IDs for resources not last updated after the specified timestamp
     * @param afterIndexId retrieve index IDs starting after this specified ID, or null to start with first ID
     * @return list of index IDs available for reindexing
     * @throws Exception
     */
    List<Long> doRetrieveIndex(FHIROperationContext operationContext, String resourceTypeName, int count, Instant notModifiedAfter, Long afterIndexId) throws Exception;
    
    /**
     * Generate a new resource id. This is typically delegated to the persistence layer, which
     * may want to create FHIR-compliant ids optimized for a certain type of storage.
     * @return
     */
    String generateResourceId();
    
    /**
     * If the underlying persistence layer supports offloading payload storage, initiate the
     * request here.
     * @param resource the resource to store (with correct Meta fields)
     * @param logicalId the logical id of the resource
     * @param newVersionNumber the version number to use
     * @return a Future response to the payload store operation, or null if it is not supported
     */
    Future<PayloadKey> storePayload(Resource resource, String logicalId, int newVersionNumber) throws Exception;
}