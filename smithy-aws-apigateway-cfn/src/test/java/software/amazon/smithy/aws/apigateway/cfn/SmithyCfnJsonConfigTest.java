/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;

public class SmithyCfnJsonConfigTest {
    @Test
    public void deserializesFullConfig() {
        Node node = Node.parse("{\"service\": \"com.example#MyService\", "
                + "\"disableCloudFormationSubstitution\": true}");
        SmithyCfnJsonConfig config = new NodeMapper().deserialize(node, SmithyCfnJsonConfig.class);

        assertThat(config.getService(), equalTo(ShapeId.from("com.example#MyService")));
        assertThat(config.getDisableCloudFormationSubstitution(), equalTo(true));
    }

    @Test
    public void defaultsDisableToFalse() {
        Node node = Node.parse("{\"service\": \"com.example#MyService\"}");
        SmithyCfnJsonConfig config = new NodeMapper().deserialize(node, SmithyCfnJsonConfig.class);

        assertThat(config.getService(), equalTo(ShapeId.from("com.example#MyService")));
        assertFalse(config.getDisableCloudFormationSubstitution());
    }
}
