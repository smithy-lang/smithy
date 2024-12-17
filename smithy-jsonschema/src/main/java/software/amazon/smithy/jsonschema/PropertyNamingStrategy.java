/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.JsonNameTrait;

/**
 * Determines the field/property/member name of a member in an object.
 */
@FunctionalInterface
public interface PropertyNamingStrategy {
    /**
     * Determine the member name of the member.
     *
     * @param containingShape Shape that contains the member.
     * @param member Member shape to compute the member name of.
     * @param config Config to use.
     * @return Returns the computed member name.
     */
    String toPropertyName(Shape containingShape, MemberShape member, JsonSchemaConfig config);

    /**
     * Creates a naming strategy that just uses the member name as-is.
     *
     * @return Returns the created strategy.
     */
    static PropertyNamingStrategy createMemberNameStrategy() {
        return (containingShape, member, config) -> member.getMemberName();
    }

    /**
     * Creates a default strategy that first checks for {@code jsonName} then
     * falls back to the member name.
     *
     * @return Returns the created strategy.
     */
    static PropertyNamingStrategy createDefaultStrategy() {
        return (containingShape, member, config) -> {
            // Use the jsonName trait if configured to do so.
            if (config.getUseJsonName() && member.hasTrait(JsonNameTrait.class)) {
                return member.expectTrait(JsonNameTrait.class).getValue();
            }

            return member.getMemberName();
        };
    }
}
