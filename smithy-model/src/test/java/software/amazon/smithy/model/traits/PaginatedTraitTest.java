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
import org.junit.jupiter.api.Test;
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
    public void loadsFullyConfiguredTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = Node.objectNode()
                .withMember("items", Node.from("items"))
                .withMember("inputToken", Node.from("inputToken"))
                .withMember("outputToken", Node.from("outputToken"))
                .withMember("pageSize", Node.from("pageSize"));

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#paginated"), ShapeId.from("ns.qux#foo"), node);
        assertThat(trait.isPresent(), is(true));
        assertThat(trait.get(), instanceOf(PaginatedTrait.class));
        PaginatedTrait paginatedTrait = (PaginatedTrait) trait.get();
        assertThat(paginatedTrait.getItems(), equalTo(Optional.of("items")));
        assertThat(paginatedTrait.getInputToken(), equalTo(Optional.of("inputToken")));
        assertThat(paginatedTrait.getOutputToken(), equalTo(Optional.of("outputToken")));
        assertThat(paginatedTrait.getPageSize(), is(Optional.of("pageSize")));
        assertThat(paginatedTrait.toNode(), equalTo(node));
        assertThat(paginatedTrait.toBuilder().build(), equalTo(paginatedTrait));
    }

    @Test
    public void allowsMissingPageSize() {
        TraitFactory provider = TraitFactory.createServiceFactory();

        assertThat(provider.createTrait(
                ShapeId.from("smithy.api#paginated"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode()
                        .withMember("items", Node.from("items"))
                        .withMember("inputToken", Node.from("inputToken"))
                        .withMember("outputToken", Node.from("outputToken"))).isPresent(), is(true));
    }

    @Test
    public void mergesWithNullReturnsSelf() {
        PaginatedTrait trait = PaginatedTrait.builder().build();

        assertThat(trait.merge(null), is(trait));
    }

    @Test
    public void mergesWithOtherTrait() {
        PaginatedTrait trait1 = PaginatedTrait.builder()
                .inputToken("foo")
                .items("baz")
                .build();
        PaginatedTrait trait2 = PaginatedTrait.builder()
                .inputToken("foo2")
                .outputToken("output")
                .items("baz2")
                .pageSize("bar")
                .items("qux")
                .build();
        PaginatedTrait trait3 = trait1.merge(trait2);

        assertThat(trait3.getInputToken(), equalTo(Optional.of("foo")));
        assertThat(trait3.getOutputToken(), equalTo(Optional.of("output")));
        assertThat(trait3.getPageSize(), equalTo(Optional.of("bar")));
        assertThat(trait3.getItems(), equalTo(Optional.of("baz")));
    }

    @Test
    public void allowsNestedOutputToken() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = Node.objectNode()
                .withMember("inputToken", Node.from("inputToken"))
                .withMember("outputToken", Node.from("result.outputToken"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#paginated"), ShapeId.from("ns.qux#foo"), node);

        assertThat(trait.isPresent(), is(true));
        assertThat(trait.get(), instanceOf(PaginatedTrait.class));
        PaginatedTrait paginatedTrait = (PaginatedTrait) trait.get();
        assertThat(paginatedTrait.getOutputToken(), equalTo(Optional.of("result.outputToken")));
    }

    @Test
    public void allowsNestedOutputItems() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = Node.objectNode()
                .withMember("items", Node.from("result.items"))
                .withMember("inputToken", Node.from("inputToken"))
                .withMember("outputToken", Node.from("outputToken"));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#paginated"), ShapeId.from("ns.qux#foo"), node);

        assertThat(trait.isPresent(), is(true));
        assertThat(trait.get(), instanceOf(PaginatedTrait.class));
        PaginatedTrait paginatedTrait = (PaginatedTrait) trait.get();
        assertThat(paginatedTrait.getItems(), equalTo(Optional.of("result.items")));
    }
}
