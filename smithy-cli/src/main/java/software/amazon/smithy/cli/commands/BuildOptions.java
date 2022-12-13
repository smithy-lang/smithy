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
    public static final String MODELS = "<MODELS>";

    private String discoverClasspath;
    private boolean allowUnknownTraits;
    private boolean discover;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(ALLOW_UNKNOWN_TRAITS, null, "Ignores unknown traits when validating models");
        printer.option(DISCOVER, "-d", "Enables model discovery, merging in models found inside of jars");
        printer.param(DISCOVER_CLASSPATH, null, "CLASSPATH",
                            "Enables model discovery using a custom classpath for models");
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
        if (DISCOVER_CLASSPATH.equals(name)) {
            return value -> discoverClasspath = value;
        }

        return null;
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
}
