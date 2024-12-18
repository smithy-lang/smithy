/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static java.lang.String.format;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Serializes and deserializes Smithy {@code Node} values to/from objects.
 *
 * <p>This class <em>does not</em> serialize a {@code Node} value as a JSON
 * string. It converts Java Object values to and from {@code Node} values.
 * Use {@link Node#printJson(Node)} to serialize JSON strings from a
 * {@code Node} value.
 *
 * <p>When stable, we may add the ability to add custom serializers and
 * deserializers. Until then, there is no way to customize the serialization
 * and deserialization rules.
 */
public final class NodeMapper {

    /**
     * Specifies the behavior of the mapper when attempting to deserialize an unknown property.
     */
    public enum WhenMissing {
        /**
         * Throws an exception when attempting to deserialize an unknown property.
         */
        FAIL {
            public void handle(Type into, String pointer, String property, Node value) {
                String message = createMessage(property, pointer, into, value);
                throw new NodeDeserializationException(message, value.getSourceLocation());
            }
        },

        /**
         * Warns when attempting to deserialize an unknown property.
         */
        WARN {
            public void handle(Type into, String pointer, String property, Node value) {
                LOGGER.warning(createMessage(property, pointer, into, value));
            }
        },

        /**
         * Ignores unknown properties.
         */
        IGNORE {
            public void handle(Type into, String pointer, String property, Node value) {}
        };

        /**
         * Invoked when an object property cannot be deserialized.
         *
         * @param into The value type being created.
         * @param pointer The JSON pointer to the type from the original node.
         * @param property The property that was unknown to the type.
         * @param value The Node being deserialized.
         */
        public abstract void handle(Type into, String pointer, String property, Node value);

        private static String createMessage(String property, String pointer, Type into, Node node) {
            String location = node.getSourceLocation() == SourceLocation.NONE
                    ? ""
                    : " " + node.getSourceLocation().toString().trim();
            return format("Deserialization error at %s%s: unable to find setter method for `%s` on %s",
                    getNormalizedPointer(pointer),
                    location,
                    property,
                    into.getTypeName());
        }
    }

    /**
     * Converts an object of type {@code T} into a {@code Node}.
     *
     * <p>This API is currently package-private until we're confident in the
     * implementation.
     *
     * @param <T> Type to convert into a {@code Node} value.
     */
    interface Serializer<T> {
        /**
         * Gets the type that this serializer handles.
         *
         * @return Returns the serialization type.
         */
        Class<T> getType();

        /**
         * Converts an object of type {@code T} into a {@code Node}.
         *
         * <p>Return {@code null} to allow other serializers to attempt to
         * serialize a type.
         *
         * @param value Value to convert into a {@code Node}.
         * @param serializedObjects Identity set used to track recursion.
         * @param mapper Mapper used to recursively serialize types.
         * @return Returns the serialized {@code Node} value.
         */
        Node serialize(T value, Set<Object> serializedObjects, NodeMapper mapper);
    }

    /**
     * Creates objects from Node values for a specific type.
     */
    @FunctionalInterface
    interface ObjectCreator {
        /**
         * Creates an Object from the given {@code Node} into the given {@code target}.
         *
         * @param node Node to convert into {@code target}.
         * @param type Type to create.
         * @param pointer The JSON pointer to the current serialization context.
         * @param mapper Mapper to invoke to recursively deserialize values.
         * @return Returns the created {@code target} instance.
         * @throws NodeDeserializationException when unable to deserialize a value.
         */
        Object create(Node node, Type type, String pointer, NodeMapper mapper);
    }

    /**
     * Factory method used to create an {@link ObjectCreator} for a given
     * {@code NodeType} and target class.
     *
     * <p>This interface is introduced to allow for caching of the reflection
     * necessary to determine <em>how</em> to create the target type.
     */
    @FunctionalInterface
    interface ObjectCreatorFactory {
        /**
         * Returns an {@code ObjectCreator} used to create {@code target} from a
         * {@code Node} of type {@code nodeType}.
         *
         * @param nodeType Node type being converted.
         * @param target The class to create from the Node.
         * @param nodeMapper The NodeMapper being used to call the ObjectCreator.
         * @return Returns the {@code ObjectCreator} or {@code null} if the factory cannot handle the given arguments.
         * @throws NodeDeserializationException when unable to create a factory.
         */
        ObjectCreator getCreator(NodeType nodeType, Type target, NodeMapper nodeMapper);
    }

    interface ObjectClassCreatorFactory {
        ObjectCreator getCreator(NodeType nodeType, Class<?> target, NodeMapper nodeMapper);
    }

    private static final Logger LOGGER = Logger.getLogger(NodeMapper.class.getName());
    private WhenMissing whenMissing = WhenMissing.WARN;
    private final Set<Type> disableToNode = new HashSet<>();
    private final Set<Type> disableFromNode = new HashSet<>();
    private boolean serializeNullValues = false;
    private boolean omitEmptyValues;

    private final List<Serializer> serializers = DefaultNodeSerializers.SERIALIZERS;
    private final ObjectCreatorFactory creatorFactory = DefaultNodeDeserializers.DEFAULT_CACHED_CREATOR;

    /**
     * Specifies if {@code null} values returned from getters are serialized.
     *
     * @param serializeNullValues Set to true to serialize {@code null} values.
     */
    public void setSerializeNullValues(boolean serializeNullValues) {
        this.serializeNullValues = serializeNullValues;
    }

    /**
     * @return Gets whether or not {@code null} values are serialized.
     */
    public boolean getSerializeNullValues() {
        return serializeNullValues;
    }

    /**
     * Sets the behavior of the deserializer when a setting is missing.
     *
     * @param whenMissing Behavior when a property is not matched to a setter.
     */
    public void setWhenMissingSetter(WhenMissing whenMissing) {
        this.whenMissing = Objects.requireNonNull(whenMissing);
    }

    /**
     * @return Gets the behavior of the deserializer when a setting is missing.
     */
    public WhenMissing getWhenMissingSetter() {
        return whenMissing;
    }

    /**
     * Disables the use of the {@code toNode} method for a specific class
     * when serializing the class as a POJO.
     *
     * <p>This method disables a specific concrete class and does not
     * disable subclasses or implementations of an interface.
     *
     * <p>This is useful when using the NodeMapper inside of a {@code toNode}
     * implementation.
     *
     * @param type Class to disable the {@code toNode} method serialization for.
     */
    public void disableToNodeForClass(Type type) {
        disableToNode.add(type);
    }

    /**
     * Enables the use of the {@code toNode} method for a specific class
     * when serializing the class as a POJO.
     *
     * @param type Class to enable the {@code toNode} method serialization for.
     */
    public void enableToNodeForClass(Type type) {
        disableToNode.remove(type);
    }

    /**
     * Gets the set of classes where {@code toNode} is disabled.
     *
     * @return Returns the disabled classes.
     */
    public Set<Type> getDisableToNode() {
        return disableToNode;
    }

    /**
     * Disables the use of {@code fromNode} method for a specific class
     * when deserializing the class.
     *
     * <p>This method disables a specific concrete class and does not
     * disable subclasses or implementations of an interface.
     *
     * <p>This is useful when using the NodeMapper inside of a {@code fromNode}
     * implementation.
     *
     * @param type Class to disable the {@code fromNode} method deserialization for.
     */
    public void disableFromNodeForClass(Type type) {
        disableFromNode.add(type);
    }

    /**
     * Enables the use of the {@code FromNode} method for a specific class
     * when deserializing the class.
     *
     * @param type Class to enable the {@code fromNode} method deserialization for.
     */
    public void enableFromNodeForClass(Type type) {
        disableFromNode.remove(type);
    }

    /**
     * Gets the set of classes where {@code fromNode} is disabled.
     *
     * @return Returns the disabled classes.
     */
    public Set<Type> getDisableFromNode() {
        return disableFromNode;
    }

    /**
     * Gets whether or not false, empty arrays, and empty objects are omitted from
     * serialized POJOs.
     *
     * @return Returns true if empty arrays and POJOs returned from POJO getters are omitted.
     */
    public boolean getOmitEmptyValues() {
        return omitEmptyValues;
    }

    /**
     * Gets whether or not false, empty arrays, and empty objects are omitted from serialized POJOs.
     *
     * @param omitEmptyValues Set to true if false, empty arrays, and objects returned from POJO getters are omitted.
     */
    public void setOmitEmptyValues(boolean omitEmptyValues) {
        this.omitEmptyValues = omitEmptyValues;
    }

    /**
     * Serializes the given {@code object} as a {@code Node}.
     *
     * <p>This method is able to serialize the following types in the
     * given evaluation order:
     *
     * <ol>
     *     <li>A {@code null} value is serialized as a {@link NullNode} if {@link #getSerializeNullValues()}
     *     returns {@code true}.
     *     </li>
     *     <li>Instances of {@link ToNode} will return the result of calling {@link ToNode#toNode()}.
     *     </li>
     *     <li>Instances of {@link Optional} will serialize a {@link NullNode} when the Optional is empty, or
     *     the result of serializing the value contained in the {@code Optional} when present.
     *     </li>
     *     <li>{@link String} value is serialized as a {@link StringNode}.
     *     </li>
     *     <li>{@link Boolean} value or {@code boolean} is serialized as a {@link BooleanNode}.
     *     </li>
     *     <li>Any instance of {@link Number} value is serialized as a {@link NumberNode}.
     *     </li>
     *     <li>The {@code toString} method is called when {@link URL}, {@link URI}, {@link Pattern}, and
     *     {@link Path} are serialized.
     *     </li>
     *     <li>A {@link File} is serialized by serializing the string value of {@link File#toURI()}.
     *     </li>
     *     <li>{@link Enum} value is serialized as a {@link StringNode} by calling its {@code toString} method.
     *     <li>{@link ShapeId} is serialized as a {@link StringNode} that contains the absolute shape ID.
     *     </li>
     *     <li>Any instance of a {@link Map} is supported as long as the key and value of the map are both
     *     supported types (note that Map keys must serialize as StringNode). A {@code Map} is converted to
     *     an {@link ObjectNode}.
     *     </li>
     *     <li>Any instance of a {@link Iterable} is supported as long as the value contained in the
     *     {@code Iterable} is a supported type. An {@code Iterable} is converted to an {@link ArrayNode}.
     *     An {@code Iterable} broadly covers many Java types, including {@link Collection}.
     *     </li>
     *     <li>Primitive arrays are converted to an {@code ArrayNode} if and only if the values contained in the
     *     array are one of the supported types supported by the serializer.
     *     </li>
     *     <li>Finally, an object is serialized using Bean style semantics; any public getter
     *     (get* or is* method with no arguments) is invoked and it's return value is put in the {@link ObjectNode}.
     *     Each property of the Bean recursively invokes the serializer and must be one of the supported types.
     *     Properties associated with a getter that are marked as {@code transient} are not serialized (where an
     *     "association" is defined as a class field with the same lowercase name as the suffix of the getter
     *     method). For example, given a method "getFoo", both "foo" and "Foo" are checked as associated
     *     property names.
     *     </li>
     * </ol>
     *
     * @param object Object to serialize.
     * @return Returns the serialized {@code Node}.
     * @throws NodeSerializationException on error.
     */
    public Node serialize(Object object) {
        return serialize(object, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Serializes the given {@code object} as a {@code Node}.
     *
     * <p>This method is used when serializing values recursively from another serializer.
     *
     * @param object Object to serialize.
     * @param serializedObject An identity set of objects that have already been serialized.
     *   This prevents infinite recursion on a self-referencing value.
     * @return Returns the serialized {@code Node}.
     * @throws NodeSerializationException on error.
     */
    @SuppressWarnings("unchecked")
    Node serialize(Object object, Set<Object> serializedObject) {
        if (object == null) {
            return Node.nullNode();
        }

        // Iterate over the serializers in the correct order.
        for (Serializer serializer : serializers) {
            if (serializer.getType().isInstance(object)) {
                Node result = serializer.serialize(object, serializedObject, this);
                if (result != null) {
                    return result;
                }
            }
        }

        // Finally, attempt to serialize using bean conventions.
        return DefaultNodeSerializers.FROM_BEAN.serialize(object, serializedObject, this);
    }

    /**
     * Deserialize a Node {@code value} into an instance of {@code T}.
     *
     * <p>This method can deserialize various kinds of values depending on the given
     * node type and the target type:
     *
     * <ol>
     *     <li>{@code null}</li>
     *     <li>String</li>
     *     <li>Primitive and boxed booleans</li>
     *     <li>Primitive and boxed {@link Number} types</li>
     *     <li>Lists and Sets of any support value</li>
     *     <li>Maps with String keys and values of any supported type</li>
     *     <li>Direct {@link Node} to {@code Node} conversions.</li>
     *     <li>Any object that has a public static {@code fromNode} method that accepts a
     *     {@link Node} and returns an instance of the object.</li>
     *     <li>Strings are deserialized to enums by finding the first enum value that has a {@code toString}
     *     method that matches the string value.</li>
     *     <li>Built-in support for URI, URL, Pattern, Path, and File</li>
     *     <li>When deserializing an object, any target object that provides a public static method named
     *     {@code builder} that returns an instance of {@link SmithyBuilder} is invoked, and the builder is then
     *     mutated using bean like setters (with an optional "set") prefix, until finally, the build method is
     *     called and its result is returned.</li>
     *     <li>When deserializing an object, the last thing tried is to find a public, zero-arg constructor,
     *     and then the object is mutated using bean-style setter conventions for each key-value pair.</li>
     *     <li>NodeMapper does not support non-static inner classes, classes with generic parameters, or
     *     generic arrays. Support for these may be added in the future.</li>
     * </ol>
     *
     * <p>Objects with a public method named {@code sourceLocation} or {@code setSourceLocation}
     * are invoked and provided the source location of the deserialized {@code value}.
     *
     * @param value Value to deserialize.
     * @param into Class to create.
     * @param <T> Type of value to create.
     * @return Returns the created value.
     * @throws NodeDeserializationException on error.
     * @see #deserializeCollection(Node, Class, Class)
     * @see #deserializeMap(Node, Class, Class)
     */
    public <T> T deserialize(Node value, Class<T> into) {
        return deserializeNext(value, "", into, this);
    }

    /**
     * Invokes setters on the given {@code objectToMutate} from the provided
     * {@code Node}.
     *
     * @param value Value to deserialize.
     * @param objectToMutate Object to mutate and populate from the node.
     * @param <T> The value to mutate using Bean style setters.
     * @return Returns the passed in value.
     * @throws NodeDeserializationException on error.
     */
    public <T> T deserializeInto(Node value, T objectToMutate) {
        try {
            DefaultNodeDeserializers.BeanMapper.apply(objectToMutate, value, objectToMutate.getClass(), "", this);
            return objectToMutate;
        } catch (ReflectiveOperationException e) {
            // Wrap unexpected exceptions.
            throw createError(objectToMutate.getClass(), "/", value, e.getMessage(), e);
        }
    }

    /**
     * Deserialize a Node {@code value} into a {@link Collection} {@code T} of {@code U} members.
     *
     * <p>This method is necessary because of Java's runtime type erasure.
     *
     * @param value Value to deserialize.
     * @param into Collection class to create.
     * @param members The collection's parametric type.
     * @param <T> Type of collection value to create.
     * @param <U> Type contained within the collection.
     * @param <V> Returned collection type.
     * @return Returns the created collection.
     * @throws NodeDeserializationException on error.
     * @see #deserialize(Node, Class)
     */
    public <T extends Collection<?>, U, V extends Collection<? extends U>> V deserializeCollection(
            Node value,
            Class<T> into,
            Class<U> members
    ) {
        ParameterizedType type = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {members};
            }

            @Override
            public Type getRawType() {
                return into;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        return deserializeNext(value, "", type, this);
    }

    /**
     * Deserialize a Node {@code value} into a {@link Map} {@code T}
     * with String keys and {@code U} values.
     *
     * <p>This method is necessary because of Java's runtime type erasure.
     *
     * @param value Value to deserialize.
     * @param into Map class to create.
     * @param members The maps's parametric type.
     * @param <T> Type of map value to create.
     * @param <U> Type contained within the map values.
     * @param <V> Returned map type.
     * @return Returns the created map.
     * @throws NodeDeserializationException on error.
     * @see #deserialize(Node, Class)
     */
    public <T extends Map<?, ?>, U, V extends Map<String, ? extends U>> V deserializeMap(
            Node value,
            Class<T> into,
            Class<U> members
    ) {
        ParameterizedType type = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {String.class, members};
            }

            @Override
            public Type getRawType() {
                return into;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        return deserializeNext(value, "", type, this);
    }

    /**
     * Performs the inner deserialization of a type.
     *
     * <p>This method is invoked by instances of {@link ObjectCreator} to
     * recursively deserialize a type while also tracking the updated JSON
     * pointer context for error reporting.
     *
     * @param value Node value to deserialize.
     * @param pointer The JSON Pointer to the location of the value being deserialized.
     * @param into The type being created.
     * @param mapper The {@code Mapper} that can be invoked to recursively deserialize.
     * @param <T> The type of value to create.
     * @return Returns the created value.
     */
    @SuppressWarnings("unchecked")
    <T> T deserializeNext(Node value, String pointer, Type into, NodeMapper mapper) {
        Objects.requireNonNull(value, "Deserialization value cannot be null");
        Objects.requireNonNull(pointer, "Deserialization pointer cannot be null");
        Objects.requireNonNull(into, "Deserialization into cannot be null");
        Objects.requireNonNull(mapper, "Deserialization mapper cannot be null");

        try {
            ObjectCreator creator = creatorFactory.getCreator(value.getType(), into, this);
            if (creator == null) {
                throw createError(into, pointer, value, null, null);
            }
            return (T) creator.create(value, into, pointer, mapper);
        } catch (NodeDeserializationException e) {
            // Rethrow already formatted exceptions.
            throw e;
        } catch (Exception e) {
            // Wrap unexpected exceptions.
            throw createError(into, pointer, value, e.getMessage(), e);
        }
    }

    private static NodeDeserializationException createError(
            Type into,
            String pointer,
            Node node,
            String message,
            Throwable cause
    ) {
        String errorMessage = createErrorMessage(into, pointer, node, message);
        return new NodeDeserializationException(errorMessage, node.getSourceLocation(), cause);
    }

    static String createErrorMessage(Type into, String pointer, Node node, String message) {
        String formatted = String.format(
                "Deserialization error at %s: unable to create %s from %s",
                getNormalizedPointer(pointer),
                into.getTypeName(),
                Node.printJson(node));
        if (message != null) {
            formatted += ": " + message;
        }
        return formatted;
    }

    private static String getNormalizedPointer(String pointer) {
        return "(" + (pointer.equals(" ") || pointer.isEmpty() ? "/" : pointer) + ")";
    }
}
