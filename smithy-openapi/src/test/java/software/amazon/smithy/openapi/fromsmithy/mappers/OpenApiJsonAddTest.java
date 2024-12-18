/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class OpenApiJsonAddTest {

    private static Model MODEL;

    @BeforeAll
    public static void before() {
        MODEL = Model.assembler()
                // Reusing another test cases's model, but that doesn't matter for the
                // purpose of this test.
                .addImport(RemoveUnusedComponentsTest.class.getResource("substitutions.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @Test
    public void addsWithPointers() {
        ObjectNode addNode = Node.objectNodeBuilder()
                .withMember("/info/description", "hello")
                .withMember("/info/foo", "bar")
                .withMember("/info/nested/abc", "nested")
                .withMember("/info/title", "custom")
                .build();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setJsonAdd(addNode.getStringMap());

        ObjectNode openApi = OpenApiConverter.create()
                .config(config)
                .convertToNode(MODEL);

        String description = NodePointer.parse("/info/description").getValue(openApi).expectStringNode().getValue();
        String infoFoo = NodePointer.parse("/info/foo").getValue(openApi).expectStringNode().getValue();
        String infoNested = NodePointer.parse("/info/nested/abc").getValue(openApi).expectStringNode().getValue();
        String infoTitle = NodePointer.parse("/info/title").getValue(openApi).expectStringNode().getValue();

        Assertions.assertEquals("hello", description);
        Assertions.assertEquals("bar", infoFoo);
        Assertions.assertEquals("nested", infoNested);
        Assertions.assertEquals("custom", infoTitle);
    }

    private static final class SearchingHandler extends Handler {
        boolean found;
        String searchString;

        SearchingHandler(String searchString) {
            this.searchString = searchString;
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getMessage().contains(searchString)) {
                found = true;
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
    }

    @Test
    public void warnsWhenAddingSchemas() {
        Logger logger = Logger.getLogger(OpenApiJsonAdd.class.getName());
        SearchingHandler handler = new SearchingHandler("Adding schemas to the generated OpenAPI model directly");
        logger.addHandler(handler);

        ObjectNode addNode = Node.objectNode()
                .withMember("/components/schemas/Merged", Node.objectNode().withMember("type", "string"));

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setJsonAdd(addNode.getStringMap());
        ObjectNode openApi = OpenApiConverter.create().config(config).convertToNode(MODEL);
        NodePointer.parse("/components/schemas/Merged").getValue(openApi).expectObjectNode();
        logger.removeHandler(handler);

        Assertions.assertTrue(handler.found);
    }
}
