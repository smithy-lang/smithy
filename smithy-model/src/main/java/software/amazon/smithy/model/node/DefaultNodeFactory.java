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

import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Default Node factory implementation.
 *
 * <p>Uses Jackson internally, but that is free to change in the
 * future if needed.
 */
public final class DefaultNodeFactory implements NodeFactory {
    private final JsonFactory jsonFactory;

    public DefaultNodeFactory() {
        this(new JsonFactory());
    }

    DefaultNodeFactory(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    @Override
    public Node createNode(String filename, String text) {
        InputStream targetStream = new ByteArrayInputStream(text.getBytes(Charset.forName("UTF-8")));
        return createNode(filename, targetStream);
    }

    public Node createNode(String filename, InputStream input) {
        JsonParser parser = createParser(filename, input);

        try {
            parser.nextToken();
            Node result = parse(filename, parser);
            parser.close();
            return result;
        } catch (IOException e) {
            SourceLocation current = getSourceLocation(filename, parser);
            String message = filename.isEmpty()
                    ? String.format("Error parsing node: %s", e.getMessage())
                    : String.format("Error parsing file `%s`: %s", filename, e.getMessage());
            throw new ModelSyntaxException(message, current);
        }
    }

    private JsonParser createParser(String filename, InputStream text) {
        try {
            return jsonFactory.createParser(text);
        } catch (IOException e) {
            throw new ModelSyntaxException(
                    String.format("Error creating parser for %s: %s", filename, e.getMessage()),
                    new SourceLocation(filename, 1, 1));
        }
    }

    private Node parse(String filename, JsonParser parser) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == null) {
            throw new IOException("Expected token not found");
        }

        SourceLocation sourceLocation = getSourceLocation(filename, parser);
        switch (token) {
            case START_OBJECT: return parseObject(sourceLocation, parser);
            case START_ARRAY: return parseArray(sourceLocation, parser);
            case VALUE_NULL: return new NullNode(sourceLocation);
            case VALUE_TRUE: return new BooleanNode(true, sourceLocation);
            case VALUE_FALSE: return new BooleanNode(false, sourceLocation);
            case VALUE_NUMBER_INT: return new NumberNode(parser.getLongValue(), sourceLocation);
            case VALUE_NUMBER_FLOAT: return new NumberNode(parser.getDoubleValue(), sourceLocation);
            case VALUE_STRING: return new StringNode(parser.getValueAsString(), sourceLocation);
            default: throw new IOException(format("Unexpected token, `%s`", token));
        }
    }

    private SourceLocation getSourceLocation(String filename, JsonParser parser) {
        JsonLocation location = parser.getTokenLocation();
        return new SourceLocation(filename, location.getLineNr(), location.getColumnNr());
    }

    private Node parseObject(SourceLocation sourceLocation, JsonParser parser) throws IOException {
        String filename = sourceLocation.getFilename();

        if (parser.nextToken() == JsonToken.END_OBJECT) {
            return new ObjectNode(MapUtils.of(), sourceLocation, false);
        }

        Map<StringNode, Node> nodes = new LinkedHashMap<>();
        do {
            // Parse the field name.
            String fieldName = parser.getCurrentName();
            StringNode keyNode = new StringNode(fieldName, getSourceLocation(filename, parser));
            // Parse the value.
            parser.nextToken();
            nodes.put(keyNode, parse(filename, parser));
            parser.nextToken();
        } while (parser.currentToken() != JsonToken.END_OBJECT);

        return new ObjectNode(nodes, sourceLocation);
    }

    private Node parseArray(SourceLocation sourceLocation, JsonParser parser) throws IOException {
        String filename = sourceLocation.getFilename();

        if (parser.nextToken() == JsonToken.END_ARRAY) {
            return new ArrayNode(ListUtils.of(), sourceLocation, false);
        }

        List<Node> nodes = new ArrayList<>();
        do {
            nodes.add(parse(filename, parser));
            parser.nextToken();
        } while (parser.currentToken() != JsonToken.END_ARRAY);

        return new ArrayNode(nodes, sourceLocation);
    }
}
