/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
 
package com.ibm.fhir.persistence.payload;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.persistence.FHIRPersistenceSupport;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;


/**
 * Strategy for reading a payload object with optional compression
 */
public class PayloadReaderImpl implements PayloadReader {
    // Is the input stream compressed?
    private final boolean uncompress;
    
    // Subset elements when parsing the Resource
    private final List<String> elements;

    /**
     * Public constructor
     * @param uncompress
     * @param elements
     */
    public PayloadReaderImpl(boolean uncompress, List<String> elements) {
        this.uncompress = uncompress;
        this.elements = elements;
    }

    @Override
    public <T extends Resource> T read(Class<T> resourceType, InputStream inputStream) throws FHIRPersistenceException {
        try {
            return FHIRPersistenceSupport.parse(resourceType, inputStream, elements, uncompress);
        } catch (IOException | FHIRParserException x) {
            throw new FHIRPersistenceException("Error reading resource", x);
        }
    }
}
