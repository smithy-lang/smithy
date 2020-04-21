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

package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
 * Selector attribute values are the data model of selectors.
 */
abstract class AttributeValue {

    /** Value used when a property or attribute value does not exist. **/
    static final AttributeValue NULL = new AttributeValue() {
        @Override
        boolean isPresent() {
            return false;
        }
    };

    /** Value created and used when a property does not exist. */
    static final Factory NULL_FACTORY = shape -> NULL;

    private static final Logger LOGGER = Logger.getLogger(AttributeValue.class.getName());

    @FunctionalInterface
    interface Factory {
        AttributeValue create(Shape shape);
    }

    @Override
    public String toString() {
        return "";
    }

    /**
     * Gets a property from the attribute value.
     *
     * <p>This method never returns null. It should instead return
     * {@link #NULL} when the property does not exist.
     *
     * @param key Property to get.
     * @return Returns the nested property.
     */
    AttributeValue getProperty(String key) {
        return NULL;
    }

    /**
     * Checks if the attribute value is considered present.
     *
     * <p>Attribute value are considered present if they are not null. If the
     * attribute value is a projection, then it is considered present if it is
     * not empty.
     *
     * @return Returns true if present.
     */
    boolean isPresent() {
        return true;
    }

    /**
     * Compares the given attribute value with the other attribute value using
     * a {@link AttributeComparator}.
     *
     * <p>This method is necessary in order to support matching on projections.
     *
     * @param comparator Comparator to use for the comparison.
     * @param other The attribute to compare against.
     * @param insensitive Whether or not to use a case-insensitive comparison.
     * @return Returns true if the attribute match the comparison.
     */
    boolean compare(AttributeComparator comparator, AttributeValue other, boolean insensitive) {
        return comparator.compare(this, other, insensitive);
    }

    /**
     * Creates the most efficient kind of selector value for the given path.
     *
     * @param current Value to path into.
     * @param path The parsed path to select from the value.
     * @return Returns the created selector value.
     */
    private static AttributeValue createPathSelector(AttributeValue current, List<String> path) {
        if (path.isEmpty()) {
            return current;
        }

        for (String segment : path) {
            current = current.getProperty(segment);
            if (!current.isPresent()) {
                break;
            }
        }
        return current;
    }

    /**
     * An attribute that contains a static, scalar String value.
     */
    static final class Literal extends AttributeValue {
        final String value;

