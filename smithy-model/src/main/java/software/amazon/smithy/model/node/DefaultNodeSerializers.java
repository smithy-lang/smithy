/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static software.amazon.smithy.model.node.NodeMapper.Serializer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * The default implementations use to convert Objects to Node values in {@link NodeMapper}.
 */
final class DefaultNodeSerializers {

    private static final Logger LOGGER = Logger.getLogger(DefaultNodeSerializers.class.getName());

    // Serialize the result of calling the ToNode#toNode method of an object.
    private static final Serializer<ToNode> TO_NODE_SERIALIZER = new Serializer<ToNode>() {
        @Override
        public Class<ToNode> getType() {
            return ToNode.class;
        }

        @Override
        public Node serialize(ToNode value, Set<Object> serializedObjects, NodeMapper mapper) {
            // Handle cases where the toNode method is disabled for a specific type.
            // This allows other serializers to attempt to serialize the value.
            if (mapper.getDisableToNode().contains(value.getClass())) {
                return null;
            }

            // TODO: make sure every instance of `toNode` is setting this
            return value.toNode();
        }
    };

    // Serialize the value contained in an Optional if present, or a NullNode if not present.
    private static final Serializer<Optional> OPTIONAL_SERIALIZER = new Serializer<Optional>() {
        @Override
        public Class<Optional> getType() {
            return Optional.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node serialize(Optional value, Set<Object> serializedObjects, NodeMapper mapper) {
            return (Node) value.map(v -> mapper.serialize(v, serializedObjects)).orElse(Node.nullNode());
        }
    };

    // Serialize a Number into a NumberNode.
    private static final Serializer<Number> NUMBER_SERIALIZER = new Serializer<Number>() {
        @Override
        public Class<Number> getType() {
            return Number.class;
        }

        @Override
        public Node serialize(Number value, Set<Object> serializedObjects, NodeMapper mapper) {
            return Node.from(value);
        }
    };

    // Serialize a String into a StringNode.
    private static final Serializer<String> STRING_SERIALIZER = new Serializer<String>() {
        @Override
        public Class<String> getType() {
            return String.class;
        }

        @Override
        public Node serialize(String value, Set<Object> serializedObjects, NodeMapper mapper) {
            return Node.from(value);
        }
    };

    // Serializes File instances.
    private static final Serializer<File> FILE_SERIALIZER = new Serializer<File>() {
        @Override
        public Class<File> getType() {
            return File.class;
        }

        @Override
        public Node serialize(File value, Set<Object> serializedObjects, NodeMapper mapper) {
            return Node.from(value.getAbsolutePath());
        }
    };

    // Serializes Path instances.
    private static final Serializer<Path> PATH_SERIALIZER = new Serializer<Path>() {
        @Override
        public Class<Path> getType() {
            return Path.class;
        }

        @Override
        public Node serialize(Path value, Set<Object> serializedObjects, NodeMapper mapper) {
            return Node.from(value.toUri().toString());
        }
    };

    private static final class ToStringSerializer<T> implements Serializer<T> {
        private Class<T> type;

        ToStringSerializer(Class<T> type) {
            this.type = type;
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public Node serialize(T value, Set<Object> serializedObjects, NodeMapper mapper) {
            return Node.from(value.toString());
        }
    }

    private static final Serializer<ShapeId> SHAPE_ID_SERIALIZER = new ToStringSerializer<>(ShapeId.class);

    // Mirror's Jackson's behavior of WRITE_ENUMS_USING_TO_STRING
    // See https://github.com/FasterXML/jackson-databind/wiki/Serialization-features
    private static final Serializer<Enum> ENUM_SERIALIZER = new ToStringSerializer<>(Enum.class);

    // Mirror's a subset of Jackson's behavior.
    // See https://github.com/FasterXML/jackson-databind/blob/62c9d3dfe4b512380fdb7cfb38f6f9a0204f0c1a/src/main/java/com/fasterxml/jackson/databind/ser/std/StringLikeSerializer.java
    private static final Serializer<URL> URL_SERIALIZER = new ToStringSerializer<>(URL.class);
    private static final Serializer<URI> URI_SERIALIZER = new ToStringSerializer<>(URI.class);
    private static final Serializer<Pattern> PATTERN_SERIALIZER = new ToStringSerializer<>(Pattern.class);

    // Serialize a Boolean/boolean into a BooleanNode.
    private static final Serializer<Boolean> BOOLEAN_SERIALIZER = new Serializer<Boolean>() {
        @Override
        public Class<Boolean> getType() {
            return Boolean.class;
        }

        @Override
        public Node serialize(Boolean value, Set<Object> serializedObjects, NodeMapper mapper) {
            return Node.from(value);
        }
    };

    // Serialize a Map into an ObjectNode.
    private static final Serializer<Map> MAP_SERIALIZER = new Serializer<Map>() {
        @Override
        public Class<Map> getType() {
            return Map.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node serialize(Map value, Set<Object> serializedObjects, NodeMapper mapper) {
            Map<StringNode, Node> mappings = new LinkedHashMap<>();
            Set<Map.Entry<Object, Object>> entries = (Set<Map.Entry<Object, Object>>) value.entrySet();

            // Iterate over the map entries and populate map entries for an ObjectNode.
            for (Map.Entry<Object, Object> entry : entries) {
                // Serialize the key and require that it is serialized as a StringNode.
                Node key = mapper.serialize(entry.getKey(), serializedObjects);
                if (key instanceof StringNode) {
                    mappings.put((StringNode) key, mapper.serialize(entry.getValue(), serializedObjects));
                } else {
                    throw new NodeSerializationException(
                            "Unable to write Map key because it was not serialized as a string: "
                                    + entry.getKey() + " -> " + Node.printJson(key));
                }
            }

            return new ObjectNode(mappings, SourceLocation.NONE);
        }
    };

    // Serialize the elements of an Iterable into an ArrayNode.
    private static final Serializer<Iterable> ITERABLE_SERIALIZER = new Serializer<Iterable>() {
        @Override
        public Class<Iterable> getType() {
            return Iterable.class;
        }

        @Override
        public Node serialize(Iterable value, Set<Object> serializedObjects, NodeMapper mapper) {
            List<Node> nodes = new ArrayList<>();
            for (Object item : value) {
                nodes.add(mapper.serialize(item, serializedObjects));
            }
            return new ArrayNode(nodes, SourceLocation.NONE);
        }
    };

    // Serialize an array of values into an ArrayNode.
    private static final Serializer<Object[]> ARRAY_SERIALIZER = new Serializer<Object[]>() {
        @Override
        public Class<Object[]> getType() {
            return Object[].class;
        }

        @Override
        public Node serialize(Object[] value, Set<Object> serializedObjects, NodeMapper mapper) {
            List<Node> nodes = new ArrayList<>();
            for (Object item : value) {
                nodes.add(mapper.serialize(item, serializedObjects));
            }
            return new ArrayNode(nodes, SourceLocation.NONE);
        }
    };

    /**
     * Contains the getters of a class that are eligible to convert to a Node.
     *
     * <p>Getters are public methods that take zero arguments and start with
     * "get" or "is". Getters that are associated with properties marked as
     * {@code transient} are not serialized.
     */
    private static final class ClassInfo {
        // Cache previously evaluated objects.
        private static final IdentityClassCache<Class, ClassInfo> CACHE = new IdentityClassCache<>();

        // Methods aren't returned normally in any particular order, so give them an order.
        final Map<String, Method> getters = new TreeMap<>();

        static ClassInfo fromClass(Class<?> klass) {
            return CACHE.getForClass(klass, klass, () -> {
                ClassInfo info = new ClassInfo();
                Set<String> transientFields = getTransientFields(klass);
                // Determine which methods are getters that aren't backed by transient properties.
                for (Method method : klass.getMethods()) {
                    // Ignore Object.class, getSourceLocation, etc.
                    if (isIgnoredMethod(klass, method)) {
                        continue;
                    }
                    int fieldPrefixChars = getGetterPrefixCharCount(method);
                    // If the method starts with the parsed prefix characters, then check if it's transient.
                    if (fieldPrefixChars > 0 && fieldPrefixChars != method.getName().length()) {
                        // Always normalize as the lowercase name (i.e., "getFoo" -> "foo").
                        String lowerFieldName = StringUtils.uncapitalize(method.getName().substring(fieldPrefixChars));
                        if (!transientFields.contains(lowerFieldName)) {
                            info.getters.put(lowerFieldName, method);
                        } else {
                            LOGGER.fine(klass.getName() + " getter " + method.getName() + " is transient");
                        }
                    }
                }
                LOGGER.fine(() -> "Detected the following getters for " + klass.getName() + ": " + info.getters);
                return info;
            });
        }

        private static boolean isIgnoredMethod(Class<?> klass, Method method) {
            // Ignore Object.class methods.
            if (method.getDeclaringClass() == Object.class) {
                return true;
            }
            // Special casing for ignore getSourceLocation.
            // Does this need to be made more generic?
            if (FromSourceLocation.class.isAssignableFrom(klass) && method.getName().equals("getSourceLocation")) {
                return true;
            }
            return false;
        }

        private static Set<String> getTransientFields(Class klass) {
            Set<String> transientFields = new HashSet<>();
            for (Field field : klass.getDeclaredFields()) {
                if (Modifier.isTransient(field.getModifiers())) {
                    // Normalize field names to lowercase the first character.
                    transientFields.add(StringUtils.uncapitalize(field.getName()));
                }
            }
            return transientFields;
        }

        private static int getGetterPrefixCharCount(Method method) {
            // Don't use static methods, or methods with arguments.
            if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                if (method.getName().startsWith("get")) {
                    return 3;
                } else if (method.getName().startsWith("is") && method.getReturnType() == boolean.class) {
                    return 2;
                }
            }
            return 0;
        }
    }

    static final Serializer<Object> FROM_BEAN = new Serializer<Object>() {
        @Override
        public Class<Object> getType() {
            return Object.class;
        }

        @Override
        public Node serialize(Object value, Set<Object> serializedObjects, NodeMapper mapper) {
            if (serializedObjects.contains(value)) {
                return Node.nullNode();
            }

            // Add the current value to the set.
            serializedObjects.add(value);
            Map<StringNode, Node> mappings = new TreeMap<>(Comparator.comparing(StringNode::getValue));
            ClassInfo info = ClassInfo.fromClass(value.getClass());

            for (Map.Entry<String, Method> entry : info.getters.entrySet()) {
                try {
                    Object getterResult = entry.getValue().invoke(value);
                    Node result = mapper.serialize(getterResult, serializedObjects);
                    if (canSerialize(mapper, result)) {
                        mappings.put(Node.from(entry.getKey()), result);
                    }
                } catch (ReflectiveOperationException e) {
                    // There's almost always a previous exception, so grab it's more useful message.
                    // If this isn't done, I observed that the message of ReflectiveOperationException is null.
                    String causeMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    String message = String.format(
                            "Error serializing `%s` field of %s using %s(): %s",
                            entry.getKey(),
                            value.getClass().getName(),
                            entry.getValue().getName(),
                            causeMessage);
                    throw new NodeSerializationException(message, e);
                }
            }

            // Remove the current value from the set to ensure that it can be serialized
            // multiple times (like in a List<T>).
            serializedObjects.remove(value);

            // Pass on the source location if it is present.
            SourceLocation sourceLocation = SourceLocation.NONE;
            if (value instanceof FromSourceLocation) {
                sourceLocation = ((FromSourceLocation) value).getSourceLocation();
            }

            return new ObjectNode(mappings, sourceLocation);
        }

        private boolean canSerialize(NodeMapper mapper, Node value) {
            if (!mapper.getSerializeNullValues() && value.isNullNode()) {
                return false;
            }

            if (mapper.getOmitEmptyValues()) {
                if (value.isObjectNode() && value.expectObjectNode().isEmpty()) {
                    return false;
                } else if (value.isArrayNode() && value.expectArrayNode().isEmpty()) {
                    return false;
                } else if (value.isBooleanNode() && !value.expectBooleanNode().getValue()) {
                    return false;
                }
            }

            return true;
        }
    };

    // The priority ordered list of default serializers that NodeMapper uses.
    //
    // The priority is determined based on the specificity of each deserializer;
    // the most specific ones should appear at the start of the list, and the
    // most generic ones should appear at the end. For example, Iterable is
    // very broad, and many things implement it. It should be at or near the
    // in of the list in case that same object implements some other
    // serializer.
    //
    // If we ever open up the API, then we should consider making the priority
    // more explicit by adding it to the Serializer interface.
    static final List<Serializer> SERIALIZERS = ListUtils.of(
            TO_NODE_SERIALIZER,
            OPTIONAL_SERIALIZER,
            STRING_SERIALIZER,
            BOOLEAN_SERIALIZER,
            NUMBER_SERIALIZER,
            MAP_SERIALIZER,
            ARRAY_SERIALIZER,
            SHAPE_ID_SERIALIZER,
            ENUM_SERIALIZER,
            URL_SERIALIZER,
            URI_SERIALIZER,
            PATTERN_SERIALIZER,
            PATH_SERIALIZER,
            FILE_SERIALIZER,
            // Lots of things implement iterable that have specialized serialization.
            ITERABLE_SERIALIZER);

    private DefaultNodeSerializers() {}
}
