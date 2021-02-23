/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.build.transforms;

import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code renameShapes} updates a model by renaming shapes. When
 * configuring the transformer, a `renamed` property must be set as a
 * map with the keys as the `from` shape ids that will be renamed `to`
 * the shape id values. Any references to a renamed shape will also be
 * updated.
 */
public final class RenameShapes extends ConfigurableProjectionTransformer<RenameShapes.Config> {

    /**
     * {@code renameShapes} configuration settings.
     */
    public static final class Config {

        private Map<String, String> renamed;

        /**
         * Sets the map of `from` shape ids to the `to` shape id values that they shapes
         * will be renamed to.
         *
         * @param renamed The map of shapes to rename.
         */
        public void setRenamed(Map<String, String> renamed) {
            this.renamed = renamed;
        }

        /**
         * Gets the map of shape ids to be renamed.
         *
         * @return The map of shapes to rename.
         */
        public Map<String, String> getRenamed() {
            return renamed;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        if (config.getRenamed() == null || config.getRenamed().isEmpty()) {
            throw new SmithyBuildException(
                    "'renamed' property must be set and non-empty on renameShapes transformer.");
        }
        Model model = context.getModel();

        return ModelTransformer.create().renameShapes(model, getShapeIdsToRename(config, model));
    }

    @Override
    public String getName() {
        return "renameShapes";
    }

    private Map<ShapeId, ShapeId> getShapeIdsToRename(Config config, Model model) {
        Map<ShapeId, ShapeId> shapeIdMap = new HashMap<>();
        for (String fromShape : config.getRenamed().keySet()) {
            ShapeId fromShapeId = getShapeIdFromString(fromShape);
            if (!model.getShape(fromShapeId.toShapeId()).isPresent()) {
                throw new SmithyBuildException(
                        String.format("'%s' to be renamed does not exist in model.", fromShapeId)
                );
            }
            shapeIdMap.put(fromShapeId, getShapeIdFromString(config.getRenamed().get(fromShape)));
        }
        return shapeIdMap;
    }

    private ShapeId getShapeIdFromString(String id) {
        try {
            return ShapeId.from(id);
        } catch (ShapeIdSyntaxException e) {
            throw new SmithyBuildException(String.format("'%s' must be a valid, absolute shape ID", id));
        }
    }
}
