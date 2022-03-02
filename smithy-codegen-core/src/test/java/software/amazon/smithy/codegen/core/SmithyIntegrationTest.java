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

package software.amazon.smithy.codegen.core;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.ListUtils;

// This test basically just ensures the generics used in SmithyIntegration work
// like we expect.
public class SmithyIntegrationTest {

    private static final class MySettings implements SmithyCodegenSettings {
        @Override
        public ShapeId service() {
            return ShapeId.from("smithy.example#MyService");
        }
    }

    private static final class MyContext implements CodegenContext<MySettings> {
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
    }

    private static final class MyIntegration implements SmithyIntegration<
            MySettings, CodeWriter, MyContext> {
        private final String name;

        MyIntegration(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<? extends CodeInterceptor<? extends CodeSection, CodeWriter>> interceptors(MyContext context) {
            return ListUtils.of(new MyInterceptor1(), new MyInterceptor2());
        }
    }

    private static final class SomeSection implements CodeSection {}

    private static final class MyInterceptor1 implements CodeInterceptor.Appender<SomeSection, CodeWriter> {
        @Override
        public Class<SomeSection> sectionType() {
            return SomeSection.class;
        }

        @Override
        public void append(CodeWriter writer, SomeSection section) {
            writer.write("Hi1");
        }
    }

    private static final class MyInterceptor2 implements CodeInterceptor.Appender<SomeSection, CodeWriter> {
        @Override
        public Class<SomeSection> sectionType() {
            return SomeSection.class;
        }

        @Override
        public void append(CodeWriter writer, SomeSection section) {
            writer.write("Hi2");
        }
    }

    @Test
    public void canRegisterInterceptors() {
        MyIntegration integration = new MyIntegration("Foo");
    }
}
