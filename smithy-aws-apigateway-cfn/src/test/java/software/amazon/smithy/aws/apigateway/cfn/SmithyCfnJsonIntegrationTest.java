/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.cfn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;

public class SmithyCfnJsonIntegrationTest {

    @Test
    public void substitutesIntegrationFieldsInRealModel() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integration-model.smithy"))
                .assemble()
                .unwrap();

        MockManifest manifest = new MockManifest();
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#MyService")
                .build();

        PluginContext context = PluginContext.builder()
                .model(model)
                .fileManifest(manifest)
                .settings(settings)
                .build();

        new SmithyCfnJson().execute(context);

        String output = manifest.getFileString("MyService.smithy.json").get();
        ObjectNode outputNode = Node.parse(output).expectObjectNode();
        ObjectNode integration = outputNode.expectObjectMember("shapes")
                .expectObjectMember("com.example#GetItems")
                .expectObjectMember("traits")
                .expectObjectMember("aws.apigateway#integration");

        // Substituted fields are wrapped in Fn::Sub
        assertThat(integration.expectObjectMember("uri").expectStringMember("Fn::Sub").getValue(),
                equalTo("arn:${AWS::Partition}:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/${MyLambda.Arn}/invocations"));
        assertThat(integration.expectObjectMember("credentials").expectStringMember("Fn::Sub").getValue(),
                equalTo("${ApiGatewayRole.Arn}"));
        assertThat(integration.expectObjectMember("connectionId").expectStringMember("Fn::Sub").getValue(),
                equalTo("${MyVpcLink}"));
        assertThat(integration.expectObjectMember("integrationTarget").expectStringMember("Fn::Sub").getValue(),
                equalTo("${MyALBListener.Arn}"));

        // Non-substituted fields remain as plain strings
        assertThat(integration.expectStringMember("type").getValue(), equalTo("aws_proxy"));
        assertThat(integration.expectStringMember("httpMethod").getValue(), equalTo("POST"));
    }

    @Test
    public void disabledSubstitutionMatchesModelSerializer() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("integration-model.smithy"))
                .assemble()
                .unwrap();

        MockManifest manifest = new MockManifest();
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#MyService")
                .withMember("disableCloudFormationSubstitution", true)
                .build();

        PluginContext context = PluginContext.builder()
                .model(model)
                .fileManifest(manifest)
                .settings(settings)
                .build();

        new SmithyCfnJson().execute(context);

        String output = manifest.getFileString("MyService.smithy.json").get();
        ObjectNode outputNode = Node.parse(output).expectObjectNode();
        ObjectNode expected = ModelSerializer.builder().build().serialize(model);

        Node.assertEquals(outputNode, expected);
    }
}
