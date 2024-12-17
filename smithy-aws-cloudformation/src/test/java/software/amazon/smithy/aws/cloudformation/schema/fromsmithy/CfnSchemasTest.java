/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.loader.SchemaLoader.SchemaLoaderBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.utils.IoUtils;

public class CfnSchemasTest {
    private static final String JSON_SCHEMA_URI_HTTP = "http://json-schema.org/draft-07/schema";
    private static final String JSON_SCHEMA = "draft-07-schema";
    private static final String DEFINITION = "provider.definition.schema.v1.json";
    private static final String CONFIGURATION = "provider.configuration.definition.schema.v1.json";
    private static final String CONFIGURATION_URI =
            "https://schema.cloudformation.us-east-1.amazonaws.com/provider.configuration.definition.schema.v1.json";
    private static final String BASE_DEFINITION_URI =
            "https://schema.cloudformation.us-east-1.amazonaws.com/base.definition.schema.v1.json";
    private static final String BASE_DEFINITION = "base.definition.schema.v1.json";

    private static Schema validationSchema;

    @BeforeAll
    public static void loadSchema() {
        try (InputStream schemaStream = CfnSchemasTest.class.getResourceAsStream(DEFINITION);
                InputStream draftSchemaStream = CfnSchemasTest.class.getResourceAsStream(JSON_SCHEMA);
                InputStream configSchemaStream = CfnSchemasTest.class.getResourceAsStream(CONFIGURATION);
                InputStream baseSchemaStream = CfnSchemasTest.class.getResourceAsStream(BASE_DEFINITION)) {

            JSONObject schemaJson = new JSONObject(new JSONTokener(schemaStream));
            JSONObject baseSchemaJson = new JSONObject(new JSONTokener(baseSchemaStream));
            JSONObject configSchemaJson = new JSONObject(new JSONTokener(configSchemaStream));
            JSONObject draftSchemaJson = new JSONObject(new JSONTokener(draftSchemaStream));

            SchemaLoaderBuilder schemaLoaderBuilder = SchemaLoader.builder()
                    .draftV7Support()
                    .schemaJson(schemaJson);
            schemaLoaderBuilder.registerSchemaByURI(new URI(BASE_DEFINITION_URI), baseSchemaJson);
            schemaLoaderBuilder.registerSchemaByURI(new URI(CONFIGURATION_URI), configSchemaJson);
            schemaLoaderBuilder.registerSchemaByURI(new URI(JSON_SCHEMA_URI_HTTP), draftSchemaJson);

            validationSchema = schemaLoaderBuilder.build().load().build();
        } catch (IOException | URISyntaxException e) {
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
