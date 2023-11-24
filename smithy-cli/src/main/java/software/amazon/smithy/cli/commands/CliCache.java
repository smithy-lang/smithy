/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.utils.IoUtils;

interface CliCache {
    Path DEFAULT_TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"));
    Path ROOT_CACHE_DIR = DEFAULT_TEMP_DIR.resolve("smithy-cache");

    static CliCache getTemplateCache() {
        return () -> ROOT_CACHE_DIR.resolve("templates");
    }

    Path getPath();

    default boolean clear() {
        return IoUtils.rmdir(getPath());
    }

    default Path get() {
        Path cachePath = getPath();
        if (Files.exists(cachePath)) {
            return cachePath;
        }
        // If cache dir does not exist, create it and all required parent directories
        try {
            return Files.createDirectories(cachePath);
        } catch (IOException e) {
            throw new CliError("Could not create cache at path: " + cachePath);
        }
    }

    static boolean clearAll() {
        return IoUtils.rmdir(ROOT_CACHE_DIR);
    }
}
