/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class WaiterTest {
    @Test
    public void setsDefaultValuesForMinAndMaxDelay() {
        Matcher<?> matcher = new Matcher.SuccessMember(true);
        Acceptor a1 = new Acceptor(AcceptorState.SUCCESS, matcher);
        Waiter waiter = Waiter.builder().addAcceptor(a1).build();

        assertThat(waiter.getMinDelay(), is(2));
        assertThat(waiter.getMaxDelay(), is(120));
    }

    @Test
    public void doesNotIncludeDefaultValuesInNode() {
        Matcher<?> matcher = new Matcher.SuccessMember(true);
        Acceptor a1 = new Acceptor(AcceptorState.SUCCESS, matcher);
        Waiter waiter = Waiter.builder().addAcceptor(a1).build();
        ObjectNode node = waiter.toNode().expectObjectNode();

        assertThat(node.getMember("minDelay"), equalTo(Optional.empty()));
        assertThat(node.getMember("maxDelay"), equalTo(Optional.empty()));

        assertThat(waiter.toBuilder().build(), equalTo(waiter));
        assertThat(Waiter.fromNode(waiter.toNode()), equalTo(waiter));
    }

    @Test
    public void includesMinDelayAndMaxDelayInNodeIfNotDefaults() {
        Matcher<?> matcher = new Matcher.SuccessMember(true);
        Acceptor a1 = new Acceptor(AcceptorState.SUCCESS, matcher);
        Waiter waiter = Waiter.builder()
                .minDelay(10)
                .maxDelay(100)
                .addAcceptor(a1)
                .build();
        ObjectNode node = waiter.toNode().expectObjectNode();

        assertThat(waiter.getMinDelay(), is(10));
        assertThat(waiter.getMaxDelay(), is(100));
        assertThat(node.getMember("minDelay"), equalTo(Optional.of(Node.from(10))));
        assertThat(node.getMember("maxDelay"), equalTo(Optional.of(Node.from(100))));

        assertThat(waiter.toBuilder().build(), equalTo(waiter));
        assertThat(Waiter.fromNode(waiter.toNode()), equalTo(waiter));
    }

    @Test
    public void loadsAndPersistsWaiters() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("errorfiles/valid-waiters.smithy"))
                .assemble()
                .unwrap();
        Shape shape = model.expectShape(ShapeId.from("smithy.example#A"));
        WaitableTrait trait = shape.expectTrait(WaitableTrait.class);

        // Test that the individual waiter was loaded correctly.
        assertThat(trait.getWaiters(), hasKey("F"));

        Waiter waiter = trait.getWaiters().get("F");
        assertThat(waiter.isDeprecated(), is(true));
        assertThat(waiter.getTags(), contains("A", "B"));

        // Test that the individual waiter is persisted correctly.
        assertThat(Waiter.fromNode(waiter.toNode()), equalTo(waiter));
        assertThat(waiter.toBuilder().build(), equalTo(waiter));
    }
}
