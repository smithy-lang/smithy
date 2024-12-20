/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code includeShapesByTag} removes shapes and trait definitions
 * that are not tagged with at least one of the tags provided
 * in the {@code tags} argument.
 *
 * <p>Prelude shapes are not removed by this transformer.
 */
public final class IncludeShapesByTag extends BackwardCompatHelper<IncludeShapesByTag.Config> {

    /**
     * {@code includeShapesByTag} configuration.
     */
    public static final class Config {
        private Set<String> tags = Collections.emptySet();

        /**
         * Gets the set of tags that cause shapes to be included.
         *
         * @return Returns the inclusion tags.
         */
        public Set<String> getTags() {
            return tags;
        }

        /**
         * Sets the set of tags that cause shapes to be included.
         *
         * @param tags Tags that cause shapes to be included.
         */
        public void setTags(Set<String> tags) {
            this.tags = tags;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "includeShapesByTag";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "tags";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Set<String> includeTags = config.getTags();
        ModelTransformer transformer = context.getTransformer();
        Model model = context.getModel();
        return transformer.filterShapes(model, shape -> {
            return Prelude.isPreludeShape(shape) || shape.getTags().stream().anyMatch(includeTags::contains);
        });
    }
}
