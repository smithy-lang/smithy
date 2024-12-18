/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.codegen.core.ShapeGenerationOrder;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class CodegenDirectorTest {

    private static final class TestDirected implements DirectedCodegen<TestContext, TestSettings, TestIntegration> {
        public final List<ShapeId> generatedShapes = new ArrayList<>();
        public final List<ShapeId> generatedEnumTypeEnums = new ArrayList<>();
        public final List<ShapeId> generatedStringTypeEnums = new ArrayList<>();

        public final List<TestIntegration> integrations = new ArrayList<>();

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
            WriterDelegator<TestWriter> delegator = new WriterDelegator<>(
                    directive.fileManifest(),
                    directive.symbolProvider(),
                    (f, s) -> new TestWriter());

            return new TestContext(directive.model(),
                    directive.settings(),
                    directive.symbolProvider(),
                    directive.fileManifest(),
                    delegator,
                    directive.service());
        }

        @Override
        public void generateService(GenerateServiceDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateResource(GenerateResourceDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateOperation(GenerateOperationDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
        }

        @Override
        public void generateStructure(GenerateStructureDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
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
                contains(
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
}
