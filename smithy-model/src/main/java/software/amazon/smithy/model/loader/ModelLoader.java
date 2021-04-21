/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.IoUtils;

/**
 * Used to load Smithy models from .json, .smithy, and .jar files.
 */
final class ModelLoader {

    private static final Logger LOGGER = Logger.getLogger(ModelLoader.class.getName());
    private static final String SMITHY = "smithy";

    private ModelLoader() {}

    /**
     * Loads the contents of a model into a {@code ModelFile}.
     *
     * <p>The format contained in the supplied {@code InputStream} is
     * determined based on the file extension in the provided
     * {@code filename}.
     *
     * @param traitFactory Factory used to create traits.
     * @param properties Bag of loading properties.
     * @param filename Filename Filename to assign to the model.
     * @param contentSupplier The supplier that provides an InputStream. The
     *   supplied {@code InputStream} is automatically closed when the loader
     *   has finished reading from it.
     * @return Returns a {@code ModelFile} if the model could be loaded, or {@code null}.
     * @throws SourceException if there is an error reading from the contents.
     */
    static ModelFile load(
            TraitFactory traitFactory,
            Map<String, Object> properties,
            String filename,
            Supplier<InputStream> contentSupplier
    ) {
        if (filename.endsWith(".json")) {
            return loadParsedNode(traitFactory, Node.parse(contentSupplier.get(), filename));
        } else if (filename.endsWith(".smithy")) {
            String contents = IoUtils.toUtf8String(contentSupplier.get());
            return new IdlModelParser(traitFactory, filename, contents).parse();
        } else if (filename.endsWith(".jar")) {
            return loadJar(traitFactory, properties, filename);
        } else if (filename.equals(SourceLocation.NONE.getFilename())) {
            // Assume it's JSON if there's a N/A filename.
            return loadParsedNode(traitFactory, Node.parse(contentSupplier.get(), filename));
        } else {
            return null;
        }
    }

    // Loads all supported JSON formats. Each JSON format is expected to have
    // a top-level version property that contains a string. This version
    // is then used to delegate loading to different versions of the
    // Smithy JSON AST format.
    //
    // This loader supports version 1.0 and 1.1. Support for 0.5 and 0.4 was removed in 0.10.
    static ModelFile loadParsedNode(TraitFactory traitFactory, Node node) {
        ObjectNode model = node.expectObjectNode("Smithy documents must be an object. Found {type}.");
        StringNode version = model.expectStringMember(SMITHY);

        if (LoaderUtils.isVersionSupported(version.getValue())) {
            return AstModelLoader.INSTANCE.load(traitFactory, model);
        } else {
            throw new ModelSyntaxException("Unsupported Smithy version number: " + version.getValue(), version);
        }
    }

    // Allows importing JAR files by discovering models inside of a JAR file.
    // This is similar to model discovery, but done using an explicit import.
    private static ModelFile loadJar(TraitFactory traitFactory, Map<String, Object> properties, String filename) {
        List<ModelFile> modelFiles = new ArrayList<>();
        URL manifestUrl = ModelDiscovery.createSmithyJarManifestUrl(filename);
        LOGGER.fine(() -> "Loading Smithy model imports from JAR: " + manifestUrl);

        for (URL model : ModelDiscovery.findModels(manifestUrl)) {
            try {
                URLConnection connection = model.openConnection();

                if (properties.containsKey(ModelAssembler.DISABLE_JAR_CACHE)) {
                    connection.setUseCaches(false);
                }

                ModelFile innerResult = load(traitFactory, properties, model.toExternalForm(), () -> {
                    try {
                        return connection.getInputStream();
                    } catch (IOException e) {
                        throw throwIoJarException(model, e);
                    }
                });
                if (innerResult != null) {
                    modelFiles.add(innerResult);
                }
            } catch (IOException e) {
                throw throwIoJarException(model, e);
            }
        }

        return new CompositeModelFile(traitFactory, modelFiles);
    }

    private static ModelImportException throwIoJarException(URL model, Throwable e) {
        return new ModelImportException(
                String.format("Error loading Smithy model from URL `%s`: %s", model, e.getMessage()), e);
    }
}
