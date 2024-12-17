/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.HelpPrinter;

/**
 * Arguments available to commands that load and build models.
 */
final class BuildOptions implements ArgumentReceiver {

    static final String ALLOW_UNKNOWN_TRAITS = "--allow-unknown-traits";
    static final String ALLOW_UNKNOWN_TRAITS_SHORT = "--aut";
    static final String MODELS = "<MODELS>";

    private boolean allowUnknownTraits;
    private String output;
    private boolean noPositionalArguments;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(ALLOW_UNKNOWN_TRAITS,
                ALLOW_UNKNOWN_TRAITS_SHORT,
                "Ignore unknown traits when validating models.");
        printer.param("--output",
                null,
                "OUTPUT_PATH",
                "Where to write Smithy artifacts, caches, and other files (defaults to './build/smithy').");

        if (!noPositionalArguments) {
            printer.positional(MODELS, "Model files and directories to load.");
        }
    }

    @Override
    public boolean testOption(String name) {
        if (ALLOW_UNKNOWN_TRAITS.equals(name) || ALLOW_UNKNOWN_TRAITS_SHORT.equalsIgnoreCase(name)) {
            allowUnknownTraits = true;
            return true;
        }
        return false;
    }

    @Override
    public Consumer<String> testParameter(String name) {
        if ("--output".equals(name)) {
            return value -> output = value;
        }
        return null;
    }

    boolean allowUnknownTraits() {
        return allowUnknownTraits;
    }

    String output() {
        return output;
    }

    void noPositionalArguments(boolean noPositionalArguments) {
        this.noPositionalArguments = noPositionalArguments;
    }

    /**
     * Resolves the correct build directory by looking at the --output argument, outputDirectory config setting,
     * and finally the default build directory.
     *
     * @param config Config to check.
     * @return Returns the resolved build directory.
     */
    Path resolveOutput(SmithyBuildConfig config) {
        if (output != null) {
            return Paths.get(output);
        } else {
            return config.getOutputDirectory()
                    .map(Paths::get)
                    .orElseGet(SmithyBuild::getDefaultOutputDirectory);
        }
    }
}
