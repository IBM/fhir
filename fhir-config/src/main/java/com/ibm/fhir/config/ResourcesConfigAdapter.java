/*
 * (C) Copyright IBM Corp. 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.fhir.config.PropertyGroup.PropertyEntry;
import com.ibm.fhir.core.FHIRVersionParam;
import com.ibm.fhir.core.util.ResourceTypeHelper;

/**
 * An abstraction for the ibm-fhir-server fhirServer/resources property group
 */
public class ResourcesConfigAdapter {
    public static final Logger log = Logger.getLogger(ResourcesConfigAdapter.class.getName());

    private final Set<String> supportedTypes;
    private final Map<Interaction, Set<String>> typesByInteraction = new HashMap<>();

    public ResourcesConfigAdapter(PropertyGroup resourcesConfig, FHIRVersionParam fhirVersion) throws Exception {
        supportedTypes = computeSupportedResourceTypes(resourcesConfig, fhirVersion);

        if (resourcesConfig == null) {
            for (Interaction interaction : Interaction.values()) {
                typesByInteraction.put(interaction, supportedTypes);
            }
            return;
        }

        for (String resourceType : supportedTypes) {
            List<String> interactions = resourcesConfig.getStringListProperty(resourceType + "/" + FHIRConfiguration.PROPERTY_FIELD_RESOURCES_INTERACTIONS);
            if (interactions == null) {
                interactions = resourcesConfig.getStringListProperty("Resource/" + FHIRConfiguration.PROPERTY_FIELD_RESOURCES_INTERACTIONS);
            }

            if (interactions == null) {
                for (Interaction interaction : Interaction.values()) {
                    typesByInteraction.computeIfAbsent(interaction, k -> new LinkedHashSet<>()).add(resourceType);
                }
                continue;
            }

            for (String interactionString : interactions) {
                Interaction interaction = Interaction.from(interactionString);
                typesByInteraction.computeIfAbsent(interaction, k -> new LinkedHashSet<>()).add(resourceType);
            }
        }
    }

    /**
     * @return an immutable, non-null set of supported resource types for the given fhirVersion
     * @throws Exception
     */
    public Set<String> getSupportedResourceTypes() {
        return supportedTypes;
    }

    /**
     * @return an immutable, non-null set of resource types that are configured for the given interaction and fhirVersion
     */
    public Set<String> getSupportedResourceTypes(Interaction interaction) {
        return typesByInteraction.get(interaction);
    }

    /**
     * Construct the list of supported resource types from the passed configuration and fhirVersion
     *
     * @param resourcesConfig
     * @param fhirVersion
     * @return
     * @throws Exception
     */
    private Set<String> computeSupportedResourceTypes(PropertyGroup resourcesConfig, FHIRVersionParam fhirVersion) throws Exception {
        Set<String> applicableTypes = ResourceTypeHelper.getResourceTypesFor(fhirVersion);

        if (resourcesConfig == null || resourcesConfig.getBooleanProperty("open", true)) {
            return applicableTypes;
        }

        Set<String> result = new LinkedHashSet<String>();
        for (PropertyEntry rsrcsEntry : resourcesConfig.getProperties()) {
            String name = rsrcsEntry.getName();

            // Ensure we skip over the special property "open"
            // and skip the abstract types Resource and DomainResource
            if (FHIRConfiguration.PROPERTY_FIELD_RESOURCES_OPEN.equals(name) ||
                    "Resource".equals(name) ||
                    "DomainResource".equals(name)) {
                continue;
            }

            if (applicableTypes.contains(name)) {
                result.add(name);
            } else if (log.isLoggable(Level.FINE)) {
                log.fine("Configured resource type '" + name + "' is not valid "
                        + "or not applicable for fhirVersion " + fhirVersion.value());
            }
        }

        return Collections.unmodifiableSet(result);
    }
}
