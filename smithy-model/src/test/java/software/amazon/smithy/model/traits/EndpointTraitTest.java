/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.pattern.InvalidPatternException;
import software.amazon.smithy.model.shapes.ShapeId;

public class EndpointTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("hostPrefix", Node.from("foo.baz-"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#endpoint"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(EndpointTrait.class));
        EndpointTrait endpoint = (EndpointTrait) trait.get();

        assertThat(endpoint.getHostPrefix().toString(), equalTo("foo.baz-"));
        assertThat(endpoint.toNode(), equalTo(node));
        assertThat(endpoint.toBuilder().build(), equalTo(endpoint));
    }

    @Test
    public void literalsMustNotContainAdjacentLabels() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            EndpointTrait.builder()
                    .hostPrefix("foo-{baz}{bar}")
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Host labels must not be adjacent"));
    }

    @Test
    public void literalsMustNotContainOpenBrace() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            EndpointTrait.builder()
                    .hostPrefix("foo-{baz")
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Unclosed label found in pattern"));
    }

    @Test
    public void literalsMustNotContainTrailingOpenBrace() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            EndpointTrait.builder()
                    .hostPrefix("foo-{")
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Unclosed label found in pattern"));
    }

    @Test
    public void literalsMustNotContainCloseBrace() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            EndpointTrait.builder()
                    .hostPrefix("foo-}baz")
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Literal segments must not contain `}`"));
    }
}
