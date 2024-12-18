/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnConverter;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class DocumentationMapperTest {

    private static Model model;

    @BeforeAll
    public static void loadModel() {
        model = Model.assembler()
                .addImport(DocumentationMapperTest.class.getResource("simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @Test
    public void supportsExternalDocumentationUrls() {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));

        ResourceSchema schema = getBarSchema(config);
        assertNotNull(schema);
        assertTrue(schema.getDocumentationUrl().isPresent());
        assertEquals("https://docs.example.com", schema.getDocumentationUrl().get());
        assertTrue(schema.getSourceUrl().isPresent());
        assertEquals("https://source.example.com", schema.getSourceUrl().get());
    }

    @Test
    public void supportsCustomExternalDocNames() {
        CfnConfig config = new CfnConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setExternalDocs(ListUtils.of("main"));
        config.setSourceDocs(ListUtils.of("code"));

        ResourceSchema schema = getBarSchema(config);
        assertNotNull(schema);
        assertTrue(schema.getDocumentationUrl().isPresent());
        assertEquals("https://docs2.example.com", schema.getDocumentationUrl().get());
        assertTrue(schema.getSourceUrl().isPresent());
        assertEquals("https://source2.example.com", schema.getSourceUrl().get());
    }

    private ResourceSchema getBarSchema(CfnConfig config) {
        for (ResourceSchema schema : CfnConverter.create().config(config).convert(model)) {
            if (schema.getTypeName().endsWith("FooResource")) {
                return schema;
            }
        }
        return null;
    }
}
