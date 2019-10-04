/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;

final class CommandUtils {
    private static final Logger LOGGER = Logger.getLogger(CommandUtils.class.getName());

    private CommandUtils() {}

    static void handleUnknownTraitsOption(Arguments arguments, ModelAssembler assembler) {
        if (arguments.has(SmithyCli.ALLOW_UNKNOWN_TRAITS)) {
            LOGGER.fine("Ignoring unknown traits");
            assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        }
    }

    static ModelAssembler createModelAssembler(ClassLoader classLoader) {
        return Model.assembler(classLoader).putProperty(ModelAssembler.DISABLE_JAR_CACHE, true);
    }

    static void handleModelDiscovery(Arguments arguments, ModelAssembler assembler, ClassLoader baseLoader) {
        if (arguments.has(SmithyCli.DISCOVER_CLASSPATH)) {
            discoverModelsWithClasspath(arguments, assembler);
        } else if (arguments.has(SmithyCli.DISCOVER)) {
            assembler.discoverModels(baseLoader);
        }
    }

    private static void discoverModelsWithClasspath(Arguments arguments, ModelAssembler assembler) {
        String rawClasspath = arguments.parameter(SmithyCli.DISCOVER_CLASSPATH);
        LOGGER.finer("Discovering models with classpath: " + rawClasspath);
        String[] classpath = rawClasspath.split(":");
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
