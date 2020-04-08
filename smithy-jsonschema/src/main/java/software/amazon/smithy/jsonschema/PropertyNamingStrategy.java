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
