/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.smf.SmfReader;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.IoUtils;

/**
 * Used to load Smithy models from .json, .smithy, and .jar files.
 */
final class ModelLoader {

    private static final Logger LOGGER = Logger.getLogger(ModelLoader.class.getName());

    private ModelLoader() {}

    /**
     * Parses models and pushes {@link LoadOperation}s to the given consumer.
     *
     * <p>The format contained in the supplied {@code InputStream} is
     * determined based on the file extension in the provided
     * {@code filename}.
     *
     * @param traitFactory Factory used to create traits.
     * @param properties Bag of loading properties.
     * @param filename Filename to assign to the model.
     * @param operationConsumer Where loader operations are published.
     * @param contentSupplier The supplier that provides an InputStream. The
     *   supplied {@code InputStream} is automatically closed when the loader
     *   has finished reading from it.
     * @return Returns true if the file was loaded. Some JSON files might be ignored and return false.
     * @throws SourceException if there is an error reading from the contents.
     */
    static boolean load(
            TraitFactory traitFactory,
            Map<String, Object> properties,
            String filename,
            Consumer<LoadOperation> operationConsumer,
            Supplier<InputStream> contentSupplier,
            Function<CharSequence, String> stringTable
    ) {
        try {
            if (filename.endsWith(".smithy")) {
                try (InputStream inputStream = contentSupplier.get()) {
                    String contents = IoUtils.toUtf8String(inputStream);
                    new IdlModelLoader(filename, contents, stringTable).parse(operationConsumer);
                }
                return true;
            } else if (filename.endsWith(".smf")) {
                try (InputStream inputStream = contentSupplier.get()) {
                    byte[] data = IoUtils.toByteArray(inputStream);
                    // Skip CRC for JAR-sourced files (URL contains "!/")
                    boolean verifyCrc = !filename.contains("!/");
                    loadSmf(data, operationConsumer, verifyCrc);
                }
                return true;
            } else if (filename.endsWith(".jar")) {
                loadJar(traitFactory, properties, filename, operationConsumer, stringTable);
                return true;
            } else if (filename.endsWith(".json") || filename.equals(SourceLocation.NONE.getFilename())) {
                try (InputStream inputStream = contentSupplier.get();
                        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    // Assume it's JSON if there's an N/A filename. Stream the AST directly from the
                    // reader, materializing Nodes only for trait/metadata values.
                    return JsonAstLoader.load(JsonAstReader.from(filename, reader, false), operationConsumer);
                }
            } else {
                if (!filename.endsWith("manifest")) {
                    LOGGER.warning(() -> "Ignoring unrecognized Smithy model file: " + filename);
                }
                return false;
            }
        } catch (IOException e) {
            throw new ModelImportException("Error loading " + filename + ": " + e.getMessage(), e);
        }
    }

    // Attempts to load a Smithy AST JSON model. JSON files that do not contain a top-level "smithy" key are skipped
    // and false is returned. The "smithy" version is used to delegate loading to different versions of the Smithy
    // JSON AST format.
    //
    // This loader supports version 1.0 and 2.0. Support for 0.5 and 0.4 was removed in 0.10.
    static boolean loadParsedNode(Node node, Consumer<LoadOperation> operationConsumer) {
        if (node.isObjectNode()) {
            ObjectNode model = node.expectObjectNode();
            if (model.containsMember("smithy")) {
                return JsonAstLoader.load(new NodeAstReader(model), operationConsumer);
            }
        }

        LOGGER.info("Ignoring unrecognized JSON file: " + node.getSourceLocation());
        return false;
    }

    private static void loadSmf(byte[] data, Consumer<LoadOperation> operationConsumer, boolean verifyCrc) {
        SmfReader.readInto(data, new SmfReader.LoadHandler() {
            final Version version = Version.VERSION_2_0;

            @Override
            public void modelVersion() {
                operationConsumer.accept(new LoadOperation.ModelVersion(version, SourceLocation.NONE));
            }

            @Override
            public void metadata(String key, Node value) {
                operationConsumer.accept(new LoadOperation.PutMetadata(version, key, value));
            }

            @Override
            public void defineShape(
                    AbstractShapeBuilder<?, ?> builder,
                    List<MemberShape.Builder> members
            ) {
                LoadOperation.DefineShape op = new LoadOperation.DefineShape(version, builder);
                for (MemberShape.Builder mb : members) {
                    op.addMember(mb);
                }
                operationConsumer.accept(op);
            }
        }, verifyCrc);
    }

    // Allows importing JAR files by discovering models inside a JAR file.
    // This is similar to model discovery, but done using an explicit import.
    private static void loadJar(
            TraitFactory traitFactory,
            Map<String, Object> properties,
            String filename,
            Consumer<LoadOperation> operationConsumer,
            Function<CharSequence, String> stringTable
    ) {
        URL manifestUrl = ModelDiscovery.createSmithyJarManifestUrl(filename);
        LOGGER.fine(() -> "Loading Smithy model imports from JAR: " + manifestUrl);

        List<URL> models;
        try {
            models = ModelDiscovery.findModels(manifestUrl);
        } catch (ModelManifestException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                LOGGER.info(() -> "Ignoring JAR without Smithy manifest: " + filename);
                return;
            }
            throw e;
        }

        for (URL model : models) {
            try {
                URLConnection connection = model.openConnection();

                if (properties.containsKey(ModelAssembler.DISABLE_JAR_CACHE)) {
                    connection.setUseCaches(false);
                }

                boolean result = load(traitFactory, properties, model.toExternalForm(), operationConsumer, () -> {
                    try {
                        return connection.getInputStream();
                    } catch (IOException e) {
                        throw throwIoJarException(model, e);
                    }
                }, stringTable);

                // Smithy will skip unrecognized model files, including JSON files that don't contain a "smithy"
                // version key/value pair. However, JAR manifests are not allowed to refer to unrecognized files.
                if (!result) {
                    throw new ModelImportException("Invalid file referenced by Smithy JAR manifest: " + model);
                }
            } catch (IOException e) {
                throw throwIoJarException(model, e);
            }
        }
    }

    private static ModelImportException throwIoJarException(URL model, Throwable e) {
        return new ModelImportException(
                String.format("Error loading Smithy model from URL `%s`: %s", model, e.getMessage()),
                e);
    }
}
