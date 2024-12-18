/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ExternalDocumentationTraitTest {
    @Test
    public void loadsTrait() {
        Node node = Node.parse("{\"API Reference\": \"https://foo.bar/api\","
                + "\"Usage Guide\": \"https://foo.bar/guide\"}");
        ExternalDocumentationTrait trait = new ExternalDocumentationTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#baz"), node);

        assertThat(trait.toNode(), equalTo(node));
        assertThat(trait.toBuilder().build(), equalTo(trait));
        assertThat(trait.getUrls(), hasKey("API Reference"));
        assertThat(trait.getUrls().get("API Reference"), equalTo("https://foo.bar/api"));
        assertThat(trait.getUrls(), hasKey("Usage Guide"));
        assertThat(trait.getUrls().get("Usage Guide"), equalTo("https://foo.bar/guide"));
    }

    @Test
    public void expectsValidUrls() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            provider.createTrait(ShapeId.from("smithy.api#externalDocumentation"),
                    ShapeId.from("ns.qux#foo"),
                    Node.parse("{\"API Reference\": \"foobarapi\""));
        });
    }
}
