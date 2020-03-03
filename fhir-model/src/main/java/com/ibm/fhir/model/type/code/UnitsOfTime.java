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
@System("http://unitsofmeasure.org")
public class UnitsOfTime extends Code {
    /**
     * second
     */
    public static final UnitsOfTime S = UnitsOfTime.builder().value(ValueSet.S).build();

    /**
     * minute
     */
    public static final UnitsOfTime MIN = UnitsOfTime.builder().value(ValueSet.MIN).build();

    /**
     * hour
     */
    public static final UnitsOfTime H = UnitsOfTime.builder().value(ValueSet.H).build();

    /**
     * day
     */
    public static final UnitsOfTime D = UnitsOfTime.builder().value(ValueSet.D).build();

    /**
     * week
     */
    public static final UnitsOfTime WK = UnitsOfTime.builder().value(ValueSet.WK).build();

    /**
     * month
     */
    public static final UnitsOfTime MO = UnitsOfTime.builder().value(ValueSet.MO).build();

    /**
     * year
     */
    public static final UnitsOfTime A = UnitsOfTime.builder().value(ValueSet.A).build();

    private volatile int hashCode;

    private UnitsOfTime(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    public static UnitsOfTime of(ValueSet value) {
        switch (value) {
        case S:
            return S;
        case MIN:
            return MIN;
        case H:
            return H;
        case D:
            return D;
        case WK:
            return WK;
        case MO:
            return MO;
        case A:
            return A;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    public static UnitsOfTime of(java.lang.String value) {
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
        UnitsOfTime other = (UnitsOfTime) obj;
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
        public UnitsOfTime build() {
            return new UnitsOfTime(this);
        }
    }

    public enum ValueSet {
        /**
         * second
         */
        S("s"),

        /**
         * minute
         */
        MIN("min"),

        /**
         * hour
         */
        H("h"),

        /**
         * day
         */
        D("d"),

        /**
         * week
         */
        WK("wk"),

        /**
         * month
         */
        MO("mo"),

        /**
         * year
         */
        A("a");

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
