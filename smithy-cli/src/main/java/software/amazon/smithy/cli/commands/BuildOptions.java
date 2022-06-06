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
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Arguments available to commands that load and build models.
 */
@SmithyInternalApi
public final class BuildOptions implements ArgumentReceiver {

    private String discoverClasspath;
    private boolean allowUnknownTraits;
    private boolean discover;

    public static void printHelp(CliPrinter printer) {
        printer.println(printer.style("    --allow-unknown-traits", Style.YELLOW));
        printer.println("        Ignores unknown traits when validating models");
        printer.println(printer.style("    --discover, -d", Style.YELLOW));
        printer.println("        Enables model discovery, merging in models found inside of jars");
        printer.println(printer.style("    --discover-classpath CLASSPATH", Style.YELLOW));
        printer.println("        Enables model discovery using a custom classpath for models");
    }

    @Override
    public boolean testOption(String name) {
        switch (name) {
            case "--allow-unknown-traits":
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
        if ("--discover-classpath".equals(name)) {
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
