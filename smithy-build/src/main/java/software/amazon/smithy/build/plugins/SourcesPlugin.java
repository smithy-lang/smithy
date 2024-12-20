/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.build.SourcesConflictException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Copies model sources into the sources plugin.
 *
 * <p>Model sources are model components that were defined in one of the
 * directories marked as "sources" in the original model or a model
 * component that is found in the updated model but not the original model.
 *
 * <p>When a JAR is provided as a source, the models contained within the
 * JAR are extracted into the sources directory under a directory with the
 * same name as the JAR without the ".jar" extension; the JAR is not copied
 * into the sources directory. For example, given a JAR at "/foo/baz.jar"
 * that contains a "bar.smithy" file, a source will be created named
 * "baz/bar.smithy".
 *
 * <p>This plugin can only run if an original model is provided.
 */
public final class SourcesPlugin implements SmithyBuildPlugin {
    private static final String NAME = "sources";
    private static final String PROJECTED_FILENAME = "model.json";
    private static final Logger LOGGER = Logger.getLogger(SourcesPlugin.class.getName());

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(PluginContext context) {
        if (!context.getOriginalModel().isPresent()) {
            LOGGER.warning("No original model was provided, so the sources plugin cannot run");
            return;
        }

        List<String> names;
        String projectionName = context.getProjectionName();

        if (projectionName.equals("source")) {
            // Copy sources directly.
            names = copySources(context);
            LOGGER.fine(() -> String.format("Copying source files to the sources of %s: %s",
                    projectionName,
                    names));
        } else {
            // Extract source shapes, traits, and metadata from the projected model.
            LOGGER.fine(() -> String.format(
                    "Creating the `%s` sources by extracting relevant components from the original model",
                    projectionName));
            names = ListUtils.of(PROJECTED_FILENAME);
            projectSources(context);
        }

        String manifest = "";
        if (names.isEmpty()) {
            LOGGER.info(String.format("Writing empty `%s` manifest because no Smithy sources found", projectionName));
        } else {
            LOGGER.fine(() -> String.format("Writing `%s` manifest", projectionName));
            // Normalize filenames to Unix style.
            manifest = names.stream().map(name -> name.replace("\\", "/")).collect(Collectors.joining("\n"));
        }
        context.getFileManifest().writeFile("manifest", manifest + "\n");
    }

    private static List<String> copySources(PluginContext context) {
        List<String> names = new ArrayList<>();
        context.getSources().forEach(path -> copyDirectory(names, context.getFileManifest(), path, path));
        return names;
    }

    private static void copyDirectory(List<String> names, FileManifest manifest, Path root, Path current) {
        try {
            if (Files.isDirectory(current)) {
                try (Stream<Path> fileList = Files.list(current)) {
                    fileList.filter(p -> !p.equals(current))
                            .filter(p -> Files.isDirectory(p) || Files.isRegularFile(p))
                            .forEach(p -> copyDirectory(names, manifest, root, p));
                }
            } else if (Files.isRegularFile(current)) {
                if (current.toString().endsWith(".jar")) {
                    // Account for just a simple file vs recursing into directories.
                    String jarRoot = root.equals(current)
                            ? ""
                            : (root.relativize(current).toString() + File.separator);
                    // Copy Smithy models out of the JAR.
                    copyModelsFromJar(names, manifest, jarRoot, current);
                } else {
                    // Account for just a simple file vs recursing into directories.
                    Path target = root.equals(current) ? current.getFileName() : root.relativize(current);
                    copyFile(names, manifest, target, IoUtils.readUtf8File(current));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading the contents of " + current + ": " + e.getMessage(), e);
        }
    }

    private static void copyFile(List<String> names, FileManifest manifest, Path target, String contents) {
        // Path#getFileName might return null.
        if (target == null) {
            return;
        }

        if (manifest.hasFile(target)) {
            throw new SourcesConflictException(
                    "Source file conflict found when attempting to add `" + target + "` to the `sources` plugin "
                            + "output. All sources must have unique filenames relative to the directories marked as a "
                            + "'source'. The files and directories that make up sources are flattened into a single "
                            + "directory and conflicts are not allowed. The manifest has the following files: "
                            + ValidationUtils.tickedList(manifest.getFiles()));
        }

        String filename = target.toString();

        // Even though sources are filtered in SmithyBuild, it's theoretically possible that someone could call this
        // plugin manually. In that case, refuse to write unsupported files to the manifest.
        if (filename.endsWith(".smithy") || filename.endsWith(".json")) {
            manifest.writeFile(target, contents);
            names.add(target.toString());
        } else {
            LOGGER.warning("Omitting unrecognized file from Smithy model manifest: " + filename);
        }
    }

    private static void projectSources(PluginContext context) {
        Model updatedModel = context.getModel();

        // New shapes, trait definitions, and metadata are considered "sources".
        ObjectNode serialized = ModelSerializer
                .builder()
                .shapeFilter(context::isSourceShape)
                .metadataFilter(context::isSourceMetadata)
                .build()
                .serialize(updatedModel);

        context.getFileManifest().writeJson(PROJECTED_FILENAME, serialized);
    }

    private static void copyModelsFromJar(List<String> names, FileManifest manifest, String jarRoot, Path jarPath)
            throws IOException {
        LOGGER.fine(() -> "Copying models from JAR " + jarPath);
        URL manifestUrl = ModelDiscovery.createSmithyJarManifestUrl(jarPath.toString());

        String prefix = computeJarFilePrefix(jarRoot, jarPath);
        for (URL model : ModelDiscovery.findModels(manifestUrl)) {
            String name = ModelDiscovery.getSmithyModelPathFromJarUrl(model);
            Path target = Paths.get(prefix + name);
            LOGGER.finer(() -> "Copying " + name + " from JAR to " + target);
            try (InputStream is = model.openStream()) {
                copyFile(names, manifest, target, IoUtils.toUtf8String(is));
            }
        }
    }

    private static String computeJarFilePrefix(String jarRoot, Path jarPath) {
        Path jarFilenamePath = jarPath.getFileName();

        if (jarFilenamePath == null) {
            return jarRoot;
        }

        String jarFilename = jarFilenamePath.toString();
        return jarRoot + jarFilename.substring(0, jarFilename.length() - ".jar".length()) + File.separator;
    }
}
