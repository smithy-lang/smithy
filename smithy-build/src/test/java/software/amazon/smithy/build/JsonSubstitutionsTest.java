/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class JsonSubstitutionsTest {
    @Test
    public void replacesValuesInObjectNode() {
        Map<String, Node> mappings = new HashMap<>();
        mappings.put("FOO_BAR", Node.from("Hi!"));
        JsonSubstitutions substitutions = JsonSubstitutions.create(mappings);
        ObjectNode node = Node.parse("{\"a\": \"FOO_BAR\", \"b\": \"FOO_BAR baz\"}").expectObjectNode();
        ObjectNode result = substitutions.apply(node).expectObjectNode();

        assertThat(result.expectMember("a").expectStringNode().getValue(), equalTo("Hi!"));
        assertThat(result.expectMember("b").expectStringNode().getValue(), equalTo("FOO_BAR baz"));
    }
}
