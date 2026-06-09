/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.ShapeGenerationOrder;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.metadata.ShapeClosure;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.MapUtils;

public class CodegenDirectorTest {

    private static final class TestDirected implements DirectedCodegen<TestContext, TestSettings, TestIntegration> {
        public final List<ShapeId> generatedShapes = new ArrayList<>();
        public final List<ShapeId> generatedEnumTypeEnums = new ArrayList<>();
        public final List<ShapeId> generatedStringTypeEnums = new ArrayList<>();

        public final List<TestIntegration> integrations = new ArrayList<>();

        public TestContext capturedContext;
        public CustomizeDirective<TestContext, TestSettings> capturedCustomizeDirective;
        public Map<ShapeId, Trait> capturedSupportedProtocols;
        public final List<GenerateServiceDirective<TestContext, TestSettings>> generatedServiceDirectives =
                new ArrayList<>();
        public final Map<ShapeId, GenerateOperationDirective<TestContext, TestSettings>> generatedOperationDirectives =
                new HashMap<>();
        public final Map<ShapeId, GenerateStructureDirective<TestContext, TestSettings>> generatedStructureDirectives =
                new HashMap<>();

        @Override
        public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<TestSettings> directive) {
            return shape -> Symbol.builder()
                    .name(shape.getId().getName())
                    .namespace(shape.getId().getNamespace(), ".")
                    .build();
        }

        @Override
        public TestContext createContext(CreateContextDirective<TestSettings, TestIntegration> directive) {
            integrations.clear();
            integrations.addAll(directive.integrations());
            capturedSupportedProtocols = directive.supportedProtocols();
            WriterDelegator<TestWriter> delegator = new WriterDelegator<>(
                    directive.fileManifest(),
                    directive.symbolProvider(),
                    (f, s) -> new TestWriter());

            capturedContext = new TestContext(directive.model(),
                    directive.settings(),
                    directive.symbolProvider(),
                    directive.fileManifest(),
                    directive.sharedFileManifest().orElse(null),
                    delegator,
                    directive.getService().orElse(null));
            return capturedContext;
        }

        @Override
        public void generateService(GenerateServiceDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
            generatedServiceDirectives.add(directive);
        }

        @Override
        public void generateResource(GenerateResourceDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateOperation(GenerateOperationDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
            generatedOperationDirectives.put(directive.shape().getId(), directive);
        }

        @Override
        public void generateStructure(GenerateStructureDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
            generatedStructureDirectives.put(directive.shape().getId(), directive);
        }

        @Override
        public void generateError(GenerateErrorDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateUnion(GenerateUnionDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateList(GenerateListDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateMap(GenerateMapDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateEnumShape(GenerateEnumDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
            GenerateEnumDirective.EnumType type = directive.getEnumType();
            switch (type) {
                case ENUM:
                    generatedEnumTypeEnums.add(directive.expectEnumShape().getId());
                    break;
                case STRING:
                    generatedStringTypeEnums.add(directive.shape().asStringShape().get().getId());
                    break;
                default:
                    throw new IllegalStateException("Only ENUM and STRING types exist.");
            }
        }

        @Override
        public void generateIntEnumShape(GenerateIntEnumDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void customizeBeforeShapeGeneration(CustomizeDirective<TestContext, TestSettings> directive) {
            capturedCustomizeDirective = directive;
        }

        @Override
        public void customizeBeforeIntegrations(CustomizeDirective<TestContext, TestSettings> directive) {}

        @Override
        public void customizeAfterIntegrations(CustomizeDirective<TestContext, TestSettings> directive) {}
    }

    @Test
    public void validatesInput() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();

        runner.directedCodegen(testDirected);
        runner.fileManifest(new MockManifest());
        runner.performDefaultCodegenTransforms();
        runner.createDedicatedInputsAndOutputs();
        runner.sortMembers();

        Assertions.assertThrows(IllegalStateException.class, runner::run);
    }

    @Test
    public void failsWhenServiceIsMissing() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();

        runner.settings(TestSettings.class, Node.objectNode().withMember("foo", "hi"));
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(Model.builder().build());
        runner.integrationClass(TestIntegration.class);

        Assertions.assertThrows(ExpectationNotMetException.class, runner::run);
    }

    @Test
    public void performsCodegen() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("directed-model.smithy"))
                .assemble()
                .unwrap();

        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.createDedicatedInputsAndOutputs();
        runner.sortMembers();
        runner.run();

        // asserts that mixin smithy.example#Paginated is not generated
        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#Foo"),
                        ShapeId.from("smithy.example#TheFoo"),
                        ShapeId.from("smithy.example#ListFooInput"),
                        ShapeId.from("smithy.example#ListFooOutput"),
                        ShapeId.from("smithy.example#FooStructure"),
                        ShapeId.from("smithy.example#FooList"),
                        ShapeId.from("smithy.example#StringMap"),
                        ShapeId.from("smithy.example#Status"),
                        ShapeId.from("smithy.example#FaceCard"),
                        ShapeId.from("smithy.example#Instruction"),
                        ShapeId.from("smithy.api#Unit"),
                        ShapeId.from("smithy.example#ListFoo")));

