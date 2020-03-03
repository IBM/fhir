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

@System("http://hl7.org/fhir/slotstatus")
@Generated("com.ibm.fhir.tools.CodeGenerator")
public class SlotStatus extends Code {
    /**
     * Busy
     * 
     * <p>Indicates that the time interval is busy because one or more events have been scheduled for that interval.
     */
    public static final SlotStatus BUSY = SlotStatus.builder().value(ValueSet.BUSY).build();

    /**
     * Free
     * 
     * <p>Indicates that the time interval is free for scheduling.
     */
    public static final SlotStatus FREE = SlotStatus.builder().value(ValueSet.FREE).build();

    /**
     * Busy (Unavailable)
     * 
     * <p>Indicates that the time interval is busy and that the interval cannot be scheduled.
     */
    public static final SlotStatus BUSY_UNAVAILABLE = SlotStatus.builder().value(ValueSet.BUSY_UNAVAILABLE).build();

    /**
     * Busy (Tentative)
     * 
     * <p>Indicates that the time interval is busy because one or more events have been tentatively scheduled for that 
     * interval.
     */
    public static final SlotStatus BUSY_TENTATIVE = SlotStatus.builder().value(ValueSet.BUSY_TENTATIVE).build();

    /**
     * Entered in error
     * 
     * <p>This instance should not have been part of this patient's medical record.
     */
    public static final SlotStatus ENTERED_IN_ERROR = SlotStatus.builder().value(ValueSet.ENTERED_IN_ERROR).build();

    private volatile int hashCode;

    private SlotStatus(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    public static SlotStatus of(ValueSet value) {
        switch (value) {
        case BUSY:
            return BUSY;
        case FREE:
            return FREE;
        case BUSY_UNAVAILABLE:
            return BUSY_UNAVAILABLE;
        case BUSY_TENTATIVE:
            return BUSY_TENTATIVE;
        case ENTERED_IN_ERROR:
            return ENTERED_IN_ERROR;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    public static SlotStatus of(java.lang.String value) {
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
        SlotStatus other = (SlotStatus) obj;
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
        public SlotStatus build() {
            return new SlotStatus(this);
        }
    }

    public enum ValueSet {
        /**
         * Busy
         * 
         * <p>Indicates that the time interval is busy because one or more events have been scheduled for that interval.
         */
        BUSY("busy"),

        /**
         * Free
         * 
         * <p>Indicates that the time interval is free for scheduling.
         */
        FREE("free"),

        /**
         * Busy (Unavailable)
         * 
         * <p>Indicates that the time interval is busy and that the interval cannot be scheduled.
         */
        BUSY_UNAVAILABLE("busy-unavailable"),

        /**
         * Busy (Tentative)
         * 
         * <p>Indicates that the time interval is busy because one or more events have been tentatively scheduled for that 
         * interval.
         */
        BUSY_TENTATIVE("busy-tentative"),

        /**
         * Entered in error
         * 
         * <p>This instance should not have been part of this patient's medical record.
         */
        ENTERED_IN_ERROR("entered-in-error");

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
