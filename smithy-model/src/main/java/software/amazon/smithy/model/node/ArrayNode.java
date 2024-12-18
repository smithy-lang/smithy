/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents an array of nodes.
 */
public final class ArrayNode extends Node implements Iterable<Node>, ToSmithyBuilder<ArrayNode> {
    static final ArrayNode EMPTY = new ArrayNode(ListUtils.of(), SourceLocation.none(), false);

    /**
     * A regex used to extract the expected class in a ClassCastException.
     *
     * <p>This regex covers the following known cases:
     *
     * "Cannot cast X to Y"
     * "class X cannot be cast to class Y ..."
     */
    private static final Pattern CAST_PATTERN_TYPE = Pattern.compile(
            "^.* to(?: class)? software\\.amazon\\.smithy\\.model\\.node\\.([A-Za-z]+).*$");

    private final List<Node> elements;

    public ArrayNode(List<Node> elements, SourceLocation sourceLocation) {
        this(elements, sourceLocation, true);
    }

    ArrayNode(List<Node> elements, SourceLocation sourceLocation, boolean defensiveCopy) {
        super(sourceLocation);
        this.elements = defensiveCopy
                ? ListUtils.copyOf(elements)
                : Collections.unmodifiableList(elements);
    }

    private ArrayNode(Builder builder) {
        super(builder.sourceLocation);
        this.elements = builder.values.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder().sourceLocation(getSourceLocation()).merge(this);
    }

    @Override
    public Iterator<Node> iterator() {
        return getElements().iterator();
    }

    @Override
    public NodeType getType() {
        return NodeType.ARRAY;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.arrayNode(this);
    }

    @Override
    public ArrayNode expectArrayNode(String errorMessage) {
        return this;
    }

    @Override
    public ArrayNode expectArrayNode(Supplier<String> errorMessage) {
        return this;
    }

    @Override
    public Optional<ArrayNode> asArrayNode() {
        return Optional.of(this);
    }

    /**
     * Gets the list of nodes.
     *
     * @return Returns a list of nodes.
     */
    public List<Node> getElements() {
        return elements;
    }

    /**
     * Gets a node from the given index.
     *
     * @param index Index of the value to get.
     * @return Returns an optional node at the given index.
     */
    public Optional<Node> get(int index) {
        return elements.size() > index && index > -1
                ? Optional.of(elements.get(index))
                : Optional.empty();
    }

    /**
     * Returns true if the array node is empty.
     *
     * @return Returns true if the array node is empty.
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Returns the number of elements in the array node.
     *
     * @return Returns the number of elements in the array node.
     */
    public int size() {
        return elements.size();
    }

    /**
     * Copies the values from the current array node, adding the given node,
     * returning them in a new {@code ArrayNode}.
     *
     * @param node The node to add.
     * @return Returns a new array node.
     */
    public ArrayNode withValue(Node node) {
        ArrayList<Node> newElements = new ArrayList<>(elements);
        newElements.add(Objects.requireNonNull(node));
        return new ArrayNode(newElements, getSourceLocation(), false);
    }

    /**
     * Gets the elements of the array as a list of a specific type of
     * {@link Node}.
     *
     * @param type Type of Node to expect in each position.
     * @param <T> Type of Node to expect in each position.
     * @return Returns the List of the specified type.
     * @throws ExpectationNotMetException if the list contains elements of a different type.
     */
    public <T extends Node> List<T> getElementsAs(Class<T> type) {
        return getElementsAs(type::cast);
    }

