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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;

final class NeighborSelector implements InternalSelector {

    static final NeighborSelector FORWARD = new NeighborSelector(Collections.emptyList(), Direction.FORWARD);
    static final NeighborSelector REVERSE = new NeighborSelector(Collections.emptyList(), Direction.REVERSE);

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
            protected Response emit(Context context, Relationship rel, Receiver next) {
                return next.apply(context, rel.expectNeighborShape());
            }

            @Override
            protected Function<Context, NeighborProvider> neighborFactory(boolean includeTraits) {
                return includeTraits
                       ? context -> context.neighborIndex.getProviderWithTraitRelationships()
                       : context -> context.neighborIndex.getProvider();
            }
        },
        REVERSE {
            @Override
            protected Response emit(Context context, Relationship rel, Receiver next) {
                return next.apply(context, rel.getShape());
            }

            @Override
            protected Function<Context, NeighborProvider> neighborFactory(boolean includeTraits) {
                return includeTraits
                       ? context -> context.neighborIndex.getReverseProviderWithTraitRelationships()
                       : context -> context.neighborIndex.getReverseProvider();
            }
        };

        protected abstract Response emit(Context context, Relationship rel, Receiver next);

        protected abstract Function<Context, NeighborProvider> neighborFactory(boolean includeTraits);
    }

    static InternalSelector forward(List<String> relationships) {
        return fromStrings(relationships, Direction.FORWARD);
    }

    static InternalSelector reverse(List<String> relationships) {
        return fromStrings(relationships, Direction.REVERSE);
    }

    private static InternalSelector fromStrings(List<String> relationships, Direction direction) {
        // Handle the deprecated synthetic "bound" relationship.
        boolean bound = relationships.removeIf(r -> r.equals("bound"));

        // Handle the deprecated synthetic "instanceOperation" relationship.
        boolean instanceOperation = relationships.removeIf(r -> r.equals("instanceOperation"));

        // The common case of not using the deprecated selectors should not need to wrap in additional selectors.
        if (!bound && !instanceOperation) {
            return new NeighborSelector(relationships, direction);
        }

        // Try to exact-size the array, though it could be oversized if there are only deprecated rels (rare).
        List<InternalSelector> predicates = new ArrayList<>(1 + (bound ? 1 : 0) + (instanceOperation ? 1 : 0));

        if (!relationships.isEmpty()) {
            predicates.add(new NeighborSelector(relationships, direction));
        }

        if (bound) {
            predicates.add(new BoundRelationship());
        }

        if (instanceOperation) {
            boolean traits = relationships.contains("trait");
            predicates.add(new InstanceOperationRelationship(direction, direction.neighborFactory(traits)));
        }

        return IsSelector.of(predicates);
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

    @Deprecated
    private static final class BoundRelationship implements InternalSelector {
        @Override
        public Response push(Context ctx, Shape shape, Receiver next) {
            // Emulating the previously buggy behavior of only emitting bound rels from services and not operations.
            if (shape.isResourceShape()) {
                for (ResourceShape resource : ctx.getModel().getResourceShapes()) {
                    if (resource.getResources().contains(shape.getId())) {
                        if (next.apply(ctx, resource) == Response.STOP) {
                            return Response.STOP;
                        }
                    }
                }
                for (ServiceShape service : ctx.getModel().getServiceShapes()) {
                    if (service.getResources().contains(shape.getId())) {
                        if (next.apply(ctx, service) == Response.STOP) {
                            return Response.STOP;
                        }
                    }
                }
            }
            return InternalSelector.Response.CONTINUE;
        }
    }

    @Deprecated
    private static final class InstanceOperationRelationship implements InternalSelector {

        private final Direction direction;
        private final Function<Context, NeighborProvider> neighborFactory;

        InstanceOperationRelationship(Direction direction, Function<Context, NeighborProvider> neighborFactory) {
            this.direction = direction;
            this.neighborFactory = neighborFactory;
        }

        @Override
        public Response push(Context ctx, Shape shape, Receiver next) {
            if (shape.isResourceShape()) {
                NeighborProvider resolvedProvider = neighborFactory.apply(ctx);
                for (Relationship rel : resolvedProvider.getNeighbors(shape)) {
                    if (rel.getRelationshipType().isInstanceOperationBinding()) {
                        if (direction.emit(ctx, rel, next) == Response.STOP) {
                            return Response.STOP;
                        }
                    }
                }
            }
            return Response.CONTINUE;
        }
    }
}
