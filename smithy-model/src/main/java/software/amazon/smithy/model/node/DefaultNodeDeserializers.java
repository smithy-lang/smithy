/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static software.amazon.smithy.model.node.NodeMapper.ObjectCreatorFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.StringUtils;

/**
 * The default implementations use to convert Nodes into Objects through a {@link NodeMapper}.
 */
final class DefaultNodeDeserializers {

    // These are the kinds of types that can come back.
    // This was informed by various other mappers, including jackson-jr:
    // https://github.com/FasterXML/jackson-jr/blob/ac845b88702a1f1b1b5a75a4791b08577f74e94d/jr-objects/src/main/java/com/fasterxml/jackson/jr/type/TypeResolver.java#L79
    static Class<?> classFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof WildcardType) {
            return classFromType(((WildcardType) type).getUpperBounds()[0]);
        } else if (type instanceof TypeVariable<?>) {
            // TODO: implement this to enable improved builder detection
            throw new IllegalArgumentException("TypeVariable targets are not implemented: " + type);
        } else if (type instanceof GenericArrayType) {
            throw new IllegalArgumentException("GenericArrayType targets are not implemented: " + type);
        } else {
            return null;
        }
    }

    // Deserialize an exact type if it matches (i.e., the setter expects a Node value).
    private static final ObjectCreatorFactory EXACT_CREATOR_FACTORY = (nodeType, targetType, nodeMapper) -> {
        Class<?> targetClass = classFromType(targetType);
        if (targetClass != null
                && Node.class.isAssignableFrom(targetClass)
                && targetClass.isAssignableFrom(nodeType.getNodeClass())) {
            return (node, target, pointer, mapper) -> node;
        } else {
            return null;
        }
    };

    // Creates booleans from BooleanNodes.
    private static final ObjectCreatorFactory BOOLEAN_CREATOR_FACTORY = (nodeType, targetType, nodeMapper) -> {
        if (nodeType == NodeType.BOOLEAN) {
            Class<?> targetClass = classFromType(targetType);
            if (targetClass == Boolean.class || targetClass == boolean.class || targetClass == Object.class) {
                return (node, target, pointer, mapper) -> node.expectBooleanNode().getValue();
            }
        }
        return null;
    };

    // Null nodes always return null values.
    private static final ObjectCreatorFactory NULL_CREATOR = (nodeType, targetType, nodeMapper) -> {
        if (nodeType == NodeType.NULL) {
            return (node, target, pointer, mapper) -> null;
        }
        return null;
    };

    // String nodes can create java.land.String or a Smithy ShapeId.
    private static final ObjectCreatorFactory STRING_CREATOR = (nodeType, targetType, nodeMapper) -> {
        if (nodeType == NodeType.STRING) {
            Class<?> targetClass = classFromType(targetType);
            if (targetClass == String.class || targetClass == Object.class) {
                return (node, target, pointer, mapper) -> node.expectStringNode().getValue();
            } else if (targetClass == ShapeId.class) {
                return (node, target, pointer, mapper) -> node.expectStringNode().expectShapeId();
            }
        }
        return null;
    };

    private static final Map<Type, Function<Number, Object>> NUMBER_MAPPERS = new HashMap<>();

    static {
        NUMBER_MAPPERS.put(Number.class, n -> n);
        NUMBER_MAPPERS.put(Object.class, n -> n);
        NUMBER_MAPPERS.put(Byte.class, Number::byteValue);
        NUMBER_MAPPERS.put(byte.class, Number::byteValue);
        NUMBER_MAPPERS.put(Short.class, Number::shortValue);
        NUMBER_MAPPERS.put(short.class, Number::shortValue);
        NUMBER_MAPPERS.put(Integer.class, Number::intValue);
        NUMBER_MAPPERS.put(int.class, Number::intValue);
        NUMBER_MAPPERS.put(Long.class, Number::longValue);
        NUMBER_MAPPERS.put(long.class, Number::longValue);
        NUMBER_MAPPERS.put(Float.class, Number::floatValue);
        NUMBER_MAPPERS.put(float.class, Number::floatValue);
        NUMBER_MAPPERS.put(Double.class, Number::doubleValue);
        NUMBER_MAPPERS.put(double.class, Number::doubleValue);
        NUMBER_MAPPERS.put(BigInteger.class, n -> BigInteger.valueOf(n.longValue()));
        NUMBER_MAPPERS.put(BigDecimal.class, n -> BigDecimal.valueOf(n.doubleValue()));
    }

    // Creates numbers from NumberNodes.
    private static final ObjectCreatorFactory NUMBER_CREATOR = (nodeType, targetType, nodeMapper) -> {
        if (nodeType == NodeType.NUMBER) {
            Class<?> targetClass = classFromType(targetType);
            if (NUMBER_MAPPERS.containsKey(targetClass)) {
                return (node, target, pointer, mapper) -> {
                    Number value = node.expectNumberNode().getValue();
                    return NUMBER_MAPPERS.get(targetClass).apply(value);
                };
            }
        }
        return null;
    };

    private interface ReflectiveSupplier<T> {
        T get() throws ReflectiveOperationException;
    }

    // Deserialize an ArrayNode into a Collection.
    private static final ObjectCreatorFactory COLLECTION_CREATOR = new ObjectCreatorFactory() {
        @Override
        public NodeMapper.ObjectCreator getCreator(NodeType nodeType, Type target, NodeMapper nodeMapper) {
            if (nodeType != NodeType.ARRAY) {
                return null;
            }

            ReflectiveSupplier<Collection<Object>> ctor = createSupplier(target);
            if (ctor == null) {
                return null;
            }

            return (node, targetType, pointer, mapper) -> {
                Collection<Object> collection;

                try {
                    collection = ctor.get();
                } catch (ReflectiveOperationException e) {
                    String message = "Unable to deserialize array into Collection: " + getCauseMessage(e);
                    throw NodeDeserializationException.fromReflectiveContext(targetType, pointer, node, e, message);
                }

                // Extract out the expected generic type of the collection.
                Type memberType = Object.class;

                // If given a Class, then attempt to find the generic superclass (e.g., extending ArrayList with a
                // concrete generic type).
                if (targetType instanceof Class) {
                    targetType = ((Class<?>) target).getGenericSuperclass();
                }

                if (targetType instanceof ParameterizedType) {
                    Type[] genericTypes = ((ParameterizedType) targetType).getActualTypeArguments();
                    if (genericTypes.length > 0) {
                        memberType = genericTypes[0];
                    }
                }

                int i = 0;
                for (Node entry : node.expectArrayNode().getElements()) {
                    Object nextValue = mapper.deserializeNext(entry, pointer + "/" + i++, memberType, mapper);
                    collection.add(nextValue);
                }
                return collection;
            };
        }

        private ReflectiveSupplier<Collection<Object>> createSupplier(Type targetType) {
            Class<?> targetClass = classFromType(targetType);
            if (targetClass != null) {
                // Create an ArrayList for most cases of lists or iterables.
                if (targetClass == List.class
                        || targetClass == Collection.class
                        || targetClass == Object.class
                        || targetClass == ArrayList.class
                        || targetClass == Iterable.class) {
                    return ArrayList::new;
                } else if (targetClass == Set.class
                        || targetClass == HashSet.class
                        || targetClass == LinkedHashSet.class) {
                    // Special casing for Set or HashSet.
                    return LinkedHashSet::new;
                } else if (Collection.class.isAssignableFrom(targetClass)) {
                    return createSupplierFromReflection(targetClass);
                }
            }

            return null;
        }

        @SuppressWarnings("unchecked")
        private ReflectiveSupplier<Collection<Object>> createSupplierFromReflection(Class<?> into) {
            try {
                // Create the collection type by assuming it has a public, zero-arg constructor.
                Class<Collection<Object>> collectionTarget = (Class<Collection<Object>>) into;
                Constructor<Collection<Object>> classCtor = collectionTarget.getDeclaredConstructor();
                classCtor.setAccessible(true);
                return classCtor::newInstance;
            } catch (NoSuchMethodException e) {
                // We *could* pass here and try to let the next deserializer take a crack, but that would
                // probably never work in practice and results in a less descriptive error message.
                throw new NodeDeserializationException(
                        "Unable to find a zero-arg constructor for Collection " + into.getName(),
                        SourceLocation.NONE,
                        e);
            }
        }
    };

    private static String getCauseMessage(Throwable e) {
        // Don't pull back more than one layer since that context might be useful.
        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }

    // Deserialize an ObjectNode into a Map.
    private static final ObjectCreatorFactory MAP_CREATOR = new ObjectCreatorFactory() {
        @Override
        public NodeMapper.ObjectCreator getCreator(NodeType nodeType, Type target, NodeMapper nodeMapper) {
            if (nodeType != NodeType.OBJECT) {
                return null;
            }

            ReflectiveSupplier<Map<Object, Object>> ctor = createSupplier(target);

            if (ctor == null) {
                return null;
            }

            return (node, targetType, pointer, mapper) -> {
                Map<Object, Object> map;

                try {
                    map = ctor.get();
                } catch (ReflectiveOperationException e) {
                    String message = "Unable to deserialize object into Map: " + getCauseMessage(e);
                    throw NodeDeserializationException.fromReflectiveContext(targetType, pointer, node, e, message);
                }

                // Extract out the expected generic types of the collection.
                Type keyType = Object.class;
                Type valueType = Object.class;
                if (targetType instanceof ParameterizedType) {
                    Type[] genericTypes = ((ParameterizedType) targetType).getActualTypeArguments();
                    if (genericTypes.length > 0) {
                        keyType = genericTypes[0];
                    }
                    if (genericTypes.length > 1) {
                        valueType = genericTypes[1];
                    }
                }

                ObjectNode objectNode = node.expectObjectNode();
                for (Map.Entry<StringNode, Node> entry : objectNode.getMembers().entrySet()) {
                    String keyValue = entry.getKey().getValue();
                    Object key = mapper.deserializeNext(
                            entry.getKey(),
                            pointer + "/(key:" + keyValue + ")",
                            keyType,
                            mapper);
                    Object value = mapper.deserializeNext(
                            entry.getValue(),
                            pointer + "/" + keyValue,
                            valueType,
                            mapper);
                    map.put(key, value);
                }

                return map;
            };
        }

        private ReflectiveSupplier<Map<Object, Object>> createSupplier(Type into) {
            Class<?> targetClass = classFromType(into);
            if (targetClass != null) {
                if (targetClass == Object.class || targetClass == Map.class || targetClass == HashMap.class) {
                    return HashMap::new;
                } else if (Map.class.isAssignableFrom(targetClass)) {
                    return createSupplierFromReflection(targetClass);
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private ReflectiveSupplier<Map<Object, Object>> createSupplierFromReflection(Class<?> into) {
            try {
                // Try to create the map from an empty constructor.
                Class<Map<Object, Object>> collectionTarget = (Class<Map<Object, Object>>) into;
                Constructor<Map<Object, Object>> mapCtor = collectionTarget.getDeclaredConstructor();
                return mapCtor::newInstance;
            } catch (NoSuchMethodException e) {
                // We *could* pass here and try to let the next deserializer take a crack, but that would
                // probably never work in practice and results in a less descriptive error message.
                throw new NodeDeserializationException(
                        "Unable to find a zero-arg constructor for Map " + into.getName(),
                        SourceLocation.NONE,
                        e);
            }
        }
    };

    // Creates an object from any type of Node using the #fromNode factory method.
    private static final ObjectCreatorFactory FROM_NODE_CREATOR = (nodeType, target, nodeMapper) -> {
        Class<?> targetClass = classFromType(target);

        if (targetClass != null && !nodeMapper.getDisableFromNode().contains(targetClass)) {
            for (Method method : targetClass.getMethods()) {
                if ((method.getName().equals("fromNode"))
                        && targetClass.isAssignableFrom(method.getReturnType())
                        && method.getParameters().length == 1
                        && Node.class.isAssignableFrom(method.getParameters()[0].getType())
                        && Modifier.isStatic(method.getModifiers())) {
                    return (node, targetType, pointer, mapper) -> {
                        try {
                            return method.invoke(null, node);
                        } catch (ReflectiveOperationException e) {
                            String message = "Unable to deserialize Node using fromNode method: " + getCauseMessage(e);
                            throw NodeDeserializationException
                                    .fromReflectiveContext(targetType, pointer, node, e, message);
                        }
                    };
                }
            }
        }
        return null;
    };

    static final class BeanMapper {
        // Cache of Pair<types, member-name> to a setter Method.
        private static final ConcurrentMap<Pair<Class<?>, String>, Method> SETTER_CACHE = new ConcurrentHashMap<>();

        static void apply(
                Object value,
                Node node,
                Type target,
                String pointer,
                NodeMapper mapper
        ) throws ReflectiveOperationException {
            for (Map.Entry<String, Node> entry : node.expectObjectNode().getStringMap().entrySet()) {
                Method setter = findSetter(target, entry.getKey());
                if (setter == null) {
                    mapper.getWhenMissingSetter().handle(target, pointer, entry.getKey(), entry.getValue());
                } else {
                    Object member = mapper.deserializeNext(
                            entry.getValue(),
                            pointer + "/" + entry.getKey(),
                            setter.getParameters()[0].getParameterizedType(),
                            mapper);
                    setter.invoke(value, member);
                }
            }
        }

        // Return value is null or a setter method to invoke.
        private static Method findSetter(Type type, String memberName) {
            Class<?> targetType = classFromType(type);

            if (targetType == null) {
                return null;
            }

            return SETTER_CACHE.computeIfAbsent(Pair.of(targetType, memberName), pair -> {
                String sanitized = sanitizePropertyName(pair.right);
                if (sanitized != null) {
                    for (Method method : targetType.getMethods()) {
                        if (isBeanOrBuilderSetter(method, targetType, sanitized)) {
                            return method;
                        }
                    }
                }
                return null;
            });
        }

        // Strips special characters by removing special characters and converting the character
        // after each special character to camel case (e.g., blah.blerg becomes "blahBlerg").
        private static String sanitizePropertyName(String value) {
            StringBuilder result = new StringBuilder(value.length());
            boolean nextUpper = false;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (!Character.isJavaIdentifierPart(c)) {
                    // Shy away from fixing things like "foo..bar". At least for now.
                    if (nextUpper) {
                        return null;
                    }
                    nextUpper = true;
                } else if (nextUpper) {
                    nextUpper = false;
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }

        private static boolean isBeanOrBuilderSetter(Method method, Class<?> type, String propertyName) {
            if (Modifier.isStatic(method.getModifiers())) {
                return false;
            }

            Parameter[] parameters = method.getParameters();
            if (parameters.length != 1) {
                return false;
            }

            // Must either return the target class itself (like a builder) or void.
            // Ideally we should attempt to resolve any generics and make an assertion of the concrete type.
            if (!(method.getReturnType() == void.class || method.getReturnType().isAssignableFrom(type))) {
                return false;
            }

            // x(x)
            if (method.getName().equals(propertyName)) {
                return true;
            }

            // setX(x)
            if (method.getName().equals("set" + StringUtils.capitalize(propertyName))) {
                return true;
            }

            return false;
        }
    }

    // Creates an object from any type of Node using the #builder factory method.
    @SuppressWarnings("unchecked")
    private static final ObjectCreatorFactory FROM_BUILDER_CREATOR = (nodeType, target, nodeMapper) -> {
        Class<?> targetClass = classFromType(target);
        if (nodeType != NodeType.OBJECT || targetClass == null) {
            return null;
        }

        for (Method method : targetClass.getMethods()) {
            if ((method.getName().equals("builder"))
                    && SmithyBuilder.class.isAssignableFrom(method.getReturnType())
                    && method.getParameters().length == 0
                    && Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                return (node, targetType, pointer, mapper) -> {
                    try {
                        SmithyBuilder<Object> builder = ((SmithyBuilder<Object>) method.invoke(null));
                        BeanMapper.apply(builder, node, builder.getClass(), pointer, mapper);
                        applySourceLocation(builder, node);
                        return builder.build();
                    } catch (ReflectiveOperationException e) {
                        String message = "Unable to deserialize Node using a builder: " + getCauseMessage(e);
                        throw NodeDeserializationException.fromReflectiveContext(targetType, pointer, node, e, message);
                    }
                };
            }
        }
        return null;
    };

    private static void applySourceLocation(Object object, FromSourceLocation sourceLocation)
            throws ReflectiveOperationException {
        Method setter = BeanMapper.findSetter(object.getClass(), "sourceLocation");
        if (setter != null) {
            setter.invoke(object, sourceLocation.getSourceLocation());
        }
    }

    // Attempts to create a Bean style POJO using a zero-value constructor.
    private static final ObjectCreatorFactory BEAN_CREATOR = (nodeType, target, nodeMapper) -> {
        Class<?> targetClass = classFromType(target);
        if (nodeType != NodeType.OBJECT || targetClass == null) {
            return null;
        }

        try {
            // TODO: we could potentially add support for this if it's not too complicated.
            if (targetClass.getEnclosingClass() != null && !Modifier.isStatic(targetClass.getModifiers())) {
                throw new NodeDeserializationException(
                        "Cannot create non-static inner class: " + targetClass.getCanonicalName(),
                        SourceLocation.NONE);
            }

            Constructor<?> ctor = targetClass.getDeclaredConstructor();
            ctor.setAccessible(true);

            return (node, targetType, pointer, mapper) -> {
                try {
                    Object value = ctor.newInstance();
                    BeanMapper.apply(value, node, targetType, pointer, mapper);
                    applySourceLocation(value, node);
                    return value;
                } catch (ReflectiveOperationException e) {
                    throw NodeDeserializationException.fromReflectiveContext(targetType,
                            pointer,
                            node,
                            e,
                            "Unable to deserialize a Node when invoking target constructor: " + getCauseMessage(e));
                }
            };
        } catch (NoSuchMethodException e) {
            // Don't fail, and instead try the next deserializer.
            return null;
        }
    };

    // Creates enums by checking each enum variant and detecting if the result of
    // calling toString on the variant matches the given string.
    // Mimic's Jackson's behavior when using READ_ENUMS_USING_TO_STRING
    // See https://github.com/FasterXML/jackson-databind/wiki/Deserialization-Features
    private static final ObjectCreatorFactory ENUM_CREATOR = (nodeType, target, nodeMapper) -> {
        Class<?> targetClass = classFromType(target);

        if (nodeType != NodeType.STRING || targetClass == null || !Enum.class.isAssignableFrom(targetClass)) {
            return null;
        }

        return (node, targetType, pointer, mapper) -> {
            String name = node.expectStringNode().getValue();
            for (Object constant : targetClass.getEnumConstants()) {
                if (constant.toString().equals(name)) {
                    return constant;
                }
            }

            // Give an error message with suggestions.
            List<String> names = new ArrayList<>();
            for (Object constant : targetClass.getEnumConstants()) {
                names.add(constant.toString());
            }

            throw NodeDeserializationException.fromContext(targetClass,
                    pointer,
                    node,
                    null,
                    "Expected one of the following enum strings: " + names);
        };
    };

    private interface FromStringClassFactory {
        Object create(String input) throws Exception;
    }

    // These types have special, built-in handling.
    // This mirrors the simpler behaviors allowed in Jackson.
    // See https://github.com/FasterXML/jackson-databind/blob/ab583fb2319ee33ef6b548b720afec84265d40a7/src/main/java/com/fasterxml/jackson/databind/deser/std/FromStringDeserializer.java
    private static final Map<Type, FromStringClassFactory> FROM_STRING_CLASSES = MapUtils.of(
            URL.class,
            URL::new,
            URI.class,
            URI::new,
            Pattern.class,
            Pattern::compile,
            Path.class,
            Paths::get,
            File.class,
            File::new);

    private static final ObjectCreatorFactory FROM_STRING = (nodeType, target, nodeMapper) -> {
        if (nodeType != NodeType.STRING || !FROM_STRING_CLASSES.containsKey(target)) {
            return null;
        }

        FromStringClassFactory factory = FROM_STRING_CLASSES.get(target);
        return (node, targetType, pointer, mapper) -> {
            String value = node.expectStringNode().getValue();
            try {
                return factory.create(value);
            } catch (Exception e) {
                throw NodeDeserializationException.fromContext(targetType, pointer, node, e, e.getMessage());
            }
        };
    };

    // The priority ordered list of default factories that NodeMapper uses.
    // The priority is determined based on the specificity of each deserializer;
    // the most specific ones should appear at the start of the list, and the
    // most generic ones should appear at the end.
    //
    // If we ever open up the API, then we should consider making the priority
    // more explicit by adding it to the ObjectCreatorFactory interface.
    private static final List<ObjectCreatorFactory> DEFAULT_FACTORIES = ListUtils.of(
            EXACT_CREATOR_FACTORY,
            NULL_CREATOR,
            FROM_NODE_CREATOR,
            BOOLEAN_CREATOR_FACTORY,
            FROM_STRING,
            STRING_CREATOR,
            ENUM_CREATOR,
            NUMBER_CREATOR,
            COLLECTION_CREATOR,
            MAP_CREATOR,
            FROM_BUILDER_CREATOR,
            BEAN_CREATOR);

    static final ObjectCreatorFactory DEFAULT_CHAIN = (nodeType, target, nodeMapper) -> {
        for (ObjectCreatorFactory factory : DEFAULT_FACTORIES) {
            NodeMapper.ObjectCreator result = factory.getCreator(nodeType, target, nodeMapper);
            if (result != null) {
                return result;
            }
        }
        return null;
    };

    // Creates an ObjectCreatorFactory that caches the result of finding ObjectCreators.
    private static ObjectCreatorFactory cachedCreator(ObjectCreatorFactory delegate) {
        IdentityClassCache<String, NodeMapper.ObjectCreator> cache = new IdentityClassCache<>();
        return (nodeType, target, nodeMapper) -> {
            String key = nodeType.getNodeClass() + ":" + target.getTypeName();
            return cache.getForClass(key, target, () -> delegate.getCreator(nodeType, target, nodeMapper));
        };
    }

    // This is the default creator used to deserialize types.
    static final ObjectCreatorFactory DEFAULT_CACHED_CREATOR = cachedCreator(DEFAULT_CHAIN);

    private DefaultNodeDeserializers() {}
}
