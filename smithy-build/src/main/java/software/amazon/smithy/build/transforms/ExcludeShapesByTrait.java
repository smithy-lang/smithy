/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Removes shapes from the model if they are marked with a specific trait.
 */
public final class ExcludeShapesByTrait extends ConfigurableProjectionTransformer<ExcludeShapesByTrait.Config> {

    public static final class Config {
        private Set<String> traits = Collections.emptySet();

        /**
         * Gets the shape IDs of the traits to filter shapes by.
         *
         * <p>Relative shape IDs are assumed to be in the smithy.api namespace.
         *
         * @return Returns the trait shape IDs.
         */
        public Set<String> getTraits() {
            return traits;
        }

        /**
         * Sets the shape IDs of the traits to filter shapes by.
         *
         * @param traits The shape IDs of the traits that if present causes a shape to be removed.
         */
        public void setTraits(Set<String> traits) {
            this.traits = traits;
        }
    }

    @Override
    public String getName() {
        return "excludeShapesByTrait";
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    protected Model transformWithConfig(TransformContext context, Config config) {
        // Resolve relative IDs by defaulting to smithy.api# if the given trait ID is relative.
        Set<ShapeId> ids = new HashSet<>(config.getTraits().size());
        for (String id : config.getTraits()) {
            ids.add(ShapeId.fromOptionalNamespace(Prelude.NAMESPACE, id));
        }

        return context.getTransformer().removeShapesIf(context.getModel(), shape -> {
            return ids.stream().anyMatch(shape::hasTrait);
        });
    }
}
