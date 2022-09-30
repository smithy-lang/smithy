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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;

class GetAttrTest {
    @Test
    void getAttrManualEqualToTemplate() {
        Expression asTemplate = Expression.parseShortform("a#b[5]", SourceLocation.none());
        Expression asGetAttr = GetAttr.fromNode(ObjectNode
                .builder()
                .withMember("fn", Node.from("getAttr"))
                .withMember("argv", Node.fromNodes(
                        ObjectNode.builder().withMember("ref", "a").build(),
                        Node.from("b[5]"))
                ).build());
        assertEquals(asTemplate, asGetAttr);
        assertEquals(asTemplate.hashCode(), asGetAttr.hashCode());
    }

}
