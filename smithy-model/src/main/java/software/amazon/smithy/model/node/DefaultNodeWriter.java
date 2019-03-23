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

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Serializes {@link Node} objects to JSON.
 *
 * <p>Uses Jackson internally, but that may change in the future if needed.
 * Providing a {@link JsonGenerator} allows for the JSON output to
 * be in any format with any style of printing.
 */
final class DefaultNodeWriter implements NodeVisitor<Void> {
    private JsonGenerator generator;

    DefaultNodeWriter(JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public Void arrayNode(ArrayNode node) {
        try {
            generator.writeStartArray(node.size());
            node.getElements().forEach(element -> element.accept(this));
            generator.writeEndArray();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void booleanNode(BooleanNode node) {
        try {
            generator.writeBoolean(node.getValue());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void nullNode(NullNode node) {
        try {
            generator.writeNull();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void numberNode(NumberNode node) {
        try {
            if (node.getValue() instanceof BigDecimal) {
                generator.writeNumber((BigDecimal) node.getValue());
            } else if (node.isFloatingPointNumber()) {
                generator.writeNumber(node.getValue().doubleValue());
            } else {
                generator.writeNumber(node.getValue().longValue());
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void objectNode(ObjectNode node) {
        try {
            generator.writeStartObject();
            for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
                generator.writeFieldName(entry.getKey().getValue());
                entry.getValue().accept(this);
            }
            generator.writeEndObject();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void stringNode(StringNode node) {
        try {
            generator.writeString(node.getValue());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
