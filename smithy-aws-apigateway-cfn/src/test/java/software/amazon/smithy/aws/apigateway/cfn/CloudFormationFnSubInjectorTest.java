/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class CloudFormationFnSubInjectorTest {

    @Test
    public void substitutesIntegrationUri() {
        ObjectNode input = buildIntegrationNode("uri", "${MyLambda.Arn}");
        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();

        assertThat(getIntegrationField(result, "uri"),
                equalTo(Node.objectNode().withMember("Fn::Sub", "${MyLambda.Arn}")));
    }

    @Test
    public void substitutesIntegrationCredentials() {
        ObjectNode input = buildIntegrationNode("credentials", "${Role.Arn}");
        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();

        assertThat(getIntegrationField(result, "credentials"),
                equalTo(Node.objectNode().withMember("Fn::Sub", "${Role.Arn}")));
    }

    @Test
    public void substitutesIntegrationTarget() {
        ObjectNode input = buildIntegrationNode("integrationTarget", "${ALB.Arn}");
        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();

        assertThat(getIntegrationField(result, "integrationTarget"),
                equalTo(Node.objectNode().withMember("Fn::Sub", "${ALB.Arn}")));
    }

    @Test
    public void doesNotSubstituteWithoutPattern() {
        ObjectNode input = buildIntegrationNode("uri", "https://example.com");
        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();

        assertThat(getIntegrationField(result, "uri"),
                equalTo(Node.from("https://example.com")));
    }

    @Test
    public void doesNotSubstituteAtNonKnownPath() {
        // "type" is not a known path for substitution
        ObjectNode input = buildIntegrationNode("type", "${SomeVar}");
        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();

        assertThat(getIntegrationField(result, "type"),
                equalTo(Node.from("${SomeVar}")));
    }

    @Test
    public void wildcardMatchesAnyShapeId() {
        // Different shape IDs should all match the wildcard
        ObjectNode input = buildShapesNode("com.foo#Op1", "uri", "${Lambda.Arn}");
        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();

        Node uri = result.expectObjectMember("shapes")
                .expectObjectMember("com.foo#Op1")
                .expectObjectMember("traits")
                .expectObjectMember("aws.apigateway#integration")
                .expectMember("uri");
        assertThat(uri, equalTo(Node.objectNode().withMember("Fn::Sub", "${Lambda.Arn}")));
    }

    @Test
    public void preservesNonStringNodes() {
        ObjectNode input = Node.objectNodeBuilder()
                .withMember("shapes",
                        Node.objectNodeBuilder()
                                .withMember("ns#Op",
                                        Node.objectNodeBuilder()
                                                .withMember("traits",
                                                        Node.objectNodeBuilder()
                                                                .withMember("aws.apigateway#integration",
                                                                        Node.objectNodeBuilder()
                                                                                .withMember("uri", "${Fn.Arn}")
                                                                                .withMember("timeoutInMillis", 29000)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build())
                .build();

        ObjectNode result = input.accept(new CloudFormationFnSubInjector()).expectObjectNode();
        Node timeout = result.expectObjectMember("shapes")
                .expectObjectMember("ns#Op")
                .expectObjectMember("traits")
                .expectObjectMember("aws.apigateway#integration")
                .expectMember("timeoutInMillis");
        assertThat(timeout, equalTo(Node.from(29000)));
    }

    private static ObjectNode buildIntegrationNode(String field, String value) {
        return buildShapesNode("ns#Op", field, value);
    }

    private static ObjectNode buildShapesNode(String shapeId, String field, String value) {
        return Node.objectNodeBuilder()
                .withMember("shapes",
                        Node.objectNodeBuilder()
                                .withMember(shapeId,
                                        Node.objectNodeBuilder()
                                                .withMember("traits",
                                                        Node.objectNodeBuilder()
                                                                .withMember("aws.apigateway#integration",
                                                                        Node.objectNodeBuilder()
                                                                                .withMember(field, value)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build())
                .build();
    }

    private static Node getIntegrationField(ObjectNode root, String field) {
        return root.expectObjectMember("shapes")
                .expectObjectMember("ns#Op")
                .expectObjectMember("traits")
                .expectObjectMember("aws.apigateway#integration")
                .expectMember(field);
    }
}
