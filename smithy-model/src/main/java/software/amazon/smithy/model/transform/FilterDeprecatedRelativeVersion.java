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

final class FilterDeprecatedRelativeVersion {
    private static final Logger LOGGER = Logger.getLogger(FilterDeprecatedRelativeVersion.class.getName());

    /**
     * SemVer regex from semantic version spec.
     * @see <a href="https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string">SemVer</a>
     */
    private static final Pattern SEMVER_REGEX = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)"
                    + "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    private final String relativeVersion;

    FilterDeprecatedRelativeVersion(String relativeVersion) {
        if (relativeVersion != null && !isSemVer(relativeVersion)) {
            throw new IllegalArgumentException("Provided relativeDate: `"
                    + relativeVersion
                    + "` is not a valid .");
        }
        this.relativeVersion = relativeVersion;
    }

    Model transform(ModelTransformer transformer, Model model) {
        // If there are no filters. Exit without traversing shapes
        if (relativeVersion == null) {
            return model;
        }

        Set<Shape> shapesToRemove = new HashSet<>();
        for (Shape shape : model.getShapesWithTrait(DeprecatedTrait.class)) {
            Optional<String> sinceOptional = shape.expectTrait(DeprecatedTrait.class).getSince();

            if (!sinceOptional.isPresent()) {
                continue;
            }

            String since = sinceOptional.get();
            // Remove any shapes that were deprecated before the specified version.
            if (isSemVer(since)) {
                if (compareSemVer(relativeVersion, since) > 0) {
                    LOGGER.fine("Filtering deprecated shape: `"
                            + shape + "`"
                            + ". Shape was deprecated as of version: " + since);
                    shapesToRemove.add(shape);
                }
            }
        }

        return transformer.removeShapes(model, shapesToRemove);
    }

    private static boolean isSemVer(String string) {
        return SEMVER_REGEX.matcher(string).matches();
    }

    static int compareSemVer(String semVer1, String semVer2) {
        String[] versionComponents1 = semVer1.split("\\.");
        String[] versionComponents2 = semVer2.split("\\.");

        int maxLength = Math.max(versionComponents1.length, versionComponents2.length);
        for (int i = 0; i < maxLength; i++) {
            // Treat all implicit components as 0's
            String component1 = i >= versionComponents1.length ? "0" : versionComponents1[i];
            String component2 = i >= versionComponents2.length ? "0" : versionComponents2[i];
            int comparison = component1.compareTo(component2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
