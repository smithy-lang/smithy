/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public class NodeMapperTest {
    @Test
    public void convertsComplexObjectsToNode() {
        List<Map<String, Foo>> value = new ArrayList<>();
        Map<String, Foo> mappings = new LinkedHashMap<>();
        mappings.put("hi", new Foo());
        value.add(mappings);
        NodeMapper mapper = new NodeMapper();
        Node result = mapper.serialize(value);

        Node.assertEquals(result, Node.parse("[ {\"hi\": { \"baz\": \"baz\", \"bar\": \"bar\" } } ]"));
    }

    private static final class Foo {
        public String getBaz() {
            return "baz";
        }

        public String getBar() {
            return "bar";
        }
    }

    @Test
    public void serializesAlphabetically() {
        NodeMapper mapper = new NodeMapper();
        ObjectNode result = mapper.serialize(new Foo()).expectObjectNode();
        Collection<String> keys = result.getStringMap().keySet();

        assertThat(keys, contains("bar", "baz"));
    }

    @Test
    public void serializesCollectionsAndArrays() {
        NodeMapper mapper = new NodeMapper();
        List<String> foos = ListUtils.of("a", "b", "c");
        String[] foosArray = foos.toArray(new String[0]);
        Node result1 = mapper.serialize(foos);
        Node result2 = mapper.serialize(foosArray);

        assertThat(result1, equalTo(result2));
        assertThat(result1, equalTo(Node.fromStrings("a", "b", "c")));
    }

    @Test
    public void doesNotSerializeTransientValues() {
        NodeMapper mapper = new NodeMapper();
        HasTransient hasTransient = new HasTransient();
        hasTransient.setFoo("foo");
        hasTransient.setHi("hi");
        ObjectNode result = mapper.serialize(hasTransient).expectObjectNode();

        Node.assertEquals(result, Node.parse("{\"hi\": \"hi\"}"));
    }

    static class HasTransient {
        private transient String foo;
        private String hi;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getHi() {
            return hi;
        }

        public void setHi(String hi) {
            this.hi = hi;
        }
    }

    @Test
    public void serializesUsingToNode() {
        NodeMapper mapper = new NodeMapper();
        HasToNode value = new HasToNode();
        Node result = mapper.serialize(value);

        Node.assertEquals(result, Node.parse("{\"foo\": \"baz\"}"));
    }

    private static final class HasToNode implements ToNode {
        @Override
        public Node toNode() {
            return Node.objectNode().withMember("foo", "baz");
        }
    }

    @Test
    public void serializesKitchenSink() {
        NodeMapper mapper = new NodeMapper();
        Node result = mapper.serialize(new KitchenSink());

        Node.assertEquals(result,
                Node.parse("{\n"
                        + "  \"long\": 4,\n"
                        + "  \"float\": 5.0,\n"
                        + "  \"string\": \"string\",\n"
                        + "  \"shapeId\": \"foo.baz#Bar\",\n"
                        + "  \"double\": 6.0,\n"
                        + "  \"boolean\": true,\n"
                        + "  \"enum\": \"array\",\n"
                        + "  \"int\": 3,\n"
                        + "  \"presentString\": \"present\",\n"
                        + "  \"short\": 2,\n"
                        + "  \"boolean2\": true,\n"
                        + "  \"byte\": 1,\n"
                        + "  \"map\": {\n"
                        + "    \"b\": [\n"
                        + "      \"2\",\n"
                        + "      \"3\"\n"
                        + "    ],\n"
                        + "    \"a\": [\n"
                        + "      \"1\",\n"
                        + "      \"2\"\n"
                        + "    ]\n"
                        + "  },\n"
                        + "  \"complexList\": [\n"
                        + "    [\n"
                        + "      \"a\",\n"
                        + "      \"b\"\n"
                        + "    ],\n"
                        + "    [\n"
                        + "      \"c\",\n"
                        + "      \"d\"\n"
                        + "    ]\n"
                        + "  ],\n"
                        + "  \"falseBoolean\": false,\n"
                        + "  \"stringIterable\": [\n"
                        + "    \"foo\",\n"
                        + "    \"baz\"\n"
                        + "  ]\n"
                        + "}"));
    }

    private static final class KitchenSink {
        public byte getByte() {
            return 1;
        }

        public short getShort() {
            return 2;
        }

        public int getInt() {
            return 3;
        }

        public long getLong() {
            return 4;
        }

        public float getFloat() {
            return 5;
        }

        public double getDouble() {
            return 6;
        }

        public String getString() {
            return "string";
        }

        public boolean isBoolean() {
            return true;
        }

        public boolean getBoolean2() {
            return true;
        }

        public boolean isFalseBoolean() {
            return false;
        }

        public ShapeId getShapeId() {
            return ShapeId.from("foo.baz#Bar");
        }

        public Optional<String> getEmptyString() {
            return Optional.empty();
        }

        public Optional<String> getPresentString() {
            return Optional.of("present");
        }

        public NodeType getEnum() {
            return NodeType.ARRAY;
        }

        public Map<String, List<String>> getMap() {
            Map<String, List<String>> result = new LinkedHashMap<>();
            result.put("a", ListUtils.of("1", "2"));
            result.put("b", ListUtils.of("2", "3"));
            return result;
        }

        public Iterable<String> getStringIterable() {
            return Arrays.asList("foo", "baz");
        }

        public List<List<String>> getComplexList() {
            List<List<String>> result = new ArrayList<>();
            result.add(Arrays.asList("a", "b"));
            result.add(Arrays.asList("c", "d"));
            return result;
        }
    }

    @Test
    public void doesNotInfinitelySerializeSelfReferentialTypes() {
        NodeMapper mapper = new NodeMapper();
        SelfReferential value = new SelfReferential();
        value.recursive = value;
        Node result = mapper.serialize(value);

        Node.assertEquals(result, Node.parse("{\"name\": \"hi\"}"));
    }

    public final class SelfReferential {
        SelfReferential recursive;

        public SelfReferential getRecursive() {
            return recursive;
        }

        public String getName() {
            return "hi";
        }
    }

    @Test
    public void serializesRepeatedButNotRecursiveValues() {
        RepeatedValues a = new RepeatedValues();
        List<RepeatedValues> repeatedValues = ListUtils.of(a, a, a);
        NodeMapper mapper = new NodeMapper();
        Node result = mapper.serialize(repeatedValues);

        Node.assertEquals(result, Node.parse("[ {\"foo\": \"foo\"}, {\"foo\": \"foo\"}, {\"foo\": \"foo\"} ]"));
    }

    public final class RepeatedValues {
        public String getFoo() {
            return "foo";
        }
    }

    @Test
    public void doesNotSerializeLotsOfStuff() {
        NodeMapper mapper = new NodeMapper();
        NonSerialized value = new NonSerialized();
        Node result = mapper.serialize(value);

        Node.assertEquals(result, Node.objectNode());
    }

    public static final class NonSerialized {
        public static String getStatic() {
            return "hi";
        }

        public static boolean isStatic() {
            return true;
        }

        public String isInvalidBoolean() {
            return "yes";
        }

        public String get() {
            return "missing";
        }
    }

    @Test
    public void canSerializeNullValues() {
        NodeMapper mapper = new NodeMapper();
        HasNulls value = new HasNulls();
        Node result = mapper.serialize(value);

        Node.assertEquals(result, Node.objectNode());

        mapper.setSerializeNullValues(true);
        Node result2 = mapper.serialize(value);
        Node.assertEquals(result2, Node.objectNode().withMember("foo", Node.nullNode()));
    }

    public static final class HasNulls {
        public String getFoo() {
            return null;
        }
    }

    @Test
    public void cannotSerializeMapsWhenKeyIsNotString() {
        Map<List<String>, String> badMap = new HashMap<>();
        badMap.put(ListUtils.of("a"), "a");
        NodeMapper mapper = new NodeMapper();

        Assertions.assertThrows(NodeSerializationException.class, () -> {
            mapper.serialize(badMap);
        });
    }

    private static final class ThrowingClass {
        public String getThrows() {
            throw new RuntimeException("Nope!");
        }
    }

    @Test
    public void catchesAndShowsErrorsWhenSerializing() {
        ThrowingClass tc = new ThrowingClass();
        NodeMapper mapper = new NodeMapper();

        NodeSerializationException e = Assertions.assertThrows(
                NodeSerializationException.class,
                () -> mapper.serialize(tc));

        assertThat(e.getMessage(),
                equalTo(
                        "Error serializing `throws` field of software.amazon.smithy.model.node.NodeMapperTest$ThrowingClass "
                                + "using getThrows(): Nope!"));
    }

    @Test
    public void omitsEmptyValues() {
        PojoWithEmptyValues pojo = new PojoWithEmptyValues();
        NodeMapper mapper = new NodeMapper();
        mapper.setOmitEmptyValues(true);
        Node result = mapper.serialize(pojo);

        assertThat(result, equalTo(Node.objectNode().withMember("true", true)));
    }

    public static final class PojoWithEmptyValues {
        public List<String> getFoo() {
            return Collections.emptyList();
        }

        public EmptyPojo getBaz() {
            return new EmptyPojo();
        }

        public boolean isFalse() {
            return false;
        }

        public boolean isTrue() {
            return true;
        }
    }

    public static final class EmptyPojo {}

    @Test
    public void canDisableToNodeInsideOfClass() {
        DisabledToNode pojo = new DisabledToNode();
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(DisabledToNode.class);
        Node result = mapper.serialize(pojo);

        Node.assertEquals(result, Node.objectNode().withMember("foo", "hi"));
    }

    public static final class DisabledToNode implements ToNode {
        @Override
        public Node toNode() {
            throw new RuntimeException("Was not meant to run!");
        }

        public String getFoo() {
            return "hi";
        }
    }

    @Test
    public void canDisableFromNodeInsideOfClass() {
        ObjectNode objectNode = ObjectNode.objectNodeBuilder().withMember("foo", "baz").build();
        NodeMapper mapper = new NodeMapper();
        mapper.disableFromNodeForClass(DisabledFromNode.class);
        DisabledFromNode result = mapper.deserialize(objectNode, DisabledFromNode.class);
        assertThat(result.getFoo(), equalTo("baz"));
    }

    public static final class DisabledFromNode {
        private String foo;

        public static DisabledFromNode fromNode() {
            throw new RuntimeException("Was not meant to run!");
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    @Test
    public void deserializesWithFromNodeFactoryAndUnknownPropertiesWithWarning() {
        Node baz = Node.parse("{\"foo\": \"hi\", \"baz\": 10, \"inner\": {\"inner\": {\"noSetter!\": \"inn!\"}}}");
        Baz result = new NodeMapper().deserialize(baz, Baz.class);

        assertThat(result.toString(),
                equalTo(
                        "Baz{foo='hi', baz=10, inner=Baz{foo='null', baz=0, inner=Baz{foo='null', baz=0, inner=null}}}"));
    }

    @Test
    public void deserializesWithFromNodeFactoryAndUnknownPropertiesWithFailure() {
        Node baz = Node.parse("{\"foo\": \"hi\", \"baz\": 10, \"inner\": {\"inner\": {\"noSetter!\": \"inn!\"}}}");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(baz, Baz.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/inner/inner) [1, 59]: unable to find setter method for `noSetter!` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$Baz"));
    }

    private static final class SimpleString {
        private String value;

        public SimpleString(String value) {
            this.value = value;
        }

        public static SimpleString fromNode(Node node) {
            return new SimpleString(node.expectStringNode().getValue());
        }
    }

    @Test
    public void detectsWhenFromNodeFails() {
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(Node.objectNode(), FailingFromNode.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create software.amazon.smithy.model.node.NodeMapperTest$FailingFromNode "
                                + "from {}: Unable to deserialize Node using fromNode method: nope"));
    }

    private static final class FailingFromNode {
        public static FailingFromNode fromNode(Node node) {
            throw new RuntimeException("nope");
        }
    }

    @Test
    public void ignoresFromNodeThatDoNotReturnSameType() {
        Node value = Node.objectNode().withMember("foo", "baz");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, BadTypeFromNode.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to find setter method for `foo` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$BadTypeFromNode"));
    }

    private static final class BadTypeFromNode {
        public static String fromNode(Node node) {
            return "hi";
        }
    }

    @Test
    public void ignoresFromNodeThatIsNotStatic() {
        Node value = Node.objectNode().withMember("foo", "baz");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, NonStaticFromNode.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to find setter method for `foo` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$NonStaticFromNode"));
    }

    private static final class NonStaticFromNode {
        public NonStaticFromNode fromNode(Node node) {
            throw new RuntimeException("nope");
        }
    }

    @Test
    public void ignoresFromNodeThatDoesNotAcceptNode() {
        Node value = Node.objectNode().withMember("foo", "baz");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, NonNodeFromNode.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to find setter method for `foo` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$NonNodeFromNode"));
    }

    private static final class NonNodeFromNode {
        public NonNodeFromNode fromNode(String invalid) {
            throw new RuntimeException("should not actually be called");
        }
    }

    @Test
    public void ignoresFromNodeThatAcceptsMultipleArgs() {
        Node value = Node.objectNode().withMember("foo", "baz");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, MultiArgFromNode.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to find setter method for `foo` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$MultiArgFromNode"));
    }

    private static final class MultiArgFromNode {
        public MultiArgFromNode fromNode(Node node1, Node node2) {
            throw new RuntimeException("nope");
        }
    }

    @Test
    public void deserializesWithFallThroughFromNodeFactory() {
        Node baz = Node.from("hello!");

        assertThat(new NodeMapper().deserialize(baz, SimpleString.class).value, equalTo("hello!"));
    }

    @Test
    public void deserializesWithBuilder() {
        Node traitValue = Node.parse("{\"min\": 10, \"max\": 100}");
        NodeMapper mapper = new NodeMapper();

        RangeTrait trait = mapper.deserialize(traitValue, RangeTrait.class);
        assertThat(trait.getMin().get().intValue(), equalTo(10));
        assertThat(trait.getMax().get().intValue(), equalTo(100));
    }

    @Test
    public void deserializesWithBuilderCatchesErrorCreatingBuilder() {
        Node value = Node.parse("{\"foo\": \"foo\"}");
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, ClassThrowingBuilderMethod.class));

        assertThat(e.getMessage(),
                startsWith(
                        "Deserialization error at (/): unable to create "
                                + "software.amazon.smithy.model.node.NodeMapperTest$ClassThrowingBuilderMethod from "
                                + "{\"foo\":\"foo\"}: Unable to deserialize Node using a builder: nope"));
    }

    private static final class ClassThrowingBuilderMethod {
        public static Builder builder() {
            throw new RuntimeException("nope");
        }

        private static final class Builder implements SmithyBuilder<ClassThrowingBuilderMethod> {
            @Override
            public ClassThrowingBuilderMethod build() {
                return new ClassThrowingBuilderMethod();
            }
        }
    }

    @Test
    public void ignoresNonStaticBuilderMethod() {
        Node value = Node.parse("{\"foo\": \"foo\"}");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, NonStaticBuilderMethod.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/) [1, 9]: unable to find setter method for `foo` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$NonStaticBuilderMethod"));
    }

    private static final class NonStaticBuilderMethod {
        public Builder builder() {
            return new Builder();
        }

        private static final class Builder implements SmithyBuilder<NonStaticBuilderMethod> {
            @Override
            public NonStaticBuilderMethod build() {
                return new NonStaticBuilderMethod();
            }
        }
    }

    @Test
    public void ignoresBuilderThatReturnsDifferentType() {
        Node value = Node.parse("{\"foo\": \"foo\"}");
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(value, BuilderBadReturnType.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/) [1, 9]: unable to find setter method for `foo` on "
                                + "software.amazon.smithy.model.node.NodeMapperTest$BuilderBadReturnType"));
    }

    private static final class BuilderBadReturnType {
        public static String builder() {
            return "hi";
        }
    }

    @Test
    public void deserializesIntoExactTypes() {
        NodeMapper mapper = new NodeMapper();

        assertThat(mapper.deserialize(Node.from("hi"), StringNode.class), equalTo(Node.from("hi")));
        assertThat(mapper.deserialize(Node.from(true), BooleanNode.class), equalTo(Node.from(true)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deserializesIntoObjectTypesForHomogeneousMap() {
        Node value = Node.parse("{\"min\": 10, \"max\": 100}");
        NodeMapper mapper = new NodeMapper();
        Object result = mapper.deserialize(value, Object.class);

        assertThat(result, instanceOf(Map.class));
        assertThat(((Map<String, Object>) result), hasKey("min"));
        assertThat(((Map<String, Object>) result), hasKey("max"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deserializesIntoObjectTypesForHeterogeneousMap() {
        Node value = Node.parse("{\"min\": 10, \"max\": \"100\", \"foo\": true}");
        NodeMapper mapper = new NodeMapper();
        Object result = mapper.deserialize(value, Object.class);

        assertThat(result, instanceOf(Map.class));
        assertThat(((Map<String, Object>) result).get("min"), equalTo(10L));
        assertThat(((Map<String, Object>) result).get("max"), equalTo("100"));
        assertThat(((Map<String, Object>) result).get("foo"), equalTo(true));
    }

    @Test
    public void deserializesComplexMaps() {
        Node value = Node.parse("{\"map\": {\"a\": [ {\"b\": [\"c\"] } ] } }");
        NodeMapper mapper = new NodeMapper();
        ComplexMap result = mapper.deserialize(value, ComplexMap.class);

        assertThat(mapper.serialize(result), equalTo(value));
    }

    public static final class ComplexMap {
        private Map<String, List<Map<String, List>>> map = Collections.emptyMap();

        public Map<String, List<Map<String, List>>> getMap() {
            return map;
        }

        public void setMap(Map<String, List<Map<String, List>>> map) {
            this.map = map;
        }
    }

    @Test
    public void deserializesIntoBooleans() {
        NodeMapper mapper = new NodeMapper();

        assertThat(mapper.deserialize(Node.from(true), Boolean.class), equalTo(true));
        assertThat(mapper.deserialize(Node.from(true), boolean.class), equalTo(true));
        assertThat(mapper.deserialize(Node.from(true), Object.class), equalTo(true));
    }

    @Test
    public void deserializesIntoNulls() {
        NodeMapper mapper = new NodeMapper();

        assertThat(mapper.deserialize(Node.nullNode(), String.class), nullValue());
    }

    @Test
    public void deserializesStrings() {
        Node value = Node.from("hi");
        NodeMapper mapper = new NodeMapper();

        assertThat(mapper.deserialize(value, Object.class), equalTo("hi"));
        assertThat(mapper.deserialize(value, String.class), equalTo("hi"));
    }

    @Test
    public void deserializesShapeIds() {
        Node value = Node.from("com.foo#Bar");
        NodeMapper mapper = new NodeMapper();

        assertThat(mapper.deserialize(value, ShapeId.class), equalTo(ShapeId.from("com.foo#Bar")));
    }

    @Test
    public void deserializesListWithSpecificMember() {
        Node value = Node.parse("[true, false, true]");

        assertThat(new NodeMapper().deserializeCollection(value, List.class, Boolean.class),
                contains(true, false, true));
    }

    @Test
    public void failsToConvertObjectNodeToList() {
        Node value = Node.parse("{\"min\": 10, \"max\": 100}");

        Assertions.assertThrows(NodeDeserializationException.class, () -> {
            new NodeMapper().deserialize(value, List.class);
        });
    }

    @Test
    public void deserializesSets() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        Set<String> result = mapper.deserializeCollection(value, Set.class, String.class);
        assertThat(result, instanceOf(LinkedHashSet.class));
        assertThat(result, contains("a", "b"));
    }

    @Test
    public void deserializesHashSets() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        Set<String> result = mapper.deserializeCollection(value, HashSet.class, String.class);
        assertThat(result, instanceOf(LinkedHashSet.class));
        assertThat(result, contains("a", "b"));
    }

    @Test
    public void deserializesLinkedHashSets() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        Set<String> result = mapper.deserializeCollection(value, LinkedHashSet.class, String.class);
        assertThat(result, instanceOf(LinkedHashSet.class));
        assertThat(result, contains("a", "b"));
    }

    @Test
    public void deserializesList() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        List<String> result = mapper.deserializeCollection(value, List.class, String.class);
        assertThat(result, instanceOf(ArrayList.class));
        assertThat(result, contains("a", "b"));
    }

    @Test
    public void deserializesArrayList() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        List<String> result = mapper.deserializeCollection(value, ArrayList.class, String.class);
        assertThat(result, instanceOf(ArrayList.class));
        assertThat(result, contains("a", "b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deserializesArrayObject() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        Object result = mapper.deserialize(value, Object.class);
        assertThat((ArrayList<Object>) result, contains("a", "b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deserializesIterable() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        Object result = mapper.deserialize(value, Iterable.class);
        assertThat(result, instanceOf(ArrayList.class));
        assertThat((ArrayList<Object>) result, contains("a", "b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deserializesCollection() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();

        Object result = mapper.deserialize(value, Collection.class);
        assertThat(result, instanceOf(ArrayList.class));
        assertThat((ArrayList<Object>) result, contains("a", "b"));
    }

    @Test
    public void cannotDeserializeNonCollectionArrays() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();
        Throwable e = Assertions.assertThrows(NodeDeserializationException.class, () -> {
            mapper.deserialize(value, PojoWithCollection.class);
        });

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create "
                                + "software.amazon.smithy.model.node.NodeMapperTest$PojoWithCollection from [\"a\",\"b\"]"));
    }

    @Test
    public void detectsWhenUnableToAddToCollection() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();
        Throwable e = Assertions.assertThrows(NodeDeserializationException.class, () -> {
            mapper.deserialize(value, ThrowingCollectionOnAdd.class);
        });

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create "
                                + "software.amazon.smithy.model.node.NodeMapperTest$ThrowingCollectionOnAdd from [\"a\",\"b\"]: "
                                + "Cannot add a"));
    }

    public static final class ThrowingCollectionOnAdd extends ArrayList<String> {
        @Override
        public boolean add(String value) {
            throw new RuntimeException("Cannot add " + value);
        }
    }

    @Test
    public void detectsWhenUnableToCreateCollection() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();
        Throwable e = Assertions.assertThrows(NodeDeserializationException.class, () -> {
            mapper.deserialize(value, ThrowingCollectionOnCreate.class);
        });

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create "
                                + "software.amazon.smithy.model.node.NodeMapperTest$ThrowingCollectionOnCreate from [\"a\",\"b\"]: "
                                + "Unable to deserialize array into Collection: nope"));
    }

    public static final class ThrowingCollectionOnCreate extends ArrayList<String> {
        public ThrowingCollectionOnCreate() {
            throw new RuntimeException("nope");
        }
    }

    @Test
    public void detectsWhenUnableToFindCollectionConstructorAndContinues() {
        Node value = Node.fromStrings("a", "b");
        NodeMapper mapper = new NodeMapper();
        Throwable e = Assertions.assertThrows(NodeDeserializationException.class, () -> {
            mapper.deserialize(value, NonZeroArgConstructorCollection.class);
        });

        assertThat(e.getMessage(),
                equalTo(
                        "Unable to find a zero-arg constructor for Collection "
                                + "software.amazon.smithy.model.node.NodeMapperTest$NonZeroArgConstructorCollection"));
    }

    public static final class NonZeroArgConstructorCollection extends ArrayList<String> {
        public NonZeroArgConstructorCollection(String notGonnaWork) {}
    }

    // We previously reported this kind of deserialization as working, but it actually was storing objects of the
    // wrong type in collections, causing ClassCastExceptions at runtime.
    @Test
    public void deserializesIntoTypedList() {
        Node value = Node.parse("[{\"a\": true}, {\"a\": false}]");
        NodeMapper mapper = new NodeMapper();

        FooList result = mapper.deserialize(value, FooList.class);

        assertThat(result, hasSize(2));
        assertThat(result.get(0), instanceOf(FooList.Foo.class));
        assertThat(result.get(0).a(), is(true));
        assertThat(result.get(1), instanceOf(FooList.Foo.class));
        assertThat(result.get(1).a(), is(false));
    }

    public static final class FooList extends ArrayList<FooList.Foo> {
        public static final class Foo {
            boolean a;

            public boolean a() {
                return a;
            }

            public void a(boolean a) {
                this.a = a;
            }
        }
    }

    // Throw some nested generics at the NodeMapper to ensure it works with them.
    @Test
    public void deserializesIntoTypedListWithNestedGenerics() {
        Node value = Node.parse("[ [[{\"a\": true}]], [[{\"a\": false}]] ]");
        NodeMapper mapper = new NodeMapper();
        NestedFooList result = mapper.deserialize(value, NestedFooList.class);

        assertThat(result, hasSize(2));
        assertThat(result.get(0), instanceOf(ArrayList.class));
        assertThat(result.get(0), hasSize(1));
        assertThat(result.get(0).get(0), hasSize(1));
        assertThat(result.get(0).get(0), instanceOf(FooList.class));
        assertThat(result.get(0).get(0).get(0), instanceOf(FooList.Foo.class));

        assertThat(result.get(1), instanceOf(ArrayList.class));
        assertThat(result.get(1), hasSize(1));
        assertThat(result.get(1).get(0), hasSize(1));
        assertThat(result.get(1).get(0), instanceOf(FooList.class));
        assertThat(result.get(1).get(0).get(0), instanceOf(FooList.Foo.class));
    }

    public static final class NestedFooList extends ArrayList<ArrayList<FooList>> {}

    @Test
    public void deserializedIntoMap() {
        Node heterogeneous = Node.parse("{\"foo\":\"foo\",\"baz\":10}");
        Node homogeneous = Node.parse("{\"foo\":\"foo\",\"baz\":\"baz\"}");
        NodeMapper mapper = new NodeMapper();

        Map<String, Object> heterogeneousMap = mapper.deserializeMap(heterogeneous, Map.class, Object.class);
        Map<String, String> homogeneousMap = mapper.deserializeMap(homogeneous, TreeMap.class, String.class);

        assertThat(heterogeneousMap, instanceOf(HashMap.class));
        assertThat(homogeneousMap, instanceOf(TreeMap.class));
        assertThat(homogeneousMap, hasKey("foo"));
        assertThat(heterogeneousMap, hasKey("foo"));
        assertThat(homogeneousMap, hasKey("baz"));
        assertThat(heterogeneousMap, hasKey("baz"));
        assertThat(homogeneousMap.get("foo"), equalTo("foo"));
        assertThat(heterogeneousMap.get("foo"), equalTo("foo"));
        assertThat(homogeneousMap.get("baz"), equalTo("baz"));
        assertThat(heterogeneousMap.get("baz"), equalTo(10L));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deserializedFromObjectIntoObjectMap() {
        Node mappings = Node.parse("{\"foo\":\"foo\"}");
        NodeMapper mapper = new NodeMapper();
        Object result = mapper.deserialize(mappings, Object.class);

        assertThat(result, instanceOf(HashMap.class));
        assertThat(((Map<String, Object>) result).get("foo"), equalTo("foo"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deserializedFromObjectIntoMap() {
        Node mappings = Node.parse("{\"foo\":\"foo\"}");
        NodeMapper mapper = new NodeMapper();
        Map<String, Object> result = mapper.deserialize(mappings, Map.class);

        assertThat(result, instanceOf(HashMap.class));
        assertThat(result.get("foo"), equalTo("foo"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deserializedFromObjectIntoHashMap() {
        Node mappings = Node.parse("{\"foo\":\"foo\"}");
        NodeMapper mapper = new NodeMapper();
        Map<String, Object> result = mapper.deserialize(mappings, HashMap.class);

        assertThat(result, instanceOf(HashMap.class));
        assertThat(result.get("foo"), equalTo("foo"));
    }

    @Test
    public void throwsWhenMapDoesNotHaveZeroArgConstructor() {
        Node object = Node.objectNode();
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(object, NoZeroArgCtorMap.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Unable to find a zero-arg constructor for Map "
                                + "software.amazon.smithy.model.node.NodeMapperTest$NoZeroArgCtorMap"));
    }

    private static final class NoZeroArgCtorMap extends HashMap<String, String> {
        public NoZeroArgCtorMap(String invalid) {}
    }

    @Test
    public void throwsWhenMapCtorThrows() {
        Node object = Node.objectNode();
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(object, BadMapCtor.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create software.amazon.smithy.model.node.NodeMapperTest$BadMapCtor "
                                + "from {}: Unable to deserialize object into Map: nope"));
    }

    private static final class BadMapCtor extends HashMap<String, String> {
        public BadMapCtor() {
            throw new RuntimeException("nope");
        }
    }

    @Test
    public void dealsWithInvalidMapTypes() {
        Node object = Node.objectNode().withMember("foo", 10);
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserializeMap(object, Map.class, String.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/foo): unable to create java.lang.String from 10"));
    }

    @Test
    public void deserializesCollectionInPojo() {
        Node value = Node.parse("{\"foo\": [{\"a\": true, \"b\": 10, \"map\": {\"key\": {\"foo\": []} } }]}");

        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);
        PojoWithCollection pj = mapper.deserialize(value, PojoWithCollection.class);

        // Round-trip the value.
        assertThat(mapper.serialize(pj), equalTo(value));
    }

    public static final class PojoWithCollection {
        private List<PojoWithCollection.Foo> foo;

        public void setFoo(List<PojoWithCollection.Foo> foo) {
            this.foo = foo;
        }

        public List<PojoWithCollection.Foo> getFoo() {
            return foo;
        }

        public static final class Foo {
            private boolean a;
            private int b;
            private Map<String, PojoWithCollection> map;

            public void setA(boolean a) {
                this.a = a;
            }

            public boolean getA() {
                return a;
            }

            public void setB(int b) {
                this.b = b;
            }

            public int getB() {
                return b;
            }

            public void setMap(Map<String, PojoWithCollection> map) {
                this.map = map;
            }

            public Map<String, PojoWithCollection> getMap() {
                return map;
            }
        }
    }

    @Test
    public void deserializeNestedValues() {
        Node list = Node.fromStrings("a", "b", "c");
        Node string = Node.from("a");
        Node bool = Node.from(true);
        Node number = Node.from(10);
        Node baz = Node.parse("{\"foo\":\"hi\",\"baz\":10,\"inner\":{\"foo\":\"inn!\"}}");
        Node expectedBaz = Node.parse("{\"foo\":\"hi\",\"baz\":10,\"inner\":{\"foo\":\"inn!\",\"baz\":0}}");
        NodeMapper mapper = new NodeMapper();

        assertThat(mapper.deserializeCollection(list, List.class, String.class), contains("a", "b", "c"));
        assertThat(mapper.deserialize(string, String.class), equalTo("a"));
        assertThat(mapper.deserialize(bool, Boolean.class), equalTo(true));
        assertThat(mapper.deserialize(number, Integer.class), equalTo(10));
        Node.assertEquals(mapper.serialize(mapper.deserialize(baz, Baz.class)), expectedBaz);
    }

    public static final class Baz {
        private String foo;
        private int baz;
        private Baz inner;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public int getBaz() {
            return baz;
        }

        public void setBaz(int baz) {
            this.baz = baz;
        }

        public Baz getInner() {
            return inner;
        }

        public void setInner(Baz inner) {
            this.inner = inner;
        }

        @Override
        public String toString() {
            return "Baz{" +
                    "foo='" + foo + '\'' +
                    ", baz=" + baz +
                    ", inner=" + inner +
                    '}';
        }
    }

    @Test
    public void detectsWhenBeanCtorFails() {
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(Node.objectNode(), FailingCtor.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create software.amazon.smithy.model.node.NodeMapperTest$FailingCtor "
                                + "from {}: Unable to deserialize a Node when invoking target constructor: nope"));
    }

    public static final class FailingCtor {
        public FailingCtor() {
            throw new RuntimeException("nope");
        }
    }

    @Test
    public void deserializesIntoValue() {
        NodeMapper mapper = new NodeMapper();
        Node node = Node.parse("{\"foo\":\"hi\",\"baz\":10,\"inner\":{\"foo\":\"inn!\", \"baz\": 0}}");
        Baz baz = new Baz();
        mapper.deserializeInto(node, baz);

        Node.assertEquals(mapper.serialize(baz), node);
    }

    @Test
    public void deserializesIntoValueAndCatchesErrors() {
        NodeMapper mapper = new NodeMapper();
        Node node = Node.parse("{\"foo\":true}");
        Baz baz = new Baz();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserializeInto(node, baz));

        assertThat(e.getMessage(),
                startsWith(
                        "Deserialization error at (/foo): unable to create java.lang.String from true"));
    }

    @Test
    public void deserializesIntoValueWithSourceLocation() {
        NodeMapper mapper = new NodeMapper();
        Map<StringNode, Node> mappings = new HashMap<>();
        mappings.put(StringNode.from("foo"), StringNode.from("foo"));
        SourceLocation sourceLocation = new SourceLocation("/abc/def");
        Node node = new ObjectNode(mappings, sourceLocation);
        HasSourceLocation hasSourceLocation = mapper.deserialize(node, HasSourceLocation.class);

        assertThat(sourceLocation, equalTo(hasSourceLocation.sourceLocation));
    }

    @Test
    public void deserializesIntoTraitWithSourceLocation() {
        NodeMapper mapper = new NodeMapper();
        Map<StringNode, Node> mappings = new HashMap<>();
        mappings.put(StringNode.from("foo"), StringNode.from("foo"));
        SourceLocation sourceLocation = new SourceLocation("/abc/def");
        Node node = new ObjectNode(mappings, sourceLocation);
        SourceLocationBearerTrait trait = mapper.deserialize(node, SourceLocationBearerTrait.class);

        assertThat(trait.getSourceLocation(), equalTo(sourceLocation));
        assertThat(trait.getFoo(), equalTo("foo"));
    }

    @Test
    public void serializesSourceLocationFromValue() {
        NodeMapper mapper = new NodeMapper();
        HasSourceLocation hs = new HasSourceLocation();
        SourceLocation sourceLocation = new SourceLocation("/foo/baz");
        hs.setSourceLocation(sourceLocation);
        hs.setFoo("hi");
        Node result = mapper.serialize(hs);

        assertThat(result.expectObjectNode().getStringMap(), hasKey("foo"));
        // The sourceLocation needs to be set on the node, not as a key.
        assertThat(result.expectObjectNode().getStringMap(), not(hasKey("sourceLocation")));
        assertThat(result.getSourceLocation(), equalTo(sourceLocation));
    }

    @Test
    public void serializesSourceLocationFromTrait() {
        SourceLocation sourceLocation = new SourceLocation("/foo/baz");
        SourceLocationBearerTrait trait = SourceLocationBearerTrait.builder()
                .sourceLocation(sourceLocation)
                .foo("foo")
                .build();
        Node result = trait.createNode();

        assertThat(result.expectObjectNode().getStringMap(), hasKey("foo"));
        // The sourceLocation needs to be set on the node, not as a key.
        assertThat(result.expectObjectNode().getStringMap(), not(hasKey("sourceLocation")));
        assertThat(result.getSourceLocation(), equalTo(sourceLocation));
    }

    private static class HasSourceLocation implements FromSourceLocation {
        private SourceLocation sourceLocation;
        private String foo;

        public void setSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getFoo() {
            return foo;
        }
    }

    private static class SourceLocationBearerTrait extends AbstractTrait
            implements ToSmithyBuilder<SourceLocationBearerTrait> {
        public static final ShapeId ID = ShapeId.from("smithy.test#sourceLocationBearer");
        private final String foo;

        public SourceLocationBearerTrait(Builder builder) {
            super(ID, builder.getSourceLocation());
            this.foo = builder.foo;
        }

        public String getFoo() {
            return foo;
        }

        @Override
        protected Node createNode() {
            NodeMapper mapper = new NodeMapper();
            mapper.disableToNodeForClass(SourceLocationBearerTrait.class);
            return mapper.serialize(this);
        }

        @Override
        public Builder toBuilder() {
            return new Builder().sourceLocation(getSourceLocation());
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder extends AbstractTraitBuilder<SourceLocationBearerTrait, Builder> {
            private String foo;

            @Override
            public SourceLocationBearerTrait build() {
                return new SourceLocationBearerTrait(this);
            }

            public Builder foo(String foo) {
                this.foo = foo;
                return this;
            }
        }
    }

    @Test
    public void deserializesEnums() {
        NodeMapper mapper = new NodeMapper();

        assertThat(FooEnum.FOO, equalTo(mapper.deserialize(Node.from("foo"), FooEnum.class)));
        assertThat(FooEnum.BAZ, equalTo(mapper.deserialize(Node.from("Baz"), FooEnum.class)));
        assertThat(FooEnum.BAR, equalTo(mapper.deserialize(Node.from("BAR"), FooEnum.class)));
    }

    @Test
    public void throwsWhenUnableToDeserializeEnums() {
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(Node.from("invalid"), FooEnum.class));

        assertThat(e.getMessage(),
                equalTo(
                        "Deserialization error at (/): unable to create software.amazon.smithy.model.node.NodeMapperTest$FooEnum "
                                + "from \"invalid\": Expected one of the following enum strings: [foo, Baz, BAR]"));
    }

    private enum FooEnum {
        FOO {
            @Override
            public String toString() {
                return "foo";
            }
        },
        BAZ {
            @Override
            public String toString() {
                return "Baz";
            }
        },
        BAR
    }

    @Test
    public void patternSerde() {
        NodeMapper mapper = new NodeMapper();
        Pattern pattern = mapper.deserialize(Node.from("[A-Z]+"), Pattern.class);
        Node result = mapper.serialize(pattern);

        assertThat(pattern.toString(), equalTo("[A-Z]+"));
        assertThat(result, equalTo(Node.from("[A-Z]+")));
    }

    @Test
    public void failsWhenUnableToParsePattern() {
        NodeMapper mapper = new NodeMapper();
        Node input = Node.from("[");

        Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(input, Pattern.class));
        // Shy away from a specific message assertion in case the message ever changes between JDK versions.
    }

    @Test
    public void pathSerde() {
        NodeMapper mapper = new NodeMapper();
        Path path = mapper.deserialize(Node.from("/foo/baz"), Path.class);
        String expected = path.toUri().toString(); // e.g., file:///foo/baz

        assertThat(path.toAbsolutePath().toUri().toString(), equalTo(expected));
        assertThat(mapper.serialize(path), equalTo(Node.from(expected)));
    }

    @Test
    public void uriSerde() {
        NodeMapper mapper = new NodeMapper();
        URI uri = mapper.deserialize(Node.from("https://example.com/"), URI.class);

        assertThat(uri.toString(), equalTo("https://example.com/"));
        assertThat(mapper.serialize(uri), equalTo(Node.from("https://example.com/")));
    }

    @Test
    public void urlSerde() {
        NodeMapper mapper = new NodeMapper();
        URL url = mapper.deserialize(Node.from("https://example.com/"), URL.class);

        assertThat(url.toString(), equalTo("https://example.com/"));
        assertThat(mapper.serialize(url), equalTo(Node.from("https://example.com/")));
    }

    @Test
    public void fileSerde() {
        // Wonky because of Windows.
        String bogusPath = new File("/does/not/exist/blah/blah").getAbsolutePath().toString();
        NodeMapper mapper = new NodeMapper();
        File file = mapper.deserialize(Node.from(bogusPath), File.class);

        assertThat(file.toString(), equalTo(bogusPath));
        assertThat(mapper.serialize(file), equalTo(Node.from(bogusPath)));
    }

    @Test
    public void deserializesWildcards() {
        NodeMapper mapper = new NodeMapper();
        Node input = Node.objectNode().withMember("values", Node.fromStrings("a", "b"));
        HasWildcard result = mapper.deserialize(input, HasWildcard.class);

        assertThat(result.getValues(), contains("a", "b"));
    }

    public static final class HasWildcard {
        private List<?> values;

        public List<?> getValues() {
            return values;
        }

        public void setValues(List<?> values) {
            this.values = values;
        }
    }

    @Test
    public void doesNotSupportNonStaticInnerClasses() {
        NodeMapper mapper = new NodeMapper();

        NodeDeserializationException e = Assertions.assertThrows(
                NodeDeserializationException.class,
                () -> mapper.deserialize(Node.objectNode(), NonStatic.class));

        assertThat(e.getMessage(), containsString("Cannot create non-static inner class"));
    }

    public final class NonStatic {}

    @Test
    public void deserializesWithCamelCase() {
        NodeMapper mapper = new NodeMapper();
        Node input = Node.objectNode()
                .withMember("set.me.here", "hi1")
                .withMember("set.Me.Here.too", "hi2")
                .withMember("ignore..me", "hi3");
        CamelCase result = mapper.deserialize(input, CamelCase.class);

        assertThat(result.getSetMeHere(), equalTo("hi1"));
        assertThat(result.getSetMeHereToo(), equalTo("hi2"));
        assertThat(result.getIgnoreMe(), nullValue());
    }

    public static final class CamelCase {
        private String setMeHere;
        private String setMeHereToo;
        private String ignoreMe;

        public String getSetMeHere() {
            return setMeHere;
        }

        public void setSetMeHere(String setMeHere) {
            this.setMeHere = setMeHere;
        }

        public String getSetMeHereToo() {
            return setMeHereToo;
        }

        public void setSetMeHereToo(String setMeHereToo) {
            this.setMeHereToo = setMeHereToo;
        }

        public String getIgnoreMe() {
            return ignoreMe;
        }

        public void setIgnoreMe(String ignoreMe) {
            this.ignoreMe = ignoreMe;
        }
    }

    @Test
    public void deserializesMapOfShapeIdToShapeType() {
        NodeMapper mapper = new NodeMapper();
        Node input = Node.objectNode()
                .withMember("shapeTypes",
                        Node.objectNode()
                                .withMember("smithy.example#A", "union")
                                .withMember("smithy.example#B", "string"));
        ShapeIdMap result = mapper.deserialize(input, ShapeIdMap.class);

        assertThat(result.getShapeTypes().keySet().iterator().next(), instanceOf(ShapeId.class));
        assertThat(result.getShapeTypes().values().iterator().next(), instanceOf(ShapeType.class));

        assertThat(result.getShapeTypes(), hasKey(ShapeId.from("smithy.example#A")));
        assertThat(result.getShapeTypes(), hasKey(ShapeId.from("smithy.example#B")));
        assertThat(result.getShapeTypes().get(ShapeId.from("smithy.example#A")), equalTo(ShapeType.UNION));
        assertThat(result.getShapeTypes().get(ShapeId.from("smithy.example#B")), equalTo(ShapeType.STRING));
    }

    public static final class ShapeIdMap {
        private Map<ShapeId, ShapeType> shapeTypes;

        public void setShapeTypes(Map<ShapeId, ShapeType> shapeTypes) {
            this.shapeTypes = shapeTypes;
        }

        public Map<ShapeId, ShapeType> getShapeTypes() {
            return shapeTypes;
        }
    }

    @Test
    public void deserializesNestedGenericTypes() {
        NodeMapper mapper = new NodeMapper();
        Node input = Node.objectNode()
                .withMember("shapeTypes",
                        Node.objectNode()
                                .withMember("smithy.example#A",
                                        Node.arrayNode(Node.objectNode()
                                                .withMember("smithy.example#B", "string"))));
        ComplicatedShapeIdMap result = mapper.deserialize(input, ComplicatedShapeIdMap.class);

        assertThat(result.getShapeTypes(), hasKey(ShapeId.from("smithy.example#A")));
        assertThat(result.getShapeTypes(),
                hasValue(ListUtils.of(
                        MapUtils.of(ShapeId.from("smithy.example#B"), ShapeType.STRING))));
    }

    public static final class ComplicatedShapeIdMap {
        private Map<ShapeId, List<Map<ShapeId, ShapeType>>> shapeTypes;

        public void setShapeTypes(Map<ShapeId, List<Map<ShapeId, ShapeType>>> shapeTypes) {
            this.shapeTypes = shapeTypes;
        }

        public Map<ShapeId, List<Map<ShapeId, ShapeType>>> getShapeTypes() {
            return shapeTypes;
        }
    }
}
