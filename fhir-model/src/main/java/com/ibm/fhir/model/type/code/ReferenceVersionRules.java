/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.model.type.code;

import com.ibm.fhir.model.annotation.System;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Extension;
import com.ibm.fhir.model.type.String;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Generated;

@Generated("com.ibm.fhir.tools.CodeGenerator")
@System("http://hl7.org/fhir/reference-version-rules")
public class ReferenceVersionRules extends Code {
    /**
     * Either Specific or independent
     * 
     * <p>The reference may be either version independent or version specific.
     */
    public static final ReferenceVersionRules EITHER = ReferenceVersionRules.builder().value(ValueSet.EITHER).build();

    /**
     * Version independent
     * 
     * <p>The reference must be version independent.
     */
    public static final ReferenceVersionRules INDEPENDENT = ReferenceVersionRules.builder().value(ValueSet.INDEPENDENT).build();

    /**
     * Version Specific
     * 
     * <p>The reference must be version specific.
     */
    public static final ReferenceVersionRules SPECIFIC = ReferenceVersionRules.builder().value(ValueSet.SPECIFIC).build();

    private volatile int hashCode;

    private ReferenceVersionRules(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    public static ReferenceVersionRules of(ValueSet value) {
        switch (value) {
        case EITHER:
            return EITHER;
        case INDEPENDENT:
            return INDEPENDENT;
        case SPECIFIC:
            return SPECIFIC;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    public static ReferenceVersionRules of(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    public static String string(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    public static Code code(java.lang.String value) {
        return of(ValueSet.from(value));
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
        ReferenceVersionRules other = (ReferenceVersionRules) obj;
        return Objects.equals(id, other.id) && Objects.equals(extension, other.extension) && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = Objects.hash(id, extension, value);
            hashCode = result;
        }
        return result;
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.id(id);
        builder.extension(extension);
        builder.value(value);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Code.Builder {
        private Builder() {
            super();
        }

        @Override
        public Builder id(java.lang.String id) {
            return (Builder) super.id(id);
        }

        @Override
        public Builder extension(Extension... extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder extension(Collection<Extension> extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder value(java.lang.String value) {
            return (value != null) ? (Builder) super.value(ValueSet.from(value).value()) : this;
        }

        public Builder value(ValueSet value) {
            return (value != null) ? (Builder) super.value(value.value()) : this;
        }

        @Override
        public ReferenceVersionRules build() {
            return new ReferenceVersionRules(this);
        }
    }

    public enum ValueSet {
        /**
         * Either Specific or independent
         * 
         * <p>The reference may be either version independent or version specific.
         */
        EITHER("either"),

        /**
         * Version independent
         * 
         * <p>The reference must be version independent.
         */
        INDEPENDENT("independent"),

        /**
         * Version Specific
         * 
         * <p>The reference must be version specific.
         */
        SPECIFIC("specific");

        private final java.lang.String value;

        ValueSet(java.lang.String value) {
            this.value = value;
        }

        public java.lang.String value() {
            return value;
        }

        public static ValueSet from(java.lang.String value) {
            for (ValueSet c : ValueSet.values()) {
                if (c.value.equals(value)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }
}
