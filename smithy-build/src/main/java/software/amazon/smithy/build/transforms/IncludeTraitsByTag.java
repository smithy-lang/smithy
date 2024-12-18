/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.Tagged;

/**
 * {@code includeTraitsByTag} removes trait definitions from a model if
 * the definition does not contain at least one of the provided {@code tags}.
 *
 * <p>This transformer does not remove prelude traits.
 */
public final class IncludeTraitsByTag extends BackwardCompatHelper<IncludeTraitsByTag.Config> {

    /**
     * {@code includeTraitsByTag} configuration settings.
     */
    public static final class Config {
        private Set<String> tags = Collections.emptySet();

        /**
         * @return the list of tags that must be present for a trait to be kept.
         */
        public Set<String> getTags() {
            return tags;
        }

        /**
         * Sets the list of tags that must be present for a trait to be included
         * in the filtered model.
         *
         * @param tags Tags to set.
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
        return "includeTraitsByTag";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "tags";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();
        Set<String> tags = config.getTags();
        return transformer.removeShapesIf(model, shape -> removeIfPredicate(shape, tags));
    }

    private boolean removeIfPredicate(Shape shape, Collection<String> tags) {
        return !Prelude.isPreludeShape(shape)
                && shape.hasTrait(TraitDefinition.class)
                && !hasAnyTag(shape, tags);
    }

    private boolean hasAnyTag(Tagged tagged, Collection<String> tags) {
        return tagged.getTags().stream().anyMatch(tags::contains);
    }
}
