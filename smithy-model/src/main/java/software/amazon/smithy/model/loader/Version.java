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
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Tracks version-specific features and validation.
 */
enum Version {

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

        @Override
        boolean isDeprecated() {
            return false;
        }

        @Override
        ValidationEvent validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
            return null;
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
        ValidationEvent validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
            String errorMessage = null;
            if (traitId.equals(MixinTrait.ID)) {
                errorMessage = String.format("Mixins can only be used in Smithy 2.0 or later. Attempted to apply "
                                             + "a @mixin trait to `%s` in a model file using version `%s`.",
                                             target, this);
            } else if (traitId.equals(DefaultTrait.ID)) {
                errorMessage = "The @default trait can only be used in Smithy 2.0 or later";
            }

            if (errorMessage != null) {
                return ValidationEvent.builder()
                        .severity(Severity.ERROR)
                        .id(Validator.MODEL_ERROR)
                        .shapeId(target)
                        .sourceLocation(value)
                        .message(errorMessage)
                        .build();
            }

            return null;
        }

        @Override
        boolean isDeprecated() {
            return true;
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

        @Override
        boolean isDeprecated() {
            return false;
        }

        @Override
        @SuppressWarnings("deprecation")
        ValidationEvent validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
            if (traitId.equals(BoxTrait.ID)) {
                return ValidationEvent.builder()
                        .id(Validator.MODEL_ERROR)
                        .severity(Severity.ERROR)
                        .shapeId(target)
                        .sourceLocation(value)
                        .message("@box is not supported in Smithy IDL 2.0")
                        .build();
            } else if (traitId.equals(EnumTrait.ID)) {
                return ValidationEvent.builder()
                        .id(Validator.MODEL_DEPRECATION)
                        .severity(Severity.WARNING)
                        .shapeId(target)
                        .sourceLocation(value)
                        .message("The enum trait is deprecated. Smithy 2.0 models should use the enum shape.")
                        .build();
            }

            return null;
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
     * @return Return true if deprecated.
     */
    abstract boolean isDeprecated();

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
    abstract ValidationEvent validateVersionedTrait(ShapeId target, ShapeId traitId, Node value);
}
