/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

public class DiscriminatedTraitTest {
    private static final ShapeId TRAIT_ID = ShapeId.from("smithy.api#discriminated");

    @Test
    public void loadsTraitWithString() {
        Node node = Node.from("kind");
        Optional<Trait> trait = TraitFactory.createServiceFactory()
                .createTrait(TRAIT_ID, ShapeId.from("ns.qux#Foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(DiscriminatedTrait.class));
        assertThat(((DiscriminatedTrait) trait.get()).getValue(), equalTo("kind"));
        assertThat(trait.get().toNode(), equalTo(node));
    }

    @Test
    public void usesDefaultWhenProvidedNullNode() {
        Optional<Trait> trait = TraitFactory.createServiceFactory()
                .createTrait(TRAIT_ID, ShapeId.from("ns.qux#Foo"), Node.nullNode());

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(DiscriminatedTrait.class));
        assertThat(((DiscriminatedTrait) trait.get()).getValue(), equalTo("__type"));
        assertThat(trait.get().toNode(), equalTo(Node.from("__type")));
    }

    @Test
    public void defaultAndExplicitDefaultAreEqual() {
        assertThat(new DiscriminatedTrait(), equalTo(new DiscriminatedTrait("__type")));
    }

    @Test
    public void loadsExplicitValueFromModel() {
        assertThat(discriminatorOf("@discriminated(\"kind\")"), equalTo("kind"));
    }

    @Test
    public void loadsBareTraitFromModelUsingDefault() {
        assertThat(discriminatorOf("@discriminated"), equalTo("__type"));
    }

    @Test
    public void loadsAsTypedTraitFromModel() {
        // Guards against the trait provider not being registered with the model loader, in which case the trait is
        // retained only as a generic trait and codecs that look up DiscriminatedTrait never see it.
        Trait trait = load("@discriminated(\"kind\")").getResult()
                .orElseThrow()
                .expectShape(ShapeId.from("ns#U"))
                .findTrait(TRAIT_ID)
                .orElseThrow();

        assertThat(trait, instanceOf(DiscriminatedTrait.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "type",
            "Type",
            "kind2",
            "_",
            "__type",
            "a-b",
            "a.b",
            "@type",
            "$type",
            "@odata.type",
            "a b",
            "1type",
            "t\u00e9"})
    public void acceptsAnyNonEmptyDiscriminatorName(String name) {
        // Like @jsonName, any string is a valid field name in JSON, and legacy APIs use names like `@type`.
        assertThat(discriminatorOf("@discriminated(\"" + name + "\")"), equalTo(name));
    }

    @Test
    public void rejectsEmptyDiscriminatorName() {
        ValidatedResult<Model> result = load("@discriminated(\"\")");

        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(1));
        assertThat(result.getValidationEvents(Severity.ERROR).get(0).getId(), equalTo("TraitValue"));
    }

    @Test
    public void loadsBareTraitUsingApplyStatement() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\nnamespace ns\n"
                                + "apply U @discriminated\nunion U { a: A }\nstructure A { x: String }\n")
                .assemble();

        assertThat(result.unwrap()
                .expectShape(ShapeId.from("ns#U"))
                .expectTrait(DiscriminatedTrait.class)
                .getValue(), equalTo("__type"));
    }

    @Test
    public void serializesBareTraitWithResolvedValue() {
        Model model = load("@discriminated").unwrap();
        Node serialized = ModelSerializer.builder().build().serialize(model);

        // The serialized model is self-contained, so the default is written out explicitly and reloads the same.
        assertThat(serialized.expectObjectNode()
                .expectObjectMember("shapes")
                .expectObjectMember("ns#U")
                .expectObjectMember("traits")
                .expectStringMember("smithy.api#discriminated")
                .getValue(), equalTo("__type"));
        assertThat(Model.assembler().addModel(model).assemble().unwrap(), equalTo(model));
    }

    @Test
    public void explicitNullIsEquivalentToBareTrait() {
        // The provider cannot tell an explicit null from a bare trait, so both take the default.
        assertThat(discriminatorOf("@discriminated(null)"), equalTo("__type"));
    }

    @Test
    public void rejectsNonStringValue() {
        ValidatedResult<Model> result = load("@discriminated(1)");

        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(1));
    }

    @Test
    public void cannotBeAppliedToStructure() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\nnamespace ns\n"
                                + "@discriminated\nstructure S {}\n")
                .assemble();

        assertTrue(result.getValidationEvents(Severity.ERROR)
                .stream()
                .anyMatch(e -> e.getId().equals("TraitTarget")));
    }

    private static String discriminatorOf(String traitUse) {
        Model model = load(traitUse).unwrap();
        return model.expectShape(ShapeId.from("ns#U"))
                .expectTrait(DiscriminatedTrait.class)
                .getValue();
    }

    private static ValidatedResult<Model> load(String traitUse) {
        return Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\nnamespace ns\n"
                                + traitUse + "\nunion U { a: A }\nstructure A { x: String }\n")
                .assemble();
    }
}
