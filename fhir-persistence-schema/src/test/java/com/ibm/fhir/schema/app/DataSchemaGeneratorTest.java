/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.schema.app;

import org.testng.annotations.Test;

import com.ibm.fhir.database.utils.common.JdbcTarget;
import com.ibm.fhir.database.utils.db2.Db2Adapter;
import com.ibm.fhir.database.utils.model.PhysicalDataModel;
import com.ibm.fhir.database.utils.version.CreateVersionHistory;
import com.ibm.fhir.database.utils.version.VersionHistoryService;
import com.ibm.fhir.schema.app.JavaBatchSchemaGeneratorTest.ConfirmTagsVisitor;
import com.ibm.fhir.schema.app.JavaBatchSchemaGeneratorTest.PrintConnection;
import com.ibm.fhir.schema.control.FhirSchemaGenerator;

public class DataSchemaGeneratorTest {
    @Test
    public void testFHIRSchemaGeneratorCheckTags() {
        JavaBatchSchemaGeneratorTest test = new JavaBatchSchemaGeneratorTest();
        PrintConnection connection = test.new PrintConnection();
        JdbcTarget target = new JdbcTarget(connection);
        Db2Adapter adapter = new Db2Adapter(target);

        // Set up the version history service first if it doesn't yet exist
        CreateVersionHistory.createTableIfNeeded(Main.ADMIN_SCHEMANAME, adapter);

        // Current version history for the database. This is used by applyWithHistory
        // to determine which updates to apply and to record the new changes as they
        // are applied
        VersionHistoryService vhs = new VersionHistoryService(Main.ADMIN_SCHEMANAME, Main.DATA_SCHEMANAME, Main.OAUTH_SCHEMANAME, Main.BATCH_SCHEMANAME);
        vhs.setTarget(adapter);

        PhysicalDataModel pdm = new PhysicalDataModel();
        FhirSchemaGenerator generator = new FhirSchemaGenerator(Main.ADMIN_SCHEMANAME, Main.DATA_SCHEMANAME, true);
        generator.buildSchema(pdm);
        pdm.apply(adapter);
        pdm.applyFunctions(adapter);
        pdm.applyProcedures(adapter);

        pdm.visit(new ConfirmTagsVisitor());
    }
}