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

import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * {@code excludeShapesBySelector} excludes the shapes matching the given selector.
 *
 * <p>Prelude shapes are not removed by this transformer.
 */
public final class ExcludeShapesBySelector extends ConfigurableProjectionTransformer<ExcludeShapesBySelector.Config> {

    /**
     * {@code excludeShapesBySelector} configuration.
     */
    public static final class Config {
        private Selector selector = null;

        /**
         * Gets the selector used to filter the shapes.
         *
         * @return The selector used to filter the shapes.
         */
        public Selector getSelector() {
            return selector;
        }

        /**
         * Sets the selector used to filter the shapes.
         *
         * @param selector The selector used to filter the shapes.
         */
        public void setSelector(Selector selector) {
            this.selector = selector;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "excludeShapesBySelector";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Selector selector = config.getSelector();
        ModelTransformer transformer = context.getTransformer();
        Model model = context.getModel();
        Set<Shape> selected = selector.select(model);
        return transformer.filterShapes(model, FunctionalUtils.not(selected::contains));
    }
}
