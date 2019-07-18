/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

/**
 * Model loading utility methods.
 */
@SuppressWarnings("checkstyle:declarationorder")
final class LoaderUtils {
    private static final String CREATE_KEY = "create";
    private static final String READ_KEY = "read";
    private static final String UPDATE_KEY = "update";
    private static final String DELETE_KEY = "delete";
    private static final String LIST_KEY = "list";
    private static final String RESOURCES_KEY = "resources";
    private static final String OPERATIONS_KEY = "operations";
    private static final String IDENTIFIERS_KEY = "identifiers";
    private static final String TYPE_KEY = "type";
    private static final String VERSION_KEY = "version";
    static final Collection<String> RESOURCE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, CREATE_KEY, READ_KEY, UPDATE_KEY, DELETE_KEY, LIST_KEY,
            IDENTIFIERS_KEY, RESOURCES_KEY, OPERATIONS_KEY);
    static final List<String> SERVICE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, VERSION_KEY, OPERATIONS_KEY, RESOURCES_KEY);

    private LoaderUtils() {}

    static void loadServiceObject(ServiceShape.Builder builder, ShapeId shapeId, ObjectNode shapeNode) {
        builder.version(shapeNode.expectMember(VERSION_KEY).expectStringNode().getValue());
        LoaderUtils.optionalIdList(shapeNode, shapeId.getNamespace(), OPERATIONS_KEY).forEach(builder::addOperation);
        LoaderUtils.optionalIdList(shapeNode, shapeId.getNamespace(), RESOURCES_KEY).forEach(builder::addResource);
    }

    static void loadResourceObject(
            ResourceShape.Builder builder,
            ShapeId shapeId,
            ObjectNode shapeNode,
            LoaderVisitor visitor
    ) {
        optionalId(shapeNode, shapeId.getNamespace(), CREATE_KEY).ifPresent(builder::create);
        optionalId(shapeNode, shapeId.getNamespace(), READ_KEY).ifPresent(builder::read);
        optionalId(shapeNode, shapeId.getNamespace(), UPDATE_KEY).ifPresent(builder::update);
        optionalId(shapeNode, shapeId.getNamespace(), DELETE_KEY).ifPresent(builder::delete);
        optionalId(shapeNode, shapeId.getNamespace(), LIST_KEY).ifPresent(builder::list);
        optionalIdList(shapeNode, shapeId.getNamespace(), OPERATIONS_KEY).forEach(builder::addOperation);
        optionalIdList(shapeNode, shapeId.getNamespace(), RESOURCES_KEY).forEach(builder::addResource);

        // Load identifiers and resolve forward references.
        shapeNode.getObjectMember(IDENTIFIERS_KEY).ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                visitor.onShapeTarget(target.getValue(), target, id -> builder.addIdentifier(name, id));
            }
        });
    }

    static Optional<ShapeId> optionalId(ObjectNode node, String namespace, String name) {
        return node.getStringMember(name).map(stringNode -> stringNode.expectShapeId(namespace));
    }

    static List<ShapeId> optionalIdList(ObjectNode node, String namespace, String name) {
        return node.getArrayMember(name)
                .map(array -> array.getElements().stream()
                        .map(Node::expectStringNode)
                        .map(s -> s.expectShapeId(namespace))
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }
}
