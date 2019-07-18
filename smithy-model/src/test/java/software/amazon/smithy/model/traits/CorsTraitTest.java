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

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class CorsTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("origin", Node.from("https://www.amazon.com"))
                .withMember("maxAge", Node.from(86400))
                .withMember("additionalAllowedHeaders", Node.fromStrings("foo", "bar"))
                .withMember("additionalExposedHeaders", Node.fromStrings("fizz", "buzz"));
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#cors"), ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(CorsTrait.class));
        CorsTrait cors = (CorsTrait) trait.get();

        assertThat(cors.getOrigin(), equalTo("https://www.amazon.com"));
        assertThat(cors.getMaxAge(), equalTo(86400));
        assertThat(cors.getAdditionalAllowedHeaders(), equalTo(Stream.of("foo", "bar").collect(Collectors.toSet())));
        assertThat(cors.getAdditionalExposedHeaders(), equalTo(Stream.of("fizz", "buzz").collect(Collectors.toSet())));
    }

    @Test
    public void injectsDefaults() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#cors"), ShapeId.from("ns.qux#foo"), Node.objectNode());
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(CorsTrait.class));
        CorsTrait cors = (CorsTrait) trait.get();

        assertThat(cors.getOrigin(), equalTo("*"));
        assertThat(cors.getMaxAge(), equalTo(600));
        assertThat(cors.getAdditionalAllowedHeaders(), equalTo(Collections.emptySet()));
        assertThat(cors.getAdditionalExposedHeaders(), equalTo(Collections.emptySet()));
    }

    @Test
    public void omitsDefaultsFromSerializedNode() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("origin", Node.from("*"))
                .withMember("maxAge", Node.from(600))
                .withMember("additionalAllowedHeaders", Node.fromStrings())
                .withMember("additionalExposedHeaders", Node.fromStrings());
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#cors"), ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(CorsTrait.class));
        ObjectNode serialized = ((CorsTrait) trait.get()).createNode().expectObjectNode();

        assertFalse(serialized.getMember("origin").isPresent());
        assertFalse(serialized.getMember("maxAge").isPresent());
        assertFalse(serialized.getMember("additionalAllowedHeaders").isPresent());
        assertFalse(serialized.getMember("additionalExposedHeaders").isPresent());
    }
}
