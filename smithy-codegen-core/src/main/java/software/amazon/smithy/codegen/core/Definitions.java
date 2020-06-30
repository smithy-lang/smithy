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

package software.amazon.smithy.codegen.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import software.amazon.smithy.model.node.FromNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * Class that defines the acceptable values that can be used in ShapeLink objects.
 * <p>
 * The types {@link Map} defines the set of types that can be used in a {@link ShapeLink} object.
 * Each key is the name of the type, and each value is a description of the type.
 * For programming languages, these types represent language-specific components.
 * For example, in Java, types should map to the possible values of {@link java.lang.annotation.ElementType}.
 * </p>
 * <p>
 * The tags {@link Map} defines the set of tags that can be used in a {@link ShapeLink} object.
 * Each key is the name of the tag, and each value is the description of the tag.
 * Tags are used to provide semantics to links. Tools that evaluate trace models
 * use these tags to inform their analysis. For example, a tag for an AWS SDK code
 * generator could be "requestBuilder" to indicate that a class is used as a builder for a request.
 * </p>
 */
public class Definitions implements ToNode, FromNode, ValidateRequirements {
    public final String typeText = "types";
    public final String tagsText = "tags";
    private Map<String, String> tags;
    private Map<String, String> types;
    private NodeMapper nodeMapper = new NodeMapper();


    /**
     * Converts an ObjectNode that represents the definitions section of the
     * trace file into a types maps and tags map.
     *
     * @param jsonNode ObjectNode that contains the JSON data inside the definitions tag of
     *                 the trace file
     */
    @Override
    public void fromNode(Node jsonNode) {
        ObjectNode node = jsonNode.expectObjectNode();

        types = nodeMapper.deserializeMap(node.expectObjectMember(typeText), Map.class, String.class);
        tags = nodeMapper.deserializeMap(node.expectObjectMember(tagsText), Map.class, String.class);

        //error handling
        validateRequiredFields();
    }

    /**
     * Parses a definitions file and converts it into a definitions object. This is useful
     * in the scenario when the user is provided the definitions specification and must create
     * the trace file from that definitions specification.
     *
     * @param filename a definitions file URI - see example
     * @return ObjectNode corresponding to parsed URI
     * @throws FileNotFoundException if the definitions file path is not found
     */
    public ObjectNode fromDefinitionsFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        return Node.parse(stream).expectObjectNode();
    }

    /**
     * Converts the types and tags Maps into a single ObjectNode.
     *
     * @return an ObjectNode that contains two ObjectNode children; one contains the tag map structure the
     * other contains the type map structure.
     */
    @Override
    public ObjectNode toNode() {
        //error handling
        validateRequiredFields();

        Map<String, Map<String, String>> toSerialize = new HashMap<>();
        toSerialize.put(typeText, types);
        toSerialize.put(tagsText, tags);

        return nodeMapper.serialize(toSerialize).expectObjectNode();
    }

    /**
     * Checks if all of the Definition's required fields are not null.
     *
     * @throws NullPointerException if any of the required fields are null
     */
    @Override
    public void validateRequiredFields() {
        Objects.requireNonNull(types);
        Objects.requireNonNull(tags);
    }

    /**
     * Gets this Definition's Tags Map.
     *
     * @return this Definition's Tags Map
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * Sets this Definition's Tags Map.
     *
     * @param tags tags map for definitions
     */
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Gets this Definition's Types Map.
     *
     * @return this Definition's Type's Map
     */
    public Map<String, String> getTypes() {
        return types;
    }

    /**
     * Sets this Definition's Types Map.
     *
     * @param types types Map for Definitions
     */
    public void setTypes(Map<String, String> types) {
        this.types = types;
    }
}
