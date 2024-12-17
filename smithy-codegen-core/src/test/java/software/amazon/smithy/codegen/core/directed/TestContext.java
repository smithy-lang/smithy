/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

final class TestContext implements CodegenContext<TestSettings, TestWriter, TestIntegration> {

    private final Model model;
    private final TestSettings settings;
    private final SymbolProvider symbolProvider;
    private final FileManifest fileManifest;
    private final WriterDelegator<TestWriter> delegator;
    private final ServiceShape service;

    static TestContext create(String modelFile, ShapeId serviceId) {
        FileManifest manifest = new MockManifest();
        SymbolProvider symbolProvider = (shape) -> Symbol.builder()
                .name(shape.getId().getName())
                .namespace("example", ".")
                .build();
        WriterDelegator<TestWriter> delegator = new WriterDelegator<>(manifest, symbolProvider, (file, namespace) -> {
            throw new UnsupportedOperationException();
        });
        Model model = Model.assembler()
                .addImport(TestContext.class.getResource(modelFile))
                .assemble()
                .unwrap();
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        return new TestContext(model, new TestSettings(), symbolProvider, manifest, delegator, service);
    }

    TestContext(
            Model model,
            TestSettings settings,
            SymbolProvider symbolProvider,
            FileManifest fileManifest,
            WriterDelegator<TestWriter> delegator,
            ServiceShape service
    ) {
        this.model = model;
        this.settings = settings;
        this.symbolProvider = symbolProvider;
        this.fileManifest = fileManifest;
        this.delegator = delegator;
        this.service = service;
    }

    @Override
    public Model model() {
        return model;
    }

    @Override
    public TestSettings settings() {
        return settings;
    }

    @Override
    public SymbolProvider symbolProvider() {
        return symbolProvider;
    }

    @Override
    public FileManifest fileManifest() {
        return fileManifest;
    }

    @Override
    public WriterDelegator<TestWriter> writerDelegator() {
        return delegator;
    }

    @Override
    public List<TestIntegration> integrations() {
        return new ArrayList<>();
    }

    public ServiceShape service() {
        return service;
    }
}
