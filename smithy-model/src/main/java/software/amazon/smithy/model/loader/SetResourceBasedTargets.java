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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Sets member targets based on referenced resource identifiers.
 *
 * <p>Structures can elide the targets of members if they're bound to a resource
 * and that resource has an identifier with a matching name. Here we set the
 * target based on that information.
 */
final class SetResourceBasedTargets implements PendingShapeModifier {
    private final ShapeId resourceId;

    SetResourceBasedTargets(ShapeId resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public Set<ShapeId> getDependencies() {
        return Collections.singleton(resourceId);
    }

    @Override
    public void modifyMember(
            AbstractShapeBuilder<?, ?> shapeBuilder,
            MemberShape.Builder memberBuilder,
            TraitContainer resolvedTraits,
            Map<ShapeId, Shape> shapeMap
    ) {
        // Fast-fail the common case of the target having already been set.
        if (memberBuilder.getTarget() != null) {
            return;
        }

        Shape fromShape = shapeMap.get(resourceId);
        if (!fromShape.isResourceShape()) {
            String message = String.format(
                    "The target of the `for` production must be a resource shape, but found a %s shape: %s",
                    fromShape.getType(),
                    resourceId
            );
            throw new SourceException(message, shapeBuilder.getSourceLocation());
        }

        ResourceShape resource = fromShape.asResourceShape().get();
        String name = memberBuilder.getId().getMember().get();
        if (resource.getIdentifiers().containsKey(name)) {
            memberBuilder.target(resource.getIdentifiers().get(name));
        }
        if (resource.getProperties().containsKey(name)) {
            memberBuilder.target(resource.getProperties().get(name));
        }
    }
}
