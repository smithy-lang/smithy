package software.amazon.smithy.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.processor.test.TestProcessorImplementation;

import static com.google.testing.compile.CompilationSubject.assertThat;

/**
 * Test the {@link software.amazon.smithy.processor.SmithyAnnotationProcessor}
 */
public class AnnotationProcessorTest {
    private static final Compiler compiler = Compiler.javac()
            .withProcessors(new TestProcessorImplementation());

    private static Compilation compilation;

    @BeforeAll
    static void compile() {
        compilation = compiler.compile(
                JavaFileObjects.forResource("testing/package-info.java")
        );
        assertThat(compilation).succeeded();
    }

    @Test
    void generatesBasicJavaFiles() {
        assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "com/example/testing/Empty.java")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("testing/Empty.java"));
    }

    @Test
    void generatesMetaInfFiles() {
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/com.example.spi.impl.Example")
                .contentsAsUtf8String()
                .isEqualTo("com.example.testing.Empty");
    }

    @Test
    void ignoresFile() {
        assertThat(compilation)
                .hadNoteContaining("Ignoring generated file: com/example/testing/Ignored.ignored");
    }
}
