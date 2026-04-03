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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
                ShapeId.from("smithy.api#cors"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode());
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
        assertFalse(serialized.getMember("origins").isPresent());
        assertFalse(serialized.getMember("maxAge").isPresent());
        assertFalse(serialized.getMember("additionalAllowedHeaders").isPresent());
        assertFalse(serialized.getMember("additionalExposedHeaders").isPresent());
    }

    @Test
    public void loadsOriginsMap() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode originsNode = Node.objectNode()
                .withMember("prod", Node.from("https://api.example.com"))
                .withMember("beta", Node.from("https://beta.example.com"));
        ObjectNode node = Node.objectNode()
                .withMember("origins", originsNode);
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#cors"), ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        CorsTrait cors = (CorsTrait) trait.get();

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("prod", "https://api.example.com");
        expected.put("beta", "https://beta.example.com");
        assertThat(cors.getOrigins(), equalTo(expected));
    }

    @Test
    public void originsDefaultsToEmptyMap() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#cors"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode());
        assertTrue(trait.isPresent());
        CorsTrait cors = (CorsTrait) trait.get();

        assertThat(cors.getOrigins(), equalTo(Collections.emptyMap()));
    }

    @Test
    public void originsRoundTripsViaBuilder() {
        Map<String, String> originsMap = new LinkedHashMap<>();
        originsMap.put("local", "http://localhost:8080");
        originsMap.put("prod", "https://api.example.com");

        CorsTrait trait = CorsTrait.builder()
                .origins(originsMap)
                .build();

        CorsTrait rebuilt = trait.toBuilder().build();
        assertThat(rebuilt.getOrigins(), equalTo(originsMap));
        assertThat(rebuilt, equalTo(trait));
    }

    @Test
    public void originsSerializedWhenNonEmpty() {
        Map<String, String> originsMap = new LinkedHashMap<>();
        originsMap.put("prod", "https://api.example.com");

        CorsTrait trait = CorsTrait.builder()
                .origins(originsMap)
                .build();

        ObjectNode serialized = trait.createNode().expectObjectNode();
        assertTrue(serialized.getMember("origins").isPresent());
        assertThat(
                serialized.getObjectMember("origins").get().getStringMember("prod").get().getValue(),
                equalTo("https://api.example.com"));
    }
}
