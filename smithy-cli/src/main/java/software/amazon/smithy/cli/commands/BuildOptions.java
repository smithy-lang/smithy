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
import software.amazon.smithy.cli.HelpPrinter;

/**
 * Arguments available to commands that load and build models.
 */
final class BuildOptions implements ArgumentReceiver {

    public static final String ALLOW_UNKNOWN_TRAITS = "--allow-unknown-traits";
    public static final String MODELS = "<MODELS>";

    private String discoverClasspath;
    private boolean allowUnknownTraits;
    private boolean discover;
    private String output;

    @Override
    public void registerHelp(HelpPrinter printer) {
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

    public String output() {
        return output;
    }
}
