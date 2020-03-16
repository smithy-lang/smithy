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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Pair;

/**
 *  Renames shapes using ShapeId pairs while ensuring that the
 *  transformed model is in a consistent state.
 *
 *  Member shapes are updated when their containing shape is updated.
 *
 *  Trait references to ShapeId values are also updated.
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

        Set<String> toRename = renamed.keySet().stream().map(ShapeId::toString).collect(Collectors.toSet());

        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode node = serializer.serialize(model);

        return Model.assembler()
                .addDocumentNode(node.accept(new RenameShapeVisitor(toRename, renamed)))
                .assemble()
                .unwrap();
    }

    private static final class RenameShapeVisitor extends NodeVisitor.Default<Node> {

        private final Set<String> toRename;
        private final Map<ShapeId, ShapeId> shapeMapping;

        RenameShapeVisitor(Set<String> toRename, Map<ShapeId, ShapeId> shapeMapping) {
            this.toRename = toRename;
            this.shapeMapping = shapeMapping;
        }

        @Override
        protected Node getDefault(Node node) {
            return node;
        }

        @Override
        public Node arrayNode(ArrayNode node) {
            return node.getElements().stream().map(element -> element.accept(this)).collect(ArrayNode.collect());
        }

        @Override
        public Node objectNode(ObjectNode node) {
            return node.getMembers().entrySet().stream()
                    .map(entry -> Pair.of(entry.getKey().accept(this), entry.getValue().accept(this)))
                    .collect(ObjectNode.collect(pair -> pair.getLeft().expectStringNode(), Pair::getRight));
        }

        @Override
        public Node stringNode(StringNode node) {
            if (toRename.contains(node.getValue())) {
                ShapeId nodeShapeId = node.expectShapeId();
                return new StringNode(shapeMapping.get(nodeShapeId).toString(), node.getSourceLocation());
            }
            return node;
        }
    }
}
