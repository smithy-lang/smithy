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

import java.util.List;
import java.util.function.Supplier;
import software.amazon.smithy.model.node.DefaultNodeFactory;
import software.amazon.smithy.utils.ListUtils;

/**
 * Used to load Smithy models.
 *
 * <p>Model loaders should look at the file extension of the provided model
 * and decide if they are the right loader to attempt to load the file. A
 * loader must return true when it does handle loading the given content, or
 * false if it is not responsible for loading the given content.
 */
@FunctionalInterface
interface ModelLoader {
    /**
     * Attempts to load the given filename and mutate the loader visitor.
     *
     * @param filename File being loaded. The provided file is either a path or a URL.
     * @param contentSupplier Method that supplies the contents of the file.
     * @param visitor The visitor to update.
     * @return Returns true if this loader was used, false otherwise.
     * @throws ModelSyntaxException when the format of the contents is invalid.
     */
    boolean load(String filename, Supplier<String> contentSupplier, LoaderVisitor visitor);

    /**
     * Creates a Model loader from many loaders.
     *
     * @param loaders Loaders to compose into a single model loader.
     * @return Returns the create ModelLoader
     */
    static ModelLoader composeLoaders(List<ModelLoader> loaders) {
        return (filename, contents, visitor) -> {
            for (ModelLoader modelLoader : loaders) {
                if (modelLoader.load(filename, contents, visitor)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Creates the default ModelLoader implementation used by the ModelAssembler.
     *
     * @return Returns the default model loader.
     */
    static ModelLoader createDefaultLoader() {
        ModelLoader delegate = ModelLoader.composeLoaders(ListUtils.of(
                new NodeModelLoader(new DefaultNodeFactory()),
                new SmithyModelLoader()));
        return new JarModelLoader(delegate);
    }
}
