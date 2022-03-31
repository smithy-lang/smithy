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

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.codegen.core.ImportContainer;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.ShapeId;

// TODO: fill in these test
public class DirectedCodegenRunnerTest {

    private static final class TestImports implements ImportContainer {
        Map<String, Symbol> imports = new TreeMap<>();

        @Override
        public void importSymbol(Symbol symbol, String alias) {
            imports.put(alias, symbol);
        }
    }

    private static final class TestWriter extends SymbolWriter<TestWriter, TestImports> {
        public TestWriter() {
            super(new TestImports());
        }

        @Override
        public TestWriter writeDocs(Consumer<TestWriter> consumer) {
            consumer.accept(this);
            return this;
        }
    }

    interface TestIntegration extends SmithyIntegration<Object, TestWriter, TestContext> {}

    private static final class Delegator extends WriterDelegator<TestWriter> {
        Delegator(FileManifest manifest, SymbolProvider symbolProvider, SymbolWriter.Factory<TestWriter> factory) {
            super(manifest, symbolProvider, factory);
        }
    }

    private static final class TestDirected implements DirectedCodegen<TestContext, Object, Delegator> {
        @Override
        public SymbolProvider createSymbolProvider(CreateSymbolProvider<Object> directive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Delegator createWriterDelegator(CreateWriterDelegator<Object, Delegator> directive) {
            return new Delegator(directive.fileManifest(), directive.symbolProvider(), (f, n) -> {
                TestWriter writer = new TestWriter();
                writer.setRelativizeSymbols(n);
                return writer;
            });
        }

        @Override
        public TestContext createContext(CreateContext<Object, Delegator> directive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void generateService(GenerateService<TestContext, Object, Delegator> directive) { }

        @Override
        public void generateResource(GenerateResource<TestContext, Object, Delegator> directive) { }

        @Override
        public void generateStructure(GenerateStructure<TestContext, Object, Delegator> directive) { }

        @Override
        public void generateError(GenerateError<TestContext, Object, Delegator> directive) { }

        @Override
        public void generateUnion(GenerateUnion<TestContext, Object, Delegator> directive) {}

        @Override
        public void finalizeBeforeIntegrations(Finalize<TestContext, Object, Delegator> directive) {}

        @Override
        public void finalizeAfterIntegrations(Finalize<TestContext, Object, Delegator> directive) {}
    }

    @Test
    public void validatesInput() {
        TestDirected testDirected = new TestDirected();
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, Object, Delegator> runner
                = new DirectedCodegenRunner<>();

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
        DirectedCodegenRunner<TestWriter, TestIntegration, TestContext, Object, Delegator> runner
                = new DirectedCodegenRunner<>();

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