        assertThat(testDirected.generatedStringTypeEnums,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#Status")));
        assertThat(testDirected.generatedEnumTypeEnums, empty());
    }

    @Test
    public void performsCodegenWithStringEnumsChangedToEnumShapes() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("directed-model.smithy"))
                .assemble()
                .unwrap();

        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.changeStringEnumsToEnumShapes(true);
        runner.createDedicatedInputsAndOutputs();
        runner.sortMembers();
        runner.run();

        // asserts that mixin smithy.example#Paginated is not generated
        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#Foo"),
                        ShapeId.from("smithy.example#TheFoo"),
                        ShapeId.from("smithy.example#ListFooInput"),
                        ShapeId.from("smithy.example#ListFooOutput"),
                        ShapeId.from("smithy.example#FooStructure"),
                        ShapeId.from("smithy.example#FooList"),
                        ShapeId.from("smithy.example#StringMap"),
                        ShapeId.from("smithy.example#Status"),
                        ShapeId.from("smithy.example#FaceCard"),
                        ShapeId.from("smithy.example#Instruction"),
                        ShapeId.from("smithy.api#Unit"),
                        ShapeId.from("smithy.example#ListFoo")));

        assertThat(testDirected.generatedEnumTypeEnums,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#Status")));
        assertThat(testDirected.generatedStringTypeEnums, empty());
    }

    @Test
    public void sortsShapesWithDefaultTopologicalOrder() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("needs-sorting.smithy"))
                .assemble()
                .unwrap();

        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.createDedicatedInputsAndOutputs();
        runner.sortMembers();
        runner.run();

        assertThat(testDirected.generatedShapes,
                contains(
                        ShapeId.from("smithy.example#D"),
                        ShapeId.from("smithy.example#C"),
                        ShapeId.from("smithy.example#B"),
                        ShapeId.from("smithy.example#A"),
                        ShapeId.from("smithy.example#FooOperationOutput"),
                        ShapeId.from("smithy.example#RecursiveA"),
                        ShapeId.from("smithy.example#RecursiveB"),
                        ShapeId.from("smithy.example#FooOperationInput"),
                        ShapeId.from("smithy.example#FooOperation"),
                        ShapeId.from("smithy.example#Foo")));
    }

    @Test
    public void testShapesGenerationWithAlphabeticalOrder() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("needs-sorting.smithy"))
                .assemble()
                .unwrap();

        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.shapeGenerationOrder(ShapeGenerationOrder.ALPHABETICAL);
        runner.run();

        assertThat(testDirected.generatedShapes,
                contains(
                        ShapeId.from("smithy.example#A"),
                        ShapeId.from("smithy.example#B"),
                        ShapeId.from("smithy.example#C"),
                        ShapeId.from("smithy.example#D"),
                        ShapeId.from("smithy.example#FooOperation"),
                        ShapeId.from("smithy.example#FooOperationInput"),
                        ShapeId.from("smithy.example#FooOperationOutput"),
                        ShapeId.from("smithy.example#RecursiveA"),
                        ShapeId.from("smithy.example#RecursiveB"),
                        ShapeId.from("smithy.example#Foo")));
    }

    @Test
    public void testShapesGenerationWithoutOrder() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("needs-sorting.smithy"))
                .assemble()
                .unwrap();

        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.shapeGenerationOrder(ShapeGenerationOrder.NONE);
        runner.run();

        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#FooOperation"),
                        ShapeId.from("smithy.example#FooOperationOutput"),
                        ShapeId.from("smithy.example#A"),
                        ShapeId.from("smithy.example#B"),
                        ShapeId.from("smithy.example#C"),
                        ShapeId.from("smithy.example#D"),
                        ShapeId.from("smithy.example#FooOperationInput"),
                        ShapeId.from("smithy.example#RecursiveA"),
                        ShapeId.from("smithy.example#RecursiveB"),
                        ShapeId.from("smithy.example#Foo")));
    }

    @Test
    public void testConfiguresIntegrations() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("needs-sorting.smithy"))
                .assemble()
                .unwrap();

        ObjectNode integrationSettings = Node.objectNode().withMember("spam", "eggs");
        ObjectNode allIntegrationSettings = Node.objectNode()
                .withMember("capturing-integration", integrationSettings);
        ObjectNode settings = Node.objectNode()
                .withMember("foo", "hi")
                .withMember("integrations", allIntegrationSettings);
        runner.settings(TestSettings.class, settings);
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.shapeGenerationOrder(ShapeGenerationOrder.NONE);
        runner.run();

        assertThat(testDirected.integrations, not(empty()));
        CapturingIntegration capturingIntegration = null;
        for (TestIntegration integration : testDirected.integrations) {
            if (integration instanceof CapturingIntegration) {
                capturingIntegration = (CapturingIntegration) integration;
            }
        }
        assertThat(capturingIntegration, notNullValue());
        assertThat(capturingIntegration.integrationSettings, equalTo(integrationSettings));
    }

    @Test
    public void testSharedManifestPassedToContext() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        FileManifest manifest = new MockManifest();
        FileManifest sharedManifest = new MockManifest();
        Model model = Model.assembler()
                .addImport(getClass().getResource("directed-model.smithy"))
                .assemble()
                .unwrap();

        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.sharedFileManifest(sharedManifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(model);
        runner.integrationClass(TestIntegration.class);
        runner.performDefaultCodegenTransforms();
        runner.createDedicatedInputsAndOutputs();
        runner.sortMembers();
        runner.run();

        assertThat(testDirected.capturedContext.sharedFileManifest().get(), equalTo(sharedManifest));
    }

    private CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> closureRunner(
            TestDirected testDirected,
            String... modelFiles
    ) {
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = new CodegenDirector<>();
        ModelAssembler assembler = Model.assembler();
        for (String modelFile : modelFiles) {
            assembler.addImport(getClass().getResource(modelFile));
        }
        runner.settings(new TestSettings());
        runner.directedCodegen(testDirected);
        runner.fileManifest(new MockManifest());
        runner.model(assembler.assemble().unwrap());
        runner.integrationClass(TestIntegration.class);
        return runner;
    }

    @Test
    public void generatesShapeClosureWithoutService() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#cityData");
        runner.run();

        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#City"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#Conditions")));

        // The directive is given the closure rather than a service.
        assertThat(testDirected.capturedContext.service(), equalTo(null));
        assertFalse(testDirected.capturedCustomizeDirective.getService().isPresent());
        assertThat(testDirected.capturedCustomizeDirective.getShapeClosureId().get(),
                equalTo("smithy.example#cityData"));

        // There is no service, operations, or protocols
        assertThat(testDirected.capturedCustomizeDirective.operations(), empty());
        assertThat(testDirected.capturedSupportedProtocols.entrySet(), empty());
    }

    @Test
    public void generatesServiceShapesInClosure() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#serviceClosure");
        runner.run();

        // The service in the closure is generated through the shape visitor.
        assertThat(testDirected.generatedShapes, hasItem(ShapeId.from("smithy.example#Weather")));
        assertThat(testDirected.generatedServiceDirectives, hasSize(1));

        // The directive distinguishes the generated service shape from the (absent)
        // driving service, and carries the closure that is driving generation.
        GenerateServiceDirective<TestContext, TestSettings> serviceDirective =
                testDirected.generatedServiceDirectives.get(0);
        assertThat(serviceDirective.shape().getId(), equalTo(ShapeId.from("smithy.example#Weather")));
        assertFalse(serviceDirective.getService().isPresent());
        assertThat(serviceDirective.getShapeClosureId().get(), equalTo("smithy.example#serviceClosure"));
    }

    @Test
    public void generatesOperationShapesInClosure() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#getCityClosure");
        runner.run();

        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#GetCity"),
                        ShapeId.from("smithy.example#GetCityInput"),
                        ShapeId.from("smithy.example#GetCityOutput"),
                        ShapeId.from("smithy.example#City"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#Conditions")));
    }

    @Test
    public void generatesTheDrivingServiceExactlyOnce() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.service(ShapeId.from("smithy.example#Weather"));
        runner.run();

        // The driving service is generated by the explicit ordered call, and the visitor
        // must not generate it a second time.
        assertThat(testDirected.generatedServiceDirectives, hasSize(1));
        GenerateServiceDirective<TestContext, TestSettings> serviceDirective =
                testDirected.generatedServiceDirectives.get(0);
        assertThat(serviceDirective.getService().get().getId(), equalTo(ShapeId.from("smithy.example#Weather")));
        assertThat(serviceDirective.shape().getId(), equalTo(ShapeId.from("smithy.example#Weather")));
    }

    @Test
    public void typeCodegenSkipsServiceShapesInAClosureButKeepsTheirDataShapes() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#getCityClosure");
        runner.generateDataShapesOnly();
        runner.run();

        // The operation is not generated (it is absent from the exact set below), but
        // the structures it reaches are.
        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#GetCityInput"),
                        ShapeId.from("smithy.example#GetCityOutput"),
                        ShapeId.from("smithy.example#City"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#Conditions")));
    }

    @Test
    public void typeCodegenOverAServiceSkipsServiceResourceAndOperationShapes() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.service(ShapeId.from("smithy.example#Weather"));
        runner.generateDataShapesOnly();
        runner.run();

        // The service, resource, and operation shapes are not generated, but their data
        // shapes are.
        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#GetCityInput"),
                        ShapeId.from("smithy.example#GetCityOutput"),
                        ShapeId.from("smithy.example#ForecastData"),
                        ShapeId.from("smithy.example#City"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#Conditions")));
        assertThat(testDirected.generatedShapes, not(hasItem(ShapeId.from("smithy.example#Weather"))));
        assertThat(testDirected.generatedShapes, not(hasItem(ShapeId.from("smithy.example#Forecast"))));
        assertThat(testDirected.generatedShapes, not(hasItem(ShapeId.from("smithy.example#GetCity"))));
        assertThat(testDirected.generatedShapes, not(hasItem(ShapeId.from("smithy.example#GetForecast"))));

        // The directive reports only the shapes being generated: there are no operations,
        // and the connected shapes exclude the service, resource, and operations.
        assertThat(testDirected.capturedCustomizeDirective.operations(), empty());
        assertThat(testDirected.capturedCustomizeDirective.connectedShapes(),
                not(hasKey(ShapeId.from("smithy.example#Weather"))));
        assertThat(testDirected.capturedCustomizeDirective.connectedShapes(),
                not(hasKey(ShapeId.from("smithy.example#GetCity"))));
    }

    @Test
    public void typeCodegenGeneratesSynthesizedShapes() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#getForecastClosure");

        // GetForecast shares ForecastData for input and output. This transform synthesizes
        // dedicated GetForecastInput/GetForecastOutput shapes. The closure must be resolved
        // from the post-transform model so the synthesized shapes are generated.
        runner.createDedicatedInputsAndOutputs();
        runner.run();

        // The synthesized input/output shapes are generated, and the original shared
        // ForecastData shape is gone (it is absent from this exact set).
        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#GetForecast"),
                        ShapeId.from("smithy.example#GetForecastInput"),
                        ShapeId.from("smithy.example#GetForecastOutput")));
    }

    @Test
    public void flattensPaginationInfoForServicesInTheClosure() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");

        // The closure is rooted at the CityDirectory service, which pulls in its
        // ListCities operation. The service defines the pagination defaults, so its
        // operations in the closure inherit them via flattening.
        runner.shapeClosure("smithy.example#cityDirectoryClosure");
        runner.flattenPaginationInfoIntoOperations();
        runner.run();

        GenerateOperationDirective<TestContext, TestSettings> operation =
                testDirected.generatedOperationDirectives.get(ShapeId.from("smithy.example#ListCities"));
        assertThat(operation, notNullValue());
        PaginatedTrait paginated = operation.shape().expectTrait(PaginatedTrait.class);

        // Defined on the operation.
        assertThat(paginated.getItems().get(), equalTo("items"));

        // Inherited from the CityDirectory service via flattening.
        assertThat(paginated.getInputToken().get(), equalTo("nextToken"));
        assertThat(paginated.getOutputToken().get(), equalTo("nextToken"));
        assertThat(paginated.getPageSize().get(), equalTo("maxResults"));
    }

    @Test
    public void performsDefaultTransformsForClosureGenerationFlatteningMixins() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#mixinUserClosure");
        runner.performDefaultCodegenTransforms();
        runner.run();

        // The mixin member was flattened onto the structure, and the mixin shape was
        // removed from the model rather than generated.
        GenerateStructureDirective<TestContext, TestSettings> mixinUser =
                testDirected.generatedStructureDirectives.get(ShapeId.from("smithy.example#MixinUser"));
        assertThat(mixinUser, notNullValue());
        assertThat(mixinUser.shape().getMemberNames(), containsInAnyOrder("id", "name"));
        assertThat(mixinUser.shape().getMixins(), empty());
        assertThat(testDirected.generatedShapes, not(hasItem(ShapeId.from("smithy.example#CommonFields"))));
    }

    @Test
    public void appliesClosureRenamesWhenOrderingShapesAlphabetically() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#renamedCityData");
        runner.shapeGenerationOrder(ShapeGenerationOrder.ALPHABETICAL);
        runner.run();

        // City is renamed to ZanyCity, so it sorts last rather than first, proving the
        // closure rename (not the original name) drives the alphabetical sort.
        assertThat(testDirected.generatedShapes,
                contains(
                        ShapeId.from("smithy.example#Conditions"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#City")));
    }

    @Test
    public void combinedModeGeneratesTheClosureWithTheServiceAsPrimary() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "combined-model.smithy");
        runner.service(ShapeId.from("smithy.example#Weather"));
        runner.shapeClosure("smithy.example#combinedClosure");
        runner.run();

        // The generated set is the closure: the service and its data shapes, plus the
        // unconnected Standalone operation and ExtraType structure the closure adds.
        assertThat(testDirected.generatedShapes,
                hasItem(ShapeId.from("smithy.example#Weather")));
        assertThat(testDirected.generatedShapes,
                hasItem(ShapeId.from("smithy.example#ExtraType")));
        assertThat(testDirected.generatedShapes,
                hasItem(ShapeId.from("smithy.example#Standalone")));

        // The primary service is exposed and generated exactly once (by the dedicated ordered
        // call, not also by the shape visitor).
        assertThat(testDirected.capturedCustomizeDirective.getService().get().getId(),
                equalTo(ShapeId.from("smithy.example#Weather")));
        assertThat(testDirected.capturedContext.service().getId(),
                equalTo(ShapeId.from("smithy.example#Weather")));
        assertThat(testDirected.generatedServiceDirectives, hasSize(1));
        GenerateServiceDirective<TestContext, TestSettings> serviceDirective =
                testDirected.generatedServiceDirectives.get(0);
        assertThat(serviceDirective.getService().get().getId(),
                equalTo(ShapeId.from("smithy.example#Weather")));
        assertThat(serviceDirective.shape().getId(), equalTo(ShapeId.from("smithy.example#Weather")));

        // operations() reflects the closure, so it contains the unconnected Standalone
        // operation in addition to the service's own operations. A top-down walk of the
        // service alone would not include Standalone.
        Set<ShapeId> operations = testDirected.capturedCustomizeDirective.operations()
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());
        assertThat(operations,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#GetCity"),
                        ShapeId.from("smithy.example#GetForecast"),
                        ShapeId.from("smithy.example#Standalone")));
    }

    @Test
    public void combinedModePartialClosureExcludesOmittedOperations() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "combined-model.smithy");

        // The closure contains the Weather service and ExtraType, but not the unconnected
        // Standalone operation, proving operations()/the generated set follow the closure and
        // not a top-down walk of the service.
        runner.service(ShapeId.from("smithy.example#Weather"));
        runner.shapeClosure("smithy.example#combinedRenamedClosure");
        runner.run();

        Set<ShapeId> operations = testDirected.capturedCustomizeDirective.operations()
                .stream()
                .map(Shape::getId)
                .collect(Collectors.toSet());
        assertThat(operations,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#GetCity"),
                        ShapeId.from("smithy.example#GetForecast")));
        assertThat(operations, not(hasItem(ShapeId.from("smithy.example#Standalone"))));
        assertThat(testDirected.generatedShapes,
                not(hasItem(ShapeId.from("smithy.example#Standalone"))));
    }

    @Test
    public void combinedModeFlattensPaginationForServicesInTheClosure() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");

        // The closure is rooted at the CityDirectory service, which is also set as the
        // primary service, so this is combined mode. Pagination flattening must still run for
        // services in the closure being generated.
        runner.service(ShapeId.from("smithy.example#CityDirectory"));
        runner.shapeClosure("smithy.example#cityDirectoryClosure");
        runner.flattenPaginationInfoIntoOperations();
        runner.run();

        GenerateOperationDirective<TestContext, TestSettings> operation =
                testDirected.generatedOperationDirectives.get(ShapeId.from("smithy.example#ListCities"));
        assertThat(operation, notNullValue());
        PaginatedTrait paginated = operation.shape().expectTrait(PaginatedTrait.class);
        assertThat(paginated.getItems().get(), equalTo("items"));
        // Inherited from the CityDirectory service via flattening.
        assertThat(paginated.getInputToken().get(), equalTo("nextToken"));
    }

    @Test
    public void failsWhenServiceIsNotAMemberOfTheShapeClosure() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "combined-model.smithy");

        // The closure does not contain the Weather service, so combined mode is invalid.
        runner.service(ShapeId.from("smithy.example#Weather"));
        runner.shapeClosure("smithy.example#serviceNotIncluded");

        Assertions.assertThrows(IllegalStateException.class, runner::run);
    }

    @Test
    public void injectsAndGeneratesAShapeClosureObject() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");

        // A closure built in code is injected into the model and resolved the normal way.
        // This generates the same shapes as the equivalent metadata-authored `cityData`
        // closure.
        ShapeClosure closure = ShapeClosure.builder()
                .id("smithy.example#injectedCityData")
                .includeBySelector("[id = 'smithy.example#City']")
                .build();
        runner.shapeClosure(closure);
        runner.run();

        assertThat(testDirected.generatedShapes,
                containsInAnyOrder(
                        ShapeId.from("smithy.example#City"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#Conditions")));

        // The injected closure id flows through the directives like a metadata closure.
        assertThat(testDirected.capturedCustomizeDirective.getShapeClosureId().get(),
                equalTo("smithy.example#injectedCityData"));
    }

    @Test
    public void injectedShapeClosureObjectAppliesRenames() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");

        ShapeClosure closure = ShapeClosure.builder()
                .id("smithy.example#injectedRenamedCityData")
                .includeBySelector("[id = 'smithy.example#City']")
                .rename(MapUtils.of(ShapeId.from("smithy.example#City"), "ZanyCity"))
                .build();
        runner.shapeClosure(closure);
        runner.shapeGenerationOrder(ShapeGenerationOrder.ALPHABETICAL);
        runner.run();

        // The injected rename drives the alphabetical sort (City -> ZanyCity sorts last),
        // proving the injected closure's renames are resolved through getRenames().
        assertThat(testDirected.generatedShapes,
                contains(
                        ShapeId.from("smithy.example#Conditions"),
                        ShapeId.from("smithy.example#Coordinates"),
                        ShapeId.from("smithy.example#City")));
    }

    @Test
    public void failsWhenInjectedShapeClosureObjectIdCollides() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");

        // smithy.example#cityData is already declared in the model metadata.
        ShapeClosure closure = ShapeClosure.builder()
                .id("smithy.example#cityData")
                .includeBySelector("[id = 'smithy.example#City']")
                .build();
        runner.shapeClosure(closure);

        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, runner::run);
        assertThat(e.getMessage(), containsString("smithy.example#cityData"));
    }

    @Test
    public void failsWhenNeitherServiceNorShapeClosureIsSet() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");

        Assertions.assertThrows(IllegalStateException.class, runner::run);
    }

    @Test
    public void failsWhenShapeClosureIsUnknown() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.shapeClosure("smithy.example#doesNotExist");

        Assertions.assertThrows(IllegalStateException.class, runner::run);
    }

    @Test
    public void allowsClosureGenerationWithNameConflictsByDefault() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = closureRunner(
                testDirected,
                "closure-name-conflict-model.smithy",
                "closure-name-conflict-other.smithy");

        // smithy.example#City and smithy.other#city collide case-insensitively.
        runner.shapeClosure("smithy.example#conflicting");
        runner.run();
    }

    @Test
    public void failsClosureGenerationWithNameConflictsOnRequest() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = closureRunner(
                testDirected,
                "closure-name-conflict-model.smithy",
                "closure-name-conflict-other.smithy");

        // smithy.example#City and smithy.other#city collide case-insensitively.
        runner.shapeClosure("smithy.example#conflicting");
        runner.requireCaseInsensitiveNames();

        CodegenException e = Assertions.assertThrows(CodegenException.class, runner::run);
        assertThat(e.getMessage(), containsString("smithy.example#City"));
        assertThat(e.getMessage(), containsString("smithy.other#city"));
    }

    @Test
    public void requireCaseInsensitiveNamesChecksShapesSynthesizedByTransforms() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = closureRunner(
                testDirected,
                "closure-synthesized-conflict-model.smithy",
                "closure-synthesized-conflict-other.smithy");
        runner.shapeClosure("smithy.example#synthesizedConflict");
        // The conflict only exists after createDedicatedInputsAndOutputs synthesizes
        // MakeThingOutput, so this passing proves the check runs after transforms.
        runner.createDedicatedInputsAndOutputs();
        runner.requireCaseInsensitiveNames();

        CodegenException e = Assertions.assertThrows(CodegenException.class, runner::run);
        assertThat(e.getMessage(), containsString("smithy.example#MakeThingOutput"));
        assertThat(e.getMessage(), containsString("smithy.other#makeThingOutput"));
    }

    @Test
    public void allowsConflictingNamesWhenDisambiguatedByARename() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner = closureRunner(
                testDirected,
                "closure-name-conflict-model.smithy",
                "closure-name-conflict-other.smithy");

        // The closure renames smithy.other#city to OtherCity, resolving the conflict.
        runner.shapeClosure("smithy.example#disambiguated");
        runner.requireCaseInsensitiveNames();
        runner.run();

        assertThat(testDirected.generatedShapes,
                hasItem(ShapeId.from("smithy.example#City")));
        assertThat(testDirected.generatedShapes,
                hasItem(ShapeId.from("smithy.other#city")));
    }

    @Test
    public void requireCaseInsensitiveNamesIsANoOpForServiceGeneration() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner =
                closureRunner(testDirected, "closure-model.smithy");
        runner.service(ShapeId.from("smithy.example#Weather"));

        // Service closures already enforce case-insensitive name uniqueness, so this
        // does nothing rather than failing.
        runner.requireCaseInsensitiveNames();
        runner.run();

        assertThat(testDirected.generatedShapes, hasItem(ShapeId.from("smithy.example#Weather")));
    }
}
