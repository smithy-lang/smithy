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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.MavenDependencyResolver;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.StringUtils;

final class WarmupCommand extends SimpleCommand {

    private static final Logger LOGGER = Logger.getLogger(WarmupCommand.class.getName());

    private enum Phase { WRAPPER, CLASSES, DUMP }

    private static final class Config implements ArgumentReceiver {
        private Phase phase = Phase.WRAPPER;

        @Override
        public Consumer<String> testParameter(String name) {
            if (name.equals("--phase")) {
                return phase -> this.phase = Phase.valueOf(phase.toUpperCase(Locale.ENGLISH));
            } else {
                return null;
            }
        }
    }

    WarmupCommand(String parentCommandName) {
        super(parentCommandName);
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    protected void configureArgumentReceivers(Arguments arguments) {
        arguments.addReceiver(new Config());
    }

    @Override
    public String getName() {
        return "warmup";
    }

    @Override
    public String getSummary() {
        return "Creates caches that speed up the CLI. This is typically performed during the installation.";
    }

    @Override
    public int run(Arguments arguments, Env env, List<String> models) {
        boolean isDebug = arguments.getReceiver(StandardOptions.class).debug();
        Phase phase = arguments.getReceiver(Config.class).phase;
        LOGGER.info(() -> "Optimizing the Smithy CLI: " + phase);

        switch (phase) {
            case WRAPPER:
                return orchestrate(isDebug, env.stderr());
            case CLASSES:
            case DUMP:
            default:
                return runCodeToOptimize(arguments, env);
        }
    }

    private int orchestrate(boolean isDebug, CliPrinter printer) {
        List<String> baseArgs = new ArrayList<>();
        String classpath = getOrThrowIfUndefinedProperty("java.class.path");
        Path javaHome = Paths.get(getOrThrowIfUndefinedProperty("java.home"));
        Path lib = javaHome.resolve("lib");
        Path bin = javaHome.resolve("bin");
        Path windowsBinary = bin.resolve("java.exe");
        Path posixBinary = bin.resolve("java");
        Path jsaFile = lib.resolve("smithy.jsa");
        Path classListFile = lib.resolve("classlist");

        // Delete the archive and classlist before regenerating them.
        classListFile.toFile().delete();
        jsaFile.toFile().delete();

        if (!Files.isDirectory(bin)) {
            throw new CliError("$JAVA_HOME/bin directory not found: " + bin);
        } else if (Files.exists(windowsBinary)) {
            baseArgs.add(windowsBinary.toString());
        } else if (Files.exists(posixBinary)) {
            baseArgs.add(posixBinary.toString());
        } else {
            throw new CliError("No java binary found in " + bin);
        }

        baseArgs.add("-classpath");
        baseArgs.add(classpath);

        try {
            // Run the command in a temp directory to avoid building whatever project the cwd might be in.
            Path baseDir = Files.createTempDirectory("smithy-warmup");

            LOGGER.info("Building class list");
            callJava(Phase.CLASSES, isDebug, printer, baseDir, baseArgs,
                     "-Xshare:off", "-XX:DumpLoadedClassList=" + classListFile,
                     SmithyCli.class.getName(), "warmup");

            LOGGER.info("Building archive from classlist");
            callJava(Phase.WRAPPER, isDebug, printer, baseDir, baseArgs,
                     "-XX:SharedClassListFile=" + classListFile, "-Xshare:dump", "-XX:SharedArchiveFile=" + jsaFile,
                     SmithyCli.class.getName(), "warmup");

            LOGGER.info("Validating that the archive was created correctly");
            callJava(null, isDebug, printer, baseDir, baseArgs,
                     "-Xshare:on", "-XX:SharedArchiveFile=" + jsaFile,
                     SmithyCli.class.getName(), "--help");

            classListFile.toFile().delete();
            return 0;
        } catch (IOException e) {
            throw new CliError("Error running warmup command", 1, e);
        }
    }

    private String getOrThrowIfUndefinedProperty(String property) {
        String result = System.getProperty(property);
        if (StringUtils.isEmpty(result)) {
            throw new CliError(result + " system property is not defined");
        }
        return result;
    }

    private void callJava(
            Phase phase,
            boolean isDebug,
            CliPrinter printer,
            Path baseDir,
            List<String> baseArgs,
            String... args) {
        List<String> resolved = new ArrayList<>(baseArgs);
        Collections.addAll(resolved, args);

        if (isDebug) {
            resolved.add("--debug");
        }

        if (phase != null) {
            resolved.add("--phase");
            resolved.add(phase.toString());
        }

        LOGGER.fine(() -> "Running Java command: " + resolved);

        StringBuilder builder = new StringBuilder();
        int result = IoUtils.runCommand(resolved, baseDir, builder, MapUtils.of());

        // Hide the output unless an error occurred or running in debug mode.
        if (isDebug || result != 0) {
            printer.println(builder.toString().trim());
        }

        if (result != 0) {
            throw new CliError("Error warming up CLI in phase " + phase, result);
        }
    }

    private int runCodeToOptimize(Arguments arguments, Env env) {
        try {
            Path tempDirWithPrefix = Files.createTempDirectory("smithy-warmup");
            DependencyResolver resolver = new MavenDependencyResolver(tempDirWithPrefix.toString());

            resolve(resolver);
            // Resolve again, but find it in the cache.
            resolve(resolver);

            // Create and load SmithyBuild files.
            File buildFile = tempDirWithPrefix.resolve("smithy-build.json").toFile();
            try (FileWriter writer = new FileWriter(buildFile)) {
                writer.write("{\n"
                             + "  \"version\": \"1.0\",\n"
                             + "  \"maven\": {\"dependencies\": [\"software.amazon.smithy:smithy-model:"
                             + SmithyCli.getVersion() + "\"]}\n"
                             + "}");
            }

            SmithyBuildConfig.builder().load(buildFile.toPath()).build();

            new ValidateCommand("a", (c, e) -> resolver).execute(arguments, env);
            new BuildCommand("a", (c, e) -> resolver).execute(arguments, env);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private void resolve(DependencyResolver resolver) {
        resolver.addRepository(MavenRepository.builder().url("https://repo.maven.apache.org/maven2").build());
        // Use a version that we know exists in Maven Central. The version doesn't matter because it will be ignored
        // when the result is filtered. This ensures that things like pre-release builds that aren't in Central
        // work correctly.
        resolver.addDependency("software.amazon.smithy:smithy-model:1.26.0");
        resolver.resolve();
    }
}
