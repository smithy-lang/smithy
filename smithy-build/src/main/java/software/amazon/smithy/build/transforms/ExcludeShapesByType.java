/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Removes shapes from the model if they match a specific type.
 */
public final class ExcludeShapesByType extends ConfigurableProjectionTransformer<ExcludeShapesByType.Config> {

    /**
     * The configuration for this transformation.
     */
    public static final class Config {
        private Set<String> shapeTypes = Collections.emptySet();

        /**
         * Gets the shape types to filter shapes by.
         *
         * @return Returns the shape types.
         */
        public Set<String> getShapeTypes() {
            return shapeTypes;
        }

        /**
         * Sets the shape types to filter shapes by.
         *
         * @param shapeTypes The shape types such that if a shape matches, it is removed.
         */
        public void setShapeTypes(Set<String> shapeTypes) {
            this.shapeTypes = shapeTypes;
        }
    }

    @Override
    public String getName() {
        return "excludeShapesByType";
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    protected Model transformWithConfig(TransformContext context, Config config) {
        return context.getTransformer()
                .removeShapesIf(context.getModel(),
                        shape -> getShapeTypesFromStrings(
                                config.getShapeTypes())
                                        .contains(shape.getType()));
    }

    private Set<ShapeType> getShapeTypesFromStrings(Set<String> shapeType) {
        return shapeType.stream()
                .map(ShapeType::fromString)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }
}
