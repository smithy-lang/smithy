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
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.ShapeId;

// TODO: fill in these test
public class DirectedCodegenRunnerTest {

    interface TestIntegration extends SmithyIntegration<Object, TestWriter, TestContext> {}

    private static final class TestDirected implements DirectedCodegen<TestContext, Object> {
        @Override
        public SymbolProvider createSymbolProvider(CreateSymbolProvider<Object> directive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TestContext createContext(CreateContext<Object> directive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void generateService(GenerateService<TestContext, Object> directive) { }

        @Override
        public void generateResource(GenerateResource<TestContext, Object> directive) { }

        @Override
        public void generateStructure(GenerateStructure<TestContext, Object> directive) { }

        @Override
        public void generateError(GenerateError<TestContext, Object> directive) { }

        @Override
        public void generateUnion(GenerateUnion<TestContext, Object> directive) {}

        @Override
        public void customizeBeforeIntegrations(Customize<TestContext, Object> directive) {}

        @Override
        public void customizeAfterIntegrations(Customize<TestContext, Object> directive) {}
    }

    @Test
    public void validatesInput() {
        TestDirected testDirected = new TestDirected();
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, Object> runner = new DirectedCodegenRunner<>();

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
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, Object> runner = new DirectedCodegenRunner<>();

        FileManifest manifest = new MockManifest();
        runner.settings(new Object());
        runner.directedCodegen(testDirected);
        runner.fileManifest(manifest);
        runner.service(ShapeId.from("smithy.example#Foo"));
        runner.model(Model.builder().build());
        runner.integrationClass(TestIntegration.class);

        Assertions.assertThrows(ExpectationNotMetException.class, runner::run);
    }
}
