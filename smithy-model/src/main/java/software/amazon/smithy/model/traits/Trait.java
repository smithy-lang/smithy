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

package software.amazon.smithy.model.traits;

import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.Validator;

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
public interface Trait extends FromSourceLocation, ToNode {
    /**
     * Gets the fully qualified name of the trait as it appears in models.
     *
     * <p>Built-in traits may or may not omit a namespace. Namespaces, if
     * present, will always come before the trait name, separated by a "#"
     * character. For example, {@code foo.bar#baz}, is a valid custom trait
     * name, {@code deprecated} is a valid built-in trait name, and so is
     * {@code smithy.api#deprecated}.
     *
     * <p>The returned name might not contain a namespace. Use the
     * {@link #getNamespace()} and {@link #getRelativeName()} to safely
     * access parts of the trait name.
     *
     * @return Returns the name of the trait.
     */
    String getName();

    /**
     * Gets the namespace of the trait (e.g., {@code smithy.api}.
     *
     * <p>If the trait name has no namespace, then the default
     * "smithy.api" namespace is returned.
     *
     * @return Returns the namespace of the trait.
     */
    default String getNamespace() {
        String name = getName();
        int index = name.indexOf("#");
        return index == -1 ? Prelude.NAMESPACE : name.substring(0, index);
    }

    /**
     * Gets the name of the trait with no namespace
     * (e.g., "required" instead of "smithy.api#required").
     *
     * @return Returns the relative name of the trait.
     */
    default String getRelativeName() {
        String name = getName();
        int index = name.indexOf("#");
        if (index == -1) {
            return name;
        } else if (index == name.length()) {
            return "";
        } else {
            return name.substring(index + 1);
        }
    }

    /**
     * Returns true if the name of the trait matches the given name.
     *
     * <p>If the provided {@code traitName} value does not contain a
     * namespace, this method with automatically prepend the "smithy.api#"
     * namespace. This makes checking if a trait name matches a given trait
     * easier since you can perform both an absolute check and a relative
     * check with the same method.
     *
     * @param traitName Trait name to check.
     * @return True if the trait has a name that matches the given string.
     */
    default boolean matchesTraitName(String traitName) {
        return getName().equals(traitName)
               || getNamespace().equals(Prelude.NAMESPACE) && getRelativeName().equals(traitName);
    }

    /**
     * Used in a stream flatMapStream to return a {@link Stream} with a
     * {@link Pair} of Shape and Trait if the trait is present on the
     * given shape.
     *
     * @param shape Shape to query for the trait.
     * @param traitClass Trait to retrieve.
     * @param <S> Shape
     * @param <T> Trait
     * @return Returns the Stream of the found trait or an empty stream.
     */
    static <S extends Shape, T extends Trait> Stream<Pair<S, T>> flatMapStream(
            S shape,
            Class<T> traitClass
    ) {
        return shape.getTrait(traitClass).map(t -> Pair.of(shape, t)).stream();
    }

    /**
     * Gets the idiomatic name of a prelude trait by stripping off the
     * smithy.api# prefix. This is used in various error messages.
     *
     * @param traitName Trait name to make idiomatic.
     * @return Returns the idiomatic trait name.
     */
    static String getIdiomaticTraitName(String traitName) {
        return traitName.replace("smithy.api#", "");
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
