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
