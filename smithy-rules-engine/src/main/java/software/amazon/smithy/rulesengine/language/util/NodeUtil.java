/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.util;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

public final class NodeUtil {
    private NodeUtil() {
    }

    public static <K extends ToNode, V extends ToNode> Node mapToNode(Map<K, V> input) {
        ObjectNode.Builder builder = ObjectNode.builder();
        input.forEach((k, v) -> {
            builder.withMember(k.toNode().expectStringNode(), v);
        });
        return builder.build();
    }

    public static <T extends ToNode> Node listToNode(List<T> input) {
        ArrayNode.Builder builder = ArrayNode.builder();
        input.forEach(el -> {
            builder.withValue(el.toNode());
        });
        return builder.build();
    }
}
