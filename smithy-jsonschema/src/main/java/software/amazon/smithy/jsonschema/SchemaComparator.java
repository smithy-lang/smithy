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
            "$schema", "$id",

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
            "definitions"
    );

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
