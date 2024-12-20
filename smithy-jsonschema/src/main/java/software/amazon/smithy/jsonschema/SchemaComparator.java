/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import software.amazon.smithy.model.node.StringNode;

/**
 * Provides a more natural sort-order to keys in JSON schemas.
 */
final class SchemaComparator implements Comparator<StringNode>, Serializable {
    private static final List<String> ORDERED_KEYS = Arrays.asList(
            // Document:
            "$schema",
            "$id",

            // Schemas
            "$ref",
            "type",
            "enum",
            "const",

            "multipleOf",
            "maximum",
            "exclusiveMaximum",
            "minimum",
            "exclusiveMinimum",

            "maxLength",
            "minLength",
            "pattern",

            "items",
            "maxItems",
            "minItems",
            "uniqueItems",

            "maxProperties",
            "minProperties",
            "required",
            "properties",
            "additionalProperties",
            "propertyNames",

            "allOf",
            "anyOf",
            "oneOf",
            "not",

            "title",
            "description",
            "format",
            "readOnly",
            "writeOnly",
            "comment",
            "examples",

            "contentEncoding",
            "contentMediaType",

            // Document: Always place definitions after the root node.
            "definitions");

    @Override
    public int compare(StringNode a, StringNode b) {
        int index1 = ORDERED_KEYS.indexOf(a.getValue());
        int index2 = ORDERED_KEYS.indexOf(b.getValue());

        if (index1 == -1) {
            return index2 == -1 ? a.getValue().compareToIgnoreCase(b.getValue()) : 1;
        } else if (index2 == -1) {
            return -1;
        } else {
            return index1 - index2;
        }
    }
}
