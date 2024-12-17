/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.IoUtils;

public class ResourceSchemaTest {

    @ParameterizedTest
    @MethodSource("resourceSchemaFiles")
    public void validateResourceSchemaFromNodeToNode(String resourceSchemaFile) {
        String json = IoUtils.readUtf8File(resourceSchemaFile);

        Node node = Node.parse(json);
        ResourceSchema schemaFromNode = ResourceSchema.fromNode(node);
        Node nodeFromSchema = schemaFromNode.toNode();

        Node.assertEquals(nodeFromSchema, node);
    }

    public static List<String> resourceSchemaFiles() {
        try {
            Path definitionPath =
                    Paths.get(ResourceSchemaTest.class.getResource("aws-sagemaker-domain.cfn.json").toURI());

            return Files.walk(Paths.get(definitionPath.getParent().toUri()))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".cfn.json"))
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
