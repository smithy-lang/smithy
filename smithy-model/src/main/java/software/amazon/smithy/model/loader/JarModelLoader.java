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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Supplier;
import java.util.logging.Logger;
import software.amazon.smithy.utils.IoUtils;

/**
 * Allows importing JAR files by discovering models inside of a JAR file.
 *
 * <p>This is similar to model discovery, but done using an explicit import.
 */
final class JarModelLoader implements ModelLoader {
    private static final Logger LOGGER = Logger.getLogger(JarModelLoader.class.getName());
    private final ModelLoader delegate;

    JarModelLoader(ModelLoader delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean load(String filename, Supplier<String> contentSupplier, LoaderVisitor visitor) {
        if (!filename.endsWith(".jar")) {
            return delegate.load(filename, contentSupplier, visitor);
        }

        URL manifestUrl = ModelDiscovery.createSmithyJarManifestUrl(filename);
        LOGGER.fine(() -> "Loading Smithy model imports from JAR: " + manifestUrl);

        for (URL model : ModelDiscovery.findModels(manifestUrl)) {
            try (InputStream is = model.openStream()) {
                String contents = IoUtils.toUtf8String(is);
                delegate.load(model.toExternalForm(), () -> contents, visitor);
            } catch (IOException e) {
                throw new ModelImportException(
                        String.format("Error loading Smithy model from URL `%s`: %s", model, e.getMessage()), e);
            }
        }

        return true;
    }
}
