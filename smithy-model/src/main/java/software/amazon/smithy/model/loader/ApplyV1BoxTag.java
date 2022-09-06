/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.TagsTrait;

/**
 * Ensures that the box trait on root level shapes isn't lost when serializing
 * a v1 model as a v2 model.
 *
 * <p>If a root level shape has the box trait, then a "box-v1" tag is added to
 * the shape so that it is serialized in a 2.0 model. When loading a 2.0 model,
 * if the "box-v1" tag is found, then a synthetic box trait is added to the
 * shape.
 *
 * <p>This is useful for tooling that integrates Smithy models with other
 * modeling languages that require knowledge of the box trait on root level
 * shapes.
 */
enum ApplyV1BoxTag {

    // This is a singleton implemented via an enum.
    INSTANCE;

    static final String TAG_NAME = "box-v1";

    void handleTagBoxing(AbstractShapeBuilder<?, ?> builder) {
        if (builder.getAllTraits().containsKey(BoxTrait.ID)) {
            TagsTrait tags = (TagsTrait) builder.getAllTraits().get(TagsTrait.ID);
            if (tags == null) {
                tags = TagsTrait.builder()
                        .sourceLocation(builder.getSourceLocation())
                        .addValue(TAG_NAME)
                        .build();
                builder.addTrait(tags);
            } else if (!tags.hasTag(TAG_NAME)) {
                builder.addTrait(tags.toBuilder().addValue(TAG_NAME).build());
            }
        } else {
            TagsTrait tags = (TagsTrait) builder.getAllTraits().get(TagsTrait.ID);
            if (tags != null && tags.hasTag(TAG_NAME)) {
                builder.addTrait(new BoxTrait());
            }
        }
    }
}
