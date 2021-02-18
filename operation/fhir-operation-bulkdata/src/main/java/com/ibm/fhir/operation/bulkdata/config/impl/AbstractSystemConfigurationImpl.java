/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.operation.bulkdata.config.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.fhir.config.FHIRConfigHelper;
import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.operation.bulkdata.config.ConfigurationAdapter;
import com.ibm.fhir.operation.bulkdata.model.type.StorageType;
import com.ibm.fhir.search.SearchConstants;

/**
 *
 */
public abstract class AbstractSystemConfigurationImpl implements ConfigurationAdapter {

    private static final String CLASSNAME = AbstractSystemConfigurationImpl.class.getName();
    private static final Logger logger = Logger.getLogger(CLASSNAME);

    public static final String APPLICATION_NAME = "fhir-bulkimportexport-webapp";
    public static final String MODULE_NAME = "fhir-bulkimportexport.war";
    public static final String JOB_XML_NAME = "jobXMLName";

    public static final String IAM_ENDPOINT = "https://iam.cloud.ibm.com/oidc/token";

    // gets the maximum number of current threads that are supported in the bulkdata processing.
    public static final int MAX_PARTITIONPROCESSING_THREADNUMBER = 5;

    // The minimal size (10MiB) for COS multiple-parts upload (NDJSON-only).
    public static final int COS_PART_MINIMALSIZE = 10485760;

    // The threshold size (200MiB) for when to start writing to a new file (NDJSON-only).
    public static final int DEFAULT_COSFILE_MAX_SIZE = 209715200;

    // The number of resources at which the server will start a new file for the next page of results (NDJSON and
    // Parquet).
    // 200,000 at 1 KB/file would lead to roughly 200 MB files; similar to the DEFAULT_COSFILE_MAX_SIZE.
    public static final int DEFAULT_COSFILE_MAX_RESOURCESNUMBER = 200000;

    private static final String FHIR_BULKDATA_ALLOWED_TYPES = "FHIR_BULKDATA_ALLOWED_TYPES";
    private static final Set<String> ALLOWED_STORAGE_TYPES = determineAllowedStorageType();

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public String getJobXMLName() {
        return JOB_XML_NAME;
    }

    @Override
    public void registerRequestContext(String tenantId, String datastoreId, String incomingUrl) throws FHIRException {
        // Create a new FHIRRequestContext and set it on the current thread.
        FHIRRequestContext context = new FHIRRequestContext(tenantId, datastoreId);
        // Don't try using FHIRConfigHelper before setting the context!
        FHIRRequestContext.set(context);
        context.setOriginalRequestUri(incomingUrl);
        context.setBulk(true);
    }

    private static final Set<String> determineAllowedStorageType() {
        Set<String> allowedStorageLimits = new HashSet<>();

        Map<String, String> envs = System.getenv();
        String env = envs.get(FHIR_BULKDATA_ALLOWED_TYPES);

        if (env != null) {
            String[] storageTypes = env.split(",");
            for (String storageType : storageTypes) {
                if (storageType != null && !storageType.isEmpty()) {
                    try {
                        StorageType type = StorageType.from(storageType);
                        allowedStorageLimits.add(type.value());
                    } catch (IllegalArgumentException iae) {
                        logger.warning("Invalid Storage Type passed in, skipping '" + storageType + "'");
                    }
                } else {
                    logger.warning("Empty BulkData StorageType passed in");
                }
            }
        } else {
            // We're allowing them all.
            for (StorageType t : StorageType.values()) {
                allowedStorageLimits.add(t.value());
            }
        }
        return allowedStorageLimits;
    }

    @Override
    public boolean enabled() {
        return FHIRConfigHelper.getBooleanProperty("fhirServer/bulkdata/enabled", Boolean.TRUE);
    }

    @Override
    public int getCoreCosMinSize() {
        return FHIRConfigHelper.getIntProperty("fhirServer/bulkdata/core/cos/min-size", COS_PART_MINIMALSIZE);
    }

    @Override
    public boolean shouldCoreCosUseServerTruststore() {
        return FHIRConfigHelper.getBooleanProperty("fhirServer/bulkdata/core/cos/use-server-truststore", Boolean.TRUE);
    }

    @Override
    public int getCoreCosRequestTimeout() {
        return FHIRConfigHelper.getIntProperty("fhirServer/bulkdata/core/cos/request-timeout", 10000);
    }

    @Override
    public int getCoreCosSocketTimeout() {
        return FHIRConfigHelper.getIntProperty("fhirServer/bulkdata/core/cos/socket-timeout", 12000);
    }

    @Override
    public int getCorePageSize() {
        int pageSize = FHIRConfigHelper.getIntProperty("fhirServer/bulkdata/core/page-size", SearchConstants.MAX_PAGE_SIZE);
        return Math.min(SearchConstants.MAX_PAGE_SIZE, pageSize);
    }

    @Override
    public int getCoreMaxPartitions() {
        return FHIRConfigHelper.getIntProperty("fhirServer/bulkdata/core/max-partitions", MAX_PARTITIONPROCESSING_THREADNUMBER);
    }

    @Override
    public String getCoreIamEndpoint() {
        return FHIRConfigHelper.getStringProperty("fhirServer/bulkdata/core/iam-endpoint", IAM_ENDPOINT);
    }

    @Override
    public int getCoreFastTxTimeout() {
        return FHIRConfigHelper.getIntProperty("fhirServer/bulkdata/core/fast-tx-timeout", MAX_PARTITIONPROCESSING_THREADNUMBER);
    }

    @Override
    public boolean isStorageTypeAllowed(String storageType) {
        return ALLOWED_STORAGE_TYPES.contains(storageType);
    }

    @Override
    public boolean checkValidFileBase(String source, String fileName) {
        return false;
    }

    @Override
    public StorageType getSourceStorageType(String source) {
        String type = FHIRConfigHelper.getStringProperty("fhirServer/bulkdata/source/" + source + "/type", "none");
        return StorageType.from(type);
    }

    @Override
    public String getTenant() {
        return FHIRRequestContext.get().getTenantId();
    }

    @Override
    public boolean getCoreCosTcpKeepAlive() {
        return true;
    }

    @Override
    public String getBaseFileLocation(String source) {
        return FHIRConfigHelper.getStringProperty("fhirServer/bulkdata/source/" + source + "/file-base", null);
    }

}