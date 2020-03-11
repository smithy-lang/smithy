/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.Pair;

/**
 *  Renames shapes using a mapping function while ensuring that the
 *  transformed model is in a consistent state.
 *
 *  This transformer ensures that when an aggregate shape is renamed, all
 *  members are updated in the model.
 *
 * @see ModelTransformer#renameShapes
 */
final class RenameShapes {
    private final Map<ShapeId, ShapeId> renamed;

    RenameShapes(Map<ShapeId, ShapeId> renamed) {
        this.renamed = renamed;
    }

    Model transform(ModelTransformer transformer, Model model) {
        renamed.keySet().removeIf(fromId -> fromId.equals(renamed.get(fromId)));
        if (renamed.isEmpty()) {
            return model;
        }
        Model.Builder builder = createRenamedModelBuilder(model, renamed);

        getUpdatedIdRefShapes(model, renamed).forEach(builder::addShape);

        return transformer.removeShapes(builder.build(), getShapesToRemove(model, renamed));
    }

    private Model.Builder createRenamedModelBuilder(Model model, Map<ShapeId, ShapeId> renamed) {
        Model.Builder builder = model.toBuilder();
        renamed.forEach((fromId, toId) -> {
            RenameShapeVisitor renameShapeVisitor = new RenameShapeVisitor(toId);
            Shape newShape = model.expectShape(fromId).accept(renameShapeVisitor);
            builder.addShape(newShape);
            builder.addShapes(newShape.members());
        });
        return builder;
    }

    private Set<Shape> getShapesToRemove(Model model, Map<ShapeId, ShapeId> renamed) {
        return renamed.keySet().stream()
                .map(model::expectShape).collect(Collectors.toSet());
    }

    private Set<Shape> getUpdatedIdRefShapes(Model model, Map<ShapeId, ShapeId> renamed) {
        Set<ShapeId> traitShapeIdsWithIdRef = model.getTraitDefinitions().keySet().stream()
                .filter(shape -> shape.hasTrait(IdRefTrait.class))
                .map(Shape::getId)
                .collect(Collectors.toSet());
        Map<Shape, Set<Trait>> shapeToIdRefTraitsMapping = model.shapes()
                .filter(shape -> traitShapeIdsWithIdRef.stream().anyMatch(shape::hasTrait))
                .map(shape -> getIdRefTraitsPair(shape, traitShapeIdsWithIdRef))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        return shapeToIdRefTraitsMapping.entrySet().stream()
                .map(entry -> buildShapeWithNewIdRefs(entry.getKey(), entry.getValue(), renamed))
                .collect(Collectors.toSet());
    }

    private Shape buildShapeWithNewIdRefs(Shape shape, Set<Trait> idRefTraits, Map<ShapeId, ShapeId> renamed) {
        Collection<Trait> traits = new ArrayList<Trait>();
        shape.getAllTraits().forEach((traitId, trait) -> {
            if (idRefTraits.contains(trait)) {
                StringNode node = trait.toNode().expectStringNode();
                ShapeId nodeValue = ShapeId.from(node.getValue());
                if (renamed.containsKey(nodeValue)) {
                    StringNode newNode = new StringNode(renamed.get(nodeValue).toString(), node.getSourceLocation());
                    Trait newTrait = new DynamicTrait(trait.toShapeId(), newNode);
                    traits.add(newTrait);
                } else {
                    traits.add(trait);
                }
            } else {
                traits.add(trait);
            }
        });
        return Shape.shapeToBuilder(shape).clearTraits().addTraits(traits).build();
    }

    private Pair<Shape, Set<Trait>> getIdRefTraitsPair(Shape shape, Set<ShapeId> traitShapeIdsWithIdRef) {
        Set<Trait> idRefTraits = new HashSet<Trait>();
        shape.getAllTraits().forEach((key, trait) -> {
            if (traitShapeIdsWithIdRef.contains(trait.toShapeId())) {
                idRefTraits.add(trait);
            }
        });
        return Pair.of(shape, idRefTraits);
    }

    private static final class RenameShapeVisitor extends ShapeVisitor.Default<Shape> {

        private final ShapeId toId;

        RenameShapeVisitor(ShapeId toId) {
            this.toId = toId;
        }

        @Override
        public Shape getDefault(Shape fromShape) {
            return Shape.shapeToBuilder(fromShape).id(toId).build();
        }

        @Override
        public Shape listShape(ListShape fromShape) {
            ListShape.Builder builder = fromShape.toBuilder().id(toId);
            return buildShape(fromShape, builder);
        }

        @Override
        public Shape mapShape(MapShape fromShape) {
            ShapeId keyId = ShapeId.fromParts(toId.getNamespace(), toId.getName(), "key");
            ShapeId valueId = ShapeId.fromParts(toId.getNamespace(), toId.getName(), "value");
            return fromShape.toBuilder().id(toId).key(keyId).value(valueId).build();
        }

        @Override
        public Shape setShape(SetShape fromShape) {
            SetShape.Builder builder = fromShape.toBuilder().id(toId);
            return buildShape(fromShape, builder);
        }

        @Override
        public Shape structureShape(StructureShape fromShape) {
            StructureShape.Builder builder = fromShape.toBuilder().id(toId);
            return buildShape(fromShape, builder);
        }

        @Override
        public Shape unionShape(UnionShape fromShape) {
            UnionShape.Builder builder = fromShape.toBuilder().id(toId);
            return buildShape(fromShape, builder);
        }

        private Shape buildShape(Shape fromShape, AbstractShapeBuilder<?, ?> builder) {
            fromShape.members().forEach(member -> {
                ShapeId memberId = updateMemberId(member, toId);
                builder.addMember(member.toBuilder().id(memberId).build());
            });
            return (Shape) builder.build();
        }

        private ShapeId updateMemberId(MemberShape member, ShapeId newContainerId) {
            return ShapeId.fromParts(newContainerId.getNamespace(), newContainerId.getName(), member.getMemberName());
        }
    }
}
