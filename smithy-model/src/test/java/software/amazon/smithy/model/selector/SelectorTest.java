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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

public class SelectorTest {

    private static Model modelJson;
    private static Model traitModel;

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
    }

    @Test
    public void supportsDeprecatedEachFunction() {
        Set<Shape> result1 = Selector.parse(":each(collection)").select(modelJson);
        Set<Shape> result2 = Selector.parse(":is(collection)").select(modelJson);

        assertThat(result1, equalTo(result2));
    }

    private List<String> ids(Model model, String expression) {
        return Selector.parse(expression)
                .select(model)
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());
    }

    @Test
    public void selectsUsingNestedTraitValues() {
        List<String> result = ids(traitModel, "[trait|range|min=1]");

        assertThat(result, hasItem("smithy.example#RangeInt1"));
        assertThat(result, not(hasItem("smithy.example#RangeInt2")));
        assertThat(result, not(hasItem("smithy.example#EnumString")));
    }

    @Test
    public void selectsUsingNestedTraitValuesUsingNegation() {
        List<String> result = ids(traitModel, "[trait|range|min!=1]");

        assertThat(result, hasItem("smithy.example#RangeInt2"));
        assertThat(result, not(hasItem("smithy.example#RangeInt1")));
        assertThat(result, not(hasItem("smithy.example#EnumString")));
    }

    @Test
    public void selectsUsingNestedTraitValuesThroughProjection() {
        List<String> result = ids(traitModel, "[trait|enum|(values)|deprecated=true]");

        assertThat(result, hasItem("smithy.example#EnumString"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString")));
    }

    @Test
    public void canSelectOnTraitObjectKeys() {
        List<String> result = ids(traitModel, "[trait|externalDocumentation|(keys)=Homepage]");

        assertThat(result, hasItem("smithy.example#DocumentedString1"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString2")));
    }

    @Test
    public void canSelectOnTraitObjectValues() {
        List<String> result = ids(traitModel, "[trait|externalDocumentation|(values)='https://www.anotherexample.com/']");

        assertThat(result, hasItem("smithy.example#DocumentedString2"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString1")));
    }

    @Test
    public void pathThroughTerminalValueReturnsNoResults() {
        List<String> result = ids(traitModel, "[trait|documentation|foo|baz='nope']");

        assertThat(result, empty());
    }

    @Test
    public void pathThroughArrayWithInvalidItemReturnsNoResults() {
        List<String> result = ids(traitModel, "[trait|tags|foo|baz='nope']");

        assertThat(result, empty());
    }

    @Test
    public void supportsNotEqualsAttribute() {
        List<String> result = ids(modelJson, "[id|member!=member]");

        assertThat(result, containsInAnyOrder("ns.foo#Map$key", "ns.foo#Map$value"));
    }

    @Test
    public void supportsMatchingDeeplyOnTraitValues() {
        List<String> result1 = ids(traitModel, "[trait|smithy.example#nestedTrait|foo|foo|bar='hi']");
        List<String> result2 = ids(traitModel, "[trait|smithy.example#nestedTrait|foo|foo|bar='bye']");

        assertThat(result1, hasItem("smithy.example#DocumentedString1"));
        assertThat(result1, not(hasItem("smithy.example#DocumentedString2")));
        assertThat(result2, hasItem("smithy.example#DocumentedString2"));
        assertThat(result2, not(hasItem("smithy.example#DocumentedString1")));
    }

    @Test
    public void emptyListDoesNotAppearWhenProjecting() {
        List<String> result = ids(traitModel, "[trait|enum|(values)|tags|(values)]");

        assertThat(result, hasItem("smithy.example#EnumString"));
        assertThat(result, hasItem("smithy.example#DocumentedString1"));
        assertThat(result, not(hasItem("smithy.example#DocumentedString2")));
    }

    @Test
    public void selectsCollections() {
        Set<Shape> result = Selector.parse("collection").select(modelJson);

        assertThat(result, containsInAnyOrder(
                SetShape.builder()
                        .id("ns.foo#Set")
                        .member(MemberShape.builder().id("ns.foo#Set$member").target("ns.foo#String").build())
                        .build(),
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

        assertThat(thrown.getMessage(), containsString("Syntax error at character 5 of 6, near `]`"));
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

        assertThat(e.getMessage(), containsString("Expected one of the following tokens"));
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
    public void detectsInvalidKnownAttributePaths() {
        List<String> exprs = ListUtils.of(
                "[service]",
                "[service|version|foo]",
                "[trait]");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
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
    public void canMatchUsingCommaSeparatedAttributeValues() {
        List<String> matches1 = ids(traitModel, "[trait|enum|(values)|value='m256.mega', 'nope']");
        List<String> matches2 = ids(traitModel, "[trait|enum|(values)|value = 'm256.mega' ,'nope' ]");
        List<String> matches3 = ids(traitModel, "[trait|enum|(values)|value = 'm256.mega' ,   nope ]");

        assertThat(matches1, equalTo(matches2));
        assertThat(matches1, equalTo(matches3));
        assertThat(matches1, containsInAnyOrder(
                "smithy.example#DocumentedString1",
                "smithy.example#DocumentedString2",
                "smithy.example#EnumString"));
    }

    @Test
    public void detectsInvalidAttributeCsv() {
        Assertions.assertThrows(
                SelectorSyntaxException.class,
                () -> Selector.parse("[trait|enum|(values)|value='m256.mega',]"));
    }
}
