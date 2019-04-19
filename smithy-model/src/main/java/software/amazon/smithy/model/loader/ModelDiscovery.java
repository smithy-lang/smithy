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

package software.amazon.smithy.model.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Discovers Smithy models by finding all {@code META-INF/smithy/manifest}
 * files on the class path and loading all of the newline separated relative
 * model files referenced from the manifest.
 *
 * <p>The URLs discovered through model discovery are imported into a
 * {@code ModelAssembler} when {@link ModelAssembler#discoverModels} is
 * called, providing a mechanism for discovering Smithy models on the
 * classpath at runtime.
 *
 * <p>The format of a {@code META-INF/smithy/manifest} file is a newline
 * separated UTF-8 text file in which each line is a resource that is
 * relative to the manifest file. A line is considered to be terminated
 * by any one of a line feed ('\n'), a carriage return ('\r'), a carriage
 * return followed immediately by a line feed, or by reaching the end-of-file
 * (EOF). The resources referenced by a manifest are loaded by resolving the
 * relative resource against the URL that contains each {@code META-INF/smithy/manifest}
 * file found using {@link ClassLoader#getResources}.
 *
 * <p>The following restrictions and interpretations apply to the names of
 * model resources that can be placed on manifest lines:
 *
 * <ul>
 *     <li>Empty lines are ignored.</li>
 *     <li>Lines must contain only ASCII characters </li>
 *     <li>Lines must not start with "/" or end with "/". Models are resolved
 *     as relative resources to the manifest URL and expected to be
 *     contained within the same JAR/JMOD as the manifest.</li>
 *     <li>Lines must not contain empty segments (//).</li>
 *     <li>Lines must not contain dot-dot segments (..).</li>
 *     <li>Lines must not contain dot segments (/./) (./).</li>
 *     <li>Lines must not contain spaces ( ) or tabs (\t).</li>
 *     <li>Lines must not contain a backslash (\).</li>
 *     <li>Lines must not contain a question mark (?).</li>
 *     <li>Lines must not contain a percent sign (%).</li>
 *     <li>Lines must not contain an asterisk (*).</li>
 *     <li>Lines must not contain a colon (:).</li>
 *     <li>Lines must not contain a vertical bar (|).</li>
 *     <li>Lines must not contain a quote (") or (').</li>
 *     <li>Lines must not contain greater than (&gt;) or less than (&lt;) signs.</li>
 *     <li>Lines must not contain pound signs (#).</li>
 * </ul>
 *
 * <p>For example, given the following {@code META-INF/smithy/manifest} file
 * discovered at {@code jar:file:///C:/foo/baz/bar.jar!/META-INF/smithy/manifest}
 * on the class path,
 *
 * <pre>
 * smithy.example.traits.smithy
 * foo/another.file.smithy
 * </pre>
 *
 * <p>Smithy will attempt to discover the following models:
 *
 * <ul>
 *     <li>{@code jar:file:///C:/foo/baz/bar.jar!/META-INF/smithy/smithy.example.traits.smithy}</li>
 *     <li>{@code jar:file:///C:/foo/baz/bar.jar!/META-INF/smithy/foo/another.file.smithy}</li>
 * </ul>
 *
 * <p>Models defined in {@code META-INF/smithy} should be named after the
 * namespace that is defined within the file. Files that define multiple
 * namespaces are free to use whatever naming scheming they choose, but
 * model files should be globally unique in an application.
 */
public final class ModelDiscovery {
    private static final Logger LOGGER = Logger.getLogger(ModelDiscovery.class.getName());
    private static final String ROOT_RESOURCE_PATH = "META-INF/smithy/";
    private static final String MANIFEST = "manifest";
    private static final String MANIFEST_PATH = ROOT_RESOURCE_PATH + MANIFEST;
    private static final Pattern PROHIBITED_RESOURCE_SEGMENT_CHARS = Pattern.compile("[\t\\\\?%*:|\"'><# ]+");

    private ModelDiscovery() {}

    /**
     * Finds Smithy models using the thread context {@code ClassLoader}.
     *
     * @return Returns the URLs of each model referenced by manifests.
     */
    public static List<URL> findModels() {
        return findModels(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Finds Smithy models using the given {@code ClassLoader}.
     *
     * @param loader ClassLoader used to discover models.
     * @return Returns the URLs of each model referenced by manifests.
     */
    public static List<URL> findModels(ClassLoader loader) {
        try {
            Enumeration<URL> manifests = loader.getResources(MANIFEST_PATH);
            return findModels(Collections.list(manifests));
        } catch (IOException e) {
            throw new ModelManifestException("Error locating Smithy model manifests", e);
        }
    }

    /**
     * Parse the Smithy models from the given list of manifest URLs.
     *
     * @param manifestUrls Manifest URLs to parse line by line.
     * @return Returns the URLs of each model referenced by manifests.
     */
    public static List<URL> findModels(List<URL> manifestUrls) {
        List<URL> result = new ArrayList<>();

        for (URL manifest : manifestUrls) {
            LOGGER.finer(() -> "Found ModelDiscovery manifest at " + manifest);
            String modelUrlPrefix = manifest.toString();
            modelUrlPrefix = modelUrlPrefix.substring(0, modelUrlPrefix.length() - MANIFEST.length());

            try {
                for (String model : parseManifest(manifest)) {
                    URL modelUrl = new URL(modelUrlPrefix + model);
                    LOGGER.finest(() -> String.format("Found Smithy model `%s` in manifest `%s`", modelUrl, manifest));
                    result.add(modelUrl);
                }
            } catch (IOException e) {
                throw new ModelManifestException("Error parsing Smithy model manifest from " + manifest, e);
            }
        }

        return result;
    }

    private static Set<String> parseManifest(URL location) throws IOException {
        Set<String> models = new LinkedHashSet<>();
        URLConnection connection = location.openConnection();
        connection.setUseCaches(false);

        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                } else if (!line.isEmpty()) {
                    if (!isValidateResourceLine(line)) {
                        throw new ModelManifestException(String.format(
                                "Illegal Smithy model manifest syntax found in `%s`: `%s`", location, line));
                    }
                    models.add(line);
                }
            }
        }

        return models;
    }

    private static boolean isValidateResourceLine(String line) {
        for (String segment : line.split("/")) {
            // Ensure each segment is valid.
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return false;
            }
            // Ensure the segment contains only allowed characters.
            if (PROHIBITED_RESOURCE_SEGMENT_CHARS.matcher(segment).find()) {
                return false;
            }
        }

        // Don't allow trailing slashes.
        return line.charAt(line.length() - 1) != '/';
    }
}
