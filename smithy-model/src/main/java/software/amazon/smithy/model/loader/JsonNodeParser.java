/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.io.Reader;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Parses JSON text into a {@link Node}.
 */
@SmithyInternalApi
public final class JsonNodeParser {

    private JsonNodeParser() {}

    /**
     * Parses a complete JSON document into a {@link Node}.
     *
     * @param filename Filename used for source locations.
     * @param content JSON text to parse.
     * @param allowComments Whether {@code //} line comments are permitted.
     * @return the parsed Node.
     * @throws ModelSyntaxException if the JSON is malformed.
     */
    public static Node parse(String filename, String content, boolean allowComments) {
        return JsonAstReader.parse(filename, content, allowComments);
    }

    /**
     * Parses a complete JSON document from a reader into a {@link Node}.
     *
     * @param filename Filename used for source locations.
     * @param reader JSON character stream to parse.
     * @param allowComments Whether {@code //} line comments are permitted.
     * @return the parsed Node.
     * @throws ModelSyntaxException if the JSON is malformed.
     */
    public static Node parse(String filename, Reader reader, boolean allowComments) {
        return JsonAstReader.parse(filename, reader, allowComments);
    }
}
