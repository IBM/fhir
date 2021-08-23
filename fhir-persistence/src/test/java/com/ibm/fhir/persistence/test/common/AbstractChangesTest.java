/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.test.common;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.model.resource.Basic;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.Element;
import com.ibm.fhir.model.type.Extension;
import com.ibm.fhir.model.type.Integer;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.persistence.ResourceChangeLogRecord;
import com.ibm.fhir.persistence.ResourceChangeLogRecord.ChangeType;

/**
 * Tests related to the high-speed export method in FHIRPersistence.
 */
public abstract class AbstractChangesTest extends AbstractPersistenceTest {
    Basic resource1;
    Basic resource2;
    Basic resource3;
    Basic resource4;

    @BeforeClass
    public void createResources() throws Exception {
        FHIRRequestContext.get().setTenantId("all");

        Basic resource = TestUtil.getMinimalResource(Basic.class);

        Basic.Builder resource1Builder = resource.toBuilder();
        Basic.Builder resource2Builder = resource.toBuilder();
        Basic.Builder resource3Builder = resource.toBuilder();
        Basic.Builder resource4Builder = resource.toBuilder();

        // number
        resource1Builder.extension(extension("http://example.org/integer", Integer.of(1)));
        resource2Builder.extension(extension("http://example.org/integer", Integer.of(2)));
        resource3Builder.extension(extension("http://example.org/integer", Integer.of(3)));
        resource4Builder.extension(extension("http://example.org/integer", Integer.of(4)));

        // save them in-order so that lastUpdated goes from 1 -> 3 as well
        resource1 = persistence.create(getDefaultPersistenceContext(), resource1Builder.meta(tag("pagingTest")).build()).getResource();
        resource2 = persistence.create(getDefaultPersistenceContext(), resource2Builder.meta(tag("pagingTest")).build()).getResource();
        resource3 = persistence.create(getDefaultPersistenceContext(), resource3Builder.meta(tag("pagingTest")).build()).getResource();
        resource4 = persistence.create(getDefaultPersistenceContext(), resource4Builder.meta(tag("pagingTest")).build()).getResource();

        // update resource3 two times so we have 3 different versions
        resource3 = persistence.update(getDefaultPersistenceContext(), resource3.getId(), resource3).getResource();
        resource3 = persistence.update(getDefaultPersistenceContext(), resource3.getId(), resource3).getResource();

        // delete resource4
        persistence.delete(getDefaultPersistenceContext(), resource4.getClass(), resource4.getId());
    }

    @AfterClass
    public void removeSavedResourcesAndResetTenant() throws Exception {
        // *** NOTE NOTE NOTE ***
        // Although we are soft-deleting the resources, this generates more records
        // in the change record log, so it's important that the tests in this class
        // are written to take that into account
        Resource[] resources = {resource1, resource2, resource3};
        if (persistence.isDeleteSupported()) {
            // as this is AfterClass, we need to manually start/end the transaction
            startTrx();
            for (Resource resource : resources) {
                persistence.delete(getDefaultPersistenceContext(), Basic.class, resource.getId());
            }
            commitTrx();
        }
        FHIRRequestContext.get().setTenantId("default");
    }

    @Test
    public void testSomeData() throws Exception {

        // Without a start time filter, we don't know how many records we'll get back because
        // several tests will end up populating the change log table. But we need to make
        // sure the query works withouth the fromLastModified filter.
        Instant fromLastModified = null;
        final Long afterResourceId = null;
        final String resourceTypeName = null;

        List<ResourceChangeLogRecord> result = persistence.changes(100, fromLastModified, afterResourceId, resourceTypeName);
        assertNotNull(result);
        assertTrue(result.size() >= 7);
        assertTrue(result.size() <= 100);
    }

