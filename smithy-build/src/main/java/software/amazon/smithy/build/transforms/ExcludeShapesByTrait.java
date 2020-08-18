/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Removes shapes from the model if they are marked with a specific trait.
 */
public final class ExcludeShapesByTrait extends ConfigurableProjectionTransformer<ExcludeShapesByTrait.Config> {

    public static final class Config {
        private String trait;

        /**
         * Gets the shape ID of the trait to filter shapes by.
         *
         * @return Returns the trait shape ID.
         */
        public String getTrait() {
            return trait;
        }

        /**
         * Sets the shape ID of the trait to filter shapes by.
         *
         * @param trait The shape ID of the trait that if present causes a shape to be removed.
         */
        public void setTrait(String trait) {
            this.trait = trait;
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
        // Default to smithy.api# if the given trait ID is relative.
        ShapeId traitId = ShapeId.fromOptionalNamespace(Prelude.NAMESPACE, config.getTrait());

        return context.getTransformer().removeShapesIf(context.getModel(), shape -> shape.hasTrait(traitId));
    }
}
