/*
 * (C) Copyright IBM Corp. 2019, 2021
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

@System("http://hl7.org/fhir/group-measure")
@Generated("com.ibm.fhir.tools.CodeGenerator")
public class GroupMeasure extends Code {
    /**
     * Mean
     * 
     * <p>Aggregated using Mean of participant values.
     */
    public static final GroupMeasure MEAN = GroupMeasure.builder().value(Value.MEAN).build();

    /**
     * Median
     * 
     * <p>Aggregated using Median of participant values.
     */
    public static final GroupMeasure MEDIAN = GroupMeasure.builder().value(Value.MEDIAN).build();

    /**
     * Mean of Study Means
     * 
     * <p>Aggregated using Mean of study mean values.
     */
    public static final GroupMeasure MEAN_OF_MEAN = GroupMeasure.builder().value(Value.MEAN_OF_MEAN).build();

    /**
     * Mean of Study Medins
     * 
     * <p>Aggregated using Mean of study median values.
     */
    public static final GroupMeasure MEAN_OF_MEDIAN = GroupMeasure.builder().value(Value.MEAN_OF_MEDIAN).build();

    /**
     * Median of Study Means
     * 
     * <p>Aggregated using Median of study mean values.
     */
    public static final GroupMeasure MEDIAN_OF_MEAN = GroupMeasure.builder().value(Value.MEDIAN_OF_MEAN).build();

    /**
     * Median of Study Medians
     * 
     * <p>Aggregated using Median of study median values.
     */
    public static final GroupMeasure MEDIAN_OF_MEDIAN = GroupMeasure.builder().value(Value.MEDIAN_OF_MEDIAN).build();

    private volatile int hashCode;

    private GroupMeasure(Builder builder) {
        super(builder);
    }

    /**
     * Get the value of this GroupMeasure as an enum constant.
     * @deprecated replaced by {@link #getValueAsEnum()}
     */
    @Deprecated
    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    /**
     * Get the value of this GroupMeasure as an enum constant.
     */
    public Value getValueAsEnum() {
        return (value != null) ? Value.from(value) : null;
    }

    /**
     * Factory method for creating GroupMeasure objects from a passed enum value.
     * @deprecated replaced by {@link #of(Value)}
     */
    @Deprecated
    public static GroupMeasure of(ValueSet value) {
        switch (value) {
        case MEAN:
            return MEAN;
        case MEDIAN:
            return MEDIAN;
        case MEAN_OF_MEAN:
            return MEAN_OF_MEAN;
        case MEAN_OF_MEDIAN:
            return MEAN_OF_MEDIAN;
        case MEDIAN_OF_MEAN:
            return MEDIAN_OF_MEAN;
        case MEDIAN_OF_MEDIAN:
            return MEDIAN_OF_MEDIAN;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    /**
     * Factory method for creating GroupMeasure objects from a passed enum value.
     */
    public static GroupMeasure of(Value value) {
        switch (value) {
        case MEAN:
            return MEAN;
        case MEDIAN:
            return MEDIAN;
        case MEAN_OF_MEAN:
            return MEAN_OF_MEAN;
        case MEAN_OF_MEDIAN:
            return MEAN_OF_MEDIAN;
        case MEDIAN_OF_MEAN:
            return MEDIAN_OF_MEAN;
        case MEDIAN_OF_MEDIAN:
            return MEDIAN_OF_MEDIAN;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    /**
     * Factory method for creating GroupMeasure objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static GroupMeasure of(java.lang.String value) {
        return of(Value.from(value));
    }

    /**
     * Inherited factory method for creating GroupMeasure objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static String string(java.lang.String value) {
        return of(Value.from(value));
    }

    /**
     * Inherited factory method for creating GroupMeasure objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static Code code(java.lang.String value) {
        return of(Value.from(value));
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
        GroupMeasure other = (GroupMeasure) obj;
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
            return (value != null) ? (Builder) super.value(Value.from(value).value()) : this;
        }

        /**
         * @deprecated replaced by  {@link #value(Value)}
         */
        @Deprecated
        public Builder value(ValueSet value) {
            return (value != null) ? (Builder) super.value(value.value()) : this;
        }

        /**
         * Primitive value for code
         * 
         * @param value
         *     An enum constant for GroupMeasure
         * 
         * @return
         *     A reference to this Builder instance
         */
        public Builder value(Value value) {
            return (value != null) ? (Builder) super.value(value.value()) : this;
        }

        @Override
        public GroupMeasure build() {
            return new GroupMeasure(this);
        }
    }

    @Deprecated
    public enum ValueSet {
        /**
         * Mean
         * 
         * <p>Aggregated using Mean of participant values.
         */
        MEAN("mean"),

        /**
         * Median
         * 
         * <p>Aggregated using Median of participant values.
         */
        MEDIAN("median"),

        /**
         * Mean of Study Means
         * 
         * <p>Aggregated using Mean of study mean values.
         */
        MEAN_OF_MEAN("mean-of-mean"),

        /**
         * Mean of Study Medins
         * 
         * <p>Aggregated using Mean of study median values.
         */
        MEAN_OF_MEDIAN("mean-of-median"),

        /**
         * Median of Study Means
         * 
         * <p>Aggregated using Median of study mean values.
         */
        MEDIAN_OF_MEAN("median-of-mean"),

        /**
         * Median of Study Medians
         * 
         * <p>Aggregated using Median of study median values.
         */
        MEDIAN_OF_MEDIAN("median-of-median");

        private final java.lang.String value;

        ValueSet(java.lang.String value) {
            this.value = value;
        }

        /**
         * @return
         *     The java.lang.String value of the code represented by this enum
         */
        public java.lang.String value() {
            return value;
        }

        /**
         * Factory method for creating GroupMeasure.Value values from a passed string value.
         * 
         * @param value
         *     A string that matches one of the allowed code values
         * @throws IllegalArgumentException
         *     If the passed string cannot be parsed into an allowed code value
         */
        public static ValueSet from(java.lang.String value) {
            for (ValueSet c : ValueSet.values()) {
                if (c.value.equals(value)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    public enum Value {
        /**
         * Mean
         * 
         * <p>Aggregated using Mean of participant values.
         */
        MEAN("mean"),

        /**
         * Median
         * 
         * <p>Aggregated using Median of participant values.
         */
        MEDIAN("median"),

        /**
         * Mean of Study Means
         * 
         * <p>Aggregated using Mean of study mean values.
         */
        MEAN_OF_MEAN("mean-of-mean"),

        /**
         * Mean of Study Medins
         * 
         * <p>Aggregated using Mean of study median values.
         */
        MEAN_OF_MEDIAN("mean-of-median"),

        /**
         * Median of Study Means
         * 
         * <p>Aggregated using Median of study mean values.
         */
        MEDIAN_OF_MEAN("median-of-mean"),

        /**
         * Median of Study Medians
         * 
         * <p>Aggregated using Median of study median values.
         */
        MEDIAN_OF_MEDIAN("median-of-median");

        private final java.lang.String value;

        Value(java.lang.String value) {
            this.value = value;
        }

        /**
         * @return
         *     The java.lang.String value of the code represented by this enum
         */
        public java.lang.String value() {
            return value;
        }

        /**
         * Factory method for creating GroupMeasure.Value values from a passed string value.
         * 
         * @param value
         *     A string that matches one of the allowed code values
         * @return
         *     The corresponding GroupMeasure.Value or null if a null value was passed
         * @throws IllegalArgumentException
         *     If the passed string is not null and cannot be parsed into an allowed code value
         */
        public static Value from(java.lang.String value) {
            if (value == null) {
                return null;
            }
            switch (value) {
            case "mean":
                return MEAN;
            case "median":
                return MEDIAN;
            case "mean-of-mean":
                return MEAN_OF_MEAN;
            case "mean-of-median":
                return MEAN_OF_MEDIAN;
            case "median-of-mean":
                return MEDIAN_OF_MEAN;
            case "median-of-median":
                return MEDIAN_OF_MEDIAN;
            default:
                throw new IllegalArgumentException(value);
            }
        }
    }
}
