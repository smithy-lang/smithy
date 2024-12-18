/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Package private implementations of attribute values.
 */
final class AttributeValueImpl {

    /** Value used when a property or attribute value does not exist. **/
    static final AttributeValue EMPTY = new AttributeValue() {
        @Override
        public String toString() {
            return "";
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public AttributeValue getProperty(String key) {
            return EMPTY;
        }
    };

    private static final Logger LOGGER = Logger.getLogger(AttributeValue.class.getName());
    private static final String KEYS = "(keys)";
    private static final String VALUES = "(values)";
    private static final String LENGTH = "(length)";
    private static final String FIRST = "(first)";

    /**
     * An attribute that contains a static, scalar String value.
     */
    static final class Literal implements AttributeValue {
        private final Object value;

        Literal(Object value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }

        @Override
        public AttributeValue getProperty(String key) {
            if (key.equals(LENGTH)) {
                return AttributeValue.literal(toString().length());
            } else {
                return EMPTY;
            }
        }
    }

    /**
     * An attribute that contains a {@link Node}.
     *
     * <p>This kind of attribute is typically used when traversing into trait values.
     * Properties of object nodes can be selected by name. {@link NullNode} are
     * not considered present. The (values) pseudo-property can be used on object
     * nodes and array nodes. The (keys) pseudo-property can be used on object nodes.
     */
    static final class NodeValue implements AttributeValue {

        private static final NodeVisitor<String> TO_STRING = new NodeToString();
        private final Node value;
        private String asString;
        private String messageString;

        NodeValue(Node value) {
            this.value = value;
        }

        @Override
        public boolean isPresent() {
            return !value.isNullNode();
        }

        @Override
        public String toString() {
            String str = asString;
            if (str == null) {
                str = value.accept(TO_STRING);
                asString = str;
            }
            return str;
        }

        @Override
        public String toMessageString() {
            String str = messageString;
            if (str == null) {
                str = Node.printJson(value);
                // Returns the JSON printed string of the Node value
                // The value is *not* pretty printed (no spaces or newlines).
                messageString = str;
            }
            return str;
        }

        @Override
        public AttributeValue getProperty(String key) {
            switch (value.getType()) {
                case OBJECT:
                    ObjectNode objectNode = value.expectObjectNode();
                    switch (key) {
                        case KEYS:
                            return project(objectNode.getMembers().keySet());
                        case VALUES:
                            return project(objectNode.getMembers().values());
                        case LENGTH:
                            return AttributeValue.literal(objectNode.getMembers().size());
                        default:
                            return value.expectObjectNode()
                                    .getMember(key)
                                    .<AttributeValue>map(NodeValue::new)
                                    .orElse(EMPTY);
                    }
                case ARRAY:
                    ArrayNode arrayNode = value.expectArrayNode();
                    switch (key) {
                        case VALUES:
                            return project(arrayNode.getElements());
                        case LENGTH:
                            return AttributeValue.literal(arrayNode.size());
                        default:
                            return EMPTY;
                    }
                case STRING:
                    if (key.equals(LENGTH)) {
                        return AttributeValue.literal(value.expectStringNode().getValue().length());
                    }
                    // fall through
                default:
                    return EMPTY;
            }
        }

        private AttributeValue project(Collection<? extends Node> nodes) {
            List<AttributeValue> values = new ArrayList<>(nodes.size());
            for (Node node : nodes) {
                values.add(AttributeValue.node(node));
            }
            return AttributeValue.projection(values);
        }

        // Only string, numbers, and booleans are converted to strings.
        private static final class NodeToString extends NodeVisitor.Default<String> {
            @Override
            protected String getDefault(software.amazon.smithy.model.node.Node node) {
                return "";
            }

            @Override
            public String stringNode(StringNode node) {
                return node.getValue();
            }

            @Override
            public String numberNode(NumberNode node) {
                return node.getValue().toString();
            }

            @Override
            public String booleanNode(BooleanNode node) {
                return Boolean.toString(node.getValue());
            }
        }
    }

    /**
     * A projected attribute value that matches N values contained within it.
     *
     * <p>Projections wrap other values and match if any contained value matches,
     * are only considered present if there are 1 or more values, and return
     * new projections based on properties contained within the projection.
     */
    static final class Projection implements AttributeValue {
        private final Collection<? extends AttributeValue> values;
        private Collection<? extends AttributeValue> flattened;
        private String messageString;

        Projection(Collection<? extends AttributeValue> values) {
            this.values = values;
        }

        @Override
        public AttributeValue getProperty(String key) {
            if (key.equals(FIRST)) {
                return values.isEmpty() ? EMPTY : getFlattenedValues().iterator().next();
            }

            // All of the values that are yielded from the projected values
            // come together to create a new projection.
            List<AttributeValue> result = new ArrayList<>();
            for (AttributeValue value : values) {
                AttributeValue next = value.getProperty(key);
                if (next.isPresent()) {
                    result.add(next);
                }
            }

            return AttributeValue.projection(result);
        }

        @Override
        public String toMessageString() {
            String str = messageString;
            if (str == null) {
                // Returns a comma separated (with a space), sorted list of each flattened
                // value's debug string.
                str = getFlattenedValues().stream()
                        .map(AttributeValue::toMessageString)
                        .sorted()
                        .collect(Collectors.joining(", ", "[", "]"));
                messageString = str;
            }
            return str;
        }

        /**
         * Computes the flattened values of the projection.
         *
         * @return Returns the flattened values of the projection.
         */
        public Collection<? extends AttributeValue> getFlattenedValues() {
            if (flattened == null) {
                List<AttributeValue> result = new ArrayList<>(values.size());
                for (AttributeValue value : values) {
                    // Projections need to be flattened!
                    if (value instanceof Projection) {
                        result.addAll(((Projection) value).getFlattenedValues());
                    } else {
                        result.add(value);
                    }
                }
                flattened = result;
            }

            return flattened;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public boolean isPresent() {
            // An empty projection is not considered present.
            return !values.isEmpty();
        }
    }

    /**
     * Attribute that contains service shape properties.
     */
    static final class Service implements AttributeValue {
        private final ServiceShape service;

        Service(ServiceShape service) {
            this.service = service;
        }

        @Override
        public String toString() {
            return service.getId().toString();
        }

        @Override
        public AttributeValue getProperty(String key) {
            if (key.equals("version")) {
                return AttributeValue.literal(service.getVersion());
            } else if (key.equals("id")) {
                return AttributeValue.id(service.getId());
            } else {
                throw new SelectorException("Invalid nested 'service' selector attribute property: " + key);
            }
        }
    }

    /**
     * Grabs values out of a {@link ShapeId} or uses the shape ID directly
     * as a string, and when cast to a string, returns the absolute shape ID.
     *
     * <p>This attribute has the following properties:
     *
     * <ul>
     *     <li>namespace: The shape ID namespace.</li>
     *     <li>name: The shape ID name.</li>
     *     <li>member: The optionally present shape ID member.</li>
     * </ul>
     */
    static final class Id implements AttributeValue {
        private final ShapeId id;

        Id(ShapeId id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id.toString();
        }

        @Override
        public AttributeValue getProperty(String property) {
            switch (property) {
                case "name":
                    return AttributeValue.literal(id.getName());
                case "namespace":
                    return AttributeValue.literal(id.getNamespace());
                case "member":
                    return id.getMember()
                            .<AttributeValue>map(Literal::new)
                            .orElse(EMPTY);
                case LENGTH:
                    // Length returns the length of the shape ID.
                    return AttributeValue.literal(id.toString().length());
                default:
                    throw new SelectorException("Invalid nested 'id' selector attribute property: " + property);
            }
        }
    }

    /**
     * Grabs values out of the traits of a shape.
     *
     * <p>This attribute value can be indexed using absolute shape IDs or
     * relative shape IDs. When a relative shape ID is provided, it is
     * resolved to the 'smithy.api' namespace.
     *
     * <p>Calling (values) on this attribute will return a projection that
     * contains all of the traits applied to the shape as Node values.
     *
     * <p>Calling (keys) on this attribute will return a projection that
     * contains all of the shape IDs of each trait applied to a shape.
     */
    static final class Traits implements AttributeValue {
        private final Shape shape;

        Traits(Shape shape) {
            this.shape = Objects.requireNonNull(shape);
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public AttributeValue getProperty(String property) {
            switch (property) {
                case KEYS:
                    // This allows the projected keys to be used like shape IDs:
                    // [trait|(keys)|namespace='com.foo']
                    List<AttributeValue> keyValues = new ArrayList<>();
                    for (ShapeId id : shape.getAllTraits().keySet()) {
                        keyValues.add(AttributeValue.id(id));
                    }
                    return AttributeValue.projection(keyValues);
                case VALUES:
                    // This allows the projected values to be used as nodes. This
                    // selector finds all traits that have 'foo' property.
                    // [trait|(values)|foo]
                    List<AttributeValue> values = new ArrayList<>();
                    for (Trait trait : shape.getAllTraits().values()) {
                        values.add(new NodeValue(trait.toNode()));
                    }
                    return AttributeValue.projection(values);
                case LENGTH:
                    return AttributeValue.literal(shape.getAllTraits().size());
                default:
                    // A normal property getter. This allows relative trait shape IDs
                    // and absolute IDs. Relative IDs resolve to 'smithy.api'.
                    return shape.findTrait(ShapeId.from(Trait.makeAbsoluteName(property)))
                            .<AttributeValue>map(trait -> new NodeValue(trait.toNode()))
                            .orElse(EMPTY);
            }
        }
    }

    static final class ShapeValue implements AttributeValue {
        private final Shape shape;
        private final Map<String, Set<Shape>> vars;

        ShapeValue(Shape shape, Map<String, Set<Shape>> vars) {
            this.shape = Objects.requireNonNull(shape);
            this.vars = vars == null ? Collections.emptyMap() : vars;
        }

        @Override
        public String toString() {
            return shape.getId().toString();
        }

        @Override
        public AttributeValue getProperty(String property) {
            switch (property) {
                case "trait":
                    return new Traits(shape);
                case "id":
                    return AttributeValue.id(shape.getId());
                case "service":
                    return shape.asServiceShape().<AttributeValue>map(Service::new).orElse(EMPTY);
                case "var":
                    return new VariableValue(vars);
                default:
                    throw new SelectorException("Invalid shape selector attribute: " + property);
            }
        }
    }

    static final class VariableValue implements AttributeValue {
        private final Map<String, Set<Shape>> vars;

        VariableValue(Map<String, Set<Shape>> vars) {
            this.vars = vars;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public AttributeValue getProperty(String property) {
            Set<Shape> shapes = vars.getOrDefault(property, Collections.emptySet());
            List<AttributeValue> values = new ArrayList<>(shapes.size());
            for (Shape shape : shapes) {
                values.add(AttributeValue.shape(shape, vars));
            }
            return AttributeValue.projection(values);
        }
    }

    private AttributeValueImpl() {}
}
