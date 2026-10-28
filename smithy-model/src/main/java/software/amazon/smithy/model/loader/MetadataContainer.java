/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Captures and merges metadata during the model loading process.
 *
 * <p>Array values that are merged for the same key are accumulated in a
 * mutable list and only turned into an {@link ArrayNode} when
 * {@link #getData()} is called. This keeps merging many array values for
 * one key linear in the total number of elements.
 */
final class MetadataContainer {
    private static final Logger LOGGER = Logger.getLogger(MetadataContainer.class.getName());

    private final Map<String, Node> data = new LinkedHashMap<>();

    // Accumulated elements of keys whose array values have been merged. While
    // hasPendingMerges is set, data holds the last merged value for these keys
    // until getData() materializes the accumulated arrays.
    private final Map<String, List<Node>> mergedArrays = new HashMap<>();
    private boolean hasPendingMerges;

    /**
     * Put metadata into the map.
     *
     * <p>If the given key conflicts with another key, then the values are
     * merged (that is, if both values are arrays, then combine them, if
     * both values are equal then ignore the new value, or fail the merge
     * and add a validation event). Merged arrays are accumulated and are
     * only materialized when {@link #getData()} is called.
     *
     * @param key Metadata key to set.
     * @param value Value to set.
     * @param events Where to add events as issues are encountered.
     */
    void putMetadata(String key, Node value, List<ValidationEvent> events) {
        Node previous = data.putIfAbsent(key, value);

        if (previous == null) {
            return;
        }

        List<Node> mergedArray = mergedArrays.get(key);

        if (mergedArray == null && LoaderUtils.isSameLocation(value, previous) && value.equals(previous)) {
            // The assumption here is that if the metadata value is exactly the
            // same and from the same location, then the same model file was
            // included more than once in a way that side-steps file and URL
            // de-duplication. For example, this can occur when a Model is assembled
            // through a ModelAssembler using model discovery, then the Model is
            // added to a subsequent ModelAssembler, and then model discovery is
            // performed again using the same classpath.
            LOGGER.finer(() -> "Ignoring duplicate metadata key from same exact location: " + key);
        } else if (previous.isArrayNode() && value.isArrayNode()) {
            if (mergedArray == null) {
                mergedArray = new ArrayList<>(previous.expectArrayNode().getElements());
                mergedArrays.put(key, mergedArray);
            }
            mergedArray.addAll(value.expectArrayNode().getElements());
            // Keep the last merged value so its location is used for conflicts and the result.
            data.put(key, value);
            hasPendingMerges = true;
        } else if (!previous.equals(value)) {
            events.add(ValidationEvent.builder()
                    .id(Validator.MODEL_ERROR)
                    .severity(Severity.ERROR)
                    .sourceLocation(value)
                    .message(format(
                            "Metadata conflict for key `%s`. Defined in both `%s` and `%s`",
                            key,
                            value.getSourceLocation(),
                            previous.getSourceLocation()))
                    .build());
        } else {
            LOGGER.fine(() -> "Ignoring duplicate metadata definition of " + key);
        }
    }

    /**
     * Gets all of the metadata in the container.
     *
     * @return Returns the metadata.
     */
    Map<String, Node> getData() {
        if (hasPendingMerges) {
            // Replacing the value of an existing key keeps its insertion order.
            for (Map.Entry<String, List<Node>> entry : mergedArrays.entrySet()) {
                SourceLocation location = data.get(entry.getKey()).getSourceLocation();
                data.put(entry.getKey(), new ArrayNode(entry.getValue(), location));
            }
            hasPendingMerges = false;
        }
        return data;
    }
}
