/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code excludeMetadata} removes metadata entries when a metadata key
 * matches {@code keys}.
 */
public final class ExcludeMetadata extends BackwardCompatHelper<ExcludeMetadata.Config> {

    /**
     * {@code excludeMetadata} configuration settings.
     */
    public static final class Config {
        private Set<String> keys = Collections.emptySet();

        /**
         * @return the list of keys to remove from metadata.
         */
        public Set<String> getKeys() {
            return keys;
        }

        /**
         * Sets the list of keys to remove from metadata.
         *
         * @param keys Metadata keys to remove.
         */
        public void setKeys(Set<String> keys) {
            this.keys = keys;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "excludeMetadata";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "keys";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();
        Set<String> keys = config.getKeys();
        return transformer.filterMetadata(model, (key, value) -> !keys.contains(key));
    }
}
