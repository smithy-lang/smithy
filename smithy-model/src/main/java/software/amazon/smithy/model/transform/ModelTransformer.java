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

package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.UnreferencedShapes;
import software.amazon.smithy.model.neighbor.UnreferencedTraitDefinitions;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Class used to transform {@link Model}s.
 */
public final class ModelTransformer {
    private final List<ModelTransformerPlugin> plugins;

    private ModelTransformer(List<ModelTransformerPlugin> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    /**
     * Creates a ModelTransformer using ModelTransformerPlugin instances
     * discovered using the {@code com.software.smithy.transform} class
     * loader and any modules found in the module path.
     *
     * @return Returns the created ModelTransformer.
     */
    public static ModelTransformer create() {
        return DefaultHolder.INSTANCE;
    }

    // Lazy initialization holder class idiom
    private static class DefaultHolder {
        static final ModelTransformer INSTANCE = createWithServiceProviders(ModelTransformer.class.getClassLoader());
    }

    private static ModelTransformer createWithServiceLoader(ServiceLoader<ModelTransformerPlugin> serviceLoader) {
        List<ModelTransformerPlugin> plugins = new ArrayList<>();
        serviceLoader.forEach(plugins::add);
        return createWithPlugins(plugins);
    }

    /**
     * Creates a ModelTransformer using a list of ModelTransformer plugins.
     *
     * @param plugins Plugins to use with the transformer.
     * @return Returns the created ModelTransformer.
     */
    public static ModelTransformer createWithPlugins(List<ModelTransformerPlugin> plugins) {
        return new ModelTransformer(plugins);
    }

    /**
     * Creates a ModelTransformer that finds {@link ModelTransformerPlugin}
     * service providers using the given {@link ClassLoader}.
     *
     * @param classLoader ClassLoader used to find ModelTransformerPlugin instances.
     * @return Returns the created ModelTransformer.
     */
    public static ModelTransformer createWithServiceProviders(ClassLoader classLoader) {
        return createWithServiceLoader(ServiceLoader.load(ModelTransformerPlugin.class, classLoader));
    }

    /**
     * Adds or replaces shapes into the model while ensuring that the model
     * is in a consistent state.
     *
     * @param model Model to transform.
     * @param shapes Shapes to add or replace in the model.base.
     * @return Returns the transformed model.base.
     */
    public Model replaceShapes(Model model, Collection<Shape> shapes) {
        if (shapes.isEmpty()) {
            return model;
        }

        return new ReplaceShapes(shapes).transform(this, model);
    }

    /**
     * Removes shapes from the model while ensuring that the model is in a
     * consistent state.
     *
     * @param model Model to transform.
     * @param shapes Shapes to add or replace in the model.base.
     * @return Returns the transformed model.base.
     */
    public Model removeShapes(Model model, Collection<Shape> shapes) {
        if (shapes.isEmpty()) {
            return model;
        }

        return new RemoveShapes(shapes, plugins).transform(this, model);
    }

    /**
     * Removes shapes from the model that match the given predicate.
     *
     * @param model Model to transform.
     * @param predicate Predicate that accepts a shape and returns true to
     *  remove it.
     * @return Returns the transformed model.base.
     */
    public Model removeShapesIf(Model model, Predicate<Shape> predicate) {
        return filterShapes(model, Predicate.not(predicate));
    }

    /**
     * Filters shapes out of the model that do not match the given predicate.
     *
     * <p>This filter will never filter out shapes that are part of the
     * prelude. Use the {@link #removeShapes} method directly if you need
     * to remove traits that are in the prelude.
     *
     * @param model Model to transform.
     * @param predicate Predicate that filters shapes.
     * @return Returns the transformed model.base.
     */
    public Model filterShapes(Model model, Predicate<Shape> predicate) {
        return new FilterShapes(predicate).transform(this, model);
    }

    /**
     * Filters traits out of the model that do not match the given predicate.
     *
     * <p>The predicate function accepts the shape that a trait is attached to
     * and the trait. If the predicate returns false, then the trait is
     * removed from the shape.
     *
     * @param model Model to transform.
     * @param predicate Predicate that accepts a (Shape, Trait) and returns
     *  false if the trait should be removed.
     * @return Returns the transformed model.base.
     */
    public Model filterTraits(Model model, BiPredicate<Shape, Trait> predicate) {
        return new FilterTraits(predicate).transform(this, model);
    }

    /**
     * Filters traits out of the model that match a predicate function.
     *
     * <p>The predicate function accepts the shape that a trait is attached to
     * and the trait. If the predicate returns true, then the trait is removed
     * from the shape.
     *
     * @param model Model to transform.
     * @param predicate Predicate that accepts a (Shape, Trait) and returns
     *  true if the trait should be removed.
     * @return Returns the transformed model.base.
     */
    public Model removeTraitsIf(Model model, BiPredicate<Shape, Trait> predicate) {
        return filterTraits(model, predicate.negate());
    }

    /**
     * Removes trait definitions from a model that do not match a predicate.
     *
     * <p>This transformation ensures that any time a trait definition is
     * removed, instances of the trait are also removed.
     *
     * <p>This method does not remove trait definitions from the model that
     * are part of the prelude. If you want to remove trait definitions that
     * are part of the prelude, use {@link #removeTraitDefinitions} directly.
     *
     * @param model Model to transform.
     * @param predicate Predicate that accepts a TraitDefinition. If the
     *  predicate returns true, the definition is kept. Otherwise, it is
     *  removed.
     * @return Returns the transformed model.base.
     */
    public Model filterTraitDefinitions(Model model, Predicate<TraitDefinition> predicate) {
        return new FilterTraitDefinitions(predicate).transform(this, model);
    }

    /**
     * Removes trait definitions from a model by fully qualified trait name.
     *
     * <p>This transformation ensures that any time a trait definition is
     * removed, instances of the trait are also removed.
     *
     * @param model Model to transform.
     * @param traitNames Fully qualified trait definitions to remove.
     * @return Returns the transformed model.base.
     */
    public Model removeTraitDefinitions(Model model, Set<String> traitNames) {
        return new RemoveTraitDefinitions(traitNames).transform(this, model);
    }

    /**
     * Filters out metadata key-value pairs from a model that do not match
     * a predicate.
     *
     * @param model Model to transform.
     * @param predicate A predicate that accepts a metadata key-value pair.
     *  If the predicate returns true, then the metadata key-value pair is
     *  kept. Otherwise, it is removed.
     * @return Returns the transformed model.base.
     */
    public Model filterMetadata(Model model, BiPredicate<String, Node> predicate) {
        return new FilterMetadata(predicate).transform(model);
    }

    /**
     * Maps over all traits in the model using a mapping function that accepts
     * the shape the trait is applied to, a trait, and returns a trait,
     * possibly even a different kind of trait.
     *
     * <p>An exception is thrown if a trait is returned that targets a
     * different shape than the {@link Shape} passed into the mapper function.
     *
     * @param model Model to transform.
     * @param mapper Mapping function that accepts a (Shape, Trait) and returns
     *  the mapped Trait.
     * @return Returns the transformed model.base.
     */
    public Model mapTraits(Model model, BiFunction<Shape, Trait, Trait> mapper) {
        return new MapTraits(mapper).transform(this, model);
    }

    /**
     * Maps over all traits in the model using multiple mapping functions.
     *
     * <p>Note: passing in a list of mappers is much more efficient than
     * invoking {@code mapTraits} multiple times because it reduces the number
     * of intermediate models that are needed to perform the transformation.
     *
     * @param model Model to transform.
     * @param mappers Mapping functions that accepts a (Shape, Trait) and
     *  returns the mapped Trait.
     * @return Returns the transformed model.base.
     * @see #mapShapes(Model, Function) for more information.
     */
    public Model mapTraits(Model model, List<BiFunction<Shape, Trait, Trait>> mappers) {
        return mapTraits(model, mappers.stream()
                .reduce((a, b) -> (s, t) -> b.apply(s, a.apply(s, t)))
                .orElse((s, t) -> t));
    }

    /**
     * Maps over all shapes in the model using a mapping function, allowing
     * shapes to be replaced with completely different shapes or slightly
     * modified shapes.
     *
     * <p>An exception is thrown if a mapper returns a shape with a different
     * shape ID or a different type.
     *
     * @param model Model to transform.
     * @param mapper Mapping function that accepts a shape and returns a shape
     *  with the same ID.
     * @return Returns the transformed model.base.
     */
    public Model mapShapes(Model model, Function<Shape, Shape> mapper) {
        return new MapShapes(mapper).transform(this, model);
    }

    /**
     * Maps over all shapes in the model using multiple mapping functions.
     *
     * <p>Note: passing in a list of mappers is much more efficient than
     * invoking {@code mapShapes}  multiple times because it reduces the
     * number of intermediate models that are needed to perform the
     * transformation.
     *
     * @param model Model to transform.
     * @param mappers Mapping functions that accepts a shape and returns a
     *  shape with the same ID.
     * @return Returns the transformed model.base.
     * @see #mapShapes(Model, Function) for more information.
     */
    public Model mapShapes(Model model, List<Function<Shape, Shape>> mappers) {
        return mapShapes(model, (mappers.stream().reduce(Function::compose).orElse(Function.identity())));
    }

    /**
     * Removes shapes (excluding service shapes) that are not referenced by
     * any other shapes.
     *
     * @param model Model to transform.
     * @return Returns the transformed model.base.
     */
    public Model removeUnreferencedShapes(Model model) {
        return removeUnreferencedShapes(model, shape -> true);
    }

    /**
     * Removes shapes (excluding service shapes) that are not referenced by
     * any other shapes.
     *
     * Shapes that are part of the prelude or that act as the shape of any
     * trait, regardless of if the trait is in use in the model, are never
     * considered unreferenced.
     *
     * @param model Model to transform.
     * @param keepFilter Predicate function that accepts an unreferenced
     *  shape and returns true to remove the shape or false to keep the shape
     *  in the model.base.
     * @return Returns the transformed model.base.
     */
    public Model removeUnreferencedShapes(Model model, Predicate<Shape> keepFilter) {
        return removeShapes(model, new UnreferencedShapes(keepFilter).compute(model));
    }

    /**
     * Removes definitions for traits that are not used by any shape in the
     * model.base.
     *
     * Trait definitions that are part of the prelude will not be removed.
     *
     * @param model Model to transform
     * @return Returns the transformed model.base.
     */
    public Model removeUnreferencedTraitDefinitions(Model model) {
        return removeUnreferencedTraitDefinitions(model, traitDefinition -> true);
    }

    /**
     * Removes definitions for traits that are not used by any shape in the
     * model.base.
     *
     * Trait definitions that are part of the prelude will not be removed.
     *
     * @param model Model to transform
     * @param keepFilter Predicate function that accepts an unreferenced
     *  trait definition and returns true to remove the definition or false to
     *  keep the definition in the model.base.
     * @return Returns the transformed model.base.
     */
    public Model removeUnreferencedTraitDefinitions(Model model, Predicate<TraitDefinition> keepFilter) {
        return removeTraitDefinitions(model, new UnreferencedTraitDefinitions(keepFilter).compute(model).stream()
                .map(TraitDefinition::getFullyQualifiedName)
                .collect(Collectors.toSet()));
    }

    /**
     * Removes all trait definitions from a model and all shapes that are
     * only connected to the graph either directly or transitively by a
     * trait definition shape.
     *
     * <p>This can be useful when serializing a Smithy model to a format that
     * does not include trait definitions and the shapes used by trait definitions
     * would have no meaning (e.g., Swagger).
     *
     * @param model Model to transform.
     * @return Returns the transformed model.base.
     */
    public Model scrubTraitDefinitions(Model model) {
        return new ScrubTraitDefinitions().transform(this, model);
    }
}
