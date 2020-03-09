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

package software.amazon.smithy.build.transforms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

public class ExcludeMetadataTest {
    @Test
    public void removesMetadataInList() {
        Map<String, Node> metadata = new HashMap<>();
        metadata.put("a", Node.arrayNode());
        metadata.put("b", Node.objectNode().withMember("foo", Node.objectNode()));
        Model model = Model.builder()
                .metadata(metadata)
                .build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("keys", Node.fromStrings("a")))
                .build();
        Model result = new ExcludeMetadata().transform(context);

        assertFalse(result.getMetadata().containsKey("a"));
        assertTrue(result.getMetadata().containsKey("b"));
    }
}
