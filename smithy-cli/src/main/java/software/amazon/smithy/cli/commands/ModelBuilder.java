/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.sourcecontext.SourceContextLoader;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Loads, builds, and report validation issues with models.
 */
final class ModelBuilder {

    private static final Logger LOGGER = Logger.getLogger(ModelBuilder.class.getName());
    private static final String CLEAR_LINE_ESCAPE = "\033[2K\r";
    private static final int DEFAULT_CODE_LINES = 6;

    private Validator.Mode validationMode;
    private CliPrinter validationPrinter;
    private Arguments arguments;
    private List<String> models;
    private Command.Env env;
    private SmithyBuildConfig config;
    private Severity defaultSeverity;
    private ValidatedResult<Model> validatedResult;
    private String titleLabel;
    private Style[] titleLabelStyles;
    private ValidationEventFormatOptions.Format validationOutputFormat;
    private boolean disableOutputFormatFraming = false;
    private boolean disableConfigModels;

    public ModelBuilder arguments(Arguments arguments) {
        this.arguments = arguments;

        // Determine how to format the output, whether it's text (the default) or CSV.
        // Only some commands (like validate) actually let you customize the output format, so assume a default.
        if (validationOutputFormat == null) {
            validationOutputFormat(arguments.hasReceiver(ValidationEventFormatOptions.class)
                    ? arguments.getReceiver(ValidationEventFormatOptions.class).format()
                    : ValidationEventFormatOptions.Format.TEXT);
        }

        return this;
    }

    public ModelBuilder disableOutputFormatFraming(boolean disableOutputFormatFraming) {
        this.disableOutputFormatFraming = disableOutputFormatFraming;
        return this;
    }

    public ModelBuilder validationOutputFormat(ValidationEventFormatOptions.Format validationOutputFormat) {
        this.validationOutputFormat = validationOutputFormat;
        return this;
    }

    public ModelBuilder models(List<String> models) {
        validatedResult = null;
        this.models = models;
        return this;
    }

    public ModelBuilder env(Command.Env env) {
        this.env = env;
        return this;
    }

    public ModelBuilder validationPrinter(CliPrinter validationPrinter) {
        this.validationPrinter = validationPrinter;
        return this;
    }

    public ModelBuilder validationMode(Validator.Mode validationMode) {
        this.validationMode = validationMode;
        return this;
    }

    public ModelBuilder config(SmithyBuildConfig config) {
        this.config = config;
        return this;
    }

    public ModelBuilder defaultSeverity(Severity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
        return this;
    }

    public ModelBuilder validatedResult(ValidatedResult<Model> validatedResult) {
        this.validatedResult = validatedResult;
        return this;
    }

    public ModelBuilder titleLabel(String titleLabel, Style... styles) {
        this.titleLabel = titleLabel;
        this.titleLabelStyles = styles;
        return this;
    }

    public ModelBuilder disableConfigModels(boolean disableConfigModels) {
        this.disableConfigModels = disableConfigModels;
        return this;
    }

    public Model build() {
        SmithyBuilder.requiredState("arguments", arguments);
        SmithyBuilder.requiredState("models", models);
        SmithyBuilder.requiredState("env", env);
        SmithyBuilder.requiredState("config", config);

        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        BuildOptions buildOptions = arguments.getReceiver(BuildOptions.class);

        // Resolve validator options and severity.
        ValidatorOptions validatorOptions = arguments.hasReceiver(ValidatorOptions.class)
                ? arguments.getReceiver(ValidatorOptions.class)
                : new ValidatorOptions();
        resolveMinSeverity(standardOptions, validatorOptions);

        ClassLoader classLoader = env.classLoader();
        ColorFormatter colors = env.colors();
        CliPrinter stderr = env.stderr();

        if (validationPrinter == null) {
            validationPrinter = env.stderr();
        }

        if (validationMode == null) {
            validationMode = Validator.Mode.from(standardOptions);
        }

        if (validatedResult == null) {
            ModelAssembler assembler = createModelAssembler(classLoader);

            if (validationMode == Validator.Mode.QUIET_CORE_ONLY) {
                assembler.disableValidation();
            }

            // Emit status updates.
            AtomicInteger issueCount = new AtomicInteger();
            assembler.validationEventListener(createStatusUpdater(standardOptions, colors, stderr, issueCount));

            handleModelDiscovery(assembler, classLoader, config);
            handleUnknownTraitsOption(buildOptions, assembler);

            // Add imports and sources from the config by default, but this can be disabled (e.g., smithy diff).
            if (!disableConfigModels) {
                config.getSources().forEach(assembler::addImport);
                config.getImports().forEach(assembler::addImport);
            }

            models.forEach(assembler::addImport);
            validatedResult = assembler.assemble();
            clearStatusUpdateIfPresent(issueCount, stderr);
        }

        // Sort events by file so that we can efficiently read files for context sequentially.
        List<ValidationEvent> sortedEvents = new ArrayList<>(validatedResult.getValidationEvents());
        sortedEvents.sort(Comparator.comparing(ValidationEvent::getSourceLocation));

        SourceContextLoader sourceContextLoader = validatedResult.getResult()
                .map(model -> SourceContextLoader.createModelAwareLoader(model, DEFAULT_CODE_LINES))
                .orElseGet(() -> SourceContextLoader.createLineBasedLoader(DEFAULT_CODE_LINES));
        PrettyAnsiValidationFormatter formatter = PrettyAnsiValidationFormatter.builder()
                .sourceContextLoader(sourceContextLoader)
                .colors(colors)
                .titleLabel(titleLabel, titleLabelStyles)
                .build();

        if (!disableOutputFormatFraming) {
            validationOutputFormat.beginPrinting(validationPrinter);
        }

        for (ValidationEvent event : sortedEvents) {
            // Only log events that are >= --severity. Note that setting --quiet inherently
            // configures events to need to be >= DANGER. Also filter using --show-validators and --hide-validators.
            if (validatorOptions.isVisible(event)) {
                validationOutputFormat.print(validationPrinter, formatter, event);
            }
        }

        if (!disableOutputFormatFraming) {
            validationOutputFormat.endPrinting(validationPrinter);
        }

        env.flush();
        // Note: disabling validation will still show a summary of failures if the model can't be loaded.
        Validator.validate(validationMode != Validator.Mode.ENABLE, colors, stderr, validatedResult);
        env.flush();

        return validatedResult.getResult().orElseThrow(() -> new RuntimeException("Expected Validator to throw"));
    }

