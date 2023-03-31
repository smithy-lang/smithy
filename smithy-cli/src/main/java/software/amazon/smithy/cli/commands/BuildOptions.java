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

    static final String ALLOW_UNKNOWN_TRAITS = "--allow-unknown-traits";
    static final String MODELS = "<MODELS>";

    private boolean allowUnknownTraits;
    private String output;
    private boolean noPositionalArguments;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.option(ALLOW_UNKNOWN_TRAITS, null, "Ignore unknown traits when validating models.");
        printer.param("--output", null, "OUTPUT_PATH",
                      "Where to write Smithy artifacts, caches, and other files (defaults to './build/smithy').");

        if (!noPositionalArguments) {
            printer.positional(MODELS, "Model files and directories to load.");
        }
    }

    @Override
    public boolean testOption(String name) {
        if (ALLOW_UNKNOWN_TRAITS.equals(name)) {
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
}
