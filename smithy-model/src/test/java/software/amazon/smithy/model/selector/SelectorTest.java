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

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.Trait;

public class SelectorTest {

    @Test
    public void selectsCollections() {
        ShapeIndex index = Model.assembler().addImport(getClass().getResource("model.json"))
                .disablePrelude()
                .assemble()
                .unwrap()
                .getShapeIndex();
        Set<Shape> result = Selector.parse("collection").select(index);

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
        ShapeIndex index = Model.assembler()
                .addImport(getClass().getResource("model-custom-trait.json"))
                .assemble()
                .unwrap()
                .getShapeIndex();
        Set<Shape> result = Selector.parse("*[trait|'com.example#beta']").select(index);

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
}
