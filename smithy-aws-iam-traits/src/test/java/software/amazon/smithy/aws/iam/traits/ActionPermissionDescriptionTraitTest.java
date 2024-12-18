/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class ActionPermissionDescriptionTraitTest {
    @Test
    @SuppressWarnings("deprecation")
    public void createsTrait() {
        Node node = Node.from("Foo baz bar");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ActionPermissionDescriptionTrait.ID,
                ShapeId.from("ns.foo#foo"),
                node);

        assertTrue(trait.isPresent());
        ActionPermissionDescriptionTrait actionDescription = (ActionPermissionDescriptionTrait) trait.get();
        assertThat(actionDescription.getValue(), equalTo("Foo baz bar"));
    }
}
