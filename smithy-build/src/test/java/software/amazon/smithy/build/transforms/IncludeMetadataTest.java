/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

public class IncludeMetadataTest {
    @Test
    public void removesMetadataNotInList() {
        Map<String, Node> metadata = new HashMap<>();
        metadata.put("a", Node.arrayNode());
        metadata.put("b", Node.objectNode().withMember("foo", Node.objectNode()));
        Model model = Model.builder()
                .metadata(metadata)
                .build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("keys", Node.fromStrings("b")))
                .build();
        Model result = new IncludeMetadata().transform(context);

        assertFalse(result.getMetadata().containsKey("a"));
        assertTrue(result.getMetadata().containsKey("b"));
    }
}
