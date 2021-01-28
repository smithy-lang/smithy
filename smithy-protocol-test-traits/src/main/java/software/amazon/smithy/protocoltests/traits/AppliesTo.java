/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.protocoltests.traits;

import java.util.Locale;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Indicates that a compliance test case is only to be implemented by
 * "client" or "server" implementations.
 */
public enum AppliesTo implements ToNode {
    CLIENT,
    SERVER;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    public static AppliesTo fromNode(Node node) {
        String value = node.expectStringNode()
                .expectOneOf("client", "server")
                .toUpperCase(Locale.ENGLISH);
        return AppliesTo.valueOf(value);
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }
}
