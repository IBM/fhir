/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.jdbc.FHIRPersistenceJDBCCache;
import com.ibm.fhir.persistence.jdbc.dao.api.IResourceReferenceDAO;
import com.ibm.fhir.persistence.jdbc.dao.api.JDBCIdentityCache;
import com.ibm.fhir.persistence.jdbc.dao.api.ParameterDAO;
import com.ibm.fhir.persistence.jdbc.dao.api.ResourceDAO;
import com.ibm.fhir.persistence.jdbc.dto.CommonTokenValue;
import com.ibm.fhir.persistence.jdbc.dto.CommonTokenValueResult;
import com.ibm.fhir.persistence.jdbc.exception.FHIRPersistenceDataAccessException;


/**
 * Pulls together the DAOs and tenant-specific cache to provide a single place
 * where we can look up the identity of various records we need
 */
public class JDBCIdentityCacheImpl implements JDBCIdentityCache {
    private static final Logger logger = Logger.getLogger(JDBCIdentityCacheImpl.class.getName());

    // The tenant-specific set of caches
    private final FHIRPersistenceJDBCCache cache;

    // The DAO providing access to parameter names
    private final ParameterDAO parameterDAO;

    // The DAO providing access to resource types
    private final ResourceDAO resourceDAO;

    private final IResourceReferenceDAO resourceReferenceDAO;

    /**
     * Public constructor
     * @param cache
     * @param parameterDAO
     * @param rrd
     */
    public JDBCIdentityCacheImpl(FHIRPersistenceJDBCCache cache, ResourceDAO resourceDAO, ParameterDAO parameterDAO, IResourceReferenceDAO rrd) {
        this.cache = cache;
        this.resourceDAO = resourceDAO;
        this.parameterDAO = parameterDAO;
        this.resourceReferenceDAO = rrd;
    }

    @Override
    public Integer getResourceTypeId(String resourceType) throws FHIRPersistenceException {
        Integer result = cache.getResourceTypeCache().getId(resourceType);
        if (result == null) {
            // try the database instead and cache the result
            result = resourceDAO.readResourceTypeId(resourceType);

            if (result == null) {
                // likely a configuration error, caused by the schema being generated
                // for a subset of all possible resource types
                throw new FHIRPersistenceDataAccessException("Resource type not registered in database: '" + resourceType + "'");
            }

            cache.getResourceTypeCache().addEntry(resourceType, result);
            cache.getResourceTypeNameCache().addEntry(result, resourceType);
        }
        return result;
    }

    @Override
    public String getResourceTypeName(Integer resourceTypeId) throws FHIRPersistenceException {
        String result = cache.getResourceTypeNameCache().getName(resourceTypeId);
        if (result == null) {
            // try the database instead and just cache all results found in this cache
            // and in the resource type cache as well
            Map<String, Integer> resourceMap = resourceDAO.readAllResourceTypeNames();
            for (Map.Entry<String, Integer> entry : resourceMap.entrySet()) {
                if (entry.getValue() == resourceTypeId) {
                    result = entry.getKey();
                }
                cache.getResourceTypeNameCache().addEntry(entry.getValue(), entry.getKey());
                cache.getResourceTypeCache().addEntry(entry.getKey(), entry.getValue());
            }

            if (result == null) {
                // likely a configuration error, caused by the schema being generated
                // for a subset of all possible resource types
                throw new FHIRPersistenceDataAccessException("Resource type ID not registered in database: '" + resourceTypeId + "'");
            }
        }
        return result;
    }

    @Override
    public Integer getCodeSystemId(String codeSystemName) throws FHIRPersistenceException {
        Integer result = cache.getResourceReferenceCache().getCodeSystemId(codeSystemName);
        if (result == null) {
            // cache miss, so hit the database
            result = parameterDAO.readOrAddCodeSystemId(codeSystemName);
            if (result != null) {
                cache.getResourceReferenceCache().addCodeSystem(codeSystemName, result);
            }
        }
        return result;
    }

