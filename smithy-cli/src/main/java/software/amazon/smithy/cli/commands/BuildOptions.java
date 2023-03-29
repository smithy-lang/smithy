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

import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.validation.Severity;

/**
 * Arguments available to commands that load and build models.
 */
final class BuildOptions implements ArgumentReceiver {

    static final String SEVERITY = "--severity";
    static final String ALLOW_UNKNOWN_TRAITS = "--allow-unknown-traits";
    static final String MODELS = "<MODELS>";

    private Severity severity;
    private String discoverClasspath;
    private boolean allowUnknownTraits;
    private boolean discover;
    private String output;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param(SEVERITY, null, "SEVERITY", "Set the minimum reported validation severity (one of NOTE, "
                                                  + "WARNING [default setting], DANGER, ERROR).");
        printer.option(ALLOW_UNKNOWN_TRAITS, null, "Ignore unknown traits when validating models");
        /*
        Hide these for now until we figure out a plan forward for these.
        printer.option(DISCOVER, "-d", "Enable model discovery, merging in models found inside of jars");
        printer.param(DISCOVER_CLASSPATH, null, "CLASSPATH",
                            "Enable model discovery using a custom classpath for models");
        */
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
            case "--discover":
            case "-d":
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
            case "--discover-classpath":
                return value -> discoverClasspath = value;
            case SEVERITY:
                return value -> {
                    severity(Severity.fromString(value).orElseThrow(() -> {
                        return new CliError("Invalid severity level: " + value);
                    }));
                };
            default:
                return null;
        }
    }

    String discoverClasspath() {
        return discoverClasspath;
    }

    boolean allowUnknownTraits() {
        return allowUnknownTraits;
    }

    boolean discover() {
        return discover;
    }

    String output() {
        return output;
    }

    void severity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Get the severity level, taking into account standard options that affect the default.
     *
     * @param options Standard options to query if no severity is explicitly set.
     * @return Returns the resolved severity option.
     */
    Severity severity(StandardOptions options) {
        if (severity != null) {
            return severity;
        } else if (options.quiet()) {
            return Severity.DANGER;
        } else {
            return Severity.WARNING;
        }
    }
}
