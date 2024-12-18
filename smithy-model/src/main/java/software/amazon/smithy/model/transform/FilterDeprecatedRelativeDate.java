/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.DeprecatedTrait;

final class FilterDeprecatedRelativeDate {
    private static final Logger LOGGER = Logger.getLogger(FilterDeprecatedRelativeDate.class.getName());
    // YYYY-MM-DD calendar date with optional hyphens.
    private static final Pattern ISO_8601_DATE_REGEX = Pattern.compile(
            "^([0-9]{4})-?(1[0-2]|0[1-9])-?(3[01]|0[1-9]|[12][0-9])$");

    private final String relativeDate;

    FilterDeprecatedRelativeDate(String relativeDate) {
        if (relativeDate != null && !isIso8601Date(relativeDate)) {
            throw new IllegalArgumentException("Provided relativeDate: `"
                    + relativeDate
                    + "` does not match ISO8601 calendar date format (YYYY-MM-DD).");
        }
        this.relativeDate = relativeDate != null ? relativeDate.replace("-", "") : null;
    }

    Model transform(ModelTransformer transformer, Model model) {
        // If there is no filter. Exit without traversing shapes
        if (relativeDate == null) {
            return model;
        }

        Set<Shape> shapesToRemove = new HashSet<>();
        for (Shape shape : model.getShapesWithTrait(DeprecatedTrait.class)) {
            Optional<String> sinceOptional = shape.expectTrait(DeprecatedTrait.class).getSince();
            if (!sinceOptional.isPresent()) {
                continue;
            }
            String since = sinceOptional.get();

            if (isIso8601Date(since)) {
                // Compare lexicographical ordering without hyphens.
                if (relativeDate.compareTo(since.replace("-", "")) > 0) {
                    LOGGER.fine("Filtering deprecated shape: `"
                            + shape + "`"
                            + ". Shape was deprecated as of: " + since);
                    shapesToRemove.add(shape);
                }
            }
        }

        return transformer.removeShapes(model, shapesToRemove);
    }

    private static boolean isIso8601Date(String string) {
        return ISO_8601_DATE_REGEX.matcher(string).matches();
    }
}
