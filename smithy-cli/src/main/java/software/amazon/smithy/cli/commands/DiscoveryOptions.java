/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.HelpPrinter;

/**
 * Arguments available to commands that use model discovery.
 *
 * <p>This is currently only used by the build and validate commands as hidden options to support the Gradle plugin.
 */
final class DiscoveryOptions implements ArgumentReceiver {

    private String discoverClasspath;
    private boolean discover;

    @Override
    public void registerHelp(HelpPrinter printer) {
        /*
        Hide these for now until we figure out a plan forward for these.
        printer.option("--discover", "-d", "Enable model discovery, merging in models found inside of jars");
        printer.param("--discover-classpath", null, "CLASSPATH",
                            "Enable model discovery using a custom classpath for models");
        */
    }

    @Override
    public boolean testOption(String name) {
        switch (name) {
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
        if (name.equals("--discover-classpath")) {
            return value -> discoverClasspath = value;
        }
        return null;
    }

    String discoverClasspath() {
        return discoverClasspath;
    }

    boolean discover() {
        return discover;
    }
}
