/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.MixinTrait;

/**
 * Tracks version-specific features and validation.
 */
enum Version {

    UNKNOWN(""),
    VERSION_1_0("1.0"),
    VERSION_2_0("2.0");

    private final String version;

    Version(String version) {
        this.version = version;
    }

    /**
     * Creates a Version from a string, or returns null if the version
     * cannot be found.
     *
     * @param value Value to convert to a Version enum.
     * @return Returns the resolved enum value or null if not found.
     */
    static Version fromString(String value) {
        switch (value) {
            case "1.0":
            case "1":
                return VERSION_1_0;
            case "2":
            case "2.0":
                return VERSION_2_0;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return version;
    }

    /**
     * Checks if this version of the IDL supports mixins.
     *
     * @return Returns true if this version supports mixins.
     */
    boolean supportsMixins() {
        return this == VERSION_2_0;
    }

    /**
     * Checks if this version of the IDL supports inlined operation IO shapes.
     *
     * @return Returns true if this version supports inlined operation IO shapes.
     */
    boolean supportsInlineOperationIO() {
        return this == VERSION_2_0;
    }

    /**
     * Checks if this version of the IDL supports eliding targets for structures
     * with mixins or structures bound to resources.
     *
     * @return Returns true if the version supports eliding targets.
     */
    boolean supportsTargetElision() {
        return this == VERSION_2_0;
    }

    /**
     * Checks if the given shape type is supported in this version.
     *
     * @param shapeType The shape type to check.
     * @return Returns true if the shape type is supported in this version.
     */
    boolean isShapeTypeSupported(ShapeType shapeType) {
        switch (shapeType) {
            case SET:
                return this == VERSION_1_0;
            case ENUM:
            case INT_ENUM:
                return this == VERSION_2_0;
            default:
                return true;
        }
    }

    /**
     * Perform version-specific trait validation.
     *
     * @param target Shape the trait is applied to.
     * @param traitId The shape ID of the trait.
     * @param value The Node value of the trait.
     * @throws ModelSyntaxException if the given trait cannot be used in this version.
     */
    void validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
        if (traitId.equals(MixinTrait.ID) && (this != Version.VERSION_2_0)) {
            throw ModelSyntaxException.builder()
                    .message(String.format("Mixins can only be used in Smithy 2.0 or later. Attempted to apply "
                                           + "a @mixin trait to `%s` in a model file using version `%s`.",
                                           target, version))
                    .shapeId(target)
                    .sourceLocation(value)
                    .build();
        }
    }

    boolean isDefaultSupported() {
        return this == VERSION_2_0;
    }
}
