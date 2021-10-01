/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.ig.davinci.hrex.test;

import java.util.List;

import org.testng.annotations.Test;

import com.ibm.fhir.examples.Index;
import com.ibm.fhir.model.resource.OperationOutcome.Issue;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.spec.test.R4ExamplesDriver;
import com.ibm.fhir.model.spec.test.SerializationProcessor;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.validation.FHIRValidator;
import com.ibm.fhir.validation.test.ValidationProcessor;

public class ExamplesValidationTest {
    @Test
    public void testHREXValidation() throws Exception {
        R4ExamplesDriver driver = new R4ExamplesDriver();
        SerializationProcessor processor = new SerializationProcessor();
        driver.setProcessor(processor);
        driver.setValidator(new ValidationProcessor());
        driver.processIndex(Index.PROFILES_HREX_JSON);
    }

    public static void main(String[] args) throws Exception {
        Resource r = TestUtil.readExampleResource("json/profiles/fhir-ig-davinci-hrex/example.json");
        List<Issue> validate = FHIRValidator.validator().validate(r);
        for (Issue issue : validate) {
            System.out.println(issue);
        }
    }
}