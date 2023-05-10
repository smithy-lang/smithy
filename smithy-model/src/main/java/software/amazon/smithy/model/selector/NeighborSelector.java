/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.selector;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.Shape;

final class NeighborSelector implements InternalSelector {

    private static final NeighborSelector FORWARD = new NeighborSelector(Collections.emptyList(), Direction.FORWARD);
    private static final NeighborSelector REVERSE = new NeighborSelector(Collections.emptyList(), Direction.REVERSE);

    private final List<String> relTypes;
    private final Direction direction;
    private final Function<Context, NeighborProvider> neighborFactory;

    private NeighborSelector(List<String> relTypes, Direction direction) {
        this.relTypes = Objects.requireNonNull(relTypes);
        this.direction = direction;
        boolean includeTraits = relTypes.contains("trait");
        this.neighborFactory = direction.neighborFactory(includeTraits);
    }

    private enum Direction {
        FORWARD {
            @Override
            Response emit(Context context, Relationship rel, Receiver next) {
                return next.apply(context, rel.getNeighborShape().get());
            }

            @Override
            Function<Context, NeighborProvider> neighborFactory(boolean includeTraits) {
                return includeTraits
                       ? context -> context.neighborIndex.getProviderWithTraitRelationships()
                       : context -> context.neighborIndex.getProvider();
            }
        },
        REVERSE {
            @Override
            Response emit(Context context, Relationship rel, Receiver next) {
                return next.apply(context, rel.getShape());
            }

            @Override
            Function<Context, NeighborProvider> neighborFactory(boolean includeTraits) {
                return includeTraits
                       ? context -> context.neighborIndex.getReverseProviderWithTraitRelationships()
                       : context -> context.neighborIndex.getReverseProvider();
            }
        };

        abstract Response emit(Context context, Relationship rel, Receiver next);

        abstract Function<Context, NeighborProvider> neighborFactory(boolean includeTraits);
    }

    static NeighborSelector forward(List<String> relTypes) {
        return relTypes.isEmpty() ? FORWARD : new NeighborSelector(relTypes, Direction.FORWARD);
    }

    static NeighborSelector reverse(List<String> relTypes) {
        return relTypes.isEmpty() ? REVERSE : new NeighborSelector(relTypes, Direction.REVERSE);
    }

    @Override
    public Response push(Context context, Shape shape, Receiver next) {
        NeighborProvider resolvedProvider = neighborFactory.apply(context);
        for (Relationship rel : resolvedProvider.getNeighbors(shape)) {
            if (matches(rel)) {
                if (direction.emit(context, rel, next) == Response.STOP) {
                    // Stop pushing shapes upstream and propagate the signal to stop.
                    return Response.STOP;
                }
            }
        }

        return Response.CONTINUE;
    }

    private boolean matches(Relationship rel) {
        return rel.getRelationshipType() != RelationshipType.MEMBER_CONTAINER
               && rel.getNeighborShape().isPresent()
               && relTypesMatchesRel(relTypes, rel);
    }

    private static boolean relTypesMatchesRel(List<String> relTypes, Relationship rel) {
        if (relTypes.isEmpty()) {
            return true;
        } else {
            String relType = rel.getSelectorLabel().orElse("");
            return relTypes.contains(relType);
        }
    }
}
