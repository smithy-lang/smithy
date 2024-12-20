/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.utils.Pair;

/**
 *  Renames shapes using ShapeId pairs while ensuring that the
 *  transformed model is in a consistent state.
 *
 *  <p>Member shapes are updated when their containing shape is updated.
 *
 *  <p>Trait references to ShapeId values are also updated.
 */
final class RenameShapes {
    private final Map<ShapeId, ShapeId> renamed;
    private final ModelAssembler assembler;

    RenameShapes(Map<ShapeId, ShapeId> renamed, Supplier<ModelAssembler> modelAssemblerSupplier) {
        this.renamed = new HashMap<>(renamed);
        this.assembler = modelAssemblerSupplier.get();
    }

    Model transform(ModelTransformer transformer, Model model) {
        // Remove any no-op pairs to avoid renaming shapes unnecessarily.
        renamed.keySet().removeIf(fromId -> fromId.equals(renamed.get(fromId)));
        if (renamed.isEmpty()) {
            return model;
        }

        // Creates a set that will be used for checking if a string value needs to be renamed or not.
        Set<String> toRename = renamed.keySet()
                .stream()
                .map(ShapeId::toString)
                .collect(Collectors.toSet());

        // TODO: this transform serializes the model, then deserializes it. Because of this, if the model
        //  contained sets via loading a 1.0 model, then the set will be serialized in a 2.0 as a list.
        //  To restore them to sets, this track the sets, then change the types after renaming. We should
        //  update this to eventually not need to serialize an intermediate model.
        Set<SetShape> sets = model.getSetShapes();

        // This transformer converts the model into an ObjectNode. This approach was chosen because the
        // JSON AST format includes fully qualified shape ID values, making it possible rename shapes across
        // the model by only needing to compare and replace StringNode values.
        ModelSerializer serializer = ModelSerializer.builder().build();
        ObjectNode node = serializer.serialize(model);

        // Use visitor to traverse node and rebuild model.
        Node newModel = node.accept(new RenameShapeVisitor(toRename, renamed));

        ValidatedResult<Model> result = assembler.addDocumentNode(newModel).assemble();

        // Transformers shouldn't perform validation ideally. They should only throw errors if the model
        // can't be transformed.
        Model modelResult = result.getResult().orElseGet(result::unwrap);

        return retypeListsBackToSets(transformer, modelResult, sets, renamed);
    }

    private Model retypeListsBackToSets(
            ModelTransformer transformer,
            Model model,
            Set<SetShape> sets,
            Map<ShapeId, ShapeId> renamed
    ) {
        if (sets.isEmpty()) {
            return model;
        }

        Map<ShapeId, ShapeType> retype = new HashMap<>(sets.size());
        for (SetShape shape : sets) {
            ShapeId renamedId = renamed.getOrDefault(shape.getId(), shape.getId());
            retype.put(renamedId, ShapeType.SET);
        }

        return transformer.changeShapeType(model, retype);
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
            return node.getElements()
                    .stream()
                    .map(element -> element.accept(this))
                    .collect(ArrayNode.collect());
        }

        @Override
        public Node objectNode(ObjectNode node) {
            return node.getMembers()
                    .entrySet()
                    .stream()
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
