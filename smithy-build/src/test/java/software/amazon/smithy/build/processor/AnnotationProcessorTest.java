/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link SmithyAnnotationProcessor}
 */
public class AnnotationProcessorTest {
    private static final Compiler compiler = Compiler.javac()
            .withProcessors(new TestProcessorImplementation());

    private static Compilation compilation;

    @BeforeAll
    static void compile() {
        compilation = compiler.compile(JavaFileObjects.forResource(
                "software/amazon/smithy/build/processor/package-info.java"));
        CompilationSubject.assertThat(compilation).succeeded();
    }

    @Test
    void generatesBasicJavaFiles() {
        System.out.println(compilation.diagnostics());
        CompilationSubject.assertThat(compilation)
                .generatedFile(StandardLocation.SOURCE_OUTPUT, "com/example/testing/Empty.java")
                .contentsAsUtf8String()
                .isEqualTo(TestBuildPlugin.getEmptyClass("com.example.testing"));
    }

    @Test
    void generatesMetaInfFiles() {
        CompilationSubject.assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "META-INF/services/com.example.spi.impl.Example")
                .contentsAsUtf8String()
                .isEqualTo("com.example.testing.Empty");
    }

    @Test
    void ignoresFile() {
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining("Executing processor: TestProcessorImplementation...");
        CompilationSubject.assertThat(compilation).hadNoteContaining("Ignoring generated file: ");
        CompilationSubject.assertThat(compilation).hadNoteContaining("Ignored.ignored");
    }
}
