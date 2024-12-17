/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code removeUnusedShapes} removes shapes from the model that are not
 * connected to any service shape.
 *
 * <p>Shapes from the prelude *are* removed if they are not referenced as
 * part of a model.
 */
public final class RemoveUnusedShapes extends BackwardCompatHelper<RemoveUnusedShapes.Config> {

    /**
     * {@code removeUnusedShapes} configuration settings.
     */
    public static final class Config {

        private Set<String> exportTagged = Collections.emptySet();

        /**
         * You can <em>export</em> shapes that are not connected to any service
         * shape by applying specific tags to the shape and adding the list of
         * export tags as an argument to the transformer.
         *
         * @param exportByTags Tags that cause shapes to be exported.
         */
        public void setExportTagged(Set<String> exportByTags) {
            this.exportTagged = exportByTags;
        }

        /**
         * Gets the set of tags that are used to export shapes.
         *
         * @return the tags that are used to export shapes.
         */
        public Set<String> getExportTagged() {
            return exportTagged;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "removeUnusedShapes";
    }

    @Override
    public String getBackwardCompatibleNameMapping() {
        return "exportTagged";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Predicate<Shape> keepShapesByTag = shape -> config.getExportTagged().stream().noneMatch(shape::hasTag);
        Predicate<Shape> keepTraitDefsByTag = trait -> config.getExportTagged().stream().noneMatch(trait::hasTag);
        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();

        int currentShapeCount;
        do {
            currentShapeCount = model.toSet().size();
            model = transformer.removeUnreferencedShapes(model, keepShapesByTag);
            model = transformer.removeUnreferencedTraitDefinitions(model, keepTraitDefsByTag);
        } while (currentShapeCount != model.toSet().size());

        return model;
    }
}
