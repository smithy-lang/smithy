package software.amazon.smithy.codegen.freemarker;

import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.DefaultDataTemplateEngine;
import software.amazon.smithy.codegen.core.TemplateEngine;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides SmithyCodegen templating support for FreeMarker.
 *
 * @see <a href="https://freemarker.apache.org/">FreeMarker</a>
 */
public final class FreeMarkerEngine implements TemplateEngine {
    private final Configuration freeMarkerConfig;

    /**
     * @param freeMarkerConfig FreeMarker configuration.
     */
    public FreeMarkerEngine(Configuration freeMarkerConfig) {
        this.freeMarkerConfig = freeMarkerConfig;
    }

    /**
     * Creates a FreeMarker template engine builder.
     *
     * <p>The builder by default will automatically register a "StringUtils"
     * variable that provides access to all of the static methods defined in
     * {@link StringUtils}.
     *
     * @return Returns the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void write(String templatePath, Writer out, Map<String, Object> dataModel) {
        try {
            Template template = freeMarkerConfig.getTemplate(templatePath);
            template.process(dataModel, out);
        } catch (IOException | TemplateException e) {
            throw new CodegenException(String.format(
                    "Error evaluating FreeMarker template [%s]: %s", templatePath, e.getMessage()), e);
        }
    }

    /**
     * Builds a new FreeMarker template engine.
     */
    public static final class Builder implements SmithyBuilder<TemplateEngine> {
        private Configuration config;
        private boolean disableObjectWrapper;
        private boolean disableStringUtils;
        private TemplateLoader templateLoader;
        private ClassLoader classLoader;
        private Class classLoaderClass;
        private final Map<String, Object> defaultProperties = new HashMap<>();

        @Override
        public TemplateEngine build() {
            if (config == null) {
                config = new Configuration(Configuration.VERSION_2_3_28);
            }

            config.setDefaultEncoding("UTF-8");
            config.setLogTemplateExceptions(false);

            if (!disableObjectWrapper) {
                config.setObjectWrapper(new SmithyObjectWrapper(Configuration.VERSION_2_3_28));
            }

            if (templateLoader != null) {
                config.setTemplateLoader(templateLoader);
            } else if (classLoader != null) {
                config.setClassLoaderForTemplateLoading(classLoader, "");
            } else if (classLoaderClass != null) {
                config.setClassForTemplateLoading(classLoaderClass, "");
            } else {
                throw new IllegalStateException(
                        "No ClassLoader, Class, or TemplateLoader set on FreeMarkerTemplate.Builder");
            }

            if (!disableStringUtils) {
                putDefaultProperty("StringUtils", loadStringUtilsIntoTemplateModel());
            }

            return DefaultDataTemplateEngine.builder()
                    .putAll(defaultProperties)
                    .delegate(new FreeMarkerEngine(config))
                    .build();
        }

        /**
         * Sets a custom FreeMarker {@link Configuration} object for use cases
         * that require full customization of the template engine.
         *
         * @param config Configuration object to set.
         * @return Returns the builder.
         */
        public Builder setConfig(Configuration config) {
            this.config = config;
            return this;
        }

        /**
         * Disables the automatic registering of the Smithy ObjectWrapper that
         * will make FreeMarker treat Smithy's {@link Node} objects like normal
         * values (for example, an {@link ObjectNode} can be accessed like a
         * {@link Map}, an {@link ArrayNode} can be accessed like a
         * {@link List}, and all of the scalar nodes like {@link StringNode},
         * {@link BooleanNode}, {@link NumberNode}, and {@link BooleanNode}
         * are accessed like a Java built-in types), empty {@link Optional}
         * values are treated like null, an Optional with a value is
         * automatically unwrapped, and a {@link Stream} is treated like an
         * {@link Iterator}.
         *
         * @return Returns the builder.
         */
        public Builder disableObjectWrapper() {
            this.disableObjectWrapper = true;
            return this;
        }

        /**
         * Disables the automatic registering of the StringUtils variable in
         * every template.
         *
         * @return Returns the builder.
         */
        public Builder disableStringUtils() {
            this.disableStringUtils = true;
            return this;
        }

        /**
         * Sets a custom FreeMarker template loader.
         *
         * @param templateLoader Template loader to set.
         * @return Returns the builder.
         */
        public Builder templateLoader(TemplateLoader templateLoader) {
            this.templateLoader = templateLoader;
            return this;
        }

        /**
         * Sets a ClassLoader to use for loading templates.
         *
         * <p>Using a ClassLoader will require that templates are loaded
         * using the entire package name to a resource with no leading "/".
         *
         * @param classLoader ClassLoader to use for loading templates.
         * @return Returns the builder.
         */
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Sets a Class to use for loading templates.
         *
         * <p>Using a Class allows for relative loading of templates
         * from the Class package.
         *
         * @param classLoader ClassLoader to use for loading templates.
         * @return Returns the builder.
         */
        public Builder classLoader(Class classLoader) {
            this.classLoaderClass = classLoader;
            return this;
        }

        /**
         * Adds a default property to every template's data model.
         *
         * @param key Name of the property.
         * @param value Value to set.
         * @return Returns the builder.
         */
        public Builder putDefaultProperty(String key, Object value) {
            defaultProperties.put(key, value);
            return this;
        }

        /**
         * Adds default properties to every template's data model.
         *
         * @param map Properties to add.
         * @return Returns the builder.
         */
        public Builder putDefaultProperties(Map<String, Object> map) {
            defaultProperties.putAll(map);
            return this;
        }

        private static TemplateModel loadStringUtilsIntoTemplateModel() {
            try {
                BeansWrapper wrapper = new BeansWrapper(Configuration.VERSION_2_3_28);
                TemplateHashModel statics = wrapper.getStaticModels();
                return statics.get(StringUtils.class.getName());
            } catch (TemplateException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
