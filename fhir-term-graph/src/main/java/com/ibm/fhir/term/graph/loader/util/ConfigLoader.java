/*
 * (C) Copyright IBM Corp. 2021, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.term.graph.loader.util;

import java.util.logging.Logger;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/*
 * This class will load a Configuration from a property file, then override parameters based on environment variables.
 */
public class ConfigLoader {
    public static final String STORAGE_HOSTNAME = "storage.hostname";
    public static final String STORAGE_PORT = "storage.port";
    public static final String STORAGE_USERNAME = "storage.username";
    public static final String STORAGE_PASSWORD = "storage.password";
    public static final String INDEX_SEARCH_HOSTNAME = "index.search.hostname";
    public static final String INDEX_SEARCH_PORT = "index.search.port";
    public static final String STORAGE_HOSTNAME_ENV = STORAGE_HOSTNAME.toUpperCase().replaceAll("\\.", "_");
    public static final String STORAGE_PORT_ENV = STORAGE_PORT.toUpperCase().replaceAll("\\.", "_");
    public static final String STORAGE_USERNAME_ENV = STORAGE_USERNAME.toUpperCase().replaceAll("\\.", "_");
    public static final String STORAGE_PASSWORD_ENV = STORAGE_PASSWORD.toUpperCase().replaceAll("\\.", "_");
    public static final String INDEX_SEARCH_HOSTNAME_ENV = INDEX_SEARCH_HOSTNAME.toUpperCase().replaceAll("\\.", "_");
    public static final String INDEX_SEARCH_PORT_ENV = INDEX_SEARCH_PORT.toUpperCase().replaceAll("\\.", "_");

    private static final Logger LOG = Logger.getLogger(ConfigLoader.class.getName());

    /*
     * Load Configuration from properties file at given location, then override using environment variables
     */
    public static final Configuration load(String propFileName) throws ConfigurationException {
        Configuration configuration = null;
        if (propFileName == null) {
            LOG.info("Could not load configuration from property file. ");
            configuration = new BaseConfiguration();
        } else {
            configuration = new PropertiesConfiguration(propFileName);
        }

        String storageHostname = System.getenv(STORAGE_HOSTNAME_ENV);
        if (storageHostname != null) {
            configuration.setProperty(STORAGE_HOSTNAME, storageHostname);
        }

        String storagePort = System.getenv(STORAGE_PORT_ENV);
        if (storagePort != null) {
            configuration.setProperty(STORAGE_PORT, storagePort);
        }

        String storageUsername = System.getenv(STORAGE_USERNAME_ENV);
        if (storageUsername != null) {
            configuration.setProperty(STORAGE_USERNAME, storageUsername);
        }

        String storagePassword = System.getenv(STORAGE_PASSWORD_ENV);
        if (storagePassword != null) {
            configuration.setProperty(STORAGE_PASSWORD, storagePassword);
        }

        String indexSearchHostname = System.getenv(INDEX_SEARCH_HOSTNAME_ENV);
        if (indexSearchHostname != null) {
            configuration.setProperty(INDEX_SEARCH_HOSTNAME, indexSearchHostname);
        }

        String indexSearchPort = System.getenv(INDEX_SEARCH_PORT_ENV);
        if (indexSearchPort != null) {
            configuration.setProperty(INDEX_SEARCH_PORT, indexSearchPort);
        }

        return configuration;
    }
}