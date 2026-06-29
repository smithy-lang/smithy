/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

    UNKNOWN("", Feature.ALL),

    VERSION_1_0("1.0", Feature.NONE),

    VERSION_2_0("2.0",
            Feature.MIXINS
                    | Feature.INLINE_OPERATION_IO
                    | Feature.TARGET_ELISION
                    | Feature.DEFAULT
                    | Feature.RESOURCE_PROPERTIES),

    VERSION_2_1("2.1",
            Feature.MIXINS
                    | Feature.INLINE_OPERATION_IO
                    | Feature.TARGET_ELISION
                    | Feature.DEFAULT
                    | Feature.RESOURCE_PROPERTIES
                    | Feature.INLINE_COLLECTIONS
                    | Feature.TAGGED_LITERALS);

    private final String label;
    private final int features;

    Version(String label, int features) {
        this.label = label;
        this.features = features;
    }

    @Override
    public String toString() {
        return label;
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
            case "2.1":
                return VERSION_2_1;
            default:
                return null;
        }
    }

    /**
     * @return Return true if deprecated.
     */
    boolean isDeprecated() {
        return this == VERSION_1_0;
    }

    /**
     * Checks if this version of the IDL supports mixins.
     *
     * @return Returns true if this version supports mixins.
     */
    boolean supportsMixins() {
        return supports(Feature.MIXINS);
    }

    /**
     * Checks if this version of the IDL supports inlined operation IO shapes.
     *
     * @return Returns true if this version supports inlined operation IO shapes.
     */
    boolean supportsInlineOperationIO() {
        return supports(Feature.INLINE_OPERATION_IO);
    }

    /**
     * Checks if this version of the IDL supports eliding targets for structures
     * with mixins or structures bound to resources.
     *
     * @return Returns true if the version supports eliding targets.
     */
    boolean supportsTargetElision() {
        return supports(Feature.TARGET_ELISION);
    }

    /**
     * Checks if the default trait is supported.
     * @return Returns true if supported (i.e., IDL 2.0+, or UNKNOWN).
     */
    boolean isDefaultSupported() {
        return supports(Feature.DEFAULT);
    }

    /**
     * Checks if this version of the IDL supports resource properties.
     *
     * @return Returns true if this version supports resource properties.
     */
    boolean supportsResourceProperties() {
        return supports(Feature.RESOURCE_PROPERTIES);
    }

    /**
     * Checks if this version of the IDL supports inline collection declarations.
     *
     * @return Returns true if this version supports inline collections.
     */
    boolean supportsInlineCollections() {
        return supports(Feature.INLINE_COLLECTIONS);
    }

    /**
     * Checks if this version of the IDL supports tagged string literals.
     *
     * @return Returns true if this version supports tagged string literals.
     */
    boolean supportsTaggedLiterals() {
        return supports(Feature.TAGGED_LITERALS);
    }

    /**
     * Checks if the given shape type is supported in this version.
     *
     * @param shapeType The shape type to check.
     * @return Returns true if the shape type is supported in this version.
     */
    boolean isShapeTypeSupported(ShapeType shapeType) {
        switch (this) {
            case VERSION_1_0:
                return shapeType != ShapeType.ENUM && shapeType != ShapeType.INT_ENUM;
            case VERSION_2_0:
            case VERSION_2_1:
                return shapeType != ShapeType.SET;
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
     * @return Returns a validation event if the trait is invalid for this version, or null.
     */
    @SuppressWarnings("deprecation")
    ValidationEvent validateVersionedTrait(ShapeId target, ShapeId traitId, Node value) {
        switch (this) {
            case VERSION_1_0:
                return validateV1VersionedTrait(target, traitId, value);
            case VERSION_2_0:
            case VERSION_2_1:
                return validateV2VersionedTrait(target, traitId, value);
            default:
                return null;
        }
    }

    private boolean supports(int feature) {
        return (features & feature) != 0;
    }

    private ValidationEvent validateV1VersionedTrait(ShapeId target, ShapeId traitId, Node value) {
        String errorMessage = null;
        if (traitId.equals(MixinTrait.ID)) {
            errorMessage = String.format("Mixins can only be used in Smithy 2.0 or later. Attempted to apply "
                    + "a @mixin trait to `%s` in a model file using version `%s`.",
                    target,
                    this);
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

    @SuppressWarnings("deprecation")
    private ValidationEvent validateV2VersionedTrait(ShapeId target, ShapeId traitId, Node value) {
        if (traitId.equals(BoxTrait.ID)) {
            return ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .shapeId(target)
                    .sourceLocation(value)
                    .message("@box is not supported in Smithy IDL " + this)
                    .build();
        } else if (traitId.equals(EnumTrait.ID)) {
            return ValidationEvent.builder()
                    .id(Validator.MODEL_DEPRECATION)
                    .severity(Severity.WARNING)
                    .shapeId(target)
                    .sourceLocation(value)
                    .message("The enum trait is deprecated. Smithy " + this
                            + " models should use the enum shape.")
                    .build();
        }

        return null;
    }

    /**
     * Feature flags for version capabilities.
     */
    static final class Feature {
        static final int NONE = 0;
        static final int MIXINS = 1;
        static final int INLINE_OPERATION_IO = 1 << 1;
        static final int TARGET_ELISION = 1 << 2;
        static final int DEFAULT = 1 << 3;
        static final int RESOURCE_PROPERTIES = 1 << 4;
        static final int INLINE_COLLECTIONS = 1 << 5;
        static final int TAGGED_LITERALS = 1 << 6;
        static final int ALL = ~0;

        private Feature() {}
    }
}
