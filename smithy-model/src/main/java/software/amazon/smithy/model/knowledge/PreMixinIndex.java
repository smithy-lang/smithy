/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;

/**
 * Index of shapes stripped down to only their introduced properties.
 *
 * <p>This is primarily useful in serialization to determine what
 * properties to actually serialize vs what is computed as part of
 * mixins.
 */
public final class PreMixinIndex implements KnowledgeIndex {

    private final Map<ShapeId, Shape> preMixinShapes = new HashMap<>();

    public PreMixinIndex(Model model) {
        MixinUnroller unroller = new MixinUnroller(model);
        model.shapes()
            .filter(shape -> !shape.isMemberShape())
            .forEach(shape -> {
                if (!shape.getMixins().isEmpty()) {
                    preMixinShapes.put(shape.getId(), shape.accept(unroller));
                }
            });
    }

    public static PreMixinIndex of(Model model) {
        return model.getKnowledge(PreMixinIndex.class, PreMixinIndex::new);
    }

    /**
     * Gets a version of the shape that has mixin properties stripped out.
     *
     * <p>NOTE: mixin members with introduced traits WILL be present in
     * their entirety. The only way to determine if those members originally
     * were defined in a mixin is to have an original copy of the shape to
     * compare against. Any members of the shape that themselves have mixins
     * are inherited.
     *
     * @param shape The shape to strip mixin data from.
     * @return A version of the shape without mixin data.
     */
    public Shape getPreMixinShape(Shape shape) {
        return preMixinShapes.getOrDefault(shape.getId(), shape);
    }

    private static final class MixinUnroller extends ShapeVisitor.Default<Shape> {

        private final Model model;

        private MixinUnroller(Model model) {
            this.model = model;
        }

        @Override
        protected Shape getDefault(Shape shape) {
            return Shape.shapeToBuilder(shape)
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values())
                    .build();
        }

        @Override
        public Shape listShape(ListShape shape) {
            return shape.toBuilder()
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values())
                    .member((MemberShape) shape.getMember().accept(this))
                    .build();
        }

        @Override
        public Shape setShape(SetShape shape) {
            return shape.toBuilder()
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values())
                    .member((MemberShape) shape.getMember().accept(this))
                    .build();
        }

        @Override
        public Shape mapShape(MapShape shape) {
            return shape.toBuilder()
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values())
                    .key((MemberShape) shape.getKey().accept(this))
                    .value((MemberShape) shape.getValue().accept(this))
                    .build();
        }

        @Override
        public Shape structureShape(StructureShape shape) {
            StructureShape.Builder builder = shape.toBuilder()
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values());
            unrollMembers(shape, builder::addMember);
            return builder.build();
        }

        @Override
        public Shape unionShape(UnionShape shape) {
            UnionShape.Builder builder = shape.toBuilder()
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values());
            unrollMembers(shape, builder::addMember);
            return builder.build();
        }

        private void unrollMembers(Shape shape, Consumer<MemberShape> consumer) {
            for (MemberShape member : shape.members()) {
                if (member.getMixins().isEmpty()) {
                    consumer.accept(member);
                } else {
                    consumer.accept((MemberShape) member.accept(this));
                }
            }
        }

        @Override
        public Shape operationShape(OperationShape shape) {
            OperationShape.Builder builder = shape.toBuilder()
                    .clearMixins()
                    .traits(shape.getIntroducedTraits().values())
                    .clearErrors();

            Set<ShapeId> previousErrors = new HashSet<>();
            for (ShapeId mixinId : shape.getMixins()) {
                previousErrors.addAll(model.expectShape(mixinId, OperationShape.class).getErrors());
            }

            addIntroduced(shape.getErrors(), previousErrors, builder::addError);

            return builder.build();
        }

        @Override
        public Shape serviceShape(ServiceShape shape) {
            ServiceShape.Builder builder = shape.toBuilder()
                    .clearMixins()
                    .clearOperations()
                    .clearResources()
                    .clearErrors()
                    .clearRename()
                    .traits(shape.getIntroducedTraits().values());

            String previousVersion = "";
            Set<ShapeId> previousOperations = new HashSet<>();
            Set<ShapeId> previousResources = new HashSet<>();
            Set<ShapeId> previousErrors = new HashSet<>();
            Map<ShapeId, String> previousRename = new HashMap<>();

            for (ShapeId mixinId : shape.getMixins()) {
                ServiceShape mixin = model.expectShape(mixinId, ServiceShape.class);
                previousVersion = mixin.getVersion();
                previousOperations.addAll(mixin.getOperations());
                previousResources.addAll(mixin.getResources());
                previousErrors.addAll(mixin.getErrors());
                previousRename.putAll(mixin.getRename());
            }

            if (shape.getVersion().equals(previousVersion)) {
                builder.version("");
            }

            addIntroduced(shape.getOperations(), previousOperations, builder::addOperation);
            addIntroduced(shape.getResources(), previousResources, builder::addResource);
            addIntroduced(shape.getErrors(), previousErrors, builder::addError);

            for (Map.Entry<ShapeId, String> entry : shape.getRename().entrySet()) {
                if (!entry.getValue().equals(previousRename.get(entry.getKey()))) {
                    builder.putRename(entry.getKey(), entry.getValue());
                }
            }

            return builder.build();
        }

        private void addIntroduced(
                Collection<ShapeId> current,
                Collection<ShapeId> previous,
                Consumer<ShapeId> consumer
        ) {
            for (ShapeId shapeId : current) {
                if (!previous.contains(shapeId)) {
                    consumer.accept(shapeId);
                }
            }
        }
    }
}
