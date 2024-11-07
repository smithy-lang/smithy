/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.utils.IoUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceSchemaTest {

    @ParameterizedTest
    @MethodSource("resourceSchemaFiles")
    public void validateResourceSchemaFromNodeToNode(String resourceSchemaFile) {
        NodeMapper mapper = new NodeMapper();
        String json = IoUtils.readUtf8File(resourceSchemaFile);

        Node node = Node.parse(json);
        ResourceSchema schemaFromNode = mapper.deserialize(node, ResourceSchema.class);
        Node nodeFromSchema = schemaFromNode.toNode();

        Node.assertEquals(nodeFromSchema.withDeepSortedKeys(), node.withDeepSortedKeys());
    }

    public static List<String> resourceSchemaFiles() {
        try {
            Path definitionPath = Paths.get(ResourceSchemaTest.class.getResource("aws-sagemaker-domain.json").toURI());

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
