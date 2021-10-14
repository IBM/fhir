/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.ig.davinci.hrex;

import com.ibm.fhir.registry.util.PackageRegistryResourceProvider;

public class HREX020ResourceProvider extends PackageRegistryResourceProvider {
    @Override
    public String getPackageId() {
        return "hl7.fhir.us.davinci-hrex.020";
    }
}