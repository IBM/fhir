/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.validation.test;

import static com.ibm.fhir.model.type.String.string;
import static com.ibm.fhir.validation.util.FHIRValidationUtil.countErrors;
import static com.ibm.fhir.validation.util.FHIRValidationUtil.hasErrors;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.model.resource.OperationOutcome.Issue;
import com.ibm.fhir.model.resource.Organization;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.resource.StructureDefinition;
import com.ibm.fhir.model.type.Canonical;
import com.ibm.fhir.model.type.ElementDefinition;
import com.ibm.fhir.model.type.ElementDefinition.Constraint;
import com.ibm.fhir.model.type.Id;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.Reference;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.model.type.code.ConstraintSeverity;
import com.ibm.fhir.model.type.code.TypeDerivationRule;
import com.ibm.fhir.registry.FHIRRegistry;
import com.ibm.fhir.registry.resource.FHIRRegistryResource;
import com.ibm.fhir.registry.util.FHIRRegistryResourceProviderAdapter;
import com.ibm.fhir.validation.FHIRValidator;

public class ConformsToLoopTest {
    @BeforeClass
    public void beforeClass() throws Exception {
        StructureDefinition structureDefinition = FHIRRegistry.getInstance().getResource("http://hl7.org/fhir/StructureDefinition/Organization", StructureDefinition.class);

        List<ElementDefinition> element = new ArrayList<>(structureDefinition.getSnapshot().getElement());
        element.set(indexOf(structureDefinition, "Organization"), addConstraints(getElementDefinition(structureDefinition, "Organization"),
            Constraint.builder()
                .key(Id.of("test-1"))
                .severity(ConstraintSeverity.ERROR)
                .human(string("The organization SHALL conform to the TestOrganization profile"))
                .expression(string("conformsTo('http://ibm.com/fhir/StructureDefinition/TestOrganization')"))
                .source(Canonical.of("http://ibm.com/fhir/StructureDefinition/TestOrganization"))
                .build(),
            Constraint.builder()
                .key(Id.of("test-2"))
                .severity(ConstraintSeverity.ERROR)
                .human(string("The organization name length SHALL be greater than 9 characters"))
                .expression(string("name.length() > 9"))
                .source(Canonical.of("http://ibm.com/fhir/StructureDefinition/TestOrganization"))
                .build()));
        element.set(indexOf(structureDefinition, "Organization.partOf"), addConstraints(getElementDefinition(structureDefinition, "Organization.partOf"), Constraint.builder()
            .key(Id.of("test-3"))
            .human(string("The partOf reference SHALL resolve to an organization that conforms the TestOrganization profile"))
            .expression(string("resolve().conformsTo('http://ibm.com/fhir/StructureDefinition/TestOrganization')"))
            .source(Canonical.of("http://ibm.com/fhir/StructureDefinition/TestOrganization"))
            .severity(ConstraintSeverity.ERROR)
            .build()));

        StructureDefinition profile = structureDefinition.toBuilder()
            .url(Uri.of("http://ibm.com/fhir/StructureDefinition/TestOrganization"))
            .baseDefinition(Canonical.of("http://hl7.org/fhir/StructureDefinition/Organization"))
            .derivation(TypeDerivationRule.CONSTRAINT)
            .snapshot(structureDefinition.getSnapshot().toBuilder()
                .element(element)
                .build())
            .build();

        FHIRRegistry.getInstance().addProvider(new FHIRRegistryResourceProviderAdapter() {
            @Override
            public FHIRRegistryResource getRegistryResource(Class<? extends Resource> resourceType, String url, String version) {
                if ("http://ibm.com/fhir/StructureDefinition/TestOrganization".equals(url)) {
                    return FHIRRegistryResource.from(profile);
                }
                return null;
            }
        });
    }

    @Test
    public void testConformsToLoop1() throws Exception {
        Organization organization = Organization.builder()
            .name(string("Test Organization"))
            .contained(Organization.builder()
                .meta(Meta.builder()
                    .profile(Canonical.of("http://ibm.com/fhir/StructureDefinition/TestOrganization"))
                    .build())
                .name(string("Child Test Organization"))
                .partOf(Reference.builder()
                    .reference(string("#"))
                    .build())
                .build())
            .build();
        List<Issue> issues = FHIRValidator.validator().validate(organization, true, "http://ibm.com/fhir/StructureDefinition/TestOrganization");
        assertFalse(hasErrors(issues));
    }

    @Test
    public void testConformsToLoop2() throws Exception {
        Organization organization = Organization.builder()
            .name(string("Test Organization"))
            .contained(Organization.builder()
                .meta(Meta.builder()
                    .profile(Canonical.of("http://ibm.com/fhir/StructureDefinition/TestOrganization"))
                    .build())
                .name(string("invalid"))
                .partOf(Reference.builder()
                    .reference(string("#"))
                    .build())
                .build())
            .build();
        List<Issue> issues = FHIRValidator.validator().validate(organization, true, "http://ibm.com/fhir/StructureDefinition/TestOrganization");
        assertTrue(hasErrors(issues));
        assertEquals(countErrors(issues), 1);
    }

    private ElementDefinition addConstraints(ElementDefinition elementDefinition, Constraint... constraints) {
        return elementDefinition.toBuilder()
            .constraint(elementDefinition.getConstraint())
            .constraint(constraints)
            .build();
    }

    private ElementDefinition getElementDefinition(StructureDefinition structureDefinition, String path) {
        for (ElementDefinition elementDefinition : structureDefinition.getSnapshot().getElement()) {
            if (elementDefinition.getPath().getValue().equals(path)) {
                return elementDefinition;
            }
        }
        return null;
    }

    private int indexOf(StructureDefinition structureDefinition, String path) {
        for (int i = 0; i < structureDefinition.getSnapshot().getElement().size(); i++) {
            ElementDefinition elementDefinition = structureDefinition.getSnapshot().getElement().get(i);
            if (elementDefinition.getPath().getValue().equals(path)) {
                return i;
            }
        }
        return -1;
    }
}