    @Test
    public void testChanges() throws Exception {

        // Make sure we start the clock at the right place otherwise our
        // selection span won't cover any data
        Instant fromLastModified = resource1.getMeta().getLastUpdated().getValue().toInstant();
        final Long afterResourceId = null;
        final String resourceTypeName = null;

        List<ResourceChangeLogRecord> result = persistence.changes(7, fromLastModified, afterResourceId, resourceTypeName);
        assertNotNull(result);

        // 4 CREATE
        // 2 UPDATE
        // 1 DELETE
        assertEquals(result.size(), 7);
        assertEquals(result.get(0).getChangeType(), ChangeType.CREATE); // resource1
        assertEquals(result.get(1).getChangeType(), ChangeType.CREATE); // resource2
        assertEquals(result.get(2).getChangeType(), ChangeType.CREATE); // resource3
        assertEquals(result.get(3).getChangeType(), ChangeType.CREATE); // resource4
        assertEquals(result.get(4).getChangeType(), ChangeType.UPDATE); // resource3
        assertEquals(result.get(5).getChangeType(), ChangeType.UPDATE); // resource3
        assertEquals(result.get(6).getChangeType(), ChangeType.DELETE); // resource4

        assertEquals(result.get(0).getResourceTypeName(), resource1.getClass().getSimpleName());
        assertEquals(result.get(0).getChangeTstamp(), fromLastModified);
        assertEquals(result.get(0).getVersionId(), 1);

        assertEquals(result.get(4).getVersionId(), 2); // resource3
        assertEquals(result.get(5).getVersionId(), 3); // resource3
        assertEquals(result.get(6).getVersionId(), 2); // resource4

        assertEquals(result.get(0).getLogicalId(), resource1.getId()); // resource1
        assertEquals(result.get(1).getLogicalId(), resource2.getId()); // resource2
        assertEquals(result.get(2).getLogicalId(), resource3.getId()); // resource3
        assertEquals(result.get(3).getLogicalId(), resource4.getId()); // resource4
        assertEquals(result.get(4).getLogicalId(), resource3.getId()); // resource3
        assertEquals(result.get(5).getLogicalId(), resource3.getId()); // resource3
        assertEquals(result.get(6).getLogicalId(), resource4.getId()); // resource4
    }

    @Test
    public void testLimit() throws Exception {

        // Make sure we start the clock at the right place otherwise our
        // selection span won't cover any data
        Instant fromLastModified = resource1.getMeta().getLastUpdated().getValue().toInstant();
        Long afterResourceId = null;
        final String resourceTypeName = null;

        List<ResourceChangeLogRecord> result = persistence.changes(4, fromLastModified, afterResourceId, resourceTypeName);
        assertNotNull(result);

        // Limit was set to 4, so we should only get partial data
        assertEquals(result.size(), 4);

        // Make another call now to get the remaining 3 changes
        fromLastModified = result.get(3).getChangeTstamp();
        afterResourceId = result.get(3).getChangeId();
        result = persistence.changes(3, fromLastModified, afterResourceId, resourceTypeName);
        assertNotNull(result);
        assertEquals(result.size(), 3);

        // And a final call to make sure we get nothing
        fromLastModified = result.get(2).getChangeTstamp();
        afterResourceId = result.get(2).getChangeId();
        result = persistence.changes(100, fromLastModified, afterResourceId, resourceTypeName);
        assertNotNull(result);
        assertEquals(result.size(), 0);
    }

    @Test
    public void testResourceTypeFilter() throws Exception {
        // just filter on the resource type name
        Instant fromLastModified = resource1.getMeta().getLastUpdated().getValue().toInstant();
        Long afterResourceId = null;
        final String resourceTypeName = resource1.getClass().getSimpleName();

        List<ResourceChangeLogRecord> result = persistence.changes(10, fromLastModified, afterResourceId, resourceTypeName);
        assertNotNull(result);
        assertEquals(result.size(), 7);
    }

    /**
     * Convenience function to create a Meta tag
     * @param tag
     * @return
     */
    private Meta tag(String tag) {
        return Meta.builder()
                   .tag(Coding.builder()
                              .code(Code.of(tag))
                              .build())
                   .build();
    }

    /**
     * Convenience function to create an extension
     * @param url
     * @param value
     * @return
     */
    private Extension extension(String url, Element value) {
        return Extension.builder()
                        .url(url)
                        .value(value)
                        .build();
    }
}
