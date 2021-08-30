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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class HttpApiKeyAuthTraitTest {
    @Test
    public void loadsTraitWithHeader() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("name", "X-Foo")
                .withMember("in", "header");
        Optional<Trait> trait = provider.createTrait(
                HttpApiKeyAuthTrait.ID, ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpApiKeyAuthTrait.class));
        HttpApiKeyAuthTrait auth = (HttpApiKeyAuthTrait) trait.get();

        assertFalse(auth.getScheme().isPresent());
        assertThat(auth.getName(), equalTo("X-Foo"));
        assertThat(auth.getIn(), equalTo(HttpApiKeyAuthTrait.Location.HEADER));
        assertThat(auth.toNode(), equalTo(node));
        assertThat(auth.toBuilder().build(), equalTo(auth));
    }

    @Test
    public void loadsTraitWithQuery() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("name", "blerg")
                .withMember("in", "query");
        Optional<Trait> trait = provider.createTrait(
                HttpApiKeyAuthTrait.ID, ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpApiKeyAuthTrait.class));
        HttpApiKeyAuthTrait auth = (HttpApiKeyAuthTrait) trait.get();

        assertFalse(auth.getScheme().isPresent());
        assertThat(auth.getName(), equalTo("blerg"));
        assertThat(auth.getIn(), equalTo(HttpApiKeyAuthTrait.Location.QUERY));
        assertThat(auth.toNode(), equalTo(node));
        assertThat(auth.toBuilder().build(), equalTo(auth));
    }

    @Test
    public void loadsTraitWithHeaderAndScheme() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("scheme", "fenty")
                .withMember("name", "X-Foo")
                .withMember("in", "header");
        Optional<Trait> trait = provider.createTrait(
                HttpApiKeyAuthTrait.ID, ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpApiKeyAuthTrait.class));
        HttpApiKeyAuthTrait auth = (HttpApiKeyAuthTrait) trait.get();

        assertThat(auth.getScheme().get(), equalTo("fenty"));
        assertThat(auth.getName(), equalTo("X-Foo"));
        assertThat(auth.getIn(), equalTo(HttpApiKeyAuthTrait.Location.HEADER));
        assertThat(auth.toNode(), equalTo(node));
        assertThat(auth.toBuilder().build(), equalTo(auth));
    }
}
