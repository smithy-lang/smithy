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

package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;

public class SelectorTest {

    private static final String OPERATIONS_MISSING_BINDINGS = "service\n"
                                                              + "$operations(~> operation)\n"
                                                              + ":test(${operations}[trait|http])\n"
                                                              + "${operations}\n"
                                                              + ":not([trait|http])";

    private static Model modelJson;
    private static Model traitModel;
    private static Model httpModel;
    private static Model resourceModel;

    @BeforeAll
    public static void before() {
        modelJson = Model.assembler().addImport(SelectorTest.class.getResource("model.json"))
                .disablePrelude()
                .assemble()
                .unwrap();
        traitModel = Model.assembler()
                .addImport(SelectorTest.class.getResource("nested-traits.smithy"))
                .assemble()
                .unwrap();
        httpModel = Model.assembler()
                .addImport(SelectorTest.class.getResource("http-model.smithy"))
                .assemble()
                .getResult() // ignore built-in errors
                .get();
        resourceModel = Model.assembler()
                .addImport(SelectorTest.class.getResource("resource.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void selectorEquality() {
        Selector a = Selector.parse("string");

        assertThat(a, equalTo(a));
        assertThat(a.hashCode(), equalTo(a.hashCode()));
    }

    @Test
    public void defaultSelectorInterfaceSelectCollectsStreamToSet() {
        Selector test = new Selector() {
            @Override
            public Stream<Shape> shapes(Model model) {
                return model.shapes();
            }

            @Override
            public Stream<ShapeMatch> matches(Model model) {
                return Stream.empty();
            }
        };

        assertThat(test.select(modelJson), equalTo(modelJson.toSet()));
    }

    @Test
    public void supportsDeprecatedEachFunction() {
        Set<Shape> result1 = Selector.parse(":each(collection)").select(modelJson);
        Set<Shape> result2 = Selector.parse(":is(collection)").select(modelJson);

        assertThat(result1, equalTo(result2));
    }

    static Set<String> ids(Model model, String expression) {
        return Selector.parse(expression)
                .select(model)
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toSet());
    }

    static Set<String> exampleIds(Model model, String expression) {
        return Selector.parse(expression)
                .select(model)
                .stream()
                .map(Shape::getId)
                .filter(id -> id.getNamespace().equals("smithy.example"))
                .map(ShapeId::toString)
                .collect(Collectors.toSet());
    }

    @Test
    public void detectsUnexpectedEof() {
        SelectorSyntaxException e = Assertions.assertThrows(SelectorSyntaxException.class, () -> {
            Selector.parse(":is(string,");
        });

        assertThat(e.getMessage(), containsString("Unexpected selector EOF"));
    }

    @Test
    public void selectsUsingNestedTraitValues() {
        Set<String> result = ids(traitModel, "[trait|range|min=1]");

        assertThat(result, hasItem("smithy.example#RangeInt1"));
        assertThat(result, not(hasItem("smithy.example#RangeInt2")));
        assertThat(result, not(hasItem("smithy.example#EnumString")));
    }

    @Test
    public void selectsUsingNestedTraitValuesUsingNegation() {
        Set<String> result = ids(traitModel, "[trait|range|min!=1]");

        assertThat(result, hasItem("smithy.example#RangeInt2"));
        assertThat(result, not(hasItem("smithy.example#RangeInt1")));
        assertThat(result, not(hasItem("smithy.example#EnumString")));
    }

    @Test
    public void selectsUsingNestedTraitValuesThroughProjection() {
        Set<String> result = ids(traitModel, "[trait|enum|(values)|deprecated=true]");

        assertThat(result, hasItem("smithy.example#EnumString"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString")));
    }

    @Test
    public void canSelectOnTraitObjectKeys() {
        Set<String> result = ids(traitModel, "[trait|externalDocumentation|(keys)=Homepage]");

        assertThat(result, hasItem("smithy.example#DocumentedString1"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString2")));
    }

    @Test
    public void canSelectOnTraitObjectValues() {
        Set<String> result = ids(traitModel, "[trait|externalDocumentation|(values)='https://www.anotherexample.com/']");

        assertThat(result, hasItem("smithy.example#DocumentedString2"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString1")));
    }

    @Test
    public void pathThroughTerminalValueReturnsNoResults() {
        Set<String> result = ids(traitModel, "[trait|documentation|foo|baz='nope']");

        assertThat(result, empty());
    }

    @Test
    public void pathThroughArrayWithInvalidItemReturnsNoResults() {
        Set<String> result = ids(traitModel, "[trait|tags|foo|baz='nope']");

        assertThat(result, empty());
    }

    @Test
    public void supportsNotEqualsAttribute() {
        Set<String> result = ids(modelJson, "[id|member!=member]");

        assertThat(result, containsInAnyOrder("ns.foo#Map$key", "ns.foo#Map$value"));
    }

    @Test
    public void supportsMatchingDeeplyOnTraitValues() {
        Set<String> result1 = ids(traitModel, "[trait|smithy.example#nestedTrait|foo|foo|bar='hi']");
        Set<String> result2 = ids(traitModel, "[trait|smithy.example#nestedTrait|foo|foo|bar='bye']");

        assertThat(result1, hasItem("smithy.example#DocumentedString1"));
        assertThat(result1, not(hasItem("smithy.example#DocumentedString2")));
        assertThat(result2, hasItem("smithy.example#DocumentedString2"));
        assertThat(result2, not(hasItem("smithy.example#DocumentedString1")));
    }

    @Test
    public void emptyListDoesNotAppearWhenProjecting() {
        Set<String> result = ids(traitModel, "[trait|enum|(values)|tags|(values)]");

        assertThat(result, hasItem("smithy.example#EnumString"));
        assertThat(result, hasItem("smithy.example#DocumentedString1"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString2")));
    }

    @Test
    public void selectsCollections() {
        // "collection" is just an alias for "list".
        Set<Shape> result = Selector.parse("collection").select(modelJson);

        assertThat(result, containsInAnyOrder(
                ListShape.builder()
                        .id("ns.foo#List")
                        .member(MemberShape.builder().id("ns.foo#List$member").target("ns.foo#String").build())
                        .build()));
    }

    @Test
    public void selectsCustomTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("model-custom-trait.json"))
                .assemble()
                .unwrap();
        Set<Shape> result = Selector.parse("*[trait|'com.example#beta']").select(model);

        Trait betaTrait = new DynamicTrait(ShapeId.from("com.example#beta"), Node.objectNode());
        Trait requiredTrait = new RequiredTrait();
        assertThat(result, containsInAnyOrder(
                MemberShape.builder()
                        .id("com.example#AnotherStructureShape$bar")
                        .target("com.example#MyShape")
                        .addTrait(betaTrait)
                        .addTrait(requiredTrait)
                        .build(),
                MemberShape.builder()
                        .id("com.example#MyShape$foo")
                        .target("com.example#StringShape")
                        .addTrait(betaTrait)
                        .addTrait(requiredTrait)
                        .build()));
    }

    @Test
    public void requiresValidAttribute() {
        Throwable thrown = Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse("[id=-]"));

        assertThat(thrown.getMessage(), containsString("Syntax error at line 1 column 6, near `]`"));
    }

    @Test
    public void detectsUnclosedQuote() {
        Throwable thrown = Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse("[id='foo]"));

        assertThat(thrown.getMessage(), containsString("Expected ' to close"));
    }

    @Test
    public void requiresAtLeastOneRelInDirectedNeighbor() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse("operation -[]->"));
    }

    @Test
    public void parsesMultiEdgeNeighbor() {
        Selector.parse("operation -[input, output]-> *");
    }

    @Test
    public void detectsUnclosedMultiEdgeNeighbor() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse("operation -[input"));
    }

    @Test
    public void detectsUnclosedMultiEdgeNeighborTrailingComma() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse("operation -[input, "));
    }

    @Test
    public void toleratesUnexpectedRelationshipTypes() {
        String expr = "operation -[foo]-> *";
        Selector selector = Selector.parse(expr);

        assertThat(expr, equalTo(selector.toString()));
    }

    @Test
    public void toleratesUnexpectedFunctionNames() {
        String expr = ":unknownFunction(string)";
        Selector selector = Selector.parse(expr);

        assertThat(expr, equalTo(selector.toString()));
        // Using the selector for an invalid function always returns an empty result.
        assertThat(selector.select(Model.builder().build()), empty());
    }

    @Test
    public void toleratesUnknownSelectorAttributes() {
        String expr = "[foof]";
        Selector selector = Selector.parse(expr);

        assertThat(expr, equalTo(selector.toString()));
        assertThat(selector.select(Model.builder().build()), empty());
    }

    @Test
    public void throwsOnInvalidShapeType() {
        SelectorSyntaxException e = Assertions.assertThrows(
                SelectorSyntaxException.class,
                () -> Selector.parse("foo"));

        assertThat(e.getMessage(), containsString("Unknown shape type"));
    }

    @Test
    public void parsesEachKindOfAttributeSelector() {
        Selector.parse("[id=abc][id!=abc][id^=abc][id$=abc][id*=abc]");
        Selector.parse("[  id = abc ] [ \nid\n!=\nabc\n]\n");
    }

    @Test
    public void parsesNestedTraitEndsWith() {
        Selector.parse("[trait|mediaType$='plain']");
    }

    @Test
    public void throwsOnInvalidComparator() {
        SelectorSyntaxException e = Assertions.assertThrows(
                SelectorSyntaxException.class,
                () -> Selector.parse("[id%=100]"));

        assertThat(e.getMessage(), containsString("Found '%', but expected one of the following tokens"));
    }

    @Test
    public void parsesCaseInsensitiveAttributes() {
        Selector.parse("[id=abc i][id=abc i ]");
    }

    @Test
    public void detectsInvalidShapeIds() {
        List<String> exprs = ListUtils.of(
                "[id=#]",
                "[id=com#]",
                "[id=com.foo#]",
                "[id=com.foo#$]",
                "[id=com.foo#baz$]",
                "[id=com.foo#.baz$]",
                "[id=com..foo#baz]",
                "[id=com#baz$.]",
                "[id=com#baz$bar$]",
                "[id=com#baz$bar#]",
                "[id=com#baz$bar.bam]",
                "[id=com.baz#foo$bar]", // Members are not permitted and must be quoted.
                "[id=Com.Baz#Foo$Bar123]",
                "[id=Com._Baz_#_Foo_$_Bar_123__]");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
        }
    }

    @Test
    public void parsesValidShapeIds() {
        List<String> exprs = ListUtils.of(
                "[id=com#foo]",
                "[id=com.baz#foo]",
                "[id=com.baz.bar#foo]",
                "[id=Foo]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void detectsInvalidNumbers() {
        List<String> exprs = ListUtils.of(
                "[id=1-]",
                "[id=-]",
                "[id=1.]",
                "[id=1..1]",
                "[id=1.0e]",
                "[id=1.0e+]",
                "[id=1e+]",
                "[id=1e++]",
                "[id=1+]",
                "[id=+1]");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
        }
    }

    @Test
    public void parsesValidNumbers() {
        List<String> exprs = ListUtils.of(
                "[id=1]",
                "[id=123]",
                "[id=0]",
                "[id=-1]",
                "[id=-10]",
                "[id=123456789]",
                "[id=1.5]",
                "[id=1.123456789]",
                "[id=1.1e+1]",
                "[id=1.1e-1]",
                "[id=1.1e-0]",
                "[id=1.1e-123456789]",
                "[id=1e+123456789]",
                "[id=1e-123456789]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void parsesValidQuotedAttributes() {
        List<String> exprs = ListUtils.of(
                "[id='1']",
                "[id=\"1\"]",
                "[id='aaaaa']",
                "[id=\"aaaaaa\"]",
                "[trait|'foo'=\"aaaaaa\"]",
                "[trait|\"foo\"=\"aaaaaa\"]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void toleratesInvalidKnownAttributePaths() {
        List<String> exprs = ListUtils.of(
                "[service]",
                "[service|version|foo]",
                "[trait]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void parsesValidAttributePathsAndToleratesUnknownPaths() {
        List<String> exprs = ListUtils.of(
                "[id=abc]",
                "[id|name=abc]",
                "[id|member=abc]",
                "[id|namespace=abc]",
                "[id|namespace|(foo)=abc]", // invalid, tolerated
                "[id|blurb=abc]",  // invalid, tolerated
                "[service|version=abc]",
                "[service|blurb=abc]",  // invalid, tolerated
                "[trait|foo=abc]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void canDeserializeSelectors() {
        String selector = "[trait|mediaType$='plain']";
        Node node = Node.objectNode().withMember("selector", selector);
        NodeMapper mapper = new NodeMapper();
        Pojo pojo = mapper.deserialize(node, Pojo.class);

        assertThat(pojo.getSelector().toString(), equalTo(selector));
    }

    public static final class Pojo {
        private Selector selector;

        public Selector getSelector() {
            return selector;
        }

        public void setSelector(Selector selector) {
            this.selector = selector;
        }
    }

    @Test
    public void canMatchOnExistence() {
        assertThat(exampleIds(traitModel, "[trait|tags]"), contains("smithy.example#EnumString"));
    }

    @Test
    public void canMatchUsingCaseInsensitiveComparison() {
        Set<String> matches = ids(traitModel, "[trait|error = 'CLIENT' i]");

        assertThat(matches, containsInAnyOrder("smithy.example#ErrorStruct2"));
    }

    @Test
    public void cannotMatchOnNonComparableAttributes() {
        assertThat(ids(traitModel, "[trait|tags='foo']"), empty());
    }

    @Test
    public void canMatchUsingCommaSeparatedAttributeValues() {
        Set<String> matches1 = ids(traitModel, "[trait|enum|(values)|value='m256.mega', 'nope']");
        Set<String> matches2 = ids(traitModel, "[trait|enum|(values)|value = 'm256.mega' ,'nope' ]");
        Set<String> matches3 = ids(traitModel, "[trait|enum|(values)|value = 'm256.mega' ,   nope ]");

        assertThat(matches1, containsInAnyOrder(
                "smithy.example#DocumentedString1",
                "smithy.example#DocumentedString2",
                "smithy.example#EnumString"));
        assertThat(matches1, equalTo(matches2));
        assertThat(matches1, equalTo(matches3));
    }

    @Test
    public void detectsInvalidAttributeCsv() {
        Assertions.assertThrows(
                SelectorSyntaxException.class,
                () -> Selector.parse("[trait|enum|(values)|value='m256.mega',]"));
    }

    @Test
    public void parsedRelativeComparators() {
        List<String> exprs = ListUtils.of(
                "[trait|httpError > 500]",
                "[trait|httpError >= 500]",
                "[trait|httpError < 500]",
                "[trait|httpError <= 500]",
                "[trait|httpError>500]",
                "[trait|httpError>=500]",
                "[trait|httpError<500]",
                "[trait|httpError<=500]",
                "[trait|httpError > 500, 400]", // silly, but supported
                "[trait|httpError >= 500, 400]", // silly, but supported
                "[trait|httpError < 500, 400]", // silly, but supported
                "[trait|httpError <= 500, 400]"); // silly, but supported

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void canMatchUsingRelativeSelectors() {
        Set<String> matches1 = ids(traitModel, "[trait|httpError >= 500]");
        Set<String> matches2 = ids(traitModel, "[trait|httpError > 499]");
        Set<String> matches3 = ids(traitModel, "[trait|httpError >= 400]");
        Set<String> matches4 = ids(traitModel, "[trait|httpError > 399]");
        Set<String> matches5 = ids(traitModel, "[trait|httpError <= 500]");
        Set<String> matches6 = ids(traitModel, "[trait|httpError < 500]");
        Set<String> matches7 = ids(traitModel, "[trait|httpError >= 500e0]");

        assertThat(matches1, containsInAnyOrder("smithy.example#ErrorStruct1"));
        assertThat(matches2, containsInAnyOrder("smithy.example#ErrorStruct1"));
        assertThat(matches3, containsInAnyOrder("smithy.example#ErrorStruct1", "smithy.example#ErrorStruct2"));
        assertThat(matches4, containsInAnyOrder("smithy.example#ErrorStruct1", "smithy.example#ErrorStruct2"));
        assertThat(matches5, containsInAnyOrder("smithy.example#ErrorStruct1", "smithy.example#ErrorStruct2"));
        assertThat(matches6, containsInAnyOrder("smithy.example#ErrorStruct2"));
        assertThat(matches7, containsInAnyOrder("smithy.example#ErrorStruct1"));
    }

    @Test
    public void invalidNumbersFailsGracefully() {
        assertThat(ids(traitModel, "[trait|httpError >= 'nope']"), empty());
        assertThat(ids(traitModel, "[trait|error >= 500]"), empty());
    }

    @Test
    public void doesNotToleratesInvalidIdAccess() {
        Assertions.assertThrows(SelectorException.class, () -> ids(traitModel, "[id|no|no=100]"));
    }

    @Test
    public void checksIfValueIsPresent() {
        Set<String> hasTags = exampleIds(traitModel, "[trait|tags?=true]");
        Set<String> noTags = exampleIds(traitModel, "[trait|tags?=false]");

        assertThat(hasTags, containsInAnyOrder("smithy.example#EnumString"));
        assertThat(noTags, not(hasItem("smithy.example#EnumString")));
        assertThat(noTags, not(empty()));
    }

    @Test
    public void matchesOnShapeIdName() {
        Set<String> enumString = ids(traitModel, "[id|name=EnumString]");

        assertThat(enumString, contains("smithy.example#EnumString"));
    }

    @Test
    public void selectsServiceExistence() {
        Set<String> services1 = ids(traitModel, "[service]");
        Set<String> services2 = ids(traitModel, "service");

        assertThat(services1, not(empty()));
        assertThat(services1, equalTo(services2));
    }

    @Test
    public void selectsServiceVersions() {
        Set<String> services1 = ids(traitModel, "[service|version='2020-04-21']");
        Set<String> services2 = ids(traitModel, "[id|name=MyService]");

        assertThat(services1, not(empty()));
        assertThat(services1, equalTo(services2));
    }

    @Test
    public void doesNotTolerateUnknownServicePaths() {
        Assertions.assertThrows(SelectorException.class, () -> ids(traitModel, "[service|foo|baz='bam']"));
    }

    @Test
    public void projectsTraitKeysAsShapeIds() {
        // All traits with a shape ID name of 'tags'.
        Set<String> shapes1 = exampleIds(traitModel, "[trait|(keys)|name='tags']");
        Set<String> shapes2 = exampleIds(traitModel, "[trait|tags]");

        assertThat(shapes1, contains("smithy.example#EnumString"));
        assertThat(shapes2, equalTo(shapes1));
    }

    @Test
    public void projectsTraitValuesAsNodes() {
        // All traits that have a property named "min".
        Set<String> shapes1 = exampleIds(traitModel, "[trait|(values)|min]");
        Set<String> shapes2 = exampleIds(traitModel, "[trait|range]");

        assertThat(shapes1, containsInAnyOrder("smithy.example#RangeInt1", "smithy.example#RangeInt2"));
        assertThat(shapes2, equalTo(shapes1));
    }

    @Test
    public void parsesValidScopedAttributes() {
        List<String> exprs = ListUtils.of(
                "[@trait: 10=10]",
                "[@trait: @{foo}=10]",
                "[@trait: @{foo}=@{foo}]",
                "[@trait: @{foo}=@{foo} && bar=bar]",
                "[@trait: @{foo}=@{foo} && bar=10]",
                "[@trait: @{foo}=@{foo} && bar=@{baz}]",
                "[@trait: @{foo}=@{foo} && bar=@{baz} && bam='abc']",
                "[@trait: @{foo}=@{foo} i && bar=@{baz} i && bam='abc' i ]",
                "[@  trait  :    @{foo}=@{foo}    i   &&bar=@{baz} i&&bam='abc'i\n]",
                "[@\r\n\t  trait\r\n\t : @{foo}=@{foo}]\r\n\t ",
                "[@trait: @{foo|baz|bam|(boo)}=@{foo|bar|(boo)|baz}]",
                "[@trait: @{foo|baz|bam|(boo)}=@{foo|bar|(boo)|baz}, @{foo|bam}]",
                // Comma separated values are or'd together.
                "[@trait: @{foo|baz|bam|(boo)}=@{foo|bar|(boo)|baz}, @{foo|bam} i && 10=10 i]",
                "[@: foo=bar]",
                "[@    :  foo   = bam  ]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void detectsInvalidScopedAttributes() {
        List<String> exprs = ListUtils.of(
                "[@",
                "[@foo",
                "[@foo:",
                "[@foo: bar",
                "[@foo: bar=",
                "[@foo: bar=bam",
                "[@foo: bar=bam i",
                "[@foo: bar+bam]", // Invalid comparator
                "[@foo: @",
                "[@foo: @{",
                "[@foo: @{abc",
                "[@foo: @{abc}",
                "[@foo: @{abc}]",
                "[@foo: @{abc}=",
                "[@foo: @{abc}=10",
                "[@foo: @{abc}=10 &&",
                "[@foo: @{abc}=10 && abc",
                "[@foo: @{abc}=10 && abc=",
                "[@foo: @{abc}=10 && abc=def",
                "[@foo: @{abc}=10 && abc=def i",
                "[@foo: @{abc{}}=10]",
                "[@foo: @{abc|}=10]",
                "[@foo: @{abc|def(baz)}=10]", // not a valid segment
                "[@foo: @{abc|()}=10]", // missing contents of ()
                "[@foo: @{abc|(.)}=10]", // invalid contents of ());
                "[@:",
                "[@: bar",
                "[@: bar]",
                "[@: bar=bam");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
        }
    }

    @Test
    public void evaluatesScopedAttributes() {
        Set<String> shapes1 = ids(traitModel, "[@trait|range: @{min}=1 && @{max}=10]");
        // Can scope to `trait` and then do assertions on all traits.
        // Not very useful, but technically supported.
        Set<String> shapes2 = ids(traitModel, "[@trait: @{range|min}=1 && @{range|max}=10]");

        assertThat(shapes1, contains("smithy.example#RangeInt1"));
        assertThat(shapes2, equalTo(shapes1));
    }

    @Test
    public void evaluatesScopedAttributesWithOr() {
        final Model enumModel = Model.assembler()
                .addImport(SelectorTest.class.getResource("enums.smithy"))
                .assemble()
                .unwrap();

        Set<String> shapes = ids(
            enumModel, "[@trait|enum|(values): @{name} ^= DIA, CLU]");

        assertThat(shapes, containsInAnyOrder("smithy.example#Suit"));

        shapes = ids(
            enumModel, "[@trait|enum|(values): @{name} ^= DIA, BLA]");

        assertThat(shapes, containsInAnyOrder("smithy.example#Color", "smithy.example#Suit"));
    }

    @Test
    public void evaluatesScopedAttributesWithProjections() {
        // Note that the projection can be on either side.
        Set<String> shapes1 = ids(traitModel, "[@trait|enum|(values): @{name}=@{value} && @{tags|(values)}=hi]");
        Set<String> shapes2 = ids(traitModel, "[@trait|enum|(values): @{name}=@{value} && hi=@{tags|(values)}]");

        assertThat(shapes1, contains("smithy.example#DocumentedString1"));
        assertThat(shapes2, equalTo(shapes1));
    }

    @Test
    public void projectionsCanMatchThemselvesThroughIntersection() {
        // Any enum with tags should match it's own tags.
        Set<String> shapes1 = ids(traitModel, "[@trait|enum|(values): @{tags|(values)}=@{tags|(values)}]");

        assertThat(shapes1, containsInAnyOrder("smithy.example#EnumString", "smithy.example#DocumentedString1"));
    }

    @Test
    public void nestedProjectionsAreFlattened() {
        Set<String> shapes1 = ids(traitModel, "[@trait|smithy.example#listyTrait|(values)|(values)|(values): @{foo}=a]");
        Set<String> shapes2 = ids(traitModel, "[@trait|smithy.example#listyTrait|(values)|(values)|(values): @{foo}=b]");
        Set<String> shapes3 = ids(traitModel, "[@trait|smithy.example#listyTrait|(values)|(values)|(values): @{foo}=c]");

        assertThat(shapes1, contains("smithy.example#MyService"));
        assertThat(shapes2, equalTo(shapes1));
        assertThat(shapes3, equalTo(shapes1));
    }

    @Test
    public void getsTheLengthOfShapeIds() {
        Set<String> shapes1 = ids(traitModel, "[id=smithy.api#String][id|(length) = 17]");
        Set<String> shapes2 = ids(traitModel, "[id=smithy.api#String][id|name|(length) = 6]");
        Set<String> shapes3 = ids(traitModel, "[id=smithy.api#String][id|namespace|(length) = 10]");

        assertThat(shapes1, contains("smithy.api#String"));
        assertThat(shapes2, equalTo(shapes1));
        assertThat(shapes3, equalTo(shapes1));
    }

    @Test
    public void getsTheLengthOfTraits() {
        Set<String> shapes = ids(traitModel, "[id=smithy.example#MyService][trait|(length) = 1]");

        assertThat(shapes, contains("smithy.example#MyService"));
    }

    @Test
    public void nullLengthIsNull() {
        assertThat(ids(traitModel, "[id|name|(foo)|(length) = 1]"), empty());
    }

    @Test
    public void getsTheLengthOfTraitString() {
        // "client"
        Set<String> shapes = ids(traitModel, "[id=smithy.example#ErrorStruct1][trait|error|(length) = 6]");

        assertThat(shapes, contains("smithy.example#ErrorStruct1"));
    }

    @Test
    public void getsTheLengthOfTraitArray() {
        Set<String> shapes = ids(
                traitModel,
                "[id=smithy.example#MyService][trait|smithy.example#listyTrait|(length) = 2]");

        assertThat(shapes, contains("smithy.example#MyService"));
    }

    @Test
    public void getsTheLengthOfTraitObject() {
        Set<String> shapes = ids(
                traitModel,
                "[id=smithy.example#DocumentedString2][trait|externalDocumentation|(length) = 1]");

        assertThat(shapes, contains("smithy.example#DocumentedString2"));
    }

    @Test
    public void projectionLengthUsesSetLogic() {
        // Find shapes with the enum trait where there are more than 1 tags on any
        // enum definition.
        Set<String> shapes = exampleIds(
                traitModel,
                "[trait|enum|(values)|tags|(length) > 1]");

        assertThat(shapes, contains("smithy.example#DocumentedString1"));
    }

    @Test
    public void canBindShapeAsContext() {
        assertThat(ids(traitModel, "[trait|trait][@: @{trait|(keys)} = @{id}]"),
                   hasItems("smithy.example#recursiveTrait",
                            "smithy.api#trait",
                            "smithy.api#documentation"));
    }

    @Test
    public void parsesSubsetOperator() {
        Selector.parse("[@: @{trait|tags|(values)} {<<} @{trait|tags|(values)}]");
    }

    @Test
    public void parsesValidProjectionComparators() {
        List<String> exprs = ListUtils.of(
                "[@: @{trait|tags|(values)} {<<} @{trait|tags|(values)}]",
                "[@: @{trait|tags|(values)} {<} @{trait|tags|(values)}]",
                "[@: @{trait|tags|(values)} {!=} @{trait|tags|(values)}]",
                "[@: @{trait|tags|(values)} {=} @{trait|tags|(values)}]",
                "[@: @{trait|tags|(values)}    {=}    @{trait|tags|(values)}]");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void detectsInvalidProjectionComparators() {
        List<String> exprs = ListUtils.of(
                "[@: @{trait|tags|(values)} {} @{trait|tags|(values)}]",
                "[@: @{trait|tags|(values)} {<<",
                "[@: @{trait|tags|(values)} {<",
                "[@: @{trait|tags|(values)} {");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
        }
    }

    @Test
    public void canQueryVariablesAtPointTheyWereMatched() {
        Model result = Model.assembler()
                .addImport(SelectorTest.class.getResource("service-with-bad-auth.smithy"))
                .assemble()
                .getResult()
                .get();

        // Find all operation shapes that are in the closure of a service that use
        // auth traits that don't match the auth traits associated with the service.
        Selector selector = Selector.parse(
                "service\n"
                + "$service(*)\n"
                + "$authTraits(-[trait]-> [trait|authDefinition])\n"
                + "~>\n"
                + "operation\n"
                + "[trait|auth]"
                + ":not([@: @{trait|auth|(values)} {<} @{var|authTraits|id}]))");

        List<Pair<Shape, Map<String, Set<Shape>>>> results = new ArrayList<>();
        selector.runner().model(result).selectMatches((s, vars) -> results.add(Pair.of(s, vars)));
        Shape service1 = result.expectShape(ShapeId.from("smithy.example#MyService1"));
        Shape service2 = result.expectShape(ShapeId.from("smithy.example#MyService2"));

        assertThat(results, hasSize(3));

        // Selectors are lazy, not eager. Each shape is passed through individually. This
        // means that the variables available to a shape are specific to each shape
        // as it passes through the selector. These test that each select saw different,
        // expected variables.
        checkMatches(results, "Expected smithy.example#HasDigestAuth with a service of smithy.example#MyService1",
                (s, v) -> s.getId().equals(ShapeId.from("smithy.example#HasDigestAuth"))
                          && v.containsKey("service")
                          && v.get("service").equals(SetUtils.of(service1)));

        checkMatches(results, "Expected smithy.example#HasDigestAuth with a service of smithy.example#MyService2",
                     (s, v) -> s.getId().equals(ShapeId.from("smithy.example#HasDigestAuth"))
                               && v.containsKey("service")
                               && v.get("service").equals(SetUtils.of(service2)));

        checkMatches(results, "Expected smithy.example#HasBasicAuth with a service of smithy.example#MyService2",
                     (s, v) -> s.getId().equals(ShapeId.from("smithy.example#HasBasicAuth"))
                               && v.containsKey("service")
                               && v.get("service").equals(SetUtils.of(service2)));
    }

    private void checkMatches(
            Iterable<Pair<Shape, Map<String, Set<Shape>>>> matches,
            String message,
            BiPredicate<Shape, Map<String, Set<Shape>>> test
    ) {
        for (Pair<Shape, Map<String, Set<Shape>>> match : matches) {
            if (test.test(match.left, match.right)) {
                return;
            }
        }

        Assertions.fail(message);
    }

    @Test
    public void canPushAndRetrieveVariables() {
        Model result = Model.assembler()
                .addImport(SelectorTest.class.getResource("service-with-bad-auth.smithy"))
                .assemble()
                .getResult()
                .get();

        // Find all operation shapes that are in the closure of a service that uses
        // http bindings on other operations, but the operation does not use http bindings.
        Selector selector = Selector.parse(
                "$service(service)\n"
                + "${service}\n"
                + "$operations(~> operation)\n"
                + "$httpOperations(${operations}[trait|http])\n"
                + "${operations}\n"
                + ":not([trait|http])");

        List<Pair<Shape, Map<String, Set<Shape>>>> results = new ArrayList<>();
        selector.runner().model(result).selectMatches((s, vars) -> results.add(Pair.of(s, vars)));
        Shape service1 = result.expectShape(ShapeId.from("smithy.example#MyService1"));
        Shape service2 = result.expectShape(ShapeId.from("smithy.example#MyService2"));

        Shape basicAuth = result.expectShape(ShapeId.from("smithy.example#HasBasicAuth"));
        Shape noAuth = result.expectShape(ShapeId.from("smithy.example#NoAuth"));
        Shape digestAuth = result.expectShape(ShapeId.from("smithy.example#HasDigestAuth"));

        assertThat(results, hasSize(2));

        for (Shape service : ListUtils.of(service1, service2)) {
            checkMatches(results, "Expected smithy.example#HasBasicAuth with a service of smithy.example#MyService1",
                         (s, v) -> s.getId().equals(ShapeId.from("smithy.example#HasBasicAuth"))
                                   && v.containsKey("service")
                                   && v.containsKey("operations")
                                   && v.containsKey("httpOperations")
                                   && v.get("service").equals(SetUtils.of(service))
                                   && v.get("operations").equals(SetUtils.of(digestAuth, noAuth, basicAuth))
                                   && v.get("httpOperations").equals(SetUtils.of(digestAuth, noAuth)));
        }
    }

    @Test
    public void parsesValidVariableAccess() {
        List<String> exprs = ListUtils.of(
                "${foo}",
                "${ foo }",
                "${\nfoo\n}\n",
                "${a}",
                "${a_b_c}",
                "${_a}",
                "${__a}");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void detectsInvalidVariableAccess() {
        List<String> exprs = ListUtils.of(
                "$",
                "${",
                "${}",
                "$}",
                "${a",
                "${*}",
                "${_}",
                "${__}");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
        }
    }

    @Test
    public void findsOperationsMissingHttpBindings() {
        Set<String> ids = exampleIds(httpModel, OPERATIONS_MISSING_BINDINGS);

        assertThat(ids, contains("smithy.example#NoHttp"));
    }

    @Test
    public void returnsStreamOfShapes() {
        Selector selector = Selector.parse(":test(string, map)");
        Set<Shape> shapes = selector
                .shapes(modelJson)
                .collect(Collectors.toSet());

        assertThat(shapes, equalTo(selector.select(modelJson)));
    }

    @Test
    public void returnsStreamOfMatches() {
        Selector selector = Selector.parse(OPERATIONS_MISSING_BINDINGS);
        Set<Selector.ShapeMatch> matches = selector
                .matches(httpModel)
                .collect(Collectors.toSet());

        Set<Selector.ShapeMatch> matches2 = new HashSet<>();
        selector.consumeMatches(httpModel, matches2::add);

        assertThat(matches, equalTo(matches2));
    }

    @Test
    public void canPathToErrorsOfStructure() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Svc")
                .version("2017-01-17")
                .addError("ns.foo#Common1")
                .build();
        StructureShape errorShape = StructureShape.builder()
                .id("ns.foo#Common1")
                .addTrait(new ErrorTrait("client"))
                .build();
        Model model = Model.builder().addShapes(service, errorShape).build();

        Selector selector = Selector.parse("service -[error]-> structure");
        Set<Shape> result = selector.select(model);

        assertThat(result, containsInAnyOrder(errorShape));
    }

    @Test
    public void selectsStructuresWithMixins() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("structure-with-mixins.smithy"))
                .assemble()
                .unwrap();
        Selector hasMixins = Selector.parse("structure :test(-[mixin]->)");
        Selector isUsedMixin = Selector.parse("structure -[mixin]->");
        Selector noMixins = Selector.parse("structure[id|namespace='smithy.example'] :not(-[mixin]->)");
        Selector unusedMixin = Selector.parse("[trait|mixin][id|namespace='smithy.example'] :not(<-[mixin]-)");

        assertThat(hasMixins.select(model).stream().map(Shape::toShapeId).collect(Collectors.toSet()),
                   containsInAnyOrder(ShapeId.from("smithy.example#Mixin2"), ShapeId.from("smithy.example#Concrete")));

        assertThat(isUsedMixin.select(model).stream().map(Shape::toShapeId).collect(Collectors.toSet()),
                   containsInAnyOrder(ShapeId.from("smithy.example#Mixin1"), ShapeId.from("smithy.example#Mixin2")));

        assertThat(noMixins.select(model).stream().map(Shape::toShapeId).collect(Collectors.toSet()),
                   containsInAnyOrder(ShapeId.from("smithy.example#Mixin1"),
                                      ShapeId.from("smithy.example#NoMixins"),
                                      ShapeId.from("smithy.example#UnusedMixin")));

        assertThat(unusedMixin.select(model).stream().map(Shape::toShapeId).collect(Collectors.toSet()),
                   contains(ShapeId.from("smithy.example#UnusedMixin")));
    }

    @Test
    public void supportsResourceProperties() {
        Set<Shape> resourcesWithProperties = Selector.parse("resource :test(-[property]->)").select(resourceModel);
        ResourceShape forecastResource = resourceModel.expectShape(ShapeId.from("example.weather#Forecast"),
                ResourceShape.class);
        ResourceShape cityResource = resourceModel.expectShape(ShapeId.from("example.weather#City"),
                ResourceShape.class);
        assertThat(resourcesWithProperties.size(), equalTo(2));
        assertThat(resourcesWithProperties, containsInAnyOrder(forecastResource, cityResource));

        Set<Shape> shapesTargettedByAnyProperty = Selector.parse("resource -[property]-> *").select(resourceModel);
        StructureShape coordinatesShape = resourceModel.expectShape(ShapeId.from("example.weather#CityCoordinates"),
                StructureShape.class);
        FloatShape floatShape = resourceModel.expectShape(ShapeId.from("smithy.api#Float"), FloatShape.class);
        StringShape stringShape = resourceModel.expectShape(ShapeId.from("smithy.api#String"), StringShape.class);
        assertThat(shapesTargettedByAnyProperty.size(), equalTo(3));
        assertThat(shapesTargettedByAnyProperty, containsInAnyOrder(coordinatesShape, floatShape,
                stringShape));

        Set<Shape> shapesTargettedByCityOnly = Selector.parse("resource [id|name=City] -[property]-> *")
                .select(resourceModel);
        assertThat(shapesTargettedByCityOnly.size(), equalTo(2));
        assertThat(shapesTargettedByCityOnly, containsInAnyOrder(coordinatesShape, stringShape));
    }

    @Test
    public void rootFunctionReturnsAllShapes() {
        Selector selector = Selector.parse("string"
                                           + ":in(:root(-[input]-> ~> *))"
                                           + ":not(:in(:root(-[output]-> ~> *)))");
        Set<Shape> result = selector.select(resourceModel);

        // This is the only string used in input but not output.
        assertThat(result, contains(resourceModel.expectShape(ShapeId.from("example.weather#CityId"))));
    }

    @Test
    public void inefficientIfNotCached() {
        Selector selector = Selector.parse(":in(:root(service ~> number))");
        Set<Shape> result = selector.select(resourceModel);

        // This is the only number used in any service.
        assertThat(result, contains(resourceModel.expectShape(ShapeId.from("smithy.api#Float"))));
    }

    @Test
    public void allowsNestedRoots() {
        Selector selector = Selector.parse(":root(:root(:root(*)))");
        Set<Shape> result = selector.select(resourceModel);

        assertThat(result, equalTo(resourceModel.toSet()));
    }

    @Test
    public void inDoesNotSupportMoreThanOneSelector() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(":in(*, *)"));
    }

    @Test
    public void inRequiresOneSelector() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(":in()"));
    }

    @Test
    public void rootDoesNotSupportMoreThanOneSelector() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(":root(*, *)"));
    }

    @Test
    public void rootRequiresOneSelector() {
        Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(":root()"));
    }
}
