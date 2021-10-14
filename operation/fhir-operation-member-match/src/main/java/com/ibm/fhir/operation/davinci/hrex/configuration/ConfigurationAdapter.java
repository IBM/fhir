/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.operation.davinci.hrex.configuration;

import com.ibm.fhir.config.PropertyGroup;

/**
 * MemberMatch adapts the FHIR Server Config to simple outputs.
 */
public interface ConfigurationAdapter {
    /**
     * indicates if the MemberMatch service is enabled.
     * @return
     */
    boolean enabled();

    /**
     * Gets the member strategy key
     * @return
     */
    String getStrategyKey();

    /**
     * gets the extended strategy property group
     * @return
     */
    PropertyGroup getExtendedStrategyPropertyGroup();
}