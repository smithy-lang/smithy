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

package software.amazon.smithy.build.plugins;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Runs a process-based plugin.
 */
public final class RunPlugin implements SmithyBuildPlugin {
    private static final Logger LOGGER = Logger.getLogger(RunPlugin.class.getName());
    private final Path root = Paths.get(".").toAbsolutePath().normalize();

    @Override
    public String getName() {
        return "run";
    }

    @Override
    public boolean requiresValidModel() {
        return true;
    }

    @Override
    public void execute(PluginContext context) {
        NodeMapper mapper = new NodeMapper();
        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);
        Settings settings = mapper.deserializeInto(context.getSettings(), new Settings());
        String artifactName = context.getArtifactName()
                .orElseThrow(() -> new SmithyBuildException("The run plugin requires an artifact name"));

        if (settings.command().isEmpty()) {
            throw new SmithyBuildException("Missing required command setting");
        }

        ensureDirectoryExists(context);
        useProjectRelativeCommandIfPossible(settings.command());
        runCommand(artifactName, context, settings);
    }

    @SmithyInternalApi
    public static final class Settings {
        private List<String> command = Collections.emptyList();
        private Map<String, String> env = Collections.emptyMap();
        private boolean sendPrelude;

        public List<String> command() {
            return command;
        }

        public void command(List<String> command) {
            this.command = command;
        }

        public boolean sendPrelude() {
            return sendPrelude;
        }

        public void sendPrelude(boolean sendPrelude) {
            this.sendPrelude = sendPrelude;
        }

        public Map<String, String> env() {
            return env;
        }

        public void env(Map<String, String> env) {
            this.env = env;
        }
    }

    private void ensureDirectoryExists(PluginContext context) {
        try {
            Files.createDirectories(context.getFileManifest().getBaseDir());
        } catch (IOException e) {
            throw new SmithyBuildException("Error creating plugin directory for " + getName(), e);
        }
    }

    private void runCommand(String artifactName, PluginContext context, Settings settings) {
        Path baseDir = context.getFileManifest().getBaseDir();
        List<String> command = settings.command();
        Map<String, String> env = prepareEnvironment(context, settings);
        InputStream inputStream = serializeModel(settings, context.getModel());
        Appendable appendable = new StringBuilder();

        LOGGER.fine(() -> "Running command for artifact "
                          + context.getArtifactName().orElse(getName())
                          + ": " + command);

        int result;
        try {
            result = IoUtils.runCommand(command, baseDir, inputStream, appendable, env);
        } catch (RuntimeException e) {
            throw new SmithyBuildException("Error running process `" + String.join(" ", command) + "` for '"
                                           + artifactName + "': " + e.getMessage(), e);
        }

        if (result != 0) {
            throw new SmithyBuildException(("Error exit code " + result + " returned from: `"
                                           + String.join(" ", command) + "`: " + appendable).trim());
        }

        LOGGER.fine(() -> command.get(0) + " output: " + appendable);
    }

    private void useProjectRelativeCommandIfPossible(List<String> command) {
        // Check if the command is found relative to the working directory.
        Path resolvedRelativeCommand = root.resolve(command.get(0));

        if (Files.isExecutable(resolvedRelativeCommand)) {
            String absolute = resolvedRelativeCommand.toAbsolutePath().toString();
            LOGGER.fine(() -> "Found command " + command.get(0) + " relative to current directory: " + absolute);
            command.set(0, absolute);
        }
    }

    private Map<String, String> prepareEnvironment(PluginContext context, Settings settings) {
        Map<String, String> env = new HashMap<>(settings.env);
        env.putIfAbsent("SMITHY_ROOT_DIR", root.toString());
        env.putIfAbsent("SMITHY_PLUGIN_DIR", context.getFileManifest().getBaseDir().toString());
        env.putIfAbsent("SMITHY_PROJECTION_NAME", context.getProjectionName());
        env.putIfAbsent("SMITHY_ARTIFACT_NAME", context.getArtifactName().orElse(""));
        env.putIfAbsent("SMITHY_INCLUDES_PRELUDE", String.valueOf(settings.sendPrelude()));
        return env;
    }

    private InputStream serializeModel(Settings settings, Model model) {
        ModelSerializer serializer = ModelSerializer.builder()
                .includePrelude(settings.sendPrelude())
                .build();
        String jsonModel = Node.printJson(serializer.serialize(model));
        return new ByteArrayInputStream(jsonModel.getBytes(StandardCharsets.UTF_8));
    }
}
