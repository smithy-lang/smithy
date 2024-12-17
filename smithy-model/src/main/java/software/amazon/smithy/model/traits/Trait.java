/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Traits provide additional context and semantics to shapes.
 *
 * <p>A trait complements a {@link Shape} by providing additional
 * information to help correctly interpret any specific representation
 * of it or to add information about constraints on the logical structure
 * of the {@code Shape}. For example, one {@code Trait} object might
 * reflect details about how a {@code Shape} is bound to JSON while
 * another might reflect details about how that same {@code Shape} is
 * bound to Ion.
 *
 * <p>Traits are discovered through Java SPI using the {@link TraitService}
 * interface. All traits that are defined in a Smithy MUST provide a
 * TraitService in order for the concrete trait type to be created for
 * the trait in code. Otherwise, the trait is created as a
 * {@link DynamicTrait}.</p>
 *
 * <p>Traits may perform as much validation in their constructor; any
 * exception thrown while creating a trait when assembling a model will
 * automatically include the name of the trait in the thrown exception
 * message. Any validation that requires more context than is provided to
 * the trait constructor should be performed by implementing a
 * {@link Validator} class for the trait that is automatically registered
 * each time the model is validated by implementing the
 * {@link Validator} interface and registering the validator through SPI.
 */
public interface Trait extends FromSourceLocation, ToNode, ToShapeId {
    /**
     * Gets the shape ID of the trait.
     *
     * @return Returns the fully-qualified shape ID of the trait.
     */
    @Override
    ShapeId toShapeId();

    /**
     * Checks if this trait is persisted with the shape, or if it is a
     * synthetic, or transient trait, only meant to temporarily aid in
     * some kind of in-memory model transformation.
     *
     * <p>Because synthetic traits are not persisted with shapes, they also
     * do not need to be defined in Smithy's semantic model before they can
     * be used in the model.
     *
     * @return Returns true if the trait is not persisted on the shape.
     */
    default boolean isSynthetic() {
        return false;
    }

    /**
     * Used in a stream flatMapStream to return a {@link Stream} with a
     * {@link Pair} of Shape and Trait if the trait is present on the
     * given shape.
     *
     * <p>This method is deprecated because it generally results in harder
     * to read code using unnamed tuples. Use {@link Shape#hasTrait(Class)}
     * and {@link Shape#expectTrait(Class)} instead.
     *
     * @param shape Shape to query for the trait.
     * @param traitClass Trait to retrieve.
     * @param <S> Shape
     * @param <T> Trait
     * @return Returns the Stream of the found trait or an empty stream.
     */
    @Deprecated
    static <S extends Shape, T extends Trait> Stream<Pair<S, T>> flatMapStream(
            S shape,
            Class<T> traitClass
    ) {
        return OptionalUtils.stream(shape.getTrait(traitClass).map(t -> Pair.of(shape, t)));
    }

    /**
     * Gets the idiomatic name of a prelude trait by stripping off the
     * smithy.api# prefix. This is used in various error messages.
     *
     * @param traitName Trait name to make idiomatic.
     * @return Returns the idiomatic trait name.
     */
    static String getIdiomaticTraitName(String traitName) {
        return traitName.startsWith("smithy.api#")
                ? traitName.substring("smithy.api#".length())
                : traitName;
    }

    /**
     * Gets the idiomatic name of a prelude trait by stripping off the
     * smithy.api# prefix. This is used in various error messages.
     *
     * @param id Trait name to make idiomatic.
     * @return Returns the idiomatic trait name.
     */
    static String getIdiomaticTraitName(ToShapeId id) {
        return getIdiomaticTraitName(id.toShapeId().toString());
    }

    /**
     * Makes the given trait name absolute if it is relative.
     *
     * <p>The namespace used to resolve with the trait name is the
     * prelude namespace, smithy.api.
     *
     * @param traitName Trait name to make absolute.
     * @return Returns the absolute trait name.
     */
    static String makeAbsoluteName(String traitName) {
        return makeAbsoluteName(traitName, Prelude.NAMESPACE);
    }

    /**
     * Makes the given trait name absolute if it is relative.
     *
     * @param traitName Trait name to make absolute.
     * @param defaultNamespace Namespace to use if the name is relative.
     * @return Returns the absolute trait name.
     */
    static String makeAbsoluteName(String traitName, String defaultNamespace) {
        return traitName.contains("#") ? traitName : defaultNamespace + "#" + traitName;
    }
}
