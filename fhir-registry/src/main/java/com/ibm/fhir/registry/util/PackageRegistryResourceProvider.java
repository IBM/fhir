/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.registry.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.resource.SearchParameter;
import com.ibm.fhir.model.resource.StructureDefinition;
import com.ibm.fhir.model.type.code.ResourceType;
import com.ibm.fhir.model.type.code.ResourceType.ValueSet;
import com.ibm.fhir.model.util.ModelSupport;
import com.ibm.fhir.registry.resource.FHIRRegistryResource;
import com.ibm.fhir.registry.resource.FHIRRegistryResource.Version;
import com.ibm.fhir.registry.spi.FHIRRegistryResourceProvider;

/**
 * A static registry resource provider that is loaded from an NPM package as specified at
 * <a href="https://confluence.hl7.org/pages/viewpage.action?pageId=35718629">https://confluence.hl7.org/pages/viewpage.action?pageId=35718629</a>
 *
 * <p>This implementation caches registry resources by resource type and url
 */
public abstract class PackageRegistryResourceProvider implements FHIRRegistryResourceProvider {
    private static final Logger log = Logger.getLogger(PackageRegistryResourceProvider.class.getName());

    private static final String HL7_STRUCTURE_DEFINITION_URL_PREFIX = "http://hl7.org/fhir/StructureDefinition/";

    protected final Collection<FHIRRegistryResource> registryResources;
    protected final Map<Class<? extends Resource>, Map<String, List<FHIRRegistryResource>>> registryResourceMap;
    protected final Map<String, List<FHIRRegistryResource>> profileResourceMap;

    public PackageRegistryResourceProvider() {
        registryResources = FHIRRegistryUtil.getRegistryResources(getPackageId());
        registryResourceMap = buildRegistryResourceMap();
        profileResourceMap = buildProfileResourceMap();
    }

    /**
     * Get the package id for this package registry resource provider (e.g. hl7.fhir.us.core)
     *
     * @return
     *     the package id for this package registry resource provider
     */
    public abstract String getPackageId();

    @Override
    public FHIRRegistryResource getRegistryResource(Class<? extends Resource> resourceType, String url, String version) {
        Objects.requireNonNull(resourceType);
        Objects.requireNonNull(url);
        List<FHIRRegistryResource> registryResources = registryResourceMap.getOrDefault(resourceType, Collections.emptyMap())
                .getOrDefault(url, Collections.emptyList());
        if (!registryResources.isEmpty()) {
            if (version != null) {
                Version v = Version.from(version);
                for (FHIRRegistryResource resource : registryResources) {
                    if (resource.getVersion().equals(v)) {
                        return resource;
                    }
                }
                log.warning("Unable to find resource: " + url + " with version: " + version);
            } else {
                return registryResources.get(registryResources.size() - 1);
            }
        }
        return null;
    }

    @Override
    public Collection<FHIRRegistryResource> getRegistryResources(Class<? extends Resource> resourceType) {
        return registryResourceMap.getOrDefault(resourceType, Collections.emptyMap()).entrySet().stream()
                .map(entry -> entry.getValue())
                .flatMap(List::stream)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public Collection<FHIRRegistryResource> getRegistryResources() {
        return Collections.unmodifiableCollection(registryResources);
    }

    @Override
    public Collection<FHIRRegistryResource> getProfileResources() {
        List<FHIRRegistryResource> profilesForAll = new ArrayList<>();
        for (ValueSet type : ResourceType.ValueSet.values()) {
            profilesForAll.addAll(getProfileResources(type.value()));
        }
        return Collections.unmodifiableList(profilesForAll);
    }

    @Override
    public Collection<FHIRRegistryResource> getProfileResources(String type) {
        Objects.requireNonNull(type);
        return Collections.unmodifiableList(profileResourceMap.getOrDefault(type, Collections.emptyList()));
    }

    @Override
    public Collection<FHIRRegistryResource> getSearchParameterResources(String type) {
        Objects.requireNonNull(type);
        return registryResourceMap.getOrDefault(SearchParameter.class, Collections.emptyMap()).entrySet().stream()
                .map(entry -> entry.getValue())
                .flatMap(List::stream)
                .filter(registryResource -> type.equals(registryResource.getType()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    private Map<Class<? extends Resource>, Map<String, List<FHIRRegistryResource>>> buildRegistryResourceMap() {
        Map<Class<? extends Resource>, Map<String, List<FHIRRegistryResource>>> registryResourceMap = new HashMap<>();
        for (FHIRRegistryResource registryResource : registryResources) {
            Map<String, List<FHIRRegistryResource>> map = registryResourceMap.computeIfAbsent(registryResource.getResourceType(), k -> new HashMap<>());
            List<FHIRRegistryResource> list = map.computeIfAbsent(registryResource.getUrl(), k -> new ArrayList<>());
            list.add(registryResource);
            Collections.sort(list);
        }
        return registryResourceMap;
    }

    private Map<String, List<FHIRRegistryResource>> buildProfileResourceMap() {
        Map<String, List<FHIRRegistryResource>> profileResourceMap = new HashMap<>();
        for (FHIRRegistryResource registryResource : getRegistryResources(StructureDefinition.class)) {
            if (!isProfileResource(registryResource)) {
                continue;
            }
            profileResourceMap.computeIfAbsent(registryResource.getType(), k -> new ArrayList<>()).add(registryResource);
        }
        return profileResourceMap;
    }

    private boolean isProfileResource(FHIRRegistryResource registryResource) {
        if (!"resource".equals(registryResource.getKind()) || registryResource.getType() == null) {
            return false;
        }
        String url = registryResource.getUrl();
        if (url.startsWith(HL7_STRUCTURE_DEFINITION_URL_PREFIX)) {
            String name = url.substring(HL7_STRUCTURE_DEFINITION_URL_PREFIX.length());
            if (ModelSupport.isResourceType(name)) {
                return false;
            }
        }
        return true;
    }
}
