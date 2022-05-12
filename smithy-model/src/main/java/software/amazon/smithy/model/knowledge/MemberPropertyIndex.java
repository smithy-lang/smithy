/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.knowledge;

import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.NotPropertyTrait;

/**
 * Index of structure member -> property inclusion.
 **/
public final class MemberPropertyIndex implements KnowledgeIndex {
    private final Map<ShapeId, Boolean> isPropertyExcluded = new HashMap<>();

    private MemberPropertyIndex(Model model) {
        for (MemberShape memberShape:model.getMemberShapes()) {
            isPropertyExcluded.put(memberShape.toShapeId(), isNotProperty(model, memberShape));
        }
    }

    public static MemberPropertyIndex of(Model model) {
        return model.getKnowledge(MemberPropertyIndex.class, MemberPropertyIndex::new);
    }

    /**
     * Returns true if member is to be ignored as a resource property.
     *
     **/
    public boolean isPropertyExcluded(ShapeId shapeId) {
        return isPropertyExcluded.getOrDefault(shapeId, false);
    }

    private boolean isNotProperty(Model model, MemberShape shape) {
        if (shape.getTrait(NotPropertyTrait.class).isPresent()) {
            return true;
        }
        return shape.getAllTraits().values().stream().map(t -> model.expectShape(t.toShapeId()))
            .filter(t -> t.hasTrait(NotPropertyTrait.class)).findAny().isPresent();
    }
}
