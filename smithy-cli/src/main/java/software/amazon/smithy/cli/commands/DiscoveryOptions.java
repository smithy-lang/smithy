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

import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.HelpPrinter;

/**
 * Arguments available to commands that use model discovery.
 *
 * <p>TODO: It would be great to just always do model discovery and remove these hidden options.
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