    /**
     * Gets the elements of the ArrayNode as a specific type by applying
     * a mapping function to each node.
     *
     * <p>Each Node is cast to {@code K} and then mapped with the provided
     * function to return type {@code T}.
     *
     * <pre>{@code
     * ArrayNode array = Node.fromStrings("foo", "baz", "bar");
     * List<String> strings = array.getElementsAs(StringNode::getValue);
     * }</pre>
     *
     * @param f Mapping function that takes {@code K} and return {@code T}.
     * @param <K> Expected Node type.
     * @param <T> Mapping function return value.
     *
     * @return Returns the List of type T.
     * @throws ExpectationNotMetException if the list contains elements of a different type than {@code K}.
     */
    @SuppressWarnings("unchecked")
    public <T, K extends Node> List<T> getElementsAs(Function<K, T> f) {
        List<T> result = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            try {
                K k = (K) elements.get(i);
                result.add(f.apply(k));
            } catch (ClassCastException e) {
                // Attempt to present a more useful error message by parsing the class
                // that was trying to be converted. The only reason this would ever
                // fail is if the message is changed in the JDK, so just in case,
                // there's a fallback code-path.
                String message = e.getMessage();
                Matcher matcher = CAST_PATTERN_TYPE.matcher(message);
                String formatted = matcher.matches()
                        ? String.format("Expected array element %d to be a %s but found %s.",
                                i,
                                nodeClassToSimpleTypeName(matcher.group(1)),
                                nodeClassToSimpleTypeName(elements.get(i).getClass().getSimpleName()))
                        : String.format("Array element at position %d is an invalid type `%s`: %s",
                                i,
                                nodeClassToSimpleTypeName(elements.get(i).getClass().getSimpleName()),
                                e.getMessage());
                throw new ExpectationNotMetException(formatted, elements.get(i));
            }
        }

        return result;
    }

    private static String nodeClassToSimpleTypeName(String className) {
        return className.replace("Node", "").toLowerCase(Locale.ENGLISH);
    }

    /**
     * Merges two ArrayNodes into a new ArrayNode.
     *
     * <p>If the current node has a source location, it is applied to the
     * result. Otherwise, the source location of {@code other} is applied
     * to the result.
     *
     * @param other Node to merge with.
     * @return Returns a new merged array node.
     */
    public ArrayNode merge(ArrayNode other) {
        List<Node> result = new ArrayList<>(elements);
        result.addAll(other.elements);
        return new ArrayNode(
                result,
                getSourceLocation() != SourceLocation.NONE ? getSourceLocation() : other.getSourceLocation(),
                false);
    }

    /**
     * @param <T> Type of value in the collection.
     * @return Creates a collector that create an ArrayNode.
     */
    public static <T extends ToNode> Collector<T, List<Node>, ArrayNode> collect() {
        return collect(SourceLocation.NONE);
    }

    /**
     * @param sloc Source location to use on the created node.
     * @param <T> Type of value in the collection.
     * @return Creates a collector that create an ArrayNode.
     */
    public static <T extends ToNode> Collector<T, List<Node>, ArrayNode> collect(SourceLocation sloc) {
        return Collector.of(
                ArrayList::new,
                (results, entry) -> results.add(entry.toNode()),
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                results -> new ArrayNode(results, sloc, false));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArrayNode && elements.equals(((ArrayNode) other).elements);
    }

    @Override
    public int hashCode() {
        return getType().hashCode() * 7 + elements.hashCode();
    }

    /**
     * Builder used to efficiently create an ArrayNode.
     */
    public static final class Builder implements SmithyBuilder<ArrayNode> {
        private final BuilderRef<List<Node>> values = BuilderRef.forList();
        private SourceLocation sourceLocation = SourceLocation.NONE;

        Builder() {}

        @Override
        public ArrayNode build() {
            return new ArrayNode(this);
        }

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Objects.requireNonNull(sourceLocation);
            return this;
        }

        public <T extends ToNode> Builder withValue(T value) {
            values.get().add(value.toNode());
            return this;
        }

        public Builder withValue(String value) {
            return withValue(from(value));
        }

        public Builder withValue(boolean value) {
            return withValue(from(value));
        }

        public Builder withValue(Number value) {
            return withValue(from(value));
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        public Builder withoutValue(Object value) {
            values.get().remove(value);
            return this;
        }

        public Builder merge(ArrayNode other) {
            for (Node value : other.getElements()) {
                withValue(value);
            }
            return this;
        }
    }
}
