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

package software.amazon.smithy.cli.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Arguments available to commands that load and build models.
 */
@SmithyInternalApi
public final class BuildOptions implements ArgumentReceiver {

    public static final String ALLOW_UNKNOWN_TRAITS = "--allow-unknown-traits";
    public static final String DISCOVER = "--discover";
    public static final String DISCOVER_SHORT = "-d";
    public static final String DISCOVER_CLASSPATH = "--discover-classpath";
    public static final String DEPENDENCY_MODE = "--dependency-mode";
    public static final String MODELS = "<MODELS>";

    private String discoverClasspath;
    private boolean allowUnknownTraits;
    private boolean discover;
    private DependencyMode dependencyMode = DependencyMode.STANDARD;
    private String output;

    /** Dependency resolution mode of the CLI. */
    public enum DependencyMode {
        /** Standard dependency resolution mode, resolving dependencies using Maven. */
        STANDARD,

        /** Disables dependency resolution by ignoring dependencies. */
        IGNORE,

        /** Forbids dependency resolution. If dependencies are declared, the CLI will fail to run. */
        FORBID
    }

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(ALLOW_UNKNOWN_TRAITS, null, "Ignores unknown traits when validating models");
        printer.option(DISCOVER, "-d", "Enables model discovery, merging in models found inside of jars");
        printer.param(DISCOVER_CLASSPATH, null, "CLASSPATH",
                            "Enables model discovery using a custom classpath for models");
        printer.option(DEPENDENCY_MODE, null, "(ignore|forbid|standard) Allows dependencies to be ignored or forbidden "
                                              + "by setting to 'ignore' or 'forbid'. Defaults to 'standard', allowing "
                                              + "dependencies to be declared and resolved using Maven.");
        printer.param("--output", null, "OUTPUT_PATH",
                      "Where to write Smithy artifacts, caches, and other files (defaults to './build/smithy').");
        printer.positional(MODELS, "Model files and directories to load");
    }

    @Override
    public boolean testOption(String name) {
        switch (name) {
            case ALLOW_UNKNOWN_TRAITS:
                allowUnknownTraits = true;
                return true;
            case DISCOVER:
            case DISCOVER_SHORT:
                discover = true;
                return true;
            default:
                return false;
        }
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case "--output":
                return value -> output = value;
            case DISCOVER_CLASSPATH:
                return value -> discoverClasspath = value;
            case DEPENDENCY_MODE:
                return value -> {
                    try {
                        dependencyMode = DependencyMode.valueOf(value.toUpperCase(Locale.ENGLISH));
                    } catch (IllegalArgumentException e) {
                        List<DependencyMode> expected = Arrays.asList(DependencyMode.values());
                        throw new CliError(String.format("Invalid %s parameter: '%s'. Expected one of: %s",
                                                         DEPENDENCY_MODE, value, expected));
                    }
                };
            default:
                return null;
        }
    }

    public String discoverClasspath() {
        return discoverClasspath;
    }

    public boolean allowUnknownTraits() {
        return allowUnknownTraits;
    }

    public boolean discover() {
        return discover;
    }

    public DependencyMode dependencyMode() {
        return dependencyMode;
    }

    public String output() {
        return output;
    }

    public boolean useModelDiscovery(SmithyBuildConfig config) {
        return discover() || (config.getMaven().isPresent() && dependencyMode == DependencyMode.STANDARD);
    }
}
