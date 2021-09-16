/*
 * (C) Copyright IBM Corp. 2019, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.profile;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ibm.fhir.model.annotation.Constraint;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.resource.StructureDefinition;
import com.ibm.fhir.model.type.Canonical;
import com.ibm.fhir.model.type.ElementDefinition;
import com.ibm.fhir.model.type.ElementDefinition.Binding;
import com.ibm.fhir.model.type.ElementDefinition.Type;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.code.TypeDerivationRule;
import com.ibm.fhir.model.util.ModelSupport;
import com.ibm.fhir.registry.FHIRRegistry;

public final class ProfileSupport {
    private static final Logger log = Logger.getLogger(ProfileSupport.class.getName());

    public static final String HL7_STRUCTURE_DEFINITION_URL_PREFIX = "http://hl7.org/fhir/StructureDefinition/";
    public static final String HL7_VALUE_SET_URL_PREFIX = "http://hl7.org/fhir/ValueSet/";

    private static final Map<String, List<Constraint>> CONSTRAINT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, ElementDefinition>> ELEMENT_DEFINITION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Binding>> BINDING_CACHE = new ConcurrentHashMap<>();
    private static final Comparator<Constraint> CONSTRAINT_COMPARATOR = new Comparator<Constraint>() {
        @Override
        public int compare(Constraint first, Constraint second) {
            return first.id().compareTo(second.id());
        }
    };

    private ProfileSupport() { }

    private static Map<String, Binding> computeBindingMap(String url) {
        StructureDefinition structureDefinition = getStructureDefinition(url);
        if (structureDefinition != null) {
            Objects.requireNonNull(structureDefinition.getSnapshot(), "StructureDefinition.snapshot element is required");
            Map<String, Binding> bindingMap = new LinkedHashMap<>();
            for (ElementDefinition elementDefinition : structureDefinition.getSnapshot().getElement()) {
                String path = elementDefinition.getPath().getValue();
                Binding binding = elementDefinition.getBinding();
                if (binding != null) {
                    bindingMap.put(path, binding);
                }
            }
            return Collections.unmodifiableMap(bindingMap);
        }
        return Collections.emptyMap();
    }

    private static List<Constraint> computeConstraints(StructureDefinition profile, Class<?> type) {
        Objects.requireNonNull(profile.getSnapshot(), "StructureDefinition.snapshot element is required");
        List<Constraint> constraints = new ArrayList<>();
        for (ElementDefinition elementDefinition : profile.getSnapshot().getElement()) {
            if (elementDefinition.getConstraint().isEmpty() || isSlice(elementDefinition)) {
                continue;
            }
            if (elementDefinition.getId().contains(":") && hasConstraintDifferential(elementDefinition)) {
                log.warning("Slice-specific constraints: " + getConstraintKeyDifferential(elementDefinition) + " found on element: " + elementDefinition.getId() + " are not supported");
                continue;
            }
            String path = elementDefinition.getPath().getValue();
            for (ElementDefinition.Constraint constraint : getConstraintDifferential(elementDefinition)) {
                constraints.add(createConstraint(path, constraint));
            }
        }
        Collections.sort(constraints, CONSTRAINT_COMPARATOR);
        ConstraintGenerator generator = new ConstraintGenerator(profile);
        constraints.addAll(generator.generate());
        return constraints;
    }

    public static boolean isSlice(ElementDefinition elementDefinition) {
        return elementDefinition.getSliceName() != null;
    }

    public static boolean isSliceDefinition(ElementDefinition elementDefinition) {
        return elementDefinition.getSlicing() != null;
    }

    public static Set<String> getReferencedProfileConstraintKeys(ElementDefinition elementDefinition) {
        Set<String> profileKeys = new HashSet<>();
        for (Type type : elementDefinition.getType()) {
            for (Canonical canonical : type.getProfile()) {
                String url = canonical.getValue();
                if (url == null) {
                    continue;
                }
                StructureDefinition profile = getProfile(url);
                if (profile == null || profile.getSnapshot() == null) {
                    continue;
                }
                profileKeys.addAll(getConstraintKeys(profile.getSnapshot().getElement().get(0)));
            }
        }
        return profileKeys;
    }

    public static boolean hasConstraintDifferential(ElementDefinition elementDefinition) {
        return !getConstraintKeyDifferential(elementDefinition).isEmpty();
    }

    public static List<ElementDefinition.Constraint> getConstraintDifferential(ElementDefinition elementDefinition) {
        return getConstraints(elementDefinition, getConstraintKeyDifferential(elementDefinition));
    }

    private static Set<String> getConstraintKeyDifferential(ElementDefinition elementDefinition) {
        if (elementDefinition.getConstraint().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> keys = new HashSet<>(getConstraintKeys(elementDefinition));
        keys.removeAll(getConstraintKeys(getBaseDefinition(elementDefinition)));
        keys.removeAll(getReferencedProfileConstraintKeys(elementDefinition));
        return keys;
    }

    private static List<ElementDefinition.Constraint> getConstraints(ElementDefinition elementDefinition, Set<String> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<ElementDefinition.Constraint> constraints = new ArrayList<>();
        for (ElementDefinition.Constraint constraint : elementDefinition.getConstraint()) {
            if (keys.contains(constraint.getKey().getValue())) {
                constraints.add(constraint);
            }
        }
        return constraints;
    }

    private static ElementDefinition getBaseDefinition(ElementDefinition elementDefinition) {
        String basePath = elementDefinition.getBase().getPath().getValue();
        return getElementDefinition(basePath);
    }

    private static Map<String, ElementDefinition> computeElementDefinitionMap(String url) {
        StructureDefinition structureDefinition = getStructureDefinition(url);
        if (structureDefinition != null) {
            Objects.requireNonNull(structureDefinition.getSnapshot(), "StructureDefinition.snapshot element is required");
            Map<String, ElementDefinition> elementDefinitionMap = new LinkedHashMap<>();
            for (ElementDefinition elementDefinition : structureDefinition.getSnapshot().getElement()) {
                String path = elementDefinition.getPath().getValue();
                elementDefinitionMap.put(path, elementDefinition);
            }
            return Collections.unmodifiableMap(elementDefinitionMap);
        }
        return Collections.emptyMap();
    }

    private static Constraint createConstraint(String path, ElementDefinition.Constraint constraint) {
        String id = constraint.getKey().getValue();
        String level = "error".equals(constraint.getSeverity().getValue()) ? Constraint.LEVEL_RULE : Constraint.LEVEL_WARNING;
        String location = path.contains(".") ? path.replace(".div", ".`div`").replace("[x]", "") : Constraint.LOCATION_BASE;
        String description = constraint.getHuman().getValue();
        String expression = constraint.getExpression().getValue();
        return createConstraint(id, level, location, description, expression, false, false);
    }

    public static Constraint createConstraint(String id, String level, String location, String description, String expression, boolean modelChecked, boolean generated) {
        return new Constraint() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Constraint.class;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String expression() {
                return expression;
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public String level() {
                return level;
            }

            @Override
            public String location() {
                return location;
            }

            @Override
            public boolean modelChecked() {
                return modelChecked;
            }

            @Override
            public boolean generated() {
                return generated;
            }

            @Override
            public String toString() {
                return new StringBuilder()
                    .append("Constraint [")
                    .append("id=").append(id).append(", ")
                    .append("level=").append(level).append(", ")
                    .append("location=").append(location).append(", ")
                    .append("description=").append(description).append(", ")
                    .append("expression=").append(expression).append(", ")
                    .append("modelChecked=").append(modelChecked).append(", ")
                    .append("generated=").append(generated)
                    .append("]")
                    .toString();
            }
        };
    }

    public static Binding getBinding(String path) {
        String url = getUrl(path);
        Map<String, Binding> bindingMap = getBindingMap(url);
        return bindingMap.get(path);
    }

    public static Map<String, Binding> getBindingMap(String url) {
        Map<String, Binding> bindingMap = BINDING_CACHE.get(url);
        if (bindingMap == null) {
            bindingMap = BINDING_CACHE.computeIfAbsent(url, ProfileSupport::computeBindingMap);
        }
        return bindingMap;
    }

    public static List<Constraint> getConstraints(List<String> urls, Class<?> type) {
        List<Constraint> constraints = new ArrayList<>();
        for (String url : urls) {
            StructureDefinition profile = getProfile(url, type);
            if (profile != null) {
                constraints.addAll(getConstraints(profile, type));
            }
        }
        return constraints;
    }

    public static List<Constraint> getConstraints(Resource resource) {
        return getConstraints(getResourceAssertedProfiles(resource), resource.getClass());
    }

    public static List<String> getResourceAssertedProfiles(Resource resource) {
        Meta meta = resource.getMeta();
        if (meta != null) {
            return meta.getProfile().stream()
                    .map(profile -> profile.getValue())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static boolean hasResourceAssertedProfile(Resource resource, StructureDefinition profile) {
        Meta meta = resource.getMeta();
        if (meta != null) {
            for (Canonical canonical : meta.getProfile()) {
                String value = canonical.getValue();

                if (value == null) {
                    continue;
                }

                String uri = value;
                String version = null;

                int index = value.indexOf("|");
                if (index != -1) {
                    uri = value.substring(0, index);
                    version = value.substring(index + 1);
                }

                if (uri.equals(profile.getUrl().getValue()) &&
                        (version == null || profile.getVersion() == null || version.equals(profile.getVersion().getValue()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Constraint> getConstraints(String url, Class<?> type) {
        return getConstraints(Collections.singletonList(url), type);
    }

    private static List<Constraint> getConstraints(StructureDefinition profile, Class<?> type) {
        String url = profile.getUrl().getValue();
        String version = profile.getVersion().getValue();
        String key = url + "|" + version;
        List<Constraint> constraints = CONSTRAINT_CACHE.get(key);
        if (constraints == null) {
            constraints = CONSTRAINT_CACHE.computeIfAbsent(key, k -> computeConstraints(profile, type));
        }
        return constraints;
    }

    public static ElementDefinition getElementDefinition(String path) {
        String url = getUrl(path);
        Map<String, ElementDefinition> elementDefinitionMap = getElementDefinitionMap(url);
        return elementDefinitionMap.get(path);
    }

    public static Map<String, ElementDefinition> getElementDefinitionMap(Class<?> type) {
        return getElementDefinitionMap(HL7_STRUCTURE_DEFINITION_URL_PREFIX + ModelSupport.getTypeName(type));
    }

    public static Map<String, ElementDefinition> getElementDefinitionMap(String url) {
        Map<String, ElementDefinition> elementDefinitionMap = ELEMENT_DEFINITION_CACHE.get(url);
        if (elementDefinitionMap == null) {
            elementDefinitionMap = ELEMENT_DEFINITION_CACHE.computeIfAbsent(url, ProfileSupport::computeElementDefinitionMap);
        }
        return elementDefinitionMap;
    }

    public static Set<String> getConstraintKeys(StructureDefinition structureDefinition) {
        Set<String> keys = new HashSet<>();
        Objects.requireNonNull(structureDefinition.getSnapshot(), "StructureDefinition.snapshot element is required");
        for (ElementDefinition elementDefinition : structureDefinition.getSnapshot().getElement()) {
            keys.addAll(getConstraintKeys(elementDefinition));
        }
        return keys;
    }

    public static Set<String> getConstraintKeys(ElementDefinition elementDefinition) {
        Set<String> keys = new HashSet<>();
        for (ElementDefinition.Constraint constraint : elementDefinition.getConstraint()) {
            keys.add(constraint.getKey().getValue());
        }
        return keys;
    }

    public static StructureDefinition getProfile(String url) {
        StructureDefinition structureDefinition = getStructureDefinition(url);
        return isProfile(structureDefinition) ? structureDefinition : null;
    }

    public static StructureDefinition getProfile(String url, Class<?> type) {
        StructureDefinition profile = getProfile(url);
        return (profile != null && isApplicable(profile, type)) ? profile : null;
    }

    public static StructureDefinition getStructureDefinition(Class<?> modelClass) {
        return getStructureDefinition(HL7_STRUCTURE_DEFINITION_URL_PREFIX + ModelSupport.getTypeName(modelClass));
    }

    public static StructureDefinition getStructureDefinition(String url) {
        return FHIRRegistry.getInstance().getResource(url, StructureDefinition.class);
    }

    private static String getUrl(String path) {
        int index = path.indexOf(".");
        String typeName = (index != -1) ? path.substring(0, index) : path;
        return HL7_STRUCTURE_DEFINITION_URL_PREFIX + typeName;
    }

    public static boolean isApplicable(StructureDefinition profile, Class<?> type) {
        if (profile == null || type == null) {
            return false;
        }
        return isApplicable(profile, ModelSupport.getTypeNames(type));
    }

    private static boolean isApplicable(StructureDefinition profile, Set<String> typeNames) {
        String type = profile.getType().getValue();
        return typeNames.contains(type.substring(type.lastIndexOf("/") + 1));
    }

    public static boolean isProfile(StructureDefinition structureDefinition) {
        return structureDefinition != null && TypeDerivationRule.CONSTRAINT.equals(structureDefinition.getDerivation());
    }
}