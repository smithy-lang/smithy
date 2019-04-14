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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Creates a template engine that always injects default values
 * into the data model.
 *
 * <p>Default values can be overridden per/template by passing in a
 * different value in the data model when rendering templates.
 *
 * <pre>
 * {@code
 * TemplateEngine myEngine = createMyTemplateEngine();
 * TemplateEngine wrappedEngine = DefaultDataTemplateEngine.builder()
 *         .delegate(myEngine)
 *         .put("foo", "baz")
 *         .put("hello", true)
 *         .build();
 * assert(wrappedEngine.renderString("{{ foo }}") == "baz");
 * }
 * </pre>
 */
public final class DefaultDataTemplateEngine implements TemplateEngine {
    private final TemplateEngine delegate;
    private final Map<String, Object> defaultContext;

    public DefaultDataTemplateEngine(Map<String, Object> defaultContext, TemplateEngine delegate) {
        this.defaultContext = defaultContext;
        this.delegate = delegate;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void write(String templatePath, Writer out, Map<String, Object> dataModel) {
        delegate.write(templatePath, out, merge(dataModel));
    }

    private Map<String, Object> merge(Map<String, Object> map) {
        if (map.isEmpty()) {
            return defaultContext;
        } else if (defaultContext.isEmpty()) {
            return map;
        }

        Map<String, Object> result = new HashMap<>(defaultContext);
        result.putAll(map);
        return result;
    }

    /**
     * Builds a new DefaultDataTemplateEngine.
     */
    public static final class Builder {
        private TemplateEngine delegate;
        private final Map<String, Object> defaultContext = new HashMap<>();

        /**
         * Builds the DefaultDataTemplateEngine.
         *
         * @return Returns the new template engine.
         * @throws IllegalStateException if a delegate was not set.
         */
        public DefaultDataTemplateEngine build() {
            return new DefaultDataTemplateEngine(defaultContext, SmithyBuilder.requiredState("delegate", delegate));
        }

        /**
         * Sets the template engine to wrap and delegate to.
         *
         * @param delegate The template engine to wrap.
         * @return Returns the builder.
         */
        public Builder delegate(TemplateEngine delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * Sets a specific template variable.
         *
         * @param key Key to set.
         * @param value Value to set.
         * @return Returns the builder.
         */
        public Builder put(String key, Object value) {
            this.defaultContext.put(key, value);
            return this;
        }

        /**
         * Sets zero or more template variables from a map of key-value pairs.
         *
         * @param map Map of value to merge into the data model.
         * @return Returns the builder.
         */
        public Builder putAll(Map<String, Object> map) {
            this.defaultContext.putAll(map);
            return this;
        }
    }
}
