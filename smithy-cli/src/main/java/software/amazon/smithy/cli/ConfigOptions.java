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

package software.amazon.smithy.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;

public final class ConfigOptions implements ArgumentReceiver {

    private static final Logger LOGGER = Logger.getLogger(ConfigOptions.class.getName());
    private final List<String> config = new ArrayList<>();

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param("--config", "-c", "CONFIG_PATH...",
                      "Path to smithy-build.json configuration (defaults to './smithy-build.json'). "
                      + "This option can be repeated and each configured will be merged.");
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case "--config":
            case "-c":
                return config::add;
            default:
                return null;
        }
    }

    public List<String> config() {
        List<String> config = this.config;
        if (config.isEmpty()) {
            Path defaultConfig = Paths.get("smithy-build.json").toAbsolutePath();
            if (Files.exists(defaultConfig)) {
                LOGGER.fine("Detected smithy-build.json at " + defaultConfig);
                config = Collections.singletonList(defaultConfig.toString());
            }
        }
        return config;
    }

    public SmithyBuildConfig createSmithyBuildConfig() {
        long startTime = System.nanoTime();
        SmithyBuildConfig smithyBuildConfig;
        List<String> config = config();

        if (config.isEmpty()) {
            smithyBuildConfig = SmithyBuildConfig.builder().version(SmithyBuild.VERSION).build();
        } else {
            LOGGER.fine(() -> String.format("Loading Smithy configs: [%s]", String.join(" ", config)));
            SmithyBuildConfig.Builder configBuilder = SmithyBuildConfig.builder();
            // Set the lastModified time in millis of the builder to the latest modified date of any config.
            long newestLastModified = 0;
            for (String configFile : config) {
                File file = new File(configFile);
                newestLastModified = Math.max(newestLastModified, file.lastModified());
                configBuilder.load(file.toPath());
            }
            configBuilder.lastModifiedInMillis(newestLastModified);
            smithyBuildConfig = configBuilder.build();
        }

        LOGGER.fine(() -> "Smithy config load time in ms: " + ((System.nanoTime() - startTime) / 1000000));
        return smithyBuildConfig;
    }
}
