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
 * {@code includeTraits} removes trait definitions when a trait name
 * does not match one of the provided {@code traits} shape IDs. Any
 * instance of the trait is also removed from the model.
 *
 * <p>End an argument with "#" to include the traits from an entire
 * namespace. Trait shape IDs that are relative are assumed to be part
 * of the {@code smithy.api} prelude namespace.
 */
public final class IncludeTraits extends BackwardCompatHelper<IncludeTraits.Config> {

    private static final Logger LOGGER = Logger.getLogger(IncludeTraits.class.getName());

    /**
     * {@code includeTraits} configuration settings.
     */
    public static final class Config {
        private Set<String> traits = Collections.emptySet();

        /**
         * Gets the list of trait shape IDs to include.
         *
         * @return shape IDs to include.
         */
        public Set<String> getTraits() {
            return traits;
        }

        /**
         * Sets the list of trait shape IDs to include.
         *
         * <p>End an argument with "#" to include the traits from an entire
         * namespace. Trait shape IDs that are relative are assumed to be
         * part of the {@code smithy.api} prelude namespace.
         *
         * @param traits Traits to include.
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
        return "includeTraits";
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
        LOGGER.info(() -> "Including traits by ID " + names + " and namespaces " + namespaces);

        // Don't remove the trait definition trait because it breaks everything!
        names.add(TraitDefinition.ID);

        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();

        Set<Shape> removeTraits = model.getShapesWithTrait(TraitDefinition.class)
                .stream()
                .filter(trait -> !TraitRemovalUtils.matchesTraitDefinition(trait, names, namespaces))
                .collect(Collectors.toSet());

        if (!removeTraits.isEmpty()) {
            LOGGER.info(() -> "Removing traits that are not explicitly allowed: " + removeTraits);
        }

        return transformer.removeShapes(model, removeTraits);
    }
}
