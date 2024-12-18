/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;

public class TestRunnerTest {

    @ParameterizedTest
    @MethodSource("integFiles")
    public void generatesResources(String modelFile) {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(modelFile)
                .assemble()
                .unwrap();

        CfnConfig config = new CfnConfig();
        config.setService(ShapeId.from("smithy.example#TestService"));

        // Handle @service trait defaulting setup.
        if (!modelFile.endsWith("-aws.smithy")) {
            config.setOrganizationName("Smithy");
        }

        Map<String, ObjectNode> result = CfnConverter.create()
                .config(config)
                .convertToNodes(model);
        Node expectedNode = Node.parse(IoUtils.readUtf8File(modelFile.replace(".smithy", ".cfn.json")));

        // Assert that we got one resource and that it matches
        assertEquals(result.keySet().size(), 1);
        Node.assertEquals(result.get(result.keySet().iterator().next()), expectedNode);
    }

    public static List<String> integFiles() {
        try {
            return Files.walk(Paths.get(TestRunnerTest.class.getResource("integ").toURI()))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".smithy"))
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
