/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    /**
     * Unknown is used for in-memory models that aren't tied to a specific version.
     * For these kinds of models, we just assume every feature is supported.
     *
     * <p>When loading IDL models with no $version specified, the default assumed
     * version is 1.0, not UNKNOWN (see {@link TraitContainer.VersionAwareTraitContainer}).
     */
    UNKNOWN {
        @Override
        public String toString() {
            return "";
        }

        @Override
        boolean supportsMixins() {
            return true;
        }

        @Override
        boolean supportsInlineOperationIO() {
            return true;
        }

        @Override
        boolean supportsTargetElision() {
            return true;
        }

        @Override
        boolean isDefaultSupported() {
            return true;
        }

        @Override
        boolean isShapeTypeSupported(ShapeType shapeType) {
            return true;
        }
    },

    VERSION_1_0 {
        @Override
        public String toString() {
            return "1.0";
        }

        @Override
        boolean supportsMixins() {
            return false;
        }

        @Override
        boolean supportsInlineOperationIO() {
            return false;
        }

        @Override
        boolean supportsTargetElision() {
            return false;
        }

        @Override
        boolean isDefaultSupported() {
            return false;
        }

        @Override
        boolean isShapeTypeSupported(ShapeType shapeType) {
            return shapeType != ShapeType.ENUM && shapeType != ShapeType.INT_ENUM;
        }

        @Override
        void validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
            if (traitId.equals(MixinTrait.ID)) {
                throw ModelSyntaxException.builder()
                        .message(String.format("Mixins can only be used in Smithy 2.0 or later. Attempted to apply "
                                               + "a @mixin trait to `%s` in a model file using version `%s`.",
                                               target, this))
                        .shapeId(target)
                        .sourceLocation(value)
                        .build();
            }
        }
    },

    VERSION_2_0 {
        @Override
        public String toString() {
            return "2.0";
        }

        @Override
        boolean supportsMixins() {
            return true;
        }

        @Override
        boolean supportsInlineOperationIO() {
            return true;
        }

        @Override
        boolean supportsTargetElision() {
            return true;
        }

        @Override
        boolean isDefaultSupported() {
            return true;
        }

        @Override
        boolean isShapeTypeSupported(ShapeType shapeType) {
            return shapeType != ShapeType.SET;
        }
    };

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

    /**
     * Checks if this version of the IDL supports resource properties.
     *
     * @return Returns true if this version supports resource properties.
     */
    boolean supportsResourceProperties() {
        return this == VERSION_2_0;
    }

    /**
     * Checks if this version of the IDL supports mixins.
     *
     * @return Returns true if this version supports mixins.
     */
    abstract boolean supportsMixins();

    /**
     * Checks if this version of the IDL supports inlined operation IO shapes.
     *
     * @return Returns true if this version supports inlined operation IO shapes.
     */
    abstract boolean supportsInlineOperationIO();

    /**
     * Checks if this version of the IDL supports eliding targets for structures
     * with mixins or structures bound to resources.
     *
     * @return Returns true if the version supports eliding targets.
     */
    abstract boolean supportsTargetElision();

    /**
     * Checks if the default trait is supported.
     * @return Returns true if supported (i.e., IDL 2.0 or UNKNOWN).
     */
    abstract boolean isDefaultSupported();

    /**
     * Checks if the given shape type is supported in this version.
     *
     * @param shapeType The shape type to check.
     * @return Returns true if the shape type is supported in this version.
     */
    abstract boolean isShapeTypeSupported(ShapeType shapeType);

    /**
     * Perform version-specific trait validation.
     *
     * @param target Shape the trait is applied to.
     * @param traitId The shape ID of the trait.
     * @param value The Node value of the trait.
     * @throws ModelSyntaxException if the given trait cannot be used in this version.
     */
    void validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
    }
}