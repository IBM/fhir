/*
 * (C) Copyright IBM Corp. 2016, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.cli.invoker;

import jakarta.json.JsonObject;

import com.ibm.fhir.model.resource.Resource;

/**
 * This class is the OperationInvoker implementation for the 'validate' operation.
 * 
 * @author padams
 */
public class ValidateInvoker extends OperationInvoker {

    /* (non-Javadoc)
     * @see com.ibm.fhir.cli.OperationInvoker#invoke(com.ibm.fhir.cli.InvocationContext)
     */
    @Override
    public void doInvoke(InvocationContext ic) throws Exception {
        Object resource = ic.getRequestResourceWithExcp();
        if (resource instanceof Resource) {
            response = client.validate((Resource)resource, requestHeaders);
        } else {
            response = client.validate((JsonObject)resource, requestHeaders);
        }
    }
}
