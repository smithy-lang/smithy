/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class CapturingIntegration implements TestIntegration {
    public ObjectNode integrationSettings = Node.objectNode();

    @Override
    public String name() {
        return "capturing-integration";
    }

    @Override
    public void configure(TestSettings settings, ObjectNode integrationSettings) {
        this.integrationSettings = integrationSettings;
    }
}
