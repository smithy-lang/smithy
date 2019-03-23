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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class PaginatedTraitTest {
    @Test
    public void doesNotRequireItems() {
        PaginatedTrait t = new PaginatedTrait.Provider().createTrait(ShapeId.from("ns.qux#foo"), Node.objectNode()
                .withMember("inputToken", Node.from("inputToken"))
                .withMember("outputToken", Node.from("outputToken")));

        assertThat(t.getItems(), equalTo(Optional.empty()));
    }

    @Test
    public void requiresInputToken() {
        Assertions.assertThrows(SourceException.class, () -> {
            new PaginatedTrait.Provider().createTrait(ShapeId.from("ns.qux#foo"), Node.objectNode()
                    .withMember("items", Node.from("value"))
                    .withMember("outputToken", Node.from("outputToken")));
        });
    }

    @Test
    public void requiresOutputToken() {
        Assertions.assertThrows(SourceException.class, () -> {
            new PaginatedTrait.Provider().createTrait(ShapeId.from("ns.qux#foo"), Node.objectNode()
                    .withMember("inputToken", Node.from("value"))
                    .withMember("item", Node.from("value")));
        });
    }

    @Test
    public void loadsFullyConfiguredTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = Node.objectNode()
                .withMember("items", Node.from("items"))
                .withMember("inputToken", Node.from("inputToken"))
                .withMember("outputToken", Node.from("outputToken"))
                .withMember("pageSize", Node.from("pageSize"));

        Optional<Trait> trait = provider.createTrait("smithy.api#paginated", ShapeId.from("ns.qux#foo"), node);
        assertThat(trait.isPresent(), is(true));
        assertThat(trait.get(), instanceOf(PaginatedTrait.class));
        PaginatedTrait paginatedTrait = (PaginatedTrait) trait.get();
        assertThat(paginatedTrait.getItems(), equalTo(Optional.of("items")));
        assertThat(paginatedTrait.getInputToken(), equalTo("inputToken"));
        assertThat(paginatedTrait.getOutputToken(), equalTo("outputToken"));
        assertThat(paginatedTrait.getPageSize(), is(Optional.of("pageSize")));
        assertThat(paginatedTrait.toNode(), equalTo(node));
        assertThat(paginatedTrait.toBuilder().build(), equalTo(paginatedTrait));
    }

    @Test
    public void allowsMissingPageSize() {
        TraitFactory provider = TraitFactory.createServiceFactory();

        assertThat(provider.createTrait("smithy.api#paginated", ShapeId.from("ns.qux#foo"), Node.objectNode()
                .withMember("items", Node.from("items"))
                .withMember("inputToken", Node.from("inputToken"))
                .withMember("outputToken", Node.from("outputToken"))).isPresent(), is(true));
    }
}
