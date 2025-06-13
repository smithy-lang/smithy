/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.Map;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.openapi.fromsmithy.Context;

/**
 * Applies the jsonName trait to a node value if applicable.
 */
public class JsonValueNodeTransformer implements NodeVisitor<Node> {
    private final Context<?> context;
    private final Shape shape;

    /**
     * Construct a JsonValueNodeTransformer.
     *
     * @param context Conversion context. Used to determine if jsonName should be used.
     * @param shape The shape of the node being converted.
     */
    public JsonValueNodeTransformer(Context<?> context, Shape shape) {
        this.context = context;
        this.shape = shape;
    }

    @Override
    public Node booleanNode(BooleanNode node) {
        return node;
    }

    @Override
    public Node nullNode(NullNode node) {
        return node;
    }

    @Override
    public Node numberNode(NumberNode node) {
        return node;
    }

    @Override
    public Node stringNode(StringNode node) {
        return node;
    }

    @Override
    public Node arrayNode(ArrayNode node) {
        ArrayNode.Builder resultBuilder = ArrayNode.builder();
        Shape listShape = shape.asMemberShape()
                .map(m -> context.getModel().expectShape(m.getTarget()))
                .orElse(shape);

        Shape target = context.getModel().expectShape(listShape.asListShape().get().getMember().getTarget());
        JsonValueNodeTransformer elementTransformer = new JsonValueNodeTransformer(context, target);
        for (Node element : node.getElements()) {
            resultBuilder.withValue(element.accept(elementTransformer));
        }
        return resultBuilder.build();
    }

    @Override
    public Node objectNode(ObjectNode node) {
        Shape actual = shape.asMemberShape()
                .map(m -> context.getModel().expectShape(m.getTarget()))
                .orElse(shape);

        if (shape.isMapShape()) {
            return mapNode(actual.asMapShape().get(), node);
        }
        return structuredNode(actual, node);
    }

    private Node structuredNode(Shape structure, ObjectNode node) {
        ObjectNode.Builder resultBuilder = ObjectNode.builder();
        for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
            String key = entry.getKey().getValue();
            if (structure.getMember(key).isPresent()) {
                MemberShape member = structure.getMember(key).get();
                Shape target = context.getModel().expectShape(member.getTarget());
                JsonValueNodeTransformer entryTransformer = new JsonValueNodeTransformer(context, target);
                resultBuilder.withMember(getKey(member), entry.getValue().accept(entryTransformer));
            } else {
                resultBuilder.withMember(key, entry.getValue());
            }
        }
        return resultBuilder.build();
    }

    private String getKey(MemberShape member) {
        if (!context.getJsonSchemaConverter().getConfig().getUseJsonName()) {
            return member.getMemberName();
        }
        return member.getTrait(JsonNameTrait.class)
                .map(JsonNameTrait::getValue)
                .orElse(member.getMemberName());
    }

    private Node mapNode(MapShape map, ObjectNode node) {
        ObjectNode.Builder resultBuilder = ObjectNode.builder();
        Shape target = context.getModel().expectShape(map.getValue().getTarget());
        JsonValueNodeTransformer entryTransformer = new JsonValueNodeTransformer(context, target);
        for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
            resultBuilder.withMember(entry.getKey(), entry.getValue().accept(entryTransformer));
        }
        return resultBuilder.build();
    }
}
