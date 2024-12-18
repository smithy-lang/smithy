/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnConverter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class RequiredMapperTest {

    @Test
    public void addsRequiredPropertiesByDefault() {
        Model model = Model.assembler()
                .addImport(RequiredMapperTest.class.getResource("simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));

        ObjectNode resourceNode = CfnConverter.create()
                .config(config)
                .convertToNodes(model)
                .get("Smithy::TestService::FooResource");

        Assertions.assertEquals(ListUtils.of("FooRequiredProperty"),
                resourceNode.expectArrayMember("required")
                        .getElementsAs(StringNode::getValue));
    }

    @Test
    public void canDisableRequiredPropertyGeneration() {
        Model model = Model.assembler()
                .addImport(RequiredMapperTest.class.getResource("simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setDisableRequiredPropertyGeneration(true);

        ObjectNode resourceNode = CfnConverter.create()
                .config(config)
                .convertToNodes(model)
                .get("Smithy::TestService::FooResource");

        assertFalse(resourceNode.getMember("required").isPresent());
    }

}
