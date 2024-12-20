/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.ListUtils;

// This test basically just ensures the generics used in SmithyIntegration work
// like we expect.
public class SmithyIntegrationTest {

    private static final class MySettings {}

    private static final class MyContext implements CodegenContext<MySettings, MySimpleWriter, MyIntegration> {
        @Override
        public Model model() {
            return null;
        }

        @Override
        public MySettings settings() {
            return null;
        }

        @Override
        public SymbolProvider symbolProvider() {
            return null;
        }

        @Override
        public FileManifest fileManifest() {
            return null;
        }

        @Override
        public WriterDelegator<MySimpleWriter> writerDelegator() {
            return null;
        }

        @Override
        public List<MyIntegration> integrations() {
            return null;
        }
    }

    private static final class MyIntegration implements SmithyIntegration<MySettings, MySimpleWriter, MyContext> {
        private final String name;

        MyIntegration(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<? extends CodeInterceptor<? extends CodeSection, MySimpleWriter>> interceptors(MyContext context) {
            return ListUtils.of(new MyInterceptor1(), new MyInterceptor2());
        }
    }

    private static final class SomeSection implements CodeSection {}

    private static final class MyInterceptor1 implements CodeInterceptor.Appender<SomeSection, MySimpleWriter> {
        @Override
        public Class<SomeSection> sectionType() {
            return SomeSection.class;
        }

        @Override
        public void append(MySimpleWriter writer, SomeSection section) {
            writer.write("Hi1");
        }
    }

    private static final class MyInterceptor2 implements CodeInterceptor.Appender<SomeSection, MySimpleWriter> {
        @Override
        public Class<SomeSection> sectionType() {
            return SomeSection.class;
        }

        @Override
        public void append(MySimpleWriter writer, SomeSection section) {
            writer.write("Hi2");
        }
    }

    @Test
    public void canRegisterInterceptors() {
        MyIntegration integration = new MyIntegration("Foo");
    }
}
