/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnConverter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;

public class JsonAddTest {
    @Test
    public void addsWithPointers() {
        Model model = Model.assembler()
                .addImport(JsonAddTest.class.getResource("simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        ObjectNode addNode = Node.objectNodeBuilder()
                .withMember("/arbitrary/foo", "whoa")
                .withMember("/arbitrary/bar/baz", "nested")
                .withMember("/documentationUrl", "https://example.com")
                .build();

        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setJsonAdd(MapUtils.of(ShapeId.from("smithy.example#FooResource"), addNode.getStringMap()));

        ObjectNode resourceNode = CfnConverter.create()
                .config(config)
                .convertToNodes(model)
                .get("Smithy::TestService::FooResource");

        String arbitraryFoo = NodePointer.parse("/arbitrary/foo").getValue(resourceNode).expectStringNode().getValue();
        String arbitraryBarBaz =
                NodePointer.parse("/arbitrary/bar/baz").getValue(resourceNode).expectStringNode().getValue();
        String documentationUrl =
                NodePointer.parse("/documentationUrl").getValue(resourceNode).expectStringNode().getValue();

        Assertions.assertEquals("whoa", arbitraryFoo);
        Assertions.assertEquals("nested", arbitraryBarBaz);
        Assertions.assertEquals("https://example.com", documentationUrl);
    }
}
