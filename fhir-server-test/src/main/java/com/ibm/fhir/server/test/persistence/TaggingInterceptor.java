/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.server.test.persistence;

import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.persistence.context.FHIRPersistenceEvent;
import com.ibm.fhir.server.spi.interceptor.FHIRPersistenceInterceptor;
import com.ibm.fhir.server.spi.interceptor.FHIRPersistenceInterceptorException;

/**
 * A sample persistence interceptor that adds a tag to each resource before it gets persisted.
 */
public class TaggingInterceptor implements FHIRPersistenceInterceptor {
    private final Coding TAG = Coding.builder()
            .system(Uri.of("http://example.com/test"))
            .code(Code.of("test"))
            .build();

    @Override
    public void beforeCreate(FHIRPersistenceEvent event) throws FHIRPersistenceInterceptorException {
        event.setFhirResource(addTag(event.getFhirResource()));
    }

    @Override
    public void beforeUpdate(FHIRPersistenceEvent event) throws FHIRPersistenceInterceptorException {
        event.setFhirResource(addTag(event.getFhirResource()));
    }

    @Override
    public void beforePatch(FHIRPersistenceEvent event) throws FHIRPersistenceInterceptorException {
        event.setFhirResource(addTag(event.getFhirResource()));
    }

    private Resource addTag(Resource r) {
        boolean hasMeta = r.getMeta() != null;
        if (hasMeta && r.getMeta().getTag().contains(TAG)) {
            return r;
        }
        Meta.Builder metaBuilder = hasMeta ? r.getMeta().toBuilder() : Meta.builder();
        return r.toBuilder().meta(metaBuilder.tag(TAG).build()).build();
    }
}
