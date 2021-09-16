/*
 * (C) Copyright IBM Corp. 2017, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.dto;

import com.ibm.fhir.persistence.exception.FHIRPersistenceException;

/**
 * A search parameter value extracted from a resource and ready to store / index for search
 */
public abstract class ExtractedParameterValue implements Comparable<ExtractedParameterValue> {

    // The name (code) of this parameter
    private String name;

    // A subset of search params are also stored at the whole-system level
    private boolean wholeSystem;

    // The resource type associated with this parameter
    private String resourceType;

    // URL and version of search parameter
    private String url;
    private String version;

    /**
     * Protected constructor
     */
    protected ExtractedParameterValue() {
    }

    /**
     * Getter for the parameter's resource type
     * @return
     */
    public String getResourceType() {
        return this.resourceType;
    }

    /**
     * Setter for the parameter's resource type
     * @param resourceType
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * We know our type, so we can call the correct method on the visitor
     */
    public abstract void accept(ExtractedParameterValueVisitor visitor) throws FHIRPersistenceException;

    /**
     * @return the wholeSystem
     */
    public boolean isWholeSystem() {
        return wholeSystem;
    }

    /**
     * @param wholeSystem the wholeSystem to set
     */
    public void setWholeSystem(boolean wholeSystem) {
        this.wholeSystem = wholeSystem;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int compareTo(ExtractedParameterValue o) {
        int retVal;
        String thisName = this.getName();
        String otherName = o.getName();
        if (thisName != null || otherName != null) {
            if (thisName == null) {
                return -1;
            } else if (otherName == null) {
                return 1;
            }
            retVal = thisName.compareTo(otherName);
            if (retVal != 0) {
                return retVal;
            }
        }
        String thisUrl = this.getUrl();
        String otherUrl = o.getUrl();
        if (thisUrl != null || otherUrl != null) {
            if (thisUrl == null) {
                return -1;
            } else if (otherUrl == null) {
                return 1;
            }
            retVal = thisUrl.compareTo(otherUrl);
            if (retVal != 0) {
                return retVal;
            }
        }
        String thisVersion = this.getVersion();
        String otherVersion = o.getVersion();
        if (thisVersion != null || otherVersion != null) {
            if (thisVersion == null) {
                return -1;
            } else if (otherVersion == null) {
                return 1;
            }
            retVal = thisVersion.compareTo(otherVersion);
            if (retVal != 0) {
                return retVal;
            }
        }
        String thisClass = this.getClass().getName();
        String otherClass = o.getClass().getName();
        if (thisClass != null || otherClass != null) {
            if (thisClass == null) {
                return -1;
            } else if (otherClass == null) {
                return 1;
            }
            retVal = thisClass.compareTo(otherClass);
            if (retVal != 0) {
                return retVal;
            }
        }
        return compareToInner(o);
    }

    /**
     * Additional extracted parameter value comparisions when the same class.
     * @param o an extracted parameter value to compare to
     * @return a negative integer, zero, or a positive integer as this extracted parameter value
     * is less than, equal to, or greater than the specified extracted parameter value.
     */
    protected abstract int compareToInner(ExtractedParameterValue o);

}