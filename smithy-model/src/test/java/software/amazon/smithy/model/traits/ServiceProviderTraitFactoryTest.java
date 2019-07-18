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

package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ServiceProviderTraitFactoryTest {
    @Test
    public void createsTraitsUsingServiceLoader() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        Optional<Trait> maybeTrait = factory.createTrait(
                ShapeId.from("smithy.api#jsonName"), ShapeId.from("ns.qux#foo"), Node.from("hi"));

        assertTrue(maybeTrait.isPresent());
        assertThat(maybeTrait.get(), instanceOf(JsonNameTrait.class));
    }

    @Test
    public void returnsEmptyWhenNoMatchingTraitIsFound() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        Optional<Trait> maybeTrait = factory.createTrait(
                ShapeId.from("missing.baz#foo"), ShapeId.from("ns.qux#foo"), Node.nullNode());
        assertFalse(maybeTrait.isPresent());
    }
}
