/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.PrivateTrait;

/**
 * Represents the prelude model available to every Smithy model.
 *
 * <p>The prelude consists of public prelude shapes like
 * {@code smithy.api#String} and traits that are available to
 * all models like {@code box} and {@code required}.
 *
 * <p>A key design requirement of the prelude is that it cannot require a
 * loaded prelude model in order to reason about the prelude as this would
 * result in infinite recursion while loading the prelude model.
 */
public final class Prelude {

    /** The Smithy prelude namespace. */
    public static final String NAMESPACE = "smithy.api";

    private Prelude() {}

    /**
     * Checks if the given shape ID is defined by the prelude.
     *
     * <p>Specifically, this checks if the namespace of the provided shape ID
     * is equal to {@code smithy.api}.
     *
     * @param id Shape ID to check.
     * @return Returns true if the shape is a prelude shape.
     */
    public static boolean isPreludeShape(ToShapeId id) {
        return id.toShapeId().getNamespace().equals(NAMESPACE);
    }

    /**
     * Checks if the given shape is a public shape or trait defined by the
     * prelude.
     *
     * @param id Shape to check.
     * @return Returns true if the shape is a public prelude shape.
     */
    public static boolean isPublicPreludeShape(ToShapeId id) {
        return getPreludeModel().getShape(id.toShapeId())
                .filter(shape -> !shape.hasTrait(PrivateTrait.class))
                .isPresent();
    }

    // Used by the ModelAssembler to load the prelude into another visitor.
    static Model getPreludeModel() {
        return PreludeHolder.PRELUDE;
    }

    // Lazy initialization holder class idiom for loading prelude traits and shapes.
    private static final class PreludeHolder {
        private static final Model PRELUDE = loadPrelude();

        private static Model loadPrelude() {
            return Model.assembler()
                    .disablePrelude()
                    // Model validation is disabled when loading the prelude
                    // because the prelude is validated during unit tests and
                    // the prelude is immutable. However, if the prelude is
                    // broken for whatever reason, ERROR events encountered
                    // when performing model validation that uses the prelude
                    // will still cause an error, meaning the prelude is still
                    // validated when actually loading and using other models.
                    .disableValidation()
                    .traitFactory(ModelAssembler.LazyTraitFactoryHolder.INSTANCE)
                    .addImport(Prelude.class.getResource("prelude.smithy"))
                    // Patch in synthetic box traits for v1 compatibility.
                    .addTrait(ShapeId.from("smithy.api#Boolean"), new BoxTrait())
                    .addTrait(ShapeId.from("smithy.api#Byte"), new BoxTrait())
                    .addTrait(ShapeId.from("smithy.api#Short"), new BoxTrait())
                    .addTrait(ShapeId.from("smithy.api#Integer"), new BoxTrait())
                    .addTrait(ShapeId.from("smithy.api#Long"), new BoxTrait())
                    .addTrait(ShapeId.from("smithy.api#Float"), new BoxTrait())
                    .addTrait(ShapeId.from("smithy.api#Double"), new BoxTrait())
                    .assemble()
                    .unwrap();
        }
    }
}
