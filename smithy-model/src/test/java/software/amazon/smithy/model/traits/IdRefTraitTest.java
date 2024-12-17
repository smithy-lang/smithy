/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class IdRefTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("selector", Node.from("integer"))
                .withMember("failWhenMissing", Node.from(true))
                .withMember("errorMessage", Node.from("foo"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#idRef"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(IdRefTrait.class));
        IdRefTrait idRef = (IdRefTrait) trait.get();

        assertThat(idRef.getErrorMessage(), equalTo(Optional.of("foo")));
        assertThat(idRef.getSelector().toString(), equalTo("integer"));
        assertThat(idRef.failWhenMissing(), is(true));
        assertThat(idRef.toBuilder().build(), equalTo(idRef));
        Trait duplicateTrait = provider.createTrait(
                ShapeId.from("smithy.api#idRef"),
                ShapeId.from("ns.qux#foo"),
                idRef.toNode()).get();
        assertThat(duplicateTrait, equalTo(idRef));
    }
}