    @Override
    public Integer getParameterNameId(String parameterName) throws FHIRPersistenceException {
        Integer result = cache.getParameterNameCache().getId(parameterName);
        if (result == null) {
            result = parameterDAO.acquireParameterNameId(parameterName);
            cache.getParameterNameCache().addEntry(parameterName, result);
        }

        return result;
    }

    @Override
    public Integer getCanonicalId(String canonicalValue) throws FHIRPersistenceException {
        Integer result = cache.getResourceReferenceCache().getCanonicalId(canonicalValue);
        if (result == null) {
            result = resourceReferenceDAO.readCanonicalId(canonicalValue);
            if (result != null) {
                cache.getResourceReferenceCache().addCanonicalValue(canonicalValue, result);
            } else {
                result = -1;
            }
        }

        return result;
    }

    @Override
    public Long getCommonTokenValueId(String codeSystem, String tokenValue) {
        Long result = cache.getResourceReferenceCache().getCommonTokenValueId(codeSystem, tokenValue);
        if (result == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Cache miss. Fetching common_token_value_id from database: '" + codeSystem + "|" + tokenValue + "'");
            }
            CommonTokenValueResult dto = resourceReferenceDAO.readCommonTokenValueId(codeSystem, tokenValue);
            if (dto != null) {
                // Value exists in the database, so we can add this to our cache. Note that we still
                // choose to add it the thread-local cache - this avoids any locking. The values will
                // be promoted to the shared cache at the end of the transaction. This avoids unnecessary
                // contention.
                result = dto.getCommonTokenValueId();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Adding common_token_value_id to cache: '" + codeSystem + "|" + tokenValue + "' = " + result);
                }
                cache.getResourceReferenceCache().addCodeSystem(codeSystem, dto.getCodeSystemId());
                cache.getResourceReferenceCache().addTokenValue(new CommonTokenValue(codeSystem, dto.getCodeSystemId(), tokenValue), result);
            }
        }
        return result;
    }

    @Override
    public Set<Long> getCommonTokenValueIds(Collection<CommonTokenValue> tokenValues) {
        Set<CommonTokenValue> misses = new HashSet<>();
        Set<Long> result = cache.getResourceReferenceCache().resolveCommonTokenValueIds(tokenValues, misses);

        if (misses.isEmpty()) {
            return result;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Cache miss. Fetching common_token_value_ids from database: " + misses);
        }

        Set<CommonTokenValueResult> readCommonTokenValueIds = resourceReferenceDAO.readCommonTokenValueIds(misses);
        result.addAll(readCommonTokenValueIds.stream()
            .map(r -> r.getCommonTokenValueId())
            .collect(Collectors.toSet()));

        for (CommonTokenValueResult dto : readCommonTokenValueIds) {
            // Value exists in the database, so we can add this to our cache. Note that we still
            // choose to add it to the thread-local cache - this avoids any locking. The values will
            // be promoted to the shared cache at the end of the transaction. This avoids unnecessary
            // contention.
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Adding common_token_value_id to cache: '" + dto.getCodeSystemId() + "|" + dto.getTokenValue() + "' = " + result);
            }
            
            // The codeSystem is not required at this stage
            cache.getResourceReferenceCache().addTokenValue(new CommonTokenValue(null, dto.getCodeSystemId(), dto.getTokenValue()), dto.getCommonTokenValueId());
        }

        return result;
    }

    @Override
    public List<Long> getCommonTokenValueIdList(String tokenValue) {
        return resourceReferenceDAO.readCommonTokenValueIdList(tokenValue);
    }

    @Override
    public List<String> getResourceTypeNames() throws FHIRPersistenceException {
        return new ArrayList<>(cache.getResourceTypeNameCache().getAllNames());
    }

    @Override
    public List<Integer> getResourceTypeIds() throws FHIRPersistenceException {
        return new ArrayList<>(cache.getResourceTypeCache().getAllIds());
    }
}