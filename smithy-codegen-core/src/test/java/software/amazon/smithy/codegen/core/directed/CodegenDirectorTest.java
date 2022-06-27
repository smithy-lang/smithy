/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core.directed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class CodegenDirectorTest {

    private static final class TestDirected implements DirectedCodegen<TestContext, TestSettings, TestIntegration> {
        public final List<ShapeId> generatedShapes = new ArrayList<>();

        @Override
        public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<TestSettings> directive) {
            return shape -> Symbol.builder()
                    .name(shape.getId().getName())
                    .namespace(shape.getId().getNamespace(), ".")
                    .build();
        }

        @Override
        public TestContext createContext(CreateContextDirective<TestSettings, TestIntegration> directive) {
            WriterDelegator<TestWriter> delegator = new WriterDelegator<>(
                    directive.fileManifest(),
                    directive.symbolProvider(),
                    (f, s) -> new TestWriter());

            return new TestContext(directive.model(), directive.settings(), directive.symbolProvider(),
                                   directive.fileManifest(), delegator, directive.service());
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
        public void generateEnumShape(GenerateEnumDirective<TestContext, TestSettings> directive) {
            generatedShapes.add(directive.shape().getId());
            GenerateEnumDirective.EnumType type = directive.getEnumType();
            if (type == GenerateEnumDirective.EnumType.STRING) {
                directive.getEnumTrait();
            } else {
                // TODO: update for idl-2.0
                throw new RuntimeException("Expected enum type to be string");
            }
        }

        @Override
        public void customizeBeforeIntegrations(CustomizeDirective<TestContext, TestSettings> directive) {}

        @Override
        public void customizeAfterIntegrations(CustomizeDirective<TestContext, TestSettings> directive) {}
    }

    @Test
    public void validatesInput() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings>
                runner = new CodegenDirector<>();

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
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner
                = new CodegenDirector<>();
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
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner
                = new CodegenDirector<>();
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
    }

    @Test
    public void sortsShapes() {
        TestDirected testDirected = new TestDirected();
        CodegenDirector<TestWriter, TestIntegration, TestContext, TestSettings> runner
                = new CodegenDirector<>();
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

        assertThat(testDirected.generatedShapes, contains(
                ShapeId.from("smithy.example#D"),
                ShapeId.from("smithy.example#C"),
                ShapeId.from("smithy.example#B"),
                ShapeId.from("smithy.example#A"),
                ShapeId.from("smithy.example#FooOperationOutput"),
                ShapeId.from("smithy.example#RecursiveA"),
                ShapeId.from("smithy.example#RecursiveB"),
                ShapeId.from("smithy.example#FooOperationInput"),
                ShapeId.from("smithy.example#Foo")
        ));
    }
}
