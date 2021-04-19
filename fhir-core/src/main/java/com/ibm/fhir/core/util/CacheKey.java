/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.core.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * A general purpose cache key class used to created composite keys
 */
public class CacheKey {
    private final Object[] values;
    private final int hashCode;

    private CacheKey(Object[] values) {
        this.values = Objects.requireNonNull(values, "values");
        hashCode = Arrays.deepHashCode(values);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CacheKey other = (CacheKey) obj;
        return Arrays.deepEquals(values, other.values);
    }

    /**
     * A factory method for creating CacheKey instances from a one or more values
     *
     * @param values
     *     the values
     * @return
     *     the CacheKey instance
     */
    public static CacheKey key(Object... values) {
        return new CacheKey(values);
    }
}
