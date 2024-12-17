/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.Pair;

/**
 * {@code excludeTraits} removes trait definitions and traits from
 * shapes when a trait name matches any of the values given in
 * {@code traits}.
 *
 * <p>Arguments that end with "#" exclude the traits of an entire
 * namespace. Trait shape IDs that are relative are assumed to be
 * part of the {@code smithy.api} prelude namespace.
 */
public final class ExcludeTraits extends BackwardCompatHelper<ExcludeTraits.Config> {

    private static final Logger LOGGER = Logger.getLogger(ExcludeTraits.class.getName());

    /**
     * {@code excludeTraits} configuration settings.
     */
    public static final class Config {
        private Set<String> traits = Collections.emptySet();

        /**
         * Gets the list of trait shape IDs/namespaces to exclude.
         *
         * @return shape IDs to exclude.
         */
        public Set<String> getTraits() {
            return traits;
        }

        /**
         * Sets the list of trait shape IDs/namespaces to exclude.
         *
         * <p>Relative shape IDs are considered traits in the prelude
         * namespace, {@code smithy.api}. Strings ending in "#" are
         * used to exclude traits from an entire namespace.
         *
         * @param traits Traits to exclude.
         */
        public void setTraits(Set<String> traits) {
            this.traits = traits;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "excludeTraits";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "traits";
    }

    @Override
    public Model transformWithConfig(TransformContext context, Config config) {
        Pair<Set<ShapeId>, Set<String>> namesAndNamespaces = TraitRemovalUtils.parseTraits(config.getTraits());
        Set<ShapeId> names = namesAndNamespaces.getLeft();
        Set<String> namespaces = namesAndNamespaces.getRight();
        LOGGER.info(() -> "Excluding traits by ID " + names + " and namespaces " + namespaces);

        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();

        Set<Shape> removeTraits = model.getShapesWithTrait(TraitDefinition.class)
                .stream()
                .filter(trait -> TraitRemovalUtils.matchesTraitDefinition(trait, names, namespaces))
                .collect(Collectors.toSet());

        if (!removeTraits.isEmpty()) {
            LOGGER.info(() -> "Excluding traits: " + removeTraits);
        }

        return transformer.removeShapes(model, removeTraits);
    }
}
