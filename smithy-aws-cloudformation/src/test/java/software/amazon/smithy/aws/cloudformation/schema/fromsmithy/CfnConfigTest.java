/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.CfnException;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;

public class CfnConfigTest {
    @Test
    public void throwsOnDifferentAlphanumericRefs() {
        CfnConfig config = new CfnConfig();

        assertTrue(config.getAlphanumericOnlyRefs());
        assertThrows(CfnException.class, () -> config.setAlphanumericOnlyRefs(false));
    }

    @Test
    public void throwsOnJsonName() {
        CfnConfig config = new CfnConfig();

        assertThrows(CfnException.class, () -> config.setUseJsonName(true));
        assertThrows(CfnException.class, () -> config.setUseJsonName(false));
    }

    @Test
    public void throwsOnDifferentMapStrategy() {
        CfnConfig config = new CfnConfig();

        assertEquals(config.getMapStrategy(), JsonSchemaConfig.MapStrategy.PATTERN_PROPERTIES);
        assertThrows(CfnException.class, () -> config.setMapStrategy(JsonSchemaConfig.MapStrategy.PROPERTY_NAMES));
    }

    @Test
    public void throwsOnDifferentUnionStrategy() {
        CfnConfig config = new CfnConfig();

        assertEquals(config.getUnionStrategy(), JsonSchemaConfig.UnionStrategy.ONE_OF);
        assertThrows(CfnException.class, () -> config.setUnionStrategy(JsonSchemaConfig.UnionStrategy.OBJECT));
        assertThrows(CfnException.class, () -> config.setUnionStrategy(JsonSchemaConfig.UnionStrategy.STRUCTURE));
    }

    @Test
    public void handlesFromNode() {
        Model model = Model.assembler()
                .addImport(CfnConfigTest.class.getResource("mappers/simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        ObjectNode addNode = Node.objectNodeBuilder()
                .withMember("/arbitrary/foo", "whoa")
                .build();

        ObjectNode configNode = Node.objectNodeBuilder()
                .withMember("organizationName", "Smithy")
                .withMember("service", "smithy.example#TestService")
                .withMember("jsonAdd",
                        Node.objectNodeBuilder()
                                .withMember("smithy.example#FooResource", addNode)
                                .build())
                .build();

        CfnConfig config = CfnConfig.fromNode(configNode);

        ObjectNode resourceNode = CfnConverter.create()
                .config(config)
                .convertToNodes(model)
                .get("Smithy::TestService::FooResource");

        String arbitraryFoo = NodePointer.parse("/arbitrary/foo").getValue(resourceNode).expectStringNode().getValue();

        Assertions.assertEquals("whoa", arbitraryFoo);
    }
}
