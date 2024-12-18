/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SimpleParser;

public class AttributeValueTest {
    @Test
    public void checksProjectionSubsetEquality() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.SUBSET;

        assertThat(comparator.compare(p1, p2, false), equalTo(true));
        assertThat(comparator.compare(p3, p3, false), equalTo(true));
        assertThat(comparator.compare(p3, p2, false), equalTo(false));
        assertThat(comparator.compare(p4, p2, false), equalTo(false));
        assertThat(comparator.compare(p1, AttributeValue.literal("hi"), false), equalTo(false));
    }

    @Test
    public void checksProjectionProperSubsetEquality() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.PROPER_SUBSET;

        assertThat(comparator.compare(p1, p2, false), equalTo(true));
        assertThat(comparator.compare(p3, p3, false), equalTo(false));
        assertThat(comparator.compare(p3, p2, false), equalTo(false));
        assertThat(comparator.compare(p4, p2, false), equalTo(false));
        assertThat(comparator.compare(p1, AttributeValue.literal("hi"), false), equalTo(false));
    }

    @Test
    public void checksProjectionEquality() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.PROJECTION_EQUALS;

        assertThat(comparator.compare(p1, p2, false), equalTo(false));
        assertThat(comparator.compare(p3, p3, false), equalTo(true));
        assertThat(comparator.compare(p3, p2, false), equalTo(false));
        assertThat(comparator.compare(p4, p2, false), equalTo(false));
        assertThat(comparator.compare(p1, AttributeValue.literal("hi"), false), equalTo(false));
    }

    @Test
    public void checksProjectionInequality() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.PROJECTION_NOT_EQUALS;

        assertThat(comparator.compare(p1, p2, false), equalTo(true));
        assertThat(comparator.compare(p3, p3, false), equalTo(false));
        assertThat(comparator.compare(p3, p2, false), equalTo(true));
        assertThat(comparator.compare(p4, p2, false), equalTo(true));
        assertThat(comparator.compare(p1, AttributeValue.literal("hi"), false), equalTo(true));
    }

    @Test
    public void checksProjectionEqualityWithEquals() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.EQUALS;

        assertThat(comparator.compare(p1, p2, false), equalTo(true));
        assertThat(comparator.compare(p3, p3, false), equalTo(true));
        assertThat(comparator.compare(p3, p2, false), equalTo(true));
        assertThat(comparator.compare(p4, p2, false), equalTo(false));
    }

    @Test
    public void checksProjectionNotEquality() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.NOT_EQUALS;

        // != is a bit useless for projections. It just states if any
        // value is not equal to any other value. It doesn't tell you
        // if one is a subset, one is a superset, etc.
        assertThat(comparator.compare(p1, p2, false), equalTo(true));
        assertThat(comparator.compare(p3, p3, false), equalTo(true));
        assertThat(comparator.compare(p3, p2, false), equalTo(true));
        assertThat(comparator.compare(p4, p2, false), equalTo(true));

        // This tells us that all the elements of both projections are
        // the same, so it returns false.
        assertThat(comparator.compare(p4, p4, false), equalTo(false));
    }

    @Test
    public void checksProjectionContains() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");

        AttributeValue p1 = AttributeValue.projection(ListUtils.of(b));
        AttributeValue p2 = AttributeValue.projection(ListUtils.of(b, c));
        AttributeValue p3 = AttributeValue.projection(ListUtils.of(a, b, c));
        AttributeValue p4 = AttributeValue.projection(ListUtils.of(a));
        AttributeComparator comparator = AttributeComparator.CONTAINS;

        assertThat(comparator.compare(p1, p2, false), equalTo(true));
        assertThat(comparator.compare(p3, p3, false), equalTo(true));
        assertThat(comparator.compare(p3, p2, false), equalTo(true));
        assertThat(comparator.compare(p4, p2, false), equalTo(false));
    }

    @Test
    public void projectionsExistIfTheyHaveValues() {
        AttributeComparator comparator = AttributeComparator.EXISTS;
        AttributeValue trueValue = AttributeValue.literal("true");
        AttributeValue a = AttributeValue.literal("a");

        assertThat(comparator.compare(AttributeValue.projection(ListUtils.of(a)), trueValue, false), equalTo(true));
        assertThat(comparator.compare(AttributeValue.projection(ListUtils.of()), trueValue, false), equalTo(false));
    }

    @Test
    public void projectionsReturnsFirstValue() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue projection = AttributeValue.projection(ListUtils.of(a));

        assertThat(projection.getProperty("(first)").toString(), equalTo(a.toString()));
    }

    @Test
    public void projectionsFirstValueReturnsEmptyWhenEmpty() {
        AttributeValue projection = AttributeValue.projection(ListUtils.of());

        assertThat(projection.getProperty("(first)"), is(AttributeValue.emptyValue()));
    }

    @Test
    public void projectionReturnsResultOfEachContainedValue() {
        AttributeValue a = AttributeValue.node(Node.objectNode().withMember("foo", 1));
        AttributeValue b = AttributeValue.node(Node.objectNode().withMember("foo", 2));
        AttributeValue c = AttributeValue.node(Node.objectNode().withMember("foo", 3));
        AttributeValue projection = AttributeValue.projection(ListUtils.of(a, b, c));
        Collection<? extends AttributeValue> values = projection.getProperty("foo").getFlattenedValues();
        List<String> strings = values.stream().map(AttributeValue::toString).collect(Collectors.toList());

        assertThat(strings, contains("1", "2", "3"));
    }

    @Test
    public void createsServiceValue() {
        ServiceShape serviceShape = ServiceShape.builder()
                .id("foo.baz#Service")
                .version("XYZ")
                .build();
        AttributeValue service = AttributeValue.service(serviceShape);

        assertThat(service.toString(), equalTo(serviceShape.getId().toString()));
        assertThat(service.getProperty("id").toString(), equalTo(serviceShape.getId().toString()));
        assertThat(service.getProperty("version").toString(), equalTo(serviceShape.getVersion()));
    }

    @Test
    public void createsIdValueNoMember() {
        ShapeId shapeId = ShapeId.from("smithy.example#Foo");
        AttributeValue id = AttributeValue.id(shapeId);

        assertThat(id.toString(), equalTo("smithy.example#Foo"));
        assertThat(id.getProperty("name").toString(), equalTo("Foo"));
        assertThat(id.getProperty("namespace").toString(), equalTo("smithy.example"));
        assertThat(id.getProperty("member").toString(), equalTo(""));
        assertThat(id.getProperty("(length)").toString(), equalTo(String.valueOf(shapeId.toString().length())));
    }

    @Test
    public void createsIdValueWithMember() {
        ShapeId shapeId = ShapeId.from("smithy.example#Foo$bar");
        AttributeValue id = AttributeValue.id(shapeId);

        assertThat(id.toString(), equalTo("smithy.example#Foo$bar"));
        assertThat(id.getProperty("name").toString(), equalTo("Foo"));
        assertThat(id.getProperty("namespace").toString(), equalTo("smithy.example"));
        assertThat(id.getProperty("member").toString(), equalTo("bar"));
        assertThat(id.getProperty("(length)").toString(), equalTo(String.valueOf(shapeId.toString().length())));
    }

    @Test
    public void createsShapeValue() {
        ServiceShape serviceShape = ServiceShape.builder()
                .id("foo.baz#Service")
                .version("XYZ")
                .addTrait(new DocumentationTrait("hi"))
                .build();
        AttributeValue service = AttributeValue.shape(serviceShape, MapUtils.of());

        assertThat(service.toString(), equalTo(serviceShape.getId().toString()));
        assertThat(service.getPath(
                ListUtils.of("id", "name")).toString(), equalTo(serviceShape.getId().getName()));
        assertThat(service.getPath(
                ListUtils.of("trait", "documentation")).toString(), equalTo("hi"));
        assertThat(service.getPath(
                ListUtils.of("service", "version")).toString(), equalTo("XYZ"));
    }

    @Test
    public void createsTraitValue() {
        ServiceShape serviceShape = ServiceShape.builder()
                .id("foo.baz#Service")
                .version("XYZ")
                .addTrait(new DocumentationTrait("hi"))
                .addTrait(TagsTrait.builder().addValue("hi").build())
                .build();
        AttributeValue service = AttributeValue.shape(serviceShape, MapUtils.of()).getProperty("trait");

        assertThat(service.toString(), equalTo(""));
        assertThat(service.toMessageString(), equalTo(""));
        assertThat(service.getPath(ListUtils.of("tags")).toMessageString(), equalTo("[\"hi\"]"));
        assertThat(service.getPath(ListUtils.of("smithy.api#tags")).toMessageString(), equalTo("[\"hi\"]"));
        assertThat(service.getPath(ListUtils.of("documentation")).toString(), equalTo("hi"));
        assertThat(service.getPath(ListUtils.of("smithy.api#documentation")).toString(), equalTo("hi"));
    }

    @Test
    public void projectionToStringAndDebugCsvString() {
        AttributeValue a = AttributeValue.literal("a");
        AttributeValue b = AttributeValue.literal("b");
        AttributeValue c = AttributeValue.literal("c");
        AttributeValue p = AttributeValue.projection(ListUtils.of(b, a, c));

        assertThat(p.toString(), equalTo(""));
        assertThat(p.toMessageString(), equalTo("[a, b, c]"));
    }

    @Test
    public void emptyValueIsEmpty() {
        AttributeValue a = AttributeValue.emptyValue();

        assertThat(a.toString(), equalTo(""));
        assertThat(a.toMessageString(), equalTo(""));
        assertThat(a.isPresent(), equalTo(false));
        assertThat(a.getProperty("hi"), equalTo(a));
        assertThat(a.getPath(ListUtils.of("hi")), equalTo(a));
    }

    @Test
    public void shapeProvidesVarAccess() {
        StringShape shape = StringShape.builder().id("foo.baz#Foo").build();
        StringShape shape2 = StringShape.builder().id("foo.baz#Foo2").build();
        StringShape shape3 = StringShape.builder().id("foo.baz#Foo3").build();
        AttributeValue attr = AttributeValue.shape(shape, MapUtils.of("a", SetUtils.of(shape2, shape3)));

        assertThat(attr.getProperty("var").toString(), equalTo(""));
        assertThat(attr.getProperty("var").toMessageString(), equalTo(""));
        assertThat(attr.getPath(ListUtils.of("var", "a")).getProperty("id").toMessageString(),
                equalTo("[foo.baz#Foo2, foo.baz#Foo3]"));
    }

    @Test
    public void canParseScopedSelectorFromParser() {
        SimpleParser parser = new SimpleParser("@{var|bar|baz}");

        assertThat(AttributeValue.parseScopedAttribute(parser), contains("var", "bar", "baz"));
    }
}
