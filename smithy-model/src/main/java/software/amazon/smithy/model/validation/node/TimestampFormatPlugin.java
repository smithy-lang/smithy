/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.validation.node;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that timestamp shapes contain values that are compatible with their
 * timestampFormat traits or contain values that are numbers or an RFC 3339
 * date-time production.
 */
public final class TimestampFormatPlugin implements NodeValidatorPlugin {
    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_Z = DateTimeFormatter.ISO_INSTANT;
    private static final Logger LOGGER = Logger.getLogger(TimestampFormatPlugin.class.getName());

    @Override
    public List<String> apply(Shape shape, Node value, Model model) {
        if (shape instanceof TimestampShape) {
            return validate(shape, shape.getTrait(TimestampFormatTrait.class).orElse(null), value);
        } else if (shape instanceof MemberShape && shape.getTrait(TimestampFormatTrait.class).isPresent()) {
            // Only perform timestamp format validation on a member when it references
            // a timestamp shape and the member has an explicit timestampFormat trait.
            return validate(shape, shape.getTrait(TimestampFormatTrait.class).get(), value);
        } else {
            // Ignore when not a timestamp or member that targets a timestamp.
            return ListUtils.of();
        }
    }

    private List<String> validate(Shape shape, TimestampFormatTrait trait, Node value) {
        if (trait == null) {
            return defaultValidation(shape, value);
        }

        switch (trait.getValue()) {
            case TimestampFormatTrait.DATE_TIME:
                return validateDatetime(shape, value);
            case TimestampFormatTrait.EPOCH_SECONDS:
                // Accepts any number including floats.
                if (!value.isNumberNode()) {
                    return ListUtils.of(String.format(
                            "Invalid %s value provided for a timestamp with a `%s` format.",
                            value.getType(), trait.getValue()));
                }
                return ListUtils.of();
            case TimestampFormatTrait.HTTP_DATE:
                return validateHttpDate(value);
            default:
                // This validator plugin doesn't know this format, but other plugins might.
                LOGGER.info(() -> "Unknown timestampFormat trait value: " + trait.getValue());
                return ListUtils.of();
        }
    }

    private List<String> defaultValidation(Shape shape, Node value) {
        // If no timestampFormat trait is present, then the shape is
        // validated by checking that the value is either a number or a
        // string that matches the date-time format.
        if (value.isNumberNode()) {
            return ListUtils.of();
        } else if (value.isStringNode()) {
            return validateDatetime(shape, value);
        } else {
            return ListUtils.of(
                    "Invalid " + value.getType() + " value provided for timestamp, `"
                    + shape.getId() + "`. Expected a number that contains epoch seconds with optional "
                    + "millisecond precision, or a string that contains an RFC 3339 formatted timestamp "
                    + "(e.g., \"1985-04-12T23:20:50.52Z\")");
        }
    }

    private List<String> validateDatetime(Shape shape, Node value) {
        if (!value.isStringNode()) {
            return ListUtils.of(
                    "Expected a string value for a date-time timestamp (e.g., \"1985-04-12T23:20:50.52Z\")");
        }

        String timestamp = value.expectStringNode().getValue();
        // Newer versions of Java support parsing instants that have an offset.
        // See: https://bugs.openjdk.java.net/browse/JDK-8166138
        // However, Smithy doesn't allow offsets for timestamp shapes.
        if (timestamp.endsWith("Z") && isValidFormat(timestamp, DATE_TIME_Z)) {
            return ListUtils.of();
        } else {
            return ListUtils.of(
                    "Invalid string value, `" + timestamp + "`, provided for timestamp, `"
                    + shape.getId() + "`. Expected an RFC 3339 formatted timestamp "
                    + "(e.g., \"1985-04-12T23:20:50.52Z\")");
        }
    }

    private List<String> validateHttpDate(Node value) {
        if (!value.asStringNode().isPresent()) {
            return createInvalidHttpDateMessage(value.getType().toString());
        }

        String dateValue = value.asStringNode().get().getValue();
        if (!isValidFormat(dateValue, HTTP_DATE) || !dateValue.endsWith("GMT")) {
            return createInvalidHttpDateMessage(dateValue);
        }

        return ListUtils.of();
    }

    private List<String> createInvalidHttpDateMessage(String dateValue) {
        return ListUtils.of(String.format(
                "Invalid value provided for %s formatted timestamp. Expected a string value that "
                + "matches the IMF-fixdate production of RFC 7231 section-7.1.1.1. Found: %s",
                TimestampFormatTrait.HTTP_DATE, dateValue));
    }

    private boolean isValidFormat(String value, DateTimeFormatter format) {
        try {
            format.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
