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

import static software.amazon.smithy.model.node.Node.loadArrayOfString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorSyntaxException;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.ListUtils;

/**
 * Model loading utility methods.
 */
@SuppressWarnings("checkstyle:declarationorder")
public final class LoaderUtils {
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
    private static final List<String> TRAIT_DEFINITION_PROPERTY_NAMES = ListUtils.of(
            TraitDefinition.SELECTOR_KEY, TraitDefinition.STRUCTURALLY_EXCLUSIVE_KEY, TraitDefinition.SHAPE_KEY,
            TraitDefinition.TAGS_KEY, TraitDefinition.CONFLICTS_KEY, TraitDefinition.DOCUMENTATION_KEY,
            TraitDefinition.EXTERNAL_DOCUMENTATION_KEY, TraitDefinition.DEPRECATED_KEY,
            TraitDefinition.DEPRECATION_REASON_KEY);

    private LoaderUtils() {}

    /**
     * Reads a file into a UTF-8 encoded string.
     * @param path Path to the file to read.
     * @return Returns the contents of the file.
     * @throws RuntimeException if the file can't be read or encoded.
     */
    public static String readUtf8File(String path) {
        return readFile(path, StandardCharsets.UTF_8);
    }

    private static String readFile(String path, Charset encoding) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded, encoding);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads an InputStream into a string.
     *
     * @param inputStream Input stream to read.
     * @param charSet Character encoding to read.
     * @return Returns the input stream as a string.
     * @throws RuntimeException if the stream can't be read or encoded.
     */
    public static String readInputStream(InputStream inputStream, String charSet) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;

        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            return result.toString(charSet);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

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
                String target = entry.getValue().expectStringNode().getValue();
                visitor.onShapeTarget(shapeId.getNamespace(), target, id -> builder.addIdentifier(name, id));
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

    /**
     * Registers a trait definition with a {@code LoaderVisitor}.
     *
     * @param namespace Namespace that contains the definition.
     * @param name Name of the trait definition.
     * @param node Value that contains the definition.
     * @param visitor Visitor to add the loaded definition to.
     */
    static void loadTraitDefinition(String namespace, String name, Node node, LoaderVisitor visitor) {
        ObjectNode members = node.expectObjectNode("Trait definitions must be defined using object nodes.");
        members.warnIfAdditionalProperties(TRAIT_DEFINITION_PROPERTY_NAMES);
        validateTraitDefinitionName(name, node);

        TraitDefinition.Builder builder = TraitDefinition.builder()
                .name(namespace + "#" + name)
                .sourceLocation(node.getSourceLocation());

        members.getMember(TraitDefinition.SELECTOR_KEY)
                .map(LoaderUtils::loadSelector)
                .ifPresent(builder::selector);

        members.getBooleanMember(TraitDefinition.STRUCTURALLY_EXCLUSIVE_KEY)
                .map(BooleanNode::getValue)
                .ifPresent(builder::structurallyExclusive);

        // Resolve shape targets only after all shapes have been loaded by the assembler.
        members.getStringMember(TraitDefinition.SHAPE_KEY)
                .map(StringNode::getValue)
                .ifPresent(shape -> visitor.onShapeTarget(namespace, shape, builder::shape));

        members.getMember(TraitDefinition.TAGS_KEY)
                .ifPresent(values -> loadArrayOfString(TraitDefinition.TAGS_KEY, values)
                        .forEach(builder::addTag));

        members.getMember(TraitDefinition.CONFLICTS_KEY)
                .ifPresent(values -> loadArrayOfString(TraitDefinition.CONFLICTS_KEY, values)
                        .forEach(builder::addConflict));

        members.getStringMember(TraitDefinition.DOCUMENTATION_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::documentation);

        members.getStringMember(TraitDefinition.EXTERNAL_DOCUMENTATION_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::externalDocumentation);

        members.getBooleanMember(TraitDefinition.DEPRECATED_KEY)
                .map(BooleanNode::getValue)
                .ifPresent(builder::deprecated);

        members.getStringMember(TraitDefinition.DEPRECATION_REASON_KEY)
                .map(StringNode::getValue)
                .ifPresent(builder::deprecationReason);

        visitor.onTraitDef(builder);
    }

    private static void validateTraitDefinitionName(String name, FromSourceLocation sourceLocation) {
        if (!ShapeId.IDENTIFIER_PATTERN.matcher(name).find()) {
            throw new SourceException(
                    "Invalid trait name `" + name + "`. Trait names must adhere to the identifier grammar: "
                    + "^" + ShapeId.IDENTIFIER + "$", sourceLocation);
        }
    }

    private static Selector loadSelector(Node node) {
        try {
            return Selector.parse(node.expectStringNode().getValue());
        } catch (SelectorSyntaxException e) {
            throw new SourceException(e.getMessage(), node, e);
        }
    }
}
