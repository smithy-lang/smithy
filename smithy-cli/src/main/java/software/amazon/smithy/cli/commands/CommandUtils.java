/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.ContextualValidationEventFormatter;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

final class CommandUtils {

    private static final Logger LOGGER = Logger.getLogger(CommandUtils.class.getName());

    private CommandUtils() {}

    static Model buildModel(Arguments arguments, ClassLoader classLoader, Set<Validator.Feature> features) {
        Severity minSeverity = arguments.has(SmithyCli.SEVERITY)
                ? parseSeverity(arguments.parameter(SmithyCli.SEVERITY))
                : Severity.NOTE;
        return buildModel(
                arguments.positionalArguments(),
                classLoader,
                minSeverity,
                arguments.has(SmithyCli.DISCOVER),
                arguments.parameter(SmithyCli.DISCOVER_CLASSPATH, null),
                arguments.has(SmithyCli.ALLOW_UNKNOWN_TRAITS),
                features
        );
    }

    static Model buildModel(
            List<String> models,
            ClassLoader classLoader,
            Severity minSeverity,
            boolean discover,
            String discoverPath,
            boolean allowUnknownTraits
    ) {
        return buildModel(
                models, classLoader, minSeverity, discover, discoverPath, allowUnknownTraits, Collections.emptySet());
    }

    static Model buildModel(
            List<String> models,
            ClassLoader classLoader,
            Severity minSeverity,
            boolean discover,
            String discoverPath,
            boolean allowUnknownTraits,
            Set<Validator.Feature> features
    ) {
        ModelAssembler assembler = CommandUtils.createModelAssembler(classLoader);

        ContextualValidationEventFormatter formatter = new ContextualValidationEventFormatter();
        boolean stdout = features.contains(Validator.Feature.STDOUT);
        boolean quiet = features.contains(Validator.Feature.QUIET);
        Consumer<String> writer = stdout ? Cli.getStdout() : Cli.getStderr();

        assembler.validationEventListener(event -> {
            // Only log events that are >= --severity.
            if (event.getSeverity().ordinal() >= minSeverity.ordinal()) {
                if (event.getSeverity() == Severity.WARNING && !quiet) {
                    // Only log warnings when not quiet
                    Colors.YELLOW.write(writer, formatter.format(event) + System.lineSeparator());
                } else if (event.getSeverity() == Severity.DANGER || event.getSeverity() == Severity.ERROR) {
                    // Always output error and danger events, even when quiet.
                    Colors.RED.write(writer, formatter.format(event) + System.lineSeparator());
                } else if (!quiet) {
                    writer.accept(formatter.format(event) + System.lineSeparator());
                }
            }
        });

        if (discoverPath != null) {
            discoverModelsWithClasspath(discoverPath, assembler);
        } else if (discover) {
            assembler.discoverModels(classLoader);
        }

        if (allowUnknownTraits) {
            LOGGER.fine("Ignoring unknown traits");
            assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        }

        models.forEach(assembler::addImport);
        ValidatedResult<Model> result = assembler.assemble();
        Validator.validate(result, features);
        return result.getResult().orElseThrow(() -> new RuntimeException("Expected Validator to throw"));
    }

    static Severity parseSeverity(String str) {
        return Severity.fromString(str).orElseThrow(() -> new IllegalArgumentException(
                "Invalid severity: " + str + ". Expected one of: " + Arrays.toString(Severity.values())));
    }

    static ModelAssembler createModelAssembler(ClassLoader classLoader) {
        return Model.assembler(classLoader).putProperty(ModelAssembler.DISABLE_JAR_CACHE, true);
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

        // See http://findbugs.sourceforge.net/bugDescriptions.html#DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            URLClassLoader urlClassLoader = new URLClassLoader(urls);
            assembler.discoverModels(urlClassLoader);
            return null;
        });
    }
}
