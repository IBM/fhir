/**
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.examples.plugin.test;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

import com.ibm.watsonhealth.fhir.examples.plugin.ExamplesPlugin;

/**
 * 
 * @author pbastide
 *
 */
public class ExamplesPluginTest extends AbstractMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testModelPlugin() throws Exception {
        File pom = getTestFile("src/test/resources/examplesplugin/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        ExamplesPlugin samplesPlugin = (ExamplesPlugin) lookupMojo("generate-examples", pom);
        assertNotNull(samplesPlugin);

        // Ideally, the test executes -> <code>samplesPlugin.execute();</code>
    }
}
