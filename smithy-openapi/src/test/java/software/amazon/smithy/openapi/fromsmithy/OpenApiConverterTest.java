/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.openapi.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;

public class OpenApiConverterTest {
    @Test
    public void convertsModelsToOpenApi() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create().convert(model, ShapeId.from("example.rest#RestService"));
        var expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("test-service.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void passesThroughTags() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create()
                .putSetting(OpenApiConstants.OPEN_API_TAGS, true)
                .putSetting(OpenApiConstants.OPEN_API_SUPPORTED_TAGS, Node.fromStrings("foo", "baz"))
                .convert(model, ShapeId.from("smithy.example#Service"));
        var expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("tagged-service.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void requiresProtocolsTrait() {
        var thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("missing-protocols-trait.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create()
                    .convert(model, ShapeId.from("smithy.example#Service"));
        });

        assertThat(thrown.getMessage(), containsString("protocol trait"));
    }

    @Test
    public void mustBeAbleToResolveProtocol() {
        var thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("unable-to-resolve-protocol.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create()
                    .convert(model, ShapeId.from("smithy.example#Service"));
        });

        assertThat(thrown.getMessage(), containsString("Unable to resolve"));
    }

    @Test
    public void mustBeAbleToResolveExplicitProtocol() {
        var thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("unable-to-resolve-protocol.json"))
                    .assemble()
                    .unwrap();
            OpenApiConverter.create()
                    .protocolName("not-found")
                    .convert(model, ShapeId.from("smithy.example#Service"));
        });

        assertThat(thrown.getMessage(), containsString("Unable to resolve"));
    }

    @Test
    public void omitsUnsupportedHttpMethods() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("unsupported-http-method.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        var expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("unsupported-http-method.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void protocolsCanOmitOperations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create()
                .addCustomProtocol(new OpenApiProtocol() {
                    @Override
                    public Pattern getProtocolNamePattern() {
                        // Intercepts every protocol.
                        return Pattern.compile(".+");
                    }

                    @Override
                    public Optional<Operation> createOperation(Context context, OperationShape operation) {
                        // Passes on every operation.
                        return Optional.empty();
                    }
                })
                .convert(model, ShapeId.from("example.rest#RestService"));

        for (var pathItem : result.getPaths().values()) {
            Assertions.assertTrue(pathItem.getGet().isEmpty());
            Assertions.assertTrue(pathItem.getHead().isEmpty());
            Assertions.assertTrue(pathItem.getDelete().isEmpty());
            Assertions.assertTrue(pathItem.getPatch().isEmpty());
            Assertions.assertTrue(pathItem.getPost().isEmpty());
            Assertions.assertTrue(pathItem.getPut().isEmpty());
            Assertions.assertTrue(pathItem.getTrace().isEmpty());
            Assertions.assertTrue(pathItem.getOptions().isEmpty());
        }
    }

    @Test
    public void addsEmptyResponseByDefault() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("adds-empty-response.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));

        assertThat(result.getPaths().get("/").getGet().get().getResponses().values(), not(empty()));
    }

    @Test
    public void addsMixedSecurityService() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .assemble()
                .unwrap();
        var result = OpenApiConverter.create().convert(model, ShapeId.from("smithy.example#Service"));
        var expectedNode = Node.parse(LoaderUtils.readInputStream(
                getClass().getResourceAsStream("mixed-security-service.openapi.json"), "UTF-8"));

        Node.assertEquals(result, expectedNode);
    }
}
