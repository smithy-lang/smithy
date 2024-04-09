/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class RequestCompressionTraitTest {
    @Test
    public void loadsTrait() {
        Node node = ObjectNode.builder()
                .withMember("encodings", ArrayNode.fromStrings("gzip"))
                .build();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#requestCompression"), ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(RequestCompressionTrait.class));
        RequestCompressionTrait requestCompressionTrait = (RequestCompressionTrait) trait.get();
        assertFalse(requestCompressionTrait.getEncodings().isEmpty());
        assertThat(requestCompressionTrait.getEncodings().size(), equalTo(1));
        assertThat(requestCompressionTrait.getEncodings().get(0), equalTo("gzip"));
        assertThat(requestCompressionTrait.toNode(), equalTo(node));
        assertThat(requestCompressionTrait.toBuilder().build(), equalTo(requestCompressionTrait));
    }
}
