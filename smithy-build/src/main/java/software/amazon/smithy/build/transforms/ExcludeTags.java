/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;

/**
 * {@code excludeTags} removes tags from shapes and trait definitions
 * that match any of the provided {@code tags}.
 */
public final class ExcludeTags extends BackwardCompatHelper<ExcludeTags.Config> {

    /**
     * {@code excludeTags} configuration.
     */
    public static final class Config {
        private Set<String> tags = Collections.emptySet();

        /**
         * Gets the set of tags that are removed from the model.
         *
         * @return Returns the tags to remove.
         */
        public Set<String> getTags() {
            return tags;
        }

        /**
         * Sets the set of tags that are removed from the model.
         *
         * @param tags The tags to remove from the model.
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
        return "excludeTags";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "tags";
    }

    @Override
    public Model transformWithConfig(TransformContext context, Config config) {
        return TagUtils.excludeShapeTags(context.getTransformer(), context.getModel(), config.getTags());
    }
}
