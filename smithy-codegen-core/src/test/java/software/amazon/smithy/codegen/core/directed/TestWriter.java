package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.SymbolWriter;

final class TestWriter extends SymbolWriter<TestWriter, TestImports> {
    public TestWriter() {
        super(new TestImports());
    }
}
