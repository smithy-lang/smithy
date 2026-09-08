/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class UnstableTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#unstable"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode());

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(UnstableTrait.class));
        assertFalse(((UnstableTrait) trait.get()).getFeatureId().isPresent());
        assertThat(trait.get().toNode(), equalTo(Node.objectNode()));
    }

    @Test
    public void loadsTraitWithFeatureId() {
        Node node = Node.parse("{\"featureId\": \"MYSERVICE_FIRST_PREVIEW\"}");
        UnstableTrait trait = new UnstableTrait.Provider()
                .createTrait(ShapeId.from("ns.qux#foo"), node);

        assertThat(trait.getFeatureId().get(), equalTo("MYSERVICE_FIRST_PREVIEW"));
        assertThat(trait.toNode(), equalTo(node));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void convertsToBuilder() {
        UnstableTrait trait = UnstableTrait.builder()
                .featureId("MYSERVICE_FIRST_PREVIEW")
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void supportsNoArgConstructor() {
        assertFalse(new UnstableTrait().getFeatureId().isPresent());
        assertThat(new UnstableTrait().toNode(), equalTo(Node.objectNode()));

        ObjectNode node = Node.objectNode().withMember("featureId", "MYSERVICE_FIRST_PREVIEW");
        UnstableTrait trait = new UnstableTrait(node);

        assertThat(trait.getFeatureId().get(), equalTo("MYSERVICE_FIRST_PREVIEW"));
        assertThat(trait.toNode(), equalTo(node));
    }
}
