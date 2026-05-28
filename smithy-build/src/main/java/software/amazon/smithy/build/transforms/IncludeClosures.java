/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.transform.ModelTransformException;

/**
 * {@code includeClosures} filters the model down to the shapes in one or more
 * shape closures declared via {@code shapeClosures} metadata.
 */
public final class IncludeClosures extends BackwardCompatHelper<IncludeClosures.Config> {

    /**
     * {@code includeClosures} configuration.
     */
    public static final class Config {
        private Set<String> closures = Collections.emptySet();

        /**
         * @return The ids of the closures to include.
         */
        public Set<String> getClosures() {
            return closures;
        }

        /**
         * Sets the ids of the closures to include.
         *
         * @param closures The closure ids.
         */
        public void setClosures(Set<String> closures) {
            this.closures = closures;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "includeClosures";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "closures";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        if (config.getClosures() == null || config.getClosures().isEmpty()) {
            throw new SmithyBuildException(
                    "'closures' property must be set and non-empty on the includeClosures transformer.");
        }
        try {
            return context.getTransformer().includeClosures(context.getModel(), config.getClosures());
        } catch (ModelTransformException e) {
            throw new SmithyBuildException(e.getMessage(), e);
        }
    }
}
