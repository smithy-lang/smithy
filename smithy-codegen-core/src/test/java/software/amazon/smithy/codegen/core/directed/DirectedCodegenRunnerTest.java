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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class DirectedCodegenRunnerTest {

    interface TestIntegration extends SmithyIntegration<TestSettings, TestWriter, TestContext> {}

    private static final class TestDirected implements DirectedCodegen<TestContext, TestSettings> {
        @Override
        public SymbolProvider createSymbolProvider(CreateSymbolProvider<TestSettings> directive) {
            return shape -> Symbol.builder()
                    .name(shape.getId().getName())
                    .namespace(shape.getId().getNamespace(), ".")
                    .build();
        }

        @Override
        public TestContext createContext(CreateContext<TestSettings> directive) {
            WriterDelegator<TestWriter> delegator = new WriterDelegator<>(
                    directive.fileManifest(),
                    directive.symbolProvider(),
                    (f, s) -> new TestWriter());

            return new TestContext(directive.model(), directive.settings(), directive.symbolProvider(),
                                   directive.fileManifest(), delegator, directive.service());
        }

        @Override
        public void generateService(GenerateService<TestContext, TestSettings> directive) { }

        @Override
        public void generateResource(GenerateResource<TestContext, TestSettings> directive) { }

        @Override
        public void generateStructure(GenerateStructure<TestContext, TestSettings> directive) { }

        @Override
        public void generateError(GenerateError<TestContext, TestSettings> directive) { }

        @Override
        public void generateUnion(GenerateUnion<TestContext, TestSettings> directive) {}

        @Override
        public void customizeBeforeIntegrations(Customize<TestContext, TestSettings> directive) {}

        @Override
        public void customizeAfterIntegrations(Customize<TestContext, TestSettings> directive) {}
    }

    @Test
    public void validatesInput() {
        TestDirected testDirected = new TestDirected();
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, TestSettings>
                runner = new DirectedCodegenRunner<>();

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
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, TestSettings> runner
                = new DirectedCodegenRunner<>();
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
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, TestSettings> runner
                = new DirectedCodegenRunner<>();
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
}
