/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Captures and merges metadata during the model loading process.
 */
final class MetadataContainer {
    private static final Logger LOGGER = Logger.getLogger(MetadataContainer.class.getName());

    private final Map<String, Node> data = new LinkedHashMap<>();
    private final List<ValidationEvent> events;

    /**
     * @param events Mutable, by-reference list of validation events.
     */
    MetadataContainer(List<ValidationEvent> events) {
        this.events = events;
    }

    /**
     * Put metadata into the map.
     *
     * <p>If the given key conflicts with another key, then the values are
     * merged (that is, if both values are arrays, then combine them, if
     * both values are equal then ignore the new value, or fail the merge
     * and add a validation event).
     *
     * @param key Metadata key to set.
     * @param value Value to set.
     */
    void putMetadata(String key, Node value) {
        if (!data.containsKey(key)) {
            data.put(key, value);
        } else if (data.get(key).isArrayNode() && value.isArrayNode()) {
            ArrayNode previous = data.get(key).expectArrayNode();
            List<Node> merged = new ArrayList<>(previous.getElements());
            merged.addAll(value.expectArrayNode().getElements());
            ArrayNode mergedArray = new ArrayNode(merged, value.getSourceLocation());
            data.put(key, mergedArray);
        } else if (!data.get(key).equals(value)) {
            events.add(ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(value)
                    .message(format(
                            "Metadata conflict for key `%s`. Defined in both `%s` and `%s`",
                            key, value.getSourceLocation(), data.get(key).getSourceLocation()))
                    .build());
        } else {
            LOGGER.fine(() -> "Ignoring duplicate metadata definition of " + key);
        }
    }

    /**
     * Merges another metadata container into this container.
     *
     * @param other Metadata container to merge into this container.
     */
    void mergeWith(Map<String, Node> other) {
        for (Map.Entry<String, Node> entry : other.entrySet()) {
            putMetadata(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Gets all of the metadata in the container.
     *
     * @return Returns the metadata.
     */
    Map<String, Node> getData() {
        return data;
    }
}
