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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.utils.IoUtils;

public class CfnSchemasTest {
    private static final String DEFINITION = "provider.definition.schema.v1.json";

    private static Schema validationSchema;

    @BeforeAll
    public static void loadSchema() {
        try (InputStream schemaStream = CfnSchemasTest.class.getResourceAsStream(DEFINITION)) {
            validationSchema = SchemaLoader.load(new JSONObject(new JSONTokener(schemaStream)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("resourceSchemaFiles")
    public void validateTestResourceSchema(String resourceSchemaFile) {
        // Validate that all of our ".cfn.json" schemas used for testing
        // pass the definition schema from CloudFormation.
        JSONObject resourceSchema = new JSONObject(IoUtils.readUtf8File(resourceSchemaFile));
        try {
            validationSchema.validate(resourceSchema);
        } catch (ValidationException e) {
            String fileName = resourceSchemaFile.substring(resourceSchemaFile.lastIndexOf("/") + 1);
            fail("Got validation errors for " + fileName + ": " + e.getErrorMessage());
        }
    }

    public static List<String> resourceSchemaFiles() {
        try {
            Path definitionPath = Paths.get(CfnSchemasTest.class.getResource(DEFINITION).toURI());

            // Check for any ".cfn.json" files at or deeper than the
            // validation schema definition.
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
