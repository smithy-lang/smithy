/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff.evaluators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

public class TraitBreakingChangeTest {

    private static final ShapeId EXAMPLE_SHAPE = ShapeId.from("smithy.example#Example");
    private static final ShapeId EXAMPLE_TRAIT = ShapeId.from("smithy.example#exampleTrait");

    @Test
    public void detectsBreakingChangeWhenRemoved() {
        validate("trait-removed.smithy", shape -> shape.removeTrait(EXAMPLE_TRAIT).build(), events -> {
            assertThat(events, hasSize(1));
            assertThat(events.get(0).getSeverity(), equalTo(Severity.ERROR));
            assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
            assertThat(events.get(0).getMessage(), equalTo("Removed trait `smithy.example#exampleTrait`"));
        });
    }

    private void validate(
            String modelFile,
            Function<StringShape.Builder, Shape> mapper,
            Consumer<List<ValidationEvent>> consumer
    ) {
        Model modelA = Model.assembler()
                .addImport(getClass().getResource("trait-breaking-change/" + modelFile))
                .assemble()
                .unwrap();

        // Find the example shape, transform it, and create a new model.
        StringShape example = modelA.expectShape(EXAMPLE_SHAPE, StringShape.class);
        Shape updated = mapper.apply(example.toBuilder());
        Model modelB = ModelTransformer.create().replaceShapes(modelA, ListUtils.of(updated));

        List<ValidationEvent> events = TestHelper.findEvents(ModelDiff.compare(modelA, modelB), "TraitBreakingChange");

        consumer.accept(events);
    }

    @Test
    public void detectsBreakingChangeWhenAdded() {
        validate(
            "trait-added.smithy",
            shape -> shape.addTrait(new DynamicTrait(EXAMPLE_TRAIT, Node.objectNode())).build(),
            events -> {
                assertThat(events, hasSize(1));
                assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Add.smithy.example#exampleTrait"));
                assertThat(events.get(0).getMessage(), equalTo("Added trait `smithy.example#exampleTrait`"));
            }
        );
    }

    @Test
    public void detectsBreakingChangePresence() {
        validate("trait-presence.smithy", shape -> shape.removeTrait(EXAMPLE_TRAIT).build(), events -> {
            assertThat(events, hasSize(1));
            assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
            assertThat(events.get(0).getMessage(), equalTo("Removed trait `smithy.example#exampleTrait`"));
        });
    }

    @Test
    public void detectsBreakingChangeAny() {
        validate("trait-any.smithy", shape -> shape.removeTrait(EXAMPLE_TRAIT).build(), events -> {
            assertThat(events, hasSize(1));
            assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
            assertThat(events.get(0).getMessage(), equalTo("Removed trait `smithy.example#exampleTrait`"));
        });
    }

    @Test
    public void canChangeSeverity() {
        validate("trait-severity.smithy", shape -> shape.removeTrait(EXAMPLE_TRAIT).build(), events -> {
            assertThat(events, hasSize(1));
            assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
            assertThat(events.get(0).getSeverity(), equalTo(Severity.WARNING));
        });
    }

    @Test
    public void canIncludeCustomMessage() {
        validate("trait-message.smithy", shape -> shape.removeTrait(EXAMPLE_TRAIT).build(), events -> {
            assertThat(events, hasSize(1));
            assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
            assertThat(events.get(0).getMessage(),
                       equalTo("Removed trait `smithy.example#exampleTrait`; This is bad!"));
        });
    }

    @Test
    public void canPathIntoListMembers() {
        validate(
            "trait-list-members.smithy",
            shape -> shape.addTrait(new DynamicTrait(EXAMPLE_TRAIT, Node.fromStrings("a", "B", "c"))).build(),
            events -> {
                assertThat(events, hasSize(1));
                assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Update.smithy.example#exampleTrait"));
                assertThat(events.get(0).getMessage(),
                           equalTo("Changed trait contents of `smithy.example#exampleTrait` at path `/1` "
                                   + "from `b` to `B`"));
            }
        );
    }

    @Test
    public void canPathIntoMapKeys() {
        validate(
                "trait-map-keys.smithy",
                shape -> shape.addTrait(new DynamicTrait(EXAMPLE_TRAIT, Node.objectNode().withMember("a", "A")))
                        .build(),
                events -> {
                    assertThat(events, hasSize(1));
                    assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
                    assertThat(events.get(0).getMessage(),
                               equalTo("Removed trait contents from `smithy.example#exampleTrait` at path `/b`. "
                                       + "Removed value: `B`"));
                }
        );
    }

    @Test
    public void canPathIntoMapValues() {
        validate(
            "trait-map-values.smithy",
            shape -> {
                Trait trait = new DynamicTrait(EXAMPLE_TRAIT,
                                               Node.objectNode().withMember("a", "A").withMember("b", "_B_"));
                return shape.addTrait(trait).build();
            },
            events -> {
                assertThat(events, hasSize(1));
                assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Update.smithy.example#exampleTrait"));
                assertThat(events.get(0).getMessage(),
                           equalTo("Changed trait contents of `smithy.example#exampleTrait` at path `/b` "
                                   + "from `B` to `_B_`"));
            }
        );
    }

    @Test
    public void canPathIntoStructureMembers() {
        validate(
            "trait-structure-members.smithy",
            shape -> {
                Trait trait = new DynamicTrait(EXAMPLE_TRAIT, Node.objectNode());
                return shape.addTrait(trait).build();
            },
            events -> {
                assertThat(events, hasSize(1));
                assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Remove.smithy.example#exampleTrait"));
                assertThat(events.get(0).getMessage(),
                           equalTo("Removed trait contents from `smithy.example#exampleTrait` at path "
                                   + "`/foo/bar`. Removed value: `hi`"));
            }
        );
    }

    @Test
    public void canPathIntoUnionMembers() {
        validate(
            "trait-union-members.smithy",
            shape -> {
                Trait trait = new DynamicTrait(EXAMPLE_TRAIT, Node.objectNode().withMember("foo", "bye"));
                return shape.addTrait(trait).build();
            },
            events -> {
                assertThat(events, hasSize(1));
                assertThat(events.get(0).getId(), equalTo("TraitBreakingChange.Update.smithy.example#exampleTrait"));
                assertThat(events.get(0).getMessage(),
                           equalTo("Changed trait contents of `smithy.example#exampleTrait` at path "
                                   + "`/foo` from `hi` to `bye`"));
            }
        );
    }
}
