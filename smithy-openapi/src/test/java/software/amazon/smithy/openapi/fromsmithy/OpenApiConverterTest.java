/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.OpenApiVersion;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.PathItem;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class OpenApiConverterTest {

    private static Model testService;

    @BeforeAll
    private static void setup() {
        testService = Model.assembler()
                .addImport(OpenApiConverterTest.class.getResource("test-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @Test
    public void convertsModelsToOpenApi() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(testService);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("test-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void passesThroughAllTags() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setTags(true);
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("tagged-service-all-tags.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void passesThroughSupportedTags() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setTags(true);
        config.setSupportedTags(ListUtils.of("baz", "foo"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("tagged-service-supported-tags.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void doesNotPassThroughTagsWithEmptySupportedTagList() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setTags(true);
        config.setSupportedTags(ListUtils.of());
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("tagged-service-empty-supported-tags.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void preservesUserSpecifiedOrderOfTags() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service-order.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setTags(true);
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("tagged-service-order.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void preservesUserSpecifiedOrderOfTagsWhenFilteringSupportedTags() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("tagged-service-order.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setTags(true);
        config.setSupportedTags(ListUtils.of("one", "two", "three", "four"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("tagged-service-order-supported-tags.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void usesOpenApiIntegers() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        config.setUseIntegerType(true);
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(testService);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("test-service-integer.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void requiresProtocolsTrait() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("missing-protocols-trait.json"))
                    .discoverModels()
                    .assemble()
                    .unwrap();
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#Service"));
            OpenApiConverter.create()
                    .config(config)
                    .convert(model);
        });

        assertThat(thrown.getMessage(), containsString("does not define any protocols"));
    }

    @Test
    public void mustBeAbleToResolveProtocolServiceProvider() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("unable-to-resolve-protocol.json"))
                    .discoverModels()
                    .assemble()
                    .unwrap();
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#Service"));
            OpenApiConverter.create()
                    .config(config)
                    .convert(model);
        });

        assertThat(thrown.getMessage(), containsString("Unable to find an OpenAPI service provider"));
    }

    @Test
    public void loadsProtocolFromConfiguration() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        config.setProtocol(ShapeId.from("aws.protocols#restJson1"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(testService);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("test-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void failsToDeriveFromMultipleProtocols() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler()
                    .addImport(getClass().getResource("service-with-multiple-protocols.smithy"))
                    .discoverModels()
                    .assemble()
                    .unwrap();
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#Service"));
            OpenApiConverter.create()
                    .config(config)
                    .convert(model);
        });

        assertThat(thrown.getMessage(), containsString("defines multiple protocols"));
    }

    @Test
    public void failsWhenConfiguredProtocolIsNoFound() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("example.rest#RestService"));
            config.setProtocol(ShapeId.from("aws.protocols#restJson99"));
            OpenApiConverter.create()
                    .config(config)
                    .convertToNode(testService);
        });

        assertThat(thrown.getMessage(), containsString("Unable to find protocol"));
    }

    @Test
    public void omitsUnsupportedHttpMethods() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("unsupported-http-method.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("unsupported-http-method.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void protocolsCanOmitOperations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("missing-http-bindings.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);

        for (PathItem pathItem : result.getPaths().values()) {
            assertFalse(pathItem.getGet().isPresent());
            assertFalse(pathItem.getHead().isPresent());
            assertFalse(pathItem.getDelete().isPresent());
            assertFalse(pathItem.getPatch().isPresent());
            assertFalse(pathItem.getPost().isPresent());
            assertFalse(pathItem.getPut().isPresent());
            assertFalse(pathItem.getTrace().isPresent());
            assertFalse(pathItem.getOptions().isPresent());
        }
    }

    @Test
    public void addsEmptyResponseByDefault() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("adds-empty-response.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);

        assertThat(result.getPaths().get("/").getGet().get().getResponses().values(), not(empty()));
    }

    @Test
    public void addsMixedSecurityService() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("mixed-security-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    private static final class NullSecurity implements OpenApiMapper {
        @Override
        public Map<String, List<String>> updateSecurity(
                Context<? extends Trait> context,
                Shape shape,
                SecuritySchemeConverter<? extends Trait> converter,
                Map<String, List<String>> requirement
        ) {
            return null;
        }
    }

    @Test
    public void canOmitSecurityRequirements() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .addOpenApiMapper(new NullSecurity())
                .config(config)
                .convert(model);

        assertThat(result.getSecurity(), empty());
        assertThat(result.getPaths().get("/2").getGet().get().getSecurity().orElse(Collections.emptyList()), empty());
    }

    private static final class ConstantSecurity implements OpenApiMapper {
        @Override
        public Map<String, List<String>> updateSecurity(
                Context<? extends Trait> context,
                Shape shape,
                SecuritySchemeConverter<? extends Trait> converter,
                Map<String, List<String>> requirement
        ) {
            return MapUtils.of("foo_baz", ListUtils.of());
        }
    }

    @Test
    public void canChangeSecurityRequirementName() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("mixed-security-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .addOpenApiMapper(new ConstantSecurity())
                .config(config)
                .convert(model);

        assertThat(result.getSecurity().get(0).keySet(), contains("foo_baz"));
        assertThat(result.getPaths().get("/2").getGet().get().getSecurity().get().get(0).keySet(), contains("foo_baz"));
    }

    @Test
    public void consolidatesSameSecurityRequirements() {
        // This service model has multiple auth types throughout it for both
        // the top level and operation level security setting. Validate that,
        // after they're set to use the same name, they're consolidated for
        // being the same.
        Model model = Model.assembler()
                .addImport(getClass().getResource("consolidates-security-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        OpenApi result = OpenApiConverter.create()
                .addOpenApiMapper(new ConstantSecurity())
                .config(config)
                .convert(model);

        assertThat(result.getSecurity().size(), equalTo(1));
        assertThat(result.getSecurity().get(0).keySet(), contains("foo_baz"));
        // This security matches the service, so isn't applied.
        assertFalse(result.getPaths().get("/1").getGet().get().getSecurity().isPresent());
        assertThat(result.getPaths().get("/2").getGet().get().getSecurity().get().size(), equalTo(1));
        assertThat(result.getPaths().get("/2").getGet().get().getSecurity().get().get(0).keySet(), contains("foo_baz"));
        assertThat(result.getPaths().get("/3").getGet().get().getSecurity().get().size(), equalTo(1));
        assertThat(result.getPaths().get("/3").getGet().get().getSecurity().get().get(0).keySet(), contains("foo_baz"));
    }

    @Test
    public void mergesInSchemaDocumentExtensions() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        config.setSchemaDocumentExtensions(Node.objectNode().withMember("foo", "baz"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(testService);

        assertThat(result.getMember("foo"), equalTo(Optional.of(Node.from("baz"))));
    }

    // Streaming traits are converted to just an application/octet-stream
    @Test
    public void convertsStreamingService() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("streaming-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Streaming"));
        config.setProtocol(ShapeId.from("aws.protocols#restJson1"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("streaming-service.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void addsDefaultSettings() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        config.setSchemaDocumentExtensions(Node.objectNode().withMember("foo", "baz"));
        OpenApiConverter.create()
                .addOpenApiMapper(new OpenApiMapper() {
                    @Override
                    public void updateDefaultSettings(Model model, OpenApiConfig config) {
                        config.putExtension("hello", "goodbye");
                    }
                })
                .config(config)
                .convertToNode(testService);

        assertThat(config.getExtensions().getMember("hello"), not(Optional.empty()));
    }

    // The input structure needs a synthesized content structure. Since the
    // path property is a "parameter" the synthesized structure must not list
    // it as required because it is not part of the payload.
    @Test
    public void properlyRemovesRequiredPropertiesFromSynthesizedInput() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-with-required-path.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("service-with-required-path.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsUnions() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("union-test.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Example"));
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("union-test.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsDocumentation() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("documentation-test.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#MyDocs"));
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("documentation-test.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsExternalDocumentation() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("externaldocs-test.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#MyDocs"));
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("externaldocs-test.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void properlyDealsWithServiceRenames() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-with-renames.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#MyService"));
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("service-with-renames.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void generatesOpenApiForSharedErrors() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("service-with-common-errors.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#MyService"));
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("service-with-common-errors.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsUnitsThatDoNotConflict() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("nonconflicting-unit.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.rest#RestService"));
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("nonconflicting-unit.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsToOpenAPI3_0_2() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("nullability-and-format.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example#Example"));
        config.setVersion(OpenApiVersion.VERSION_3_0_2);
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("openapi-3-0-2.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsToOpenAPI3_1_0() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("nullability-and-format.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example#Example"));
        config.setVersion(OpenApiVersion.VERSION_3_1_0);
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("openapi-3-1-0.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void removesMixins() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("model-with-mixins.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#HasMixin"));
        config.setProtocol(ShapeId.from("aws.protocols#restJson1"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("model-with-mixins.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertsMemberDocumentation() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("documentation-test-members.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#MyDocs"));
        config.setVersion(OpenApiVersion.VERSION_3_1_0);
        config.setAddReferenceDescriptions(true);
        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("documentation-test-members.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }

    @Test
    public void convertingMemberDocsRequired3_1() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("documentation-test-members.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#MyDocs"));
        config.setAddReferenceDescriptions(true);
        OpenApiConverter converter = OpenApiConverter.create().config(config);

        assertThrows(OpenApiException.class, () -> converter.convertToNode(model));
    }
}
