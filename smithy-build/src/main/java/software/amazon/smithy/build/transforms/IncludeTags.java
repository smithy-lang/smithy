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
 * {@code includeTags} removes tags from shapes and trait
 * definitions that are not in the set of tags defined in
 * the {@code tags} property.
 */
public final class IncludeTags extends BackwardCompatHelper<IncludeTags.Config> {

    /**
     * {@code includeTags} configuration.
     */
    public static final class Config {
        private Set<String> tags = Collections.emptySet();

        /**
         * Gets the set of tags that are retained in the model.
         *
         * @return Returns the tags to retain.
         */
        public Set<String> getTags() {
            return tags;
        }

        /**
         * Sets the set of tags that are retained in the model.
         *
         * @param tags The tags to retain in the model.
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
        return "includeTags";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "tags";
    }

    @Override
    public Model transformWithConfig(TransformContext context, Config config) {
        return TagUtils.includeShapeTags(context.getTransformer(), context.getModel(), config.getTags());
    }
}
