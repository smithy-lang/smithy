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

package software.amazon.smithy.cli;

import java.io.File;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.smithy.cli.commands.BuildCommand;
import software.amazon.smithy.cli.commands.DiffCommand;
import software.amazon.smithy.cli.commands.GenerateCommand;
import software.amazon.smithy.cli.commands.OptimizeCommand;
import software.amazon.smithy.cli.commands.ValidateCommand;

/**
 * Entry point of the Smithy CLI.
 *
 * This CLI uses the following environment variables:
 *
 * <ul>
 *     <li>SMITHY_HOME: The home directory where Smithy module libraries,
 *     and other configuration settings are stored. Defaults to the
 *     user home directory + ".smithy" (e.g., {@code ~/.smithy}.</li>
 *     <li>SMITHY_MODULE_PATH: A ";" separated list of module paths to
 *     load when running the Smithy CLI. Defaults to "$SMITHY_HOME/modules".
 *     See {@link ModuleFinder#of} for details on how modules are loaded.</li>
 * </ul>
 */
public final class SmithyCli {
    private static final String SMITHY_HOME = "SMITHY_HOME";
    private static final String SMITHY_MODULE_PATH = "SMITHY_MODULE_PATH";

    private SmithyCli() {}

    public static void main(String... args) {
        run(System::exit, args);
    }

    public static int run(Consumer<Integer> exitFunction, String... args) {
        Cli cli = new Cli("smithy");
        cli.addCommand(new ValidateCommand());
        cli.addCommand(new BuildCommand());
        cli.addCommand(new DiffCommand());
        cli.addCommand(new GenerateCommand());
        cli.addCommand(new OptimizeCommand());
        int code = cli.run(args);
        if (code != 0) {
            exitFunction.accept(code);
        }

        return 0;
    }

    public static String getSmithyHome() {
        String home = System.getenv(SMITHY_HOME);
        return home != null ? home : System.getProperty("user.home") + File.separator + ".smithy";
    }

    public static String getDefaultModulePath() {
        return getSmithyHome() + File.separator + "modules";
    }

    public static Path[] getModulePaths() {
        return getModulePaths(System.getenv(SMITHY_MODULE_PATH), getDefaultModulePath());
    }

    static Path[] getModulePaths(String firstCheck, String fallback) {
        return Optional.ofNullable(firstCheck)
                .map(value -> value.split(";"))
                .map(values -> {
                    // Filter out duplicate paths and paths that don't exist.
                    Set<Path> uniqueValues = new HashSet<>();
                    for (var value : values) {
                        if (!value.isBlank()) {
                            var path = Paths.get(value);
                            if (Files.exists(path)) {
                                uniqueValues.add(path);
                            }
                        }
                    }
                    return uniqueValues.toArray(new Path[0]);
                })
                .orElseGet(() -> {
                    // Return the default module path if it exists.
                    var defaultPath = Paths.get(fallback);
                    return Files.exists(defaultPath) ? new Path[]{defaultPath} : new Path[0];
                });
    }

    public static ModuleLayer createModuleLayer() {
        Path[] modulePaths = getModulePaths();
        Module currentModule = SmithyCli.class.getModule();

        // Don't assemble a module layer if no module paths are found.
        if (modulePaths.length == 0) {
            return currentModule.getLayer();
        }

        var moduleFinder = ModuleFinder.of(modulePaths);
        // Gather the name of the current module and modules found in the path.
        var moduleNames = moduleFinder.findAll().stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .collect(Collectors.toSet());
        moduleNames.add(currentModule.getName());

        var parent = currentModule.getLayer();
        var cf = parent.configuration().resolve(moduleFinder, ModuleFinder.of(), moduleNames);
        return parent.defineModulesWithManyLoaders(cf, SmithyCli.class.getClassLoader());
    }
}
