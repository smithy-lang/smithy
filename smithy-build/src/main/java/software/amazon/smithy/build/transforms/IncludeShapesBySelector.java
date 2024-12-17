/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code includeShapesBySelector} includes the shapes matching the given selector.
 *
 * <p>Prelude shapes are not removed by this transformer.
 */
public final class IncludeShapesBySelector extends ConfigurableProjectionTransformer<IncludeShapesBySelector.Config> {

    /**
     * {@code includeShapesBySelector} configuration.
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
        return "includeShapesBySelector";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Selector selector = config.getSelector();
        ModelTransformer transformer = context.getTransformer();
        Model model = context.getModel();
        Set<Shape> selected = selector.select(model);
        return transformer.filterShapes(model, selected::contains);
    }
}
