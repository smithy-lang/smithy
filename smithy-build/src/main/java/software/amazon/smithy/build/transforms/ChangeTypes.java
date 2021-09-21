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

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * {@code changeType} is used to change the type of one or more shapes.
 */
public final class ChangeTypes extends ConfigurableProjectionTransformer<ChangeTypes.Config> {

    /**
     * {@code flattenNamespaces} configuration settings.
     */
    public static final class Config {

        private final Map<ShapeId, ShapeType> shapeTypes = new LinkedHashMap<>();

        /**
         * Sets the map of shape IDs to shape types to set.
         *
         * @param shapeTypes Map of shape ID to shape type.
         */
        public void setShapeTypes(Map<ShapeId, ShapeType> shapeTypes) {
            this.shapeTypes.clear();
            this.shapeTypes.putAll(shapeTypes);
        }

        public Map<ShapeId, ShapeType> getShapeTypes() {
            return shapeTypes;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "changeTypes";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        if (config.getShapeTypes().isEmpty()) {
            throw new SmithyBuildException(getName() + ": shapeTypes must not be empty");
        }

        return context.getTransformer().changeShapeType(context.getModel(), config.getShapeTypes());
    }
}
