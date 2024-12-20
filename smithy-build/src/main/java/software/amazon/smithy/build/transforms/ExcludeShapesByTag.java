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
 * {@code excludeShapesByTag} removes shapes if they are tagged with one or more
 * of the given arguments.
 *
 * <p>Prelude shapes are not removed by this transformer.
 */
public final class ExcludeShapesByTag extends BackwardCompatHelper<ExcludeShapesByTag.Config> {

    /**
     * {@code excludeShapesByTag} configuration.
     */
    public static final class Config {
        private Set<String> tags = Collections.emptySet();

        /**
         * Gets the set of tags that causes shapes to be removed.
         *
         * @return Returns the removal tags.
         */
        public Set<String> getTags() {
            return tags;
        }

        /**
         * Sets the set of tags that causes shapes to be removed.
         *
         * @param tags Tags that cause shapes to be removed.
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
        return "excludeShapesByTag";
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
            return Prelude.isPreludeShape(shape) || shape.getTags().stream().noneMatch(includeTags::contains);
        });
    }
}
