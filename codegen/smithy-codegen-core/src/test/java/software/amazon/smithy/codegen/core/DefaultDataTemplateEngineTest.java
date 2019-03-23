/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DefaultDataTemplateEngineTest {
    @Test
    public void injectsDefaultValues() {
        DefaultDataTemplateEngine engine = DefaultDataTemplateEngine.builder()
                .put("foo", "baz")
                .delegate(new Custom())
                .build();

        assertThat(engine.render("foo"), equalTo("baz"));
        assertThat(engine.render("notThere"), equalTo("null"));
    }

    @Test
    public void canOverrideDefaults() {
        DefaultDataTemplateEngine engine = DefaultDataTemplateEngine.builder()
                .put("foo", "baz")
                .delegate(new Custom())
                .build();
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("foo", "qux");

        assertThat(engine.render("foo", dataModel), equalTo("qux"));
        assertThat(engine.render("notThere", dataModel), equalTo("null"));
    }

    @Test
    public void canPassInEmptyCustomValues() {
        DefaultDataTemplateEngine engine = DefaultDataTemplateEngine.builder().delegate(new Custom()).build();
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("foo", "qux");

        assertThat(engine.render("foo", dataModel), equalTo("qux"));
        assertThat(engine.render("notThere", dataModel), equalTo("null"));
    }

    private static final class Custom implements TemplateEngine {
        @Override
        public void write(String templatePath, Writer out, Map<String, Object> dataModel) {
            try {
                out.write(String.valueOf(dataModel.get(templatePath)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
