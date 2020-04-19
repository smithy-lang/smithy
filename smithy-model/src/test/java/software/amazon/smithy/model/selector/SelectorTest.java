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
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.Trait;

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

        assertThat(thrown.getMessage(), containsString("Invalid attribute start character"));
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
    }
}
