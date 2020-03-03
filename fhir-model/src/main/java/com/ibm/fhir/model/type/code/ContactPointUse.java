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
@System("http://hl7.org/fhir/contact-point-use")
public class ContactPointUse extends Code {
    /**
     * Home
     * 
     * <p>A communication contact point at a home; attempted contacts for business purposes might intrude privacy and chances 
     * are one will contact family or other household members instead of the person one wishes to call. Typically used with 
     * urgent cases, or if no other contacts are available.
     */
    public static final ContactPointUse HOME = ContactPointUse.builder().value(ValueSet.HOME).build();

    /**
     * Work
     * 
     * <p>An office contact point. First choice for business related contacts during business hours.
     */
    public static final ContactPointUse WORK = ContactPointUse.builder().value(ValueSet.WORK).build();

    /**
     * Temp
     * 
     * <p>A temporary contact point. The period can provide more detailed information.
     */
    public static final ContactPointUse TEMP = ContactPointUse.builder().value(ValueSet.TEMP).build();

    /**
     * Old
     * 
     * <p>This contact point is no longer in use (or was never correct, but retained for records).
     */
    public static final ContactPointUse OLD = ContactPointUse.builder().value(ValueSet.OLD).build();

    /**
     * Mobile
     * 
     * <p>A telecommunication device that moves and stays with its owner. May have characteristics of all other use codes, 
     * suitable for urgent matters, not the first choice for routine business.
     */
    public static final ContactPointUse MOBILE = ContactPointUse.builder().value(ValueSet.MOBILE).build();

    private volatile int hashCode;

    private ContactPointUse(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    public static ContactPointUse of(ValueSet value) {
        switch (value) {
        case HOME:
            return HOME;
        case WORK:
            return WORK;
        case TEMP:
            return TEMP;
        case OLD:
            return OLD;
        case MOBILE:
            return MOBILE;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    public static ContactPointUse of(java.lang.String value) {
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
        ContactPointUse other = (ContactPointUse) obj;
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
        public ContactPointUse build() {
            return new ContactPointUse(this);
        }
    }

    public enum ValueSet {
        /**
         * Home
         * 
         * <p>A communication contact point at a home; attempted contacts for business purposes might intrude privacy and chances 
         * are one will contact family or other household members instead of the person one wishes to call. Typically used with 
         * urgent cases, or if no other contacts are available.
         */
        HOME("home"),

        /**
         * Work
         * 
         * <p>An office contact point. First choice for business related contacts during business hours.
         */
        WORK("work"),

        /**
         * Temp
         * 
         * <p>A temporary contact point. The period can provide more detailed information.
         */
        TEMP("temp"),

        /**
         * Old
         * 
         * <p>This contact point is no longer in use (or was never correct, but retained for records).
         */
        OLD("old"),

        /**
         * Mobile
         * 
         * <p>A telecommunication device that moves and stays with its owner. May have characteristics of all other use codes, 
         * suitable for urgent matters, not the first choice for routine business.
         */
        MOBILE("mobile");

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