        Literal(String value) {
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public String toString() {
            return value;
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
    static final class NodeValue extends AttributeValue {
        static final NodeVisitor<String> TO_STRING = new NodeToString();
        final Node value;

        NodeValue(Node value) {
            this.value = value;
        }

        @Override
        public boolean isPresent() {
            return !value.isNullNode();
        }

        @Override
        public String toString() {
            return value.accept(TO_STRING);
        }

        @Override
        AttributeValue getProperty(String key) {
            if (value.isObjectNode()) {
                ObjectNode node = value.expectObjectNode();
                if (key.equals("(keys)")) {
                    return project(node.getMembers().keySet());
                } else if (key.equals("(values)")) {
                    return project(node.getMembers().values());
                } else {
                    return value.expectObjectNode()
                            .getMember(key)
                            .<AttributeValue>map(NodeValue::new)
                            .orElse(NULL);
                }
            } else if (value.isArrayNode() && key.equals("(values)")) {
                return project(value.expectArrayNode().getElements());
            } else {
                return NULL;
            }
        }

        private AttributeValue project(Collection<? extends Node> nodes) {
            return new Projection(nodes.stream().map(NodeValue::new).collect(Collectors.toList()));
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
    static final class Projection extends AttributeValue {
        final Collection<? extends AttributeValue> values;

        Projection(Collection<? extends AttributeValue> values) {
            this.values = values;
        }

        @Override
        AttributeValue getProperty(String key) {
            // All of the values that are yielded from the projected values
            // come together to create a new projection.
            List<AttributeValue> result = new ArrayList<>();
            for (AttributeValue value : values) {
                AttributeValue next = value.getProperty(key);
                if (next.isPresent()) {
                    result.add(next);
                }
            }

            return new Projection(result);
        }

        @Override
        boolean isPresent() {
            // An empty projection is not considered present.
            return !values.isEmpty();
        }

        @Override
        public boolean compare(AttributeComparator comparator, AttributeValue other, boolean insensitive) {
            // Projections match if any shape contained in the projection matches.
            for (AttributeValue value : values) {
                if (value.compare(comparator, other, insensitive)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Attribute that contains service shape properties.
     *
     * <p>Using this attribute as a string yields an empty string. This
     * attribute has the following properties:
     *
     * <ul>
     *     <li>version: The service version as a string.</li>
     * </ul>
     */
    static final class Service extends AttributeValue {
        final ServiceShape service;

        Service(ServiceShape service) {
            this.service = service;
        }

        static AttributeValue.Factory createFactory(List<String> path) {
            // Optimization to help with debugging selectors.
            if (path.size() > 1) {
                LOGGER.warning("Too many path segments for `service` attribute: " + path);
            }

            return shape -> shape.asServiceShape()
                    .<AttributeValue>map(Service::new)
                    .map(value -> AttributeValue.createPathSelector(value, path))
                    .orElse(NULL);
        }

        @Override
        AttributeValue getProperty(String key) {
            switch (key) {
                case "version":
                    return new Literal(service.getVersion());
                case "(keys)":
                    return new Projection(Collections.singleton(new Literal("version")));
                case "(values)":
                    return new Projection(Collections.singleton(new Literal(service.getVersion())));
                default:
                    return NULL;
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
    static final class Id extends AttributeValue {
        final ShapeId id;

        Id(ShapeId id) {
            this.id = id;
        }

        static AttributeValue.Factory createFactory(List<String> path) {
            if (path.size() > 1) {
                // Make debugging selectors easier.
                LOGGER.warning("Too many selector path segments provided when selecting into `id`: " + path);
            }

            return shape -> createPathSelector(new Id(shape.getId()), path);
        }

        @Override
        public String toString() {
            return id.toString();
        }

        @Override
        AttributeValue getProperty(String property) {
            switch (property) {
                case "name":
                    return new Literal(id.getName());
                case "namespace":
                    return new Literal(id.getNamespace());
                case "member":
                    return id.getMember()
                            .<AttributeValue>map(Literal::new)
                            .orElse(NULL);
                case "(keys)":
                    List<AttributeValue> keys = new ArrayList<>(3);
                    keys.add(new Literal("namespace"));
                    keys.add(new Literal("name"));
                    id.getMember().ifPresent(member -> keys.add(new Literal("member")));
                    return new Projection(keys);
                case "(values)":
                    List<AttributeValue> values = new ArrayList<>(3);
                    values.add(new Literal(id.getNamespace()));
                    values.add(new Literal(id.getName()));
                    id.getMember().ifPresent(member -> values.add(new Literal(member)));
                    return new Projection(values);
                default:
                    return NULL;
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
    static final class Traits extends AttributeValue {
        final Shape shape;

        Traits(Shape shape) {
            this.shape = Objects.requireNonNull(shape);
        }

        static AttributeValue.Factory createFactory(List<String> path) {
            return shape -> createPathSelector(new Traits(shape), path);
        }

        @Override
        AttributeValue getProperty(String property) {
            if (property.equals("(keys)")) {
                // This allows the projected keys to be used like shape IDs:
                // [trait|(keys)|namespace='com.foo']
                List<AttributeValue> values = new ArrayList<>();
                for (ShapeId id : shape.getAllTraits().keySet()) {
                    values.add(new Id(id));
                }
                return new Projection(values);
            } else if (property.equals("(values)")) {
                // This allows the projected values to be used as nodes. This
                // selector finds all traits that have 'foo' property.
                // [trait|(values)|foo]
                List<AttributeValue> values = new ArrayList<>();
                for (Trait trait : shape.getAllTraits().values()) {
                    values.add(new NodeValue(trait.toNode()));
                }
                return new Projection(values);
            } else {
                // A normal property getter. This allows relative trait shape IDs
                // and absolute IDs. Relative IDs resolve to 'smithy.api'.
                return shape.findTrait(ShapeId.from(Trait.makeAbsoluteName(property)))
                        .<AttributeValue>map(trait -> new NodeValue(trait.toNode()))
                        .orElse(NULL);
            }
        }
    }
}