    static Consumer<ValidationEvent> createStatusUpdater(
            StandardOptions standardOptions,
            ColorFormatter colors,
            CliPrinter stderr,
            AtomicInteger issueCount
    ) {
        // Only show the status if not quiet and the terminal supports ANSI.
        if (standardOptions.quiet() || !colors.isColorEnabled()) {
            return null;
        }

        return event -> {
            if (event.getSeverity() != Severity.SUPPRESSED) {
                int encountered = issueCount.incrementAndGet();
                String line = "Validating model: " + encountered + " issues";
                if (encountered > 1) {
                    line = '\r' + line;
                }
                stderr.append(line);
                stderr.flush();
            }
        };
    }

    // If a status update was printed, then clear it out.
    static void clearStatusUpdateIfPresent(AtomicInteger issueCount, CliPrinter stderr) {
        if (issueCount.get() > 0) {
            stderr.append(CLEAR_LINE_ESCAPE);
            stderr.flush();
        }
    }

    private static void handleUnknownTraitsOption(BuildOptions options, ModelAssembler assembler) {
        if (options.allowUnknownTraits()) {
            LOGGER.fine("Ignoring unknown traits");
            assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        }
    }

    private void handleModelDiscovery(ModelAssembler assembler, ClassLoader baseLoader, SmithyBuildConfig config) {
        String discoverClasspath = null;
        boolean discover = false;
        if (arguments.hasReceiver(DiscoveryOptions.class)) {
            DiscoveryOptions discoveryOptions = arguments.getReceiver(DiscoveryOptions.class);
            discoverClasspath = discoveryOptions.discoverClasspath();
            discover = discoveryOptions.discover();
        }

        if (discoverClasspath != null) {
            discoverModelsWithClasspath(discoverClasspath, assembler);
        } else if (shouldDiscoverDependencies(config, discover)) {
            assembler.discoverModels(baseLoader);
        }
    }

    private boolean shouldDiscoverDependencies(SmithyBuildConfig config, boolean discoverModels) {
        if (discoverModels) {
            return true;
        } else {
            return config.getMaven().isPresent()
                   && EnvironmentVariable.SMITHY_DEPENDENCY_MODE.get().equals("standard");
        }
    }

    private static void discoverModelsWithClasspath(String rawClasspath, ModelAssembler assembler) {
        LOGGER.finer("Discovering models with classpath: " + rawClasspath);

        // Use System.getProperty here each time since it allows the value to be changed.
        String[] classpath = rawClasspath.split(System.getProperty("path.separator"));
        URL[] urls = new URL[classpath.length];

        for (int i = 0; i < classpath.length; i++) {
            try {
                urls[i] = Paths.get(classpath[i]).toUri().toURL();
            } catch (MalformedURLException e) {
                throw new CliError("Error parsing model discovery URL: " + classpath[i]);
            }
        }

        URLClassLoader urlClassLoader = new URLClassLoader(urls);
        assembler.discoverModels(urlClassLoader);
    }

    // Determine a default severity if one wasn't given, by inspecting if there is a --severity option.
    private Severity resolveMinSeverity(StandardOptions standardOptions, ValidatorOptions validatorOption) {
        if (defaultSeverity != null && validatorOption.severity() == null) {
            validatorOption.severity(defaultSeverity);
        }
        return validatorOption.detectAndGetSeverity(standardOptions);
    }

    static ModelAssembler createModelAssembler(ClassLoader classLoader) {
        return Model.assembler(classLoader).putProperty(ModelAssembler.DISABLE_JAR_CACHE, true);
    }
}
