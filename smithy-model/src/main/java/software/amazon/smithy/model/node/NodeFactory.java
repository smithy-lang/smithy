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

package software.amazon.smithy.model.node;

import software.amazon.smithy.model.loader.ModelSyntaxException;

/**
 * A {@code NodeFactory} is responsible for creating a {@link Node} objects
 * from strings.
 */
@FunctionalInterface
@Deprecated
public interface NodeFactory {
    /**
     * Creates a {@link Node} from a document {@code String}.
     *
     * <p>The filename is used in error messages and for populating source
     * locations for the built node objects.
     *
     * @param filename The name of the document that is being parsed to create nodes.
     * @param document The document to parse into nodes.
     * @return Returns a {@link Node} representing the document.
     * @throws ModelSyntaxException when a RuntimeException occurs.
     */
    Node createNode(String filename, String document);
}
