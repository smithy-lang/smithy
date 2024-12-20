/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.validation.Severity;

/**
 * Validates that timestamp shapes contain values that are compatible with their
 * timestampFormat traits or contain values that are numbers or an RFC 3339
 * date-time production.
 */
final class TimestampFormatPlugin implements NodeValidatorPlugin {

    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_Z = DateTimeFormatter.ISO_INSTANT;
    private static final Logger LOGGER = Logger.getLogger(TimestampFormatPlugin.class.getName());

    @Override
    public void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (shape instanceof TimestampShape) {
            // Don't validate the timestamp target if a referring member had the timestampFormat trait.
            boolean fromMemberWithTrait = context.getReferringMember()
                    .filter(member -> member.hasTrait(TimestampFormatTrait.class))
                    .isPresent();
            if (!fromMemberWithTrait) {
                validate(shape, shape.getTrait(TimestampFormatTrait.class).orElse(null), value, emitter);
            }
        } else if (shape instanceof MemberShape && shape.getTrait(TimestampFormatTrait.class).isPresent()) {
            // Only perform timestamp format validation on a member when it references
            // a timestamp shape and the member has an explicit timestampFormat trait.
            validate(shape, shape.getTrait(TimestampFormatTrait.class).get(), value, emitter);
        }
    }

    private void validate(Shape shape, TimestampFormatTrait trait, Node value, Emitter emitter) {
        if (trait == null) {
            defaultValidation(shape, value, emitter);
        } else {
            switch (trait.getValue()) {
                case TimestampFormatTrait.DATE_TIME:
                    validateDatetime(shape, value, emitter);
                    break;
                case TimestampFormatTrait.EPOCH_SECONDS:
                    // Accepts any number including floats.
                    if (!value.isNumberNode()) {
                        emitter.accept(value,
                                Severity.ERROR,
                                String.format(
                                        "Invalid %s value provided for a timestamp with a `%s` format.",
                                        value.getType(),
                                        trait.getValue()));
                    }
                    break;
                case TimestampFormatTrait.HTTP_DATE:
                    validateHttpDate(value, emitter);
                    break;
                default:
                    // This validator plugin doesn't know this format, but other plugins might.
                    LOGGER.info(() -> "Unknown timestampFormat trait value: " + trait.getValue());
            }
        }
    }

    private void defaultValidation(Shape shape, Node value, Emitter emitter) {
        // If no timestampFormat trait is present, then the shape is validated by checking
        // that the value is either a number or a string that matches the date-time format.
        if (!value.isNumberNode()) {
            if (value.isStringNode()) {
                validateDatetime(shape, value, emitter);
            } else {
                emitter.accept(value,
                        "Invalid " + value.getType() + " value provided for timestamp, `"
                                + shape.getId() + "`. Expected a number that contains epoch seconds with "
                                + "optional millisecond precision, or a string that contains an RFC 3339 "
                                + "formatted timestamp (e.g., \"1985-04-12T23:20:50.52Z\")");
            }
        }
    }

    private void validateDatetime(Shape shape, Node value, Emitter emitter) {
        if (!value.isStringNode()) {
            emitter.accept(value,
                    "Expected a string value for a date-time timestamp "
                            + "(e.g., \"1985-04-12T23:20:50.52Z\")");
            return;
        }

        String timestamp = value.expectStringNode().getValue();
        // Newer versions of Java support parsing instants that have an offset.
        // See: https://bugs.openjdk.java.net/browse/JDK-8166138
        // However, Smithy doesn't allow offsets for timestamp shapes.
        if (!(timestamp.endsWith("Z") && isValidFormat(timestamp, DATE_TIME_Z))) {
            emitter.accept(value,
                    "Invalid string value, `" + timestamp + "`, provided for timestamp, `"
                            + shape.getId() + "`. Expected an RFC 3339 formatted timestamp (e.g., "
                            + "\"1985-04-12T23:20:50.52Z\")");
        }
    }

    private void validateHttpDate(Node value, Emitter emitter) {
        if (!value.asStringNode().isPresent()) {
            emitter.accept(value, Severity.ERROR, createInvalidHttpDateMessage(value.getType().toString()));
        } else {
            String dateValue = value.asStringNode().get().getValue();
            if (!isValidFormat(dateValue, HTTP_DATE) || !dateValue.endsWith("GMT")) {
                emitter.accept(value, Severity.ERROR, createInvalidHttpDateMessage(dateValue));
            }
        }
    }

    private String createInvalidHttpDateMessage(String dateValue) {
        return String.format(
                "Invalid value provided for %s formatted timestamp. Expected a string value that "
                        + "matches the IMF-fixdate production of RFC 9110 section-5.6.7. Found: %s",
                TimestampFormatTrait.HTTP_DATE,
                dateValue);
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
