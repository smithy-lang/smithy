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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Creates traits from {@link Node} values.
 *
 * <p>This is the interface used to create traits when loading a model.
 * If a trait implementation does not have a corresponding
 * {@link TraitService}, the concrete class for the trait will not be
 * used in code, and instead a {@link DynamicTrait} trait will be used.
 */
public interface TraitService {
    /**
     * @return Gets the name of the trait that this provider created.
     */
    String getTraitName();

    /**
     * Creates the trait from a node value.
     *
     * @param target The shape targeted by the trait.
     * @param value The value of the trait.
     * @return Returns the created trait.
     */
    Trait createTrait(ShapeId target, Node value);

    /**
     * Creates a TraitService provider that builds an annotation {@link Trait}
     * (a trait that requires a boolean value set to true).
     *
     * @param traitName Trait name to create.
     * @param constructor Trait constructor to invoke.
     * @return Returns the created trait provider.
     */
    static TraitService createAnnotationProvider(String traitName, Function<SourceLocation, Trait> constructor) {
        return createProvider(traitName, (target, value) -> {
            BooleanNode booleanNode = value.expectBooleanNode();
            if (!booleanNode.getValue()) {
                throw new SourceException(String.format(
                        "Boolean trait `%s` expects a value of `true`, but found `false`", traitName), value);
            }
            return constructor.apply(value.getSourceLocation());
        });
    }

    /**
     * Creates a TraitService provider that builds a trait that expects a
     * string value.
     *
     * @param traitName Trait name to create.
     * @param constructor Trait constructor to invoke.
     * @return Returns the created trait provider.
     */
    static TraitService createStringProvider(
            String traitName,
            BiFunction<String, SourceLocation, Trait> constructor
    ) {
        return createProvider(traitName, (target, value) -> constructor.apply(
                value.expectStringNode().getValue(), value.getSourceLocation()));
    }

    /**
     * Creates a trait provider for traits made up of a list of strings.
     *
     * @param traitName Trait name to register and create.
     * @param constructor The constructor of the trait.
     * @param <T> The type of trait being created.
     * @return Returns the created TraitService that can be a service provider.
     */
    static <T extends StringListTrait> TraitService createStringListProvider(
            String traitName,
            StringListTrait.StringListTraitConstructor<T> constructor
    ) {
        return createProvider(traitName, (target, value) -> {
            List<String> values = Node.loadArrayOfString(traitName, value);
            return constructor.create(values, value.getSourceLocation());
        });
    }

    /**
     * Creates a TraitService provider that builds a trait that expects an
     * integer value.
     *
     * @param traitName Trait name to create.
     * @param constructor Trait constructor to invoke.
     * @return Returns the created trait provider.
     */
    static TraitService createIntegerProvider(
            String traitName,
            BiFunction<Integer, SourceLocation, Trait> constructor
    ) {
        return createProvider(traitName, (target, value) -> constructor.apply(
                value.expectNumberNode().getValue().intValue(), value.getSourceLocation()));
    }

    /**
     * Creates a simple TraitService provider.
     *
     * @param traitName Trait name of the provider.
     * @param creator Factory function used to create the trait.
     * @return Returns the created TraitService.
     */
    static TraitService createProvider(String traitName, BiFunction<ShapeId, Node, Trait> creator) {
        return new TraitService() {
            @Override
            public String getTraitName() {
                return traitName;
            }

            @Override
            public Trait createTrait(ShapeId target, Node value) {
                return creator.apply(target, value);
            }
        };
    }
}
