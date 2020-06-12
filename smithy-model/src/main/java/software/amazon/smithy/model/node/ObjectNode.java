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

package software.amazon.smithy.model.node;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents an object node.
 */
public final class ObjectNode extends Node implements ToSmithyBuilder<ObjectNode> {
    static final ObjectNode EMPTY = new ObjectNode(MapUtils.of(), SourceLocation.none(), false);
    private static final Logger LOGGER = Logger.getLogger(ObjectNode.class.getName());

    private final Map<StringNode, Node> nodeMap;
    /** A cache of computed string to Node values. */
    private transient Map<String, Node> stringMap;

    public ObjectNode(Map<StringNode, Node> members, SourceLocation sourceLocation) {
        this(members, sourceLocation, true);
    }

    // Constructor used internally to reduce copies.
    ObjectNode(Map<StringNode, Node> nodeMap, SourceLocation sourceLocation, boolean defensiveCopy) {
        super(sourceLocation);
        this.nodeMap = defensiveCopy
                ? Collections.unmodifiableMap(new LinkedHashMap<>(nodeMap))
                : Collections.unmodifiableMap(nodeMap);
    }

    public static ObjectNode fromStringMap(Map<String, String> map) {
        return map.entrySet().stream().collect(collectStringKeys(Map.Entry::getKey, e -> from(e.getValue())));
    }

    @Override
    public NodeType getType() {
        return NodeType.OBJECT;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.objectNode(this);
    }

    @Override
    public ObjectNode expectObjectNode(String errorMessage) {
        return this;
    }

    @Override
    public Optional<ObjectNode> asObjectNode() {
        return Optional.of(this);
    }

    /**
     * Constructs a new object node with the given member added.
     *
     * @param key Name of the member to add.
     * @param <T> Type of the value member to add.
     * @param value Value of the member to add.
     * @return Returns a new object node.
     */
    public <T extends ToNode> ObjectNode withMember(StringNode key, T value) {
        Map<StringNode, Node> nodeMapCopy = new LinkedHashMap<>(nodeMap);
        nodeMapCopy.put(Objects.requireNonNull(key), Objects.requireNonNull(value).toNode());
        // Use the constructor that doesn't re-copy.
        return new ObjectNode(nodeMapCopy, getSourceLocation(), false);
    }

    /**
     * Constructs a new object node with the given member added.
     *
     * @param key Name of the member to add as a string.
     * @param <T> Type of the value member to add.
     * @param value Value of the member to add.
     * @return Returns a new object node.
     */
    public <T extends ToNode> ObjectNode withMember(String key, T value) {
        return withMember(from(key), value);
    }

    /**
     * Constructs a new object node with the given member added.
     *
     * @param key Name of the member to add as a string.
     * @param value Value of the member to add.
     * @return Returns a new object node.
     */
    public ObjectNode withMember(String key, String value) {
        return withMember(from(key), from(value));
    }

    /**
     * Constructs a new object node with the given member added.
     *
     * @param key Name of the member to add as a string.
     * @param value Value of the member to add.
     * @return Returns a new object node.
     */
    public ObjectNode withMember(String key, boolean value) {
        return withMember(from(key), from(value));
    }

    /**
     * Constructs a new object node with the given member added.
     *
     * @param key Name of the member to add as a string.
     * @param value Value of the member to add.
     * @return Returns a new object node.
     */
    public ObjectNode withMember(String key, Number value) {
        return withMember(from(key), from(value));
    }

    /**
     * Adds a member to a new ObjectNode if the provided value is present.
     *
     * @param key Key to set if value is present.
     * @param <T> Type of the value member to add.
     * @param value Value that may be present.
     * @return Returns an object with the optionally added member.
     */
    public <T extends ToNode> ObjectNode withOptionalMember(String key, Optional<T> value) {
        return value.map(val -> withMember(key, val)).orElse(this);
    }

    /**
     * Constructs a new object node from the current node, but without
     * the named member.
     *
     * @param memberName Name of a member that should be omitted.
     * @return Returns a new object node.
     */
    public ObjectNode withoutMember(String memberName) {
        if (!getStringMap().containsKey(memberName)) {
            return this;
        }
        Map<StringNode, Node> copiedMembers = new LinkedHashMap<>(nodeMap);
        copiedMembers.keySet().removeIf(k -> k.getValue().equals(memberName));
        // Use the constructor that doesn't re-copy.
        return new ObjectNode(copiedMembers, getSourceLocation(), false);
    }

