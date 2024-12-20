/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines a custom serialization format for a timestamp.
 */
public final class TimestampFormatTrait extends StringTrait {
    public static final String EPOCH_SECONDS = "epoch-seconds";
    public static final String DATE_TIME = "date-time";
    public static final String HTTP_DATE = "http-date";
    public static final ShapeId ID = ShapeId.from("smithy.api#timestampFormat");

    public TimestampFormatTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public TimestampFormatTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    /**
     * Gets the {@code timestampFormat} value as a {@code Format} enum.
     *
     * @return Returns the {@code Format} enum.
     */
    public Format getFormat() {
        return Format.fromString(getValue());
    }

    public static final class Provider extends StringTrait.Provider<TimestampFormatTrait> {
        public Provider() {
            super(ID, TimestampFormatTrait::new);
        }
    }

    /**
     * The known {@code timestampFormat} values.
     */
    public enum Format {
        EPOCH_SECONDS(TimestampFormatTrait.EPOCH_SECONDS),
        DATE_TIME(TimestampFormatTrait.DATE_TIME),
        HTTP_DATE(TimestampFormatTrait.HTTP_DATE),
        UNKNOWN("unknown");

        private String value;

        Format(String value) {
            this.value = value;
        }

        /**
         * Create a {@code Format} from a string that would appear in a model.
         *
         * <p>Any unknown value is returned as {@code Unknown}.
         *
         * @param value Value from a trait or model.
         * @return Returns the Format enum value.
         */
        public static Format fromString(String value) {
            for (Format format : values()) {
                if (format.value.equals(value)) {
                    return format;
                }
            }

            return UNKNOWN;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
