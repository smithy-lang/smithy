/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;

public class EndpointValueTest {
    @Test
    public void loadsFromNode() {
        Value value = Value.endpointValue(Node.objectNode().withMember("url", "https://foo.com"));

        assertEquals("https://foo.com", value.toNode().expectObjectNode().expectStringMember("url").getValue());
    }
}
