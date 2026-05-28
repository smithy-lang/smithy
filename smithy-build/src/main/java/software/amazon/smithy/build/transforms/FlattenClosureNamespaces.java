/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ShapeClosureIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * {@code flattenClosureNamespaces} flattens the namespaces of every
 * non-prelude shape in a single shape closure into a target namespace,
 * applying any renames declared by that closure. Member shapes follow their
 * containers, and shapes outside the closure are left untouched.
 */
public final class FlattenClosureNamespaces
        extends ConfigurableProjectionTransformer<FlattenClosureNamespaces.Config> {

    /**
     * {@code flattenClosureNamespaces} configuration.
     */
    public static final class Config {
        private String namespace;
        private String closure;

        /**
         * Sets the target namespace that closure shapes will be flattened into.
         *
         * @param namespace The target namespace.
         */
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        /**
         * @return The target namespace.
         */
        public String getNamespace() {
            return namespace;
        }

        /**
         * Sets the id of the closure to flatten.
         *
         * @param closure The closure id.
         */
        public void setClosure(String closure) {
            this.closure = closure;
        }

        /**
         * @return The id of the closure to flatten.
         */
        public String getClosure() {
            return closure;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "flattenClosureNamespaces";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        if (config.getClosure() == null || config.getNamespace() == null) {
            throw new SmithyBuildException("'closure' and 'namespace' properties must be set on the "
                    + "flattenClosureNamespaces transformer.");
        }

        Model model = context.getModel();
        ShapeClosureIndex index = ShapeClosureIndex.of(model);
        if (!index.getClosureIds().contains(config.getClosure())) {
            throw new SmithyBuildException("No `shapeClosures` closure named `" + config.getClosure()
                    + "` is defined in the model.");
        }

        Map<ShapeId, String> declaredRenames = index.getRenames(config.getClosure());
        Set<Shape> closureShapes = index.getShapesInClosure(config.getClosure());

        Map<ShapeId, ShapeId> shapesToRename = new HashMap<>();
        for (Shape shape : closureShapes) {
            ShapeId id = shape.getId();
            if (Prelude.isPreludeShape(id) || shape.isMemberShape()) {
                continue;
            }
            String name = declaredRenames.getOrDefault(id, id.getName());
            ShapeId target = ShapeId.fromParts(config.getNamespace(), name);
            if (!target.equals(id)) {
                shapesToRename.put(id, target);
            }
        }

        if (shapesToRename.isEmpty()) {
            return model;
        }
        return context.getTransformer().renameShapes(model, shapesToRename);
    }
}