    /**
     * Gets the map of members.
     *
     * @return Returns a map of nodes.
     */
    public Map<StringNode, Node> getMembers() {
        return nodeMap;
    }

    /**
     * Checks if the given member name exists in the ObjectNode.
     *
     * @param memberName Member name to check.
     * @return Returns true if this member is in the ObjectNode.
     */
    public boolean containsMember(String memberName) {
        return getStringMap().containsKey(memberName);
    }

    /**
     * Gets the member with the given name.
     *
     * @param memberName Name of the member to get.
     * @return Returns the optional node with the given member name.
     */
    public Optional<Node> getMember(String memberName) {
        return Optional.ofNullable(getStringMap().get(memberName));
    }

    /**
     * Gets a map of all members where the key starts with the given prefix.
     *
     * @param prefix Prefix to search for in keys.
     * @return Returns the map of matching members.
     */
    public Map<String, Node> getMembersByPrefix(String prefix) {
        return getStringMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets an immutable {@code Map<String, Node>} that represents the ObjectNode.
     *
     * @return Returns the immutable map.
     */
    public Map<String, Node> getStringMap() {
        Map<String, Node> map = stringMap;
        if (map == null) {
            map = new LinkedHashMap<>(nodeMap.size());
            for (Map.Entry<StringNode, Node> entry : nodeMap.entrySet()) {
                map.put(entry.getKey().getValue(), entry.getValue());
            }
            stringMap = Collections.unmodifiableMap(map);
        }

        return stringMap;
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a string.
     *
     * @param memberName Name of the member to get.
     * @return Returns the optional node with the given member name.
     * @throws ExpectationNotMetException if the member is not a string.
     */
    public Optional<StringNode> getStringMember(String memberName) {
        return getMember(memberName)
                .map(n -> n.expectStringNode(format("Expected `%s` to be a string; found {type}", memberName)));
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a string, otherwise returns the default value.
     *
     * @param memberName Name of the member to get.
     * @param defaultValue Default string value to return if the member is not present.
     * @return Returns a String value contained in the String node or the default value.
     * @throws ExpectationNotMetException if the member is not a string.
     */
    public String getStringMemberOrDefault(String memberName, String defaultValue) {
        return getStringMember(memberName).map(StringNode::getValue).orElse(defaultValue);
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a number.
     *
     * @param memberName Name of the member to get.
     * @return Returns the optional node with the given member name.
     * @throws ExpectationNotMetException if the member is not a number.
     */
    public Optional<NumberNode> getNumberMember(String memberName) {
        return getMember(memberName)
                .map(n -> n.expectNumberNode(format("Expected `%s` to be a number; found {type}", memberName)));
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a number, otherwise returns the default value.
     *
     * @param memberName Name of the member to get.
     * @param defaultValue Default value to return if a member is not found.
     * @return Returns the Number value or a default value.
     * @throws ExpectationNotMetException if the member is not a number.
     */
    public Number getNumberMemberOrDefault(String memberName, Number defaultValue) {
        return getNumberMember(memberName).map(NumberNode::getValue).orElse(defaultValue);
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be an array.
     *
     * @param memberName Name of the member to get.
     * @return Returns the optional node with the given member name.
     * @throws ExpectationNotMetException if the member is not an array.
     */
    public Optional<ArrayNode> getArrayMember(String memberName) {
        return getMember(memberName)
                .map(n -> n.expectArrayNode(format("Expected `%s` to be an array; found {type}", memberName)));
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be an object.
     *
     * @param memberName Name of the member to get.
     * @return Returns the optional node with the given member name.
     * @throws ExpectationNotMetException if the member is not an object.
     */
    public Optional<ObjectNode> getObjectMember(String memberName) {
        return getMember(memberName)
                .map(n -> n.expectObjectNode(format("Expected `%s` to be an object; found {type}", memberName)));
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a boolean.
     *
     * @param memberName Name of the member to get.
     * @return Returns the optional node with the given member name.
     * @throws ExpectationNotMetException if the member is not a boolean.
     */
    public Optional<BooleanNode> getBooleanMember(String memberName) {
        return getMember(memberName)
                .map(n -> n.expectBooleanNode(format("Expected `%s` to be a boolean; found {type}", memberName)));
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a boolean, otherwise returns a default value.
     *
     * @param memberName Name of the member to get.
     * @param defaultValue Default value to return if not present.
     * @return Returns the boolean value or a default value.
     * @throws ExpectationNotMetException if the member is not a boolean.
     */
    public Boolean getBooleanMemberOrDefault(String memberName, Boolean defaultValue) {
        return getBooleanMember(memberName).map(BooleanNode::getValue).orElse(defaultValue);
    }

    /**
     * Gets the member with the given name, and if present, expects it to
     * be a boolean, otherwise returns false.
     *
     * @param memberName Name of the member to get.
     * @return Returns the boolean value or false if not found.
     * @throws ExpectationNotMetException if the member is found and not a boolean.
     */
    public boolean getBooleanMemberOrDefault(String memberName) {
        return getBooleanMemberOrDefault(memberName, false);
    }

    /**
     * Gets the member with the given name.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws IllegalArgumentException when {@code memberName} is null.
     * @throws ExpectationNotMetException when {@code memberName} is is not
     *  present in the members map.
     */
    public Node expectMember(String name) {
        return expectMember(name, format("Missing expected member `%s`.", name));
    }

    /**
     * Gets the member with the given name, throwing
     * {@link ExpectationNotMetException} when the member is not present.
     *
     * @param name Name of the member to get.
     * @param errorMessage The error message to use if the expectation is not met.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when {@code memberName} is is not
     *  present in the members map.
     */
    public Node expectMember(String name, String errorMessage) {
        return getMember(name).orElseThrow(() -> new ExpectationNotMetException(errorMessage, this));
    }

    /**
     * Gets a member and requires it to be an array.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when not present or not an array.
     */
    public ArrayNode expectArrayMember(String name) {
        return expectMember(name)
                .expectArrayNode(format("Expected `%s` member to be an array, but found {type}.", name));
    }

    /**
     * Gets a member and requires it to be a boolean.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when not present or not a boolean.
     */
    public BooleanNode expectBooleanMember(String name) {
        return expectMember(name)
                .expectBooleanNode(format("Expected `%s` member to be a boolean, but found {type}.", name));
    }

    /**
     * Gets a member and requires it to be a null.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when not present or not a null.
     */
    public NullNode expectNullMember(String name) {
        return expectMember(name)
                .expectNullNode(format("Expected `%s` member to be null, but found {type}.", name));
    }

    /**
     * Gets a member and requires it to be a number.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when not present or not a number.
     */
    public NumberNode expectNumberMember(String name) {
        return expectMember(name)
                .expectNumberNode(format("Expected `%s` member to be a number, but found {type}.", name));
    }

    /**
     * Gets a member and requires it to be an object.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when not present or not an object.
     */
    public ObjectNode expectObjectMember(String name) {
        return expectMember(name)
                .expectObjectNode(format("Expected `%s` member to be an object, but found {type}.", name));
    }

    /**
     * Gets a member and requires it to be a string.
     *
     * @param name Name of the member to get.
     * @return Returns the node with the given member name.
     * @throws ExpectationNotMetException when not present or not a string.
     */
    public StringNode expectStringMember(String name) {
        return expectMember(name)
                .expectStringNode(format("Expected `%s` member to be a string, but found {type}.", name));
    }

    /**
     * Ensures that there are no additional properties other than the
     * provided member names.
     *
     * @param allowedProperties Properties that may exist.
     * @return Returns self
     * @throws ExpectationNotMetException if other properties are found.
     */
    public ObjectNode expectNoAdditionalProperties(Collection<String> allowedProperties) {
        for (String key : getStringMap().keySet()) {
            if (!allowedProperties.contains(key)) {
                Set<String> additional = new HashSet<>(getStringMap().keySet());
                additional.removeAll(allowedProperties);
                throw new ExpectationNotMetException(String.format(
                        "Expected an object with possible properties of %s, but found additional properties: %s",
                        ValidationUtils.tickedList(allowedProperties), ValidationUtils.tickedList(additional)), this);
            }
        }

        return this;
    }

    /**
     * Warns if unknown properties are found in object.
     *
     * @param allowedProperties Properties that may exist.
     * @return Returns self
     */
    public ObjectNode warnIfAdditionalProperties(Collection<String> allowedProperties) {
        try {
            expectNoAdditionalProperties(allowedProperties);
        } catch (ExpectationNotMetException e) {
            LOGGER.warning(e.getMessage() + " (" + getSourceLocation() + ")");
        }
        return this;
    }

    /**
     * Returns true if this object has no members.
     *
     * @return Returns if this object is empty.
     */
    public boolean isEmpty() {
        return nodeMap.isEmpty();
    }

    /**
     * Returns the number of members.
     *
     * @return Returns the number of members.
     */
    public int size() {
        return nodeMap.size();
    }

    /**
     * Merge this object node with another, creating a new ObjectNode.
     *
     * <p>Conflicting keys are overwritten by {@code other}. If the current
     * object has a source location, it is applied to the result. Otherwise,
     * the source location of {@code other} is applied to the result.
     *
     * @param other Other object node to merge with.
     * @return Returns the merged object node.
     */
    public ObjectNode merge(ObjectNode other) {
        Map<StringNode, Node> result = new LinkedHashMap<>(getMembers());
        result.putAll(other.nodeMap);
        return new ObjectNode(
                result,
                getSourceLocation() != SourceLocation.NONE ? getSourceLocation() : other.getSourceLocation(),
                false); // Use the constructor that doesn't re-copy.
    }

    /**
     * Creates a collector that creates an ObjectNode.
     *
     * @param keyMapper Key mapping function that returns a ToNode.
     * @param valueMapper Value mapping function that returns a ToNode.
     * @param <T> Type being collected over (e.g., Map.Entry, Pair, etc.).
     * @return Returns the created collector.
     */
    public static <T> Collector<T, Map<StringNode, Node>, ObjectNode> collect(
            Function<T, StringNode> keyMapper,
            Function<T, ToNode> valueMapper
    ) {
        return Collector.of(
                LinkedHashMap::new,
                (results, entry) -> results.put(keyMapper.apply(entry), valueMapper.apply(entry).toNode()),
                (left, right) -> {
                    left.putAll(right);
                    return left;
                },
                // Use the constructor that doesn't need to re-copy.
                results -> new ObjectNode(results, SourceLocation.NONE, false)
        );
    }

    /**
     * Creates a collector that creates an ObjectNode.
     *
     * @param keyMapper Key mapping function that returns a string.
     * @param valueMapper Value mapping function that returns a ToNode.
     * @param <T> Type being collected over (e.g., Map.Entry, Pair, etc.).
     * @return Returns the created collector.
     */
    public static <T> Collector<T, Map<StringNode, Node>, ObjectNode> collectStringKeys(
            Function<T, String> keyMapper,
            Function<T, ToNode> valueMapper
    ) {
        return collect(entry -> from(keyMapper.apply(entry)), valueMapper);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ObjectNode && nodeMap.equals(((ObjectNode) other).nodeMap);
    }

    @Override
    public int hashCode() {
        return getType().hashCode() * 7 + nodeMap.hashCode();
    }

    @Override
    public Builder toBuilder() {
        return new Builder().merge(this);
    }

    /**
     * Builder used to efficiently create an ObjectNode.
     */
    public static final class Builder implements SmithyBuilder<ObjectNode> {
        private final Map<StringNode, Node> nodeMap = new LinkedHashMap<>();
        private SourceLocation sourceLocation = SourceLocation.NONE;

        Builder() {}

        @Override
        public ObjectNode build() {
            return new ObjectNode(nodeMap, sourceLocation);
        }

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Objects.requireNonNull(sourceLocation);
            return this;
        }

        public <T extends ToNode> Builder withMember(StringNode key, T value) {
            nodeMap.put(key, value.toNode());
            return this;
        }

        public <T extends ToNode> Builder withMember(String key, T value) {
            return withMember(from(key), value.toNode());
        }

        public Builder withMember(String key, String value) {
            return withMember(from(key), from(value));
        }

        public Builder withMember(String key, boolean value) {
            return withMember(from(key), from(value));
        }

        public Builder withMember(String key, Number value) {
            return withMember(from(key), from(value));
        }

        public <T extends ToNode> Builder withOptionalMember(String key, Optional<T> value) {
            return value.map(val -> withMember(key, val.toNode())).orElse(this);
        }

        public Builder withoutMember(String memberName) {
            nodeMap.keySet().removeIf(key -> key.getValue().equals(memberName));
            return this;
        }

        public Builder merge(ObjectNode other) {
            for (Map.Entry<StringNode, Node> entry : other.getMembers().entrySet()) {
                nodeMap.put(entry.getKey(), entry.getValue());
            }
            return this;
        }
    }
}
