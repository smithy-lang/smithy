/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Regression coverage for moving the assembler-generated marker trait into the
 * prelude as {@code smithy.api#synthetic}.
 *
 * <p>Before the move the trait was {@code smithy.synthetic#generated}, which had
 * no prelude definition. It was serialized into the JSON AST, so any consumer
 * that resolved traits against only the prelude reported it as an unresolved
 * trait. Being in the prelude, the trait must now resolve cleanly and must not
 * trip private-access validation even though it is attached to shapes in a
 * different namespace.
 */
public class SyntheticShapePreludeTest {

    private static final String INLINE_COLLECTIONS_MODEL =
            "$version: \"2.1\"\n"
                    + "namespace smithy.example\n"
                    + "structure MyStructure {\n"
                    + "    strings: [String]\n"
                    + "    tags: {String: String}\n"
                    + "    nested: {String: [Integer]}\n"
                    + "}\n";

    @Test
    public void syntheticTraitResolvesWithoutValidationEvents() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("inline.smithy", INLINE_COLLECTIONS_MODEL)
                .assemble();

        // No unresolved-trait events (the failure the prelude move fixes) and no
        // private-access noise from a prelude trait attached to smithy.example shapes.
        assertThat(eventIds(result, "Model"), is(empty()));
        assertThat(eventIds(result, "PrivateAccess"), is(empty()));
    }

    @Test
    public void syntheticShapesCarryPreludeTrait() {
        Model model = Model.assembler()
                .addUnparsedModel("inline.smithy", INLINE_COLLECTIONS_MODEL)
                .assemble()
                .unwrap();

        Shape listOfString = model.expectShape(ShapeId.from("smithy.example#_SyntheticListOfString"));
        assertThat(listOfString.hasTrait(SyntheticShapeTrait.ID), is(true));
        assertThat(SyntheticShapeTrait.ID, is(ShapeId.from("smithy.api#synthetic")));
    }

    @Test
    public void syntheticTraitSurvivesAstRoundTrip() {
        Model model = Model.assembler()
                .addUnparsedModel("inline.smithy", INLINE_COLLECTIONS_MODEL)
                .assemble()
                .unwrap();

        // Serialize to the JSON AST and reload; the trait must round-trip and resolve.
        ObjectNode serialized = ModelSerializer.builder().build().serialize(model);
        ValidatedResult<Model> reloaded = Model.assembler()
                .addDocumentNode(serialized)
                .assemble();

        assertThat(eventIds(reloaded, "Model"), is(empty()));
        Model reloadedModel = reloaded.unwrap();
        Shape listOfString =
                reloadedModel.expectShape(ShapeId.from("smithy.example#_SyntheticListOfString"));
        assertThat(listOfString.hasTrait(SyntheticShapeTrait.ID), is(true));
    }

    @Test
    public void syntheticTraitIsSerializedInTheAst() {
        Model model = Model.assembler()
                .addUnparsedModel("inline.smithy", INLINE_COLLECTIONS_MODEL)
                .assemble()
                .unwrap();

        ObjectNode serialized = ModelSerializer.builder().build().serialize(model);
        ObjectNode shapes = serialized.expectObjectMember("shapes");
        ObjectNode listShape = shapes
                .expectObjectMember("smithy.example#_SyntheticListOfString");
        ObjectNode traits = listShape.expectObjectMember("traits");
        assertThat(traits.getMember("smithy.api#synthetic").isPresent(), is(true));
    }

    private static List<String> eventIds(ValidatedResult<Model> result, String id) {
        return result.getValidationEvents().stream()
                .filter(event -> event.getId().equals(id))
                .map(ValidationEvent::getMessage)
                .collect(Collectors.toList());
    }
}
