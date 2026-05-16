/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

/**
 * Generates deterministic names for synthetic shapes created from inline
 * collection declarations.
 */
final class SyntheticShapeNaming {

    private static final String PREFIX = "_Synthetic";

    private SyntheticShapeNaming() {}

    /**
     * Generates a synthetic name for an inline list shape.
     *
     * @param memberTarget The target name as written (e.g., "String" or "com.foo#Bar").
     * @return The synthetic shape name (without namespace).
     */
    static String listName(String memberTarget) {
        return PREFIX + "ListOf" + simpleName(memberTarget);
    }

    /**
     * Generates a synthetic name for an inline map shape.
     *
     * @param keyTarget The key target name as written.
     * @param valueTarget The value target name as written.
     * @return The synthetic shape name (without namespace).
     */
    static String mapName(String keyTarget, String valueTarget) {
        return PREFIX + "MapOf" + simpleName(keyTarget) + "To" + simpleName(valueTarget);
    }

    private static String simpleName(String target) {
        int hashIndex = target.indexOf('#');
        return hashIndex >= 0 ? target.substring(hashIndex + 1) : target;
    }
}
