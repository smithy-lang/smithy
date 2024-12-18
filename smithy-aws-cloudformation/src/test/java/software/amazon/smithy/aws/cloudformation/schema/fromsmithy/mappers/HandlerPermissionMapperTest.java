/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnConverter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

public final class HandlerPermissionMapperTest {

    private static Model model;

    @BeforeAll
    public static void loadModel() {
        model = Model.assembler()
                .addImport(DocumentationMapperTest.class.getResource("simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    private ObjectNode getResourceByName(String resourceName) {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        Map<String, ObjectNode> resourceNodes = CfnConverter.create().config(config).convertToNodes(model);
        return resourceNodes.get(resourceName);
    }

    @Test
    public void addsCRUHandlerPermissionsByDefault() {
        ObjectNode fooResourceNode = getResourceByName("Smithy::TestService::FooResource");
        Map<String, Node> handlersDefined = fooResourceNode.expectObjectMember("handlers").getStringMap();
        Assertions.assertEquals(3, handlersDefined.size());
        assertThat(handlersDefined.keySet(), containsInAnyOrder("create", "read", "update"));

        assertThat(handlersDefined.get("create")
                .expectObjectNode()
                .expectArrayMember("permissions")
                .getElementsAs(StringNode::getValue),
                containsInAnyOrder("testservice:CreateFooOperation", "otherservice:DescribeDependencyComponent"));
        assertThat(handlersDefined.get("read")
                .expectObjectNode()
                .expectArrayMember("permissions")
                .getElementsAs(StringNode::getValue),
                containsInAnyOrder("testservice:GetFooOperation", "otherservice:DescribeThing"));
        assertThat(handlersDefined.get("update")
                .expectObjectNode()
                .expectArrayMember("permissions")
                .getElementsAs(StringNode::getValue),
                contains("testservice:UpdateFooOperation"));
    }

    @Test
    public void addsPutHandlerPermissionsByDefault() {
        ObjectNode barResourceNode = getResourceByName("Smithy::TestService::BarResource");
        Map<String, Node> handlersDefined = barResourceNode.expectObjectMember("handlers").getStringMap();
        Assertions.assertEquals(2, handlersDefined.size());
        assertThat(handlersDefined.keySet(), containsInAnyOrder("create", "update"));

        assertThat(handlersDefined.get("create")
                .expectObjectNode()
                .expectArrayMember("permissions")
                .getElementsAs(StringNode::getValue),
                contains("testservice:CreateBar"));
        assertThat(handlersDefined.get("update")
                .expectObjectNode()
                .expectArrayMember("permissions")
                .getElementsAs(StringNode::getValue),
                contains("testservice:CreateBar"));
    }

    @Test
    public void addsPutWithNoReplaceHandlerPermissionsByDefault() {
        ObjectNode bazResourceNode = getResourceByName("Smithy::TestService::BazResource");
        Map<String, Node> handlersDefined = bazResourceNode.expectObjectMember("handlers").getStringMap();
        Assertions.assertEquals(1, handlersDefined.size());
        assertThat(handlersDefined.keySet(), contains("create"));

        assertThat(handlersDefined.get("create")
                .expectObjectNode()
                .expectArrayMember("permissions")
                .getElementsAs(StringNode::getValue),
                contains("testservice:CreateBaz"));
    }

    @Test
    public void canDisableHandlerPermissionsGeneration() {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setDisableHandlerPermissionGeneration(true);

        ObjectNode resourceNode = CfnConverter.create()
                .config(config)
                .convertToNodes(model)
                .get("Smithy::TestService::FooResource");

        assertFalse(resourceNode.getMember("handlers").isPresent());
    }
}
