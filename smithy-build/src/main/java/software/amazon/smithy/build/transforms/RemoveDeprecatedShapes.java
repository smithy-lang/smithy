/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code removeDeprecatedShapes} removes shapes from a model if they have been deprecated before a version or date.
 */
public final class RemoveDeprecatedShapes extends ConfigurableProjectionTransformer<RemoveDeprecatedShapes.Config> {

    /**
     * {@code RemoveDeprecatedShapes} configuration settings.
     */
    public static final class Config {
        private String relativeDate;
        private String relativeVersion;

        /**
         * Gets the date used to filter deprecated shapes.
         *
         * @return The date used to filter deprecated shapes.
         */
        public String getRelativeDate() {
            return relativeDate;
        }

        /**
         * Sets the date used to filter deprecated shapes.
         *
         * @param relativeDate The date used to filter deprecated shapes.
         */
        public void setRelativeDate(String relativeDate) {
            this.relativeDate = relativeDate;
        }

        /**
         * Gets the version used to filter deprecated shapes.
         *
         * @return The version used to filter deprecated shapes.
         */
        public String getRelativeVersion() {
            return relativeVersion;
        }

        /**
         * Sets the version used to filter deprecated shapes.
         *
         * @param relativeVersion The version used to filter deprecated shapes.
         */
        public void setRelativeVersion(String relativeVersion) {
            this.relativeVersion = relativeVersion;
        }
    }

    @Override
    public String getName() {
        return "removeDeprecatedShapes";
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();
        model = transformer.filterDeprecatedRelativeDate(model, config.getRelativeDate());
        model = transformer.filterDeprecatedRelativeVersion(model, config.getRelativeVersion());
        return model;
    }
}
