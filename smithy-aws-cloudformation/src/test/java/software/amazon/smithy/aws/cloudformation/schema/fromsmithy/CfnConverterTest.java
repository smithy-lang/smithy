/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class CfnConverterTest {

    private static Model testService;

    @BeforeAll
    private static void setup() {
        testService = Model.assembler()
                .addImport(CfnConverterTest.class.getResource("test-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @Test
    public void convertsResourcesToCloudFormation() {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(testService);

        assertEquals(result.keySet().size(), 3);
        assertThat(result.keySet(),
                containsInAnyOrder(ListUtils.of(
                        "Smithy::TestService::Bar",
                        "Smithy::TestService::Basil",
                        "Smithy::TestService::FooResource").toArray()));
        for (String resourceTypeName : result.keySet()) {
            String filename = Smithy2Cfn.getFileNameFromResourceType(resourceTypeName);
            // Handle our convention of using ".cfn.json" for schema validation.
            filename = filename.replace(".json", ".cfn.json");
            Node expectedNode = Node.parse(IoUtils.toUtf8String(
                    getClass().getResourceAsStream(filename)));

            ObjectNode generatedResource = result.get(resourceTypeName);
            Node.assertEquals(generatedResource, expectedNode);

            // Assert that the additionalProperties property is set to false,
            // so that this behavior is enforced regardless of other changes.
            assertEquals(generatedResource.expectMember("additionalProperties"), Node.from(false));
        }
    }

    @Test
    public void handlesAwsServiceTraitDefaulting() {
        Model model = Model.assembler()
                .addImport(CfnConverterTest.class.getResource("simple-service-aws.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        CfnConfig config = new CfnConfig();
        config.setService(ShapeId.from("smithy.example#TestService"));
        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(model);

        assertEquals(result.keySet().size(), 1);
        assertThat(result.keySet(), containsInAnyOrder(ListUtils.of("AWS::SomeThing::FooResource").toArray()));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("simple-service-aws.cfn.json")));

        Node.assertEquals(result.get("AWS::SomeThing::FooResource"), expectedNode);
    }

    @Test
    public void usesConfiguredServiceName() {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setServiceName("ExampleService");
        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(testService);

        assertEquals(result.keySet().size(), 3);
        assertThat(result.keySet(),
                containsInAnyOrder(ListUtils.of(
                        "Smithy::ExampleService::Bar",
                        "Smithy::ExampleService::Basil",
                        "Smithy::ExampleService::FooResource").toArray()));
    }

    @Test
    public void handlesDisabledPropertyCaps() {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setDisableCapitalizedProperties(true);
        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(testService);

        assertEquals(result.keySet().size(), 3);
        assertThat(result.keySet(),
                containsInAnyOrder(ListUtils.of(
                        "Smithy::TestService::Bar",
                        "Smithy::TestService::Basil",
                        "Smithy::TestService::FooResource").toArray()));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("disable-caps-fooresource.cfn.json")));

        Node.assertEquals(result.get("Smithy::TestService::FooResource"), expectedNode);
    }

    @Test
    public void resourcePropertiesWithTagsTest() {
        Model model = Model.assembler()
                .addImport(CfnConverterTest.class.getResource("weather.smithy"))
                .addImport(CfnConverterTest.class.getResource("tagging.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("example.weather#Weather"));
        config.setServiceName("Weather");
        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(model);
        assertEquals(1, result.keySet().size());
        result.keySet().contains("Smithy::Weather::City");

        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("weather.cfn.json")));

        Node.assertEquals(result.get("Smithy::Weather::City"), expectedNode);
    }

    @Test
    public void resourcePropertiesWithTagsServiceWideTest() {
        Model model = Model.assembler()
                .addImport(CfnConverterTest.class.getResource("weather-service-wide.smithy"))
                .addImport(CfnConverterTest.class.getResource("tagging.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("example.weather#Weather"));
        config.setServiceName("Weather");
        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(model);
        assertEquals(1, result.keySet().size());
        result.keySet().contains("Smithy::Weather::City");

        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("weather-service-wide.cfn.json")));

        Node.assertEquals(result.get("Smithy::Weather::City"), expectedNode);
    }
}
