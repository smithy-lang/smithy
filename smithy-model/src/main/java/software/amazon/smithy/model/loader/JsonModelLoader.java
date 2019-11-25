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

import java.util.function.Supplier;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * A singleton implementation of a ModelLoader that loads all JSON
 * formats. Each JSON format is expected to have a top-level
 * {@code version} property that contains a string. This version
 * is then used to delegate loading to different versions of the
 * Smithy JSON AST format.
 *
 * <p>This loader supports version 0.4.0 and 0.5.0.
 */
enum JsonModelLoader implements ModelLoader {

    INSTANCE;

    private static final String SMITHY = "smithy";

    @Override
    public boolean load(String filename, Supplier<String> contentSupplier, LoaderVisitor visitor) {
        if (!guessIfJson(filename, contentSupplier)) {
            return false;
        }

        Node node = Node.parse(contentSupplier.get(), filename);
        return loadParsedNode(visitor, node);
    }

    private boolean guessIfJson(String path, Supplier<String> contentSupplier) {
        if (path.endsWith(".json")) {
            return true;
        }

        // Loads the contents of a file if the file has a source location
        // of "N/A", isn't empty, and the first character is "{".
        String contents = contentSupplier.get();
        return path.equals(SourceLocation.NONE.getFilename())
               && !contents.isEmpty()
               && contents.charAt(0) == '{';
    }

    boolean loadParsedNode(LoaderVisitor visitor, Node node) {
        ObjectNode model = node.expectObjectNode("Smithy documents must be an object. Found {type}.");
        StringNode version = model.expectStringMember(SMITHY);

        if (version.getValue().equals(SmithyVersion.VERSION_0_4_0.value)) {
            DeprecatedAstModelLoader.INSTANCE.load(model, version, visitor);
            return true;
        } else if (version.getValue().equals(SmithyVersion.VERSION_0_5_0.value)) {
            AstModelLoader.INSTANCE.load(model, version, visitor);
            return true;
        } else {
            return false;
        }
    }
}
