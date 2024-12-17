/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
     * @param events Where to add events as issues are encountered.
     */
    void putMetadata(String key, Node value, List<ValidationEvent> events) {
        Node previous = data.putIfAbsent(key, value);

        if (previous == null) {
            return;
        }

        if (LoaderUtils.isSameLocation(value, previous) && value.equals(previous)) {
            // The assumption here is that if the metadata value is exactly the
            // same and from the same location, then the same model file was
            // included more than once in a way that side-steps file and URL
            // de-duplication. For example, this can occur when a Model is assembled
            // through a ModelAssembler using model discovery, then the Model is
            // added to a subsequent ModelAssembler, and then model discovery is
            // performed again using the same classpath.
            LOGGER.finer(() -> "Ignoring duplicate metadata key from same exact location: " + key);
        } else if (previous.isArrayNode() && value.isArrayNode()) {
            ArrayNode previousArray = previous.expectArrayNode();
            List<Node> merged = new ArrayList<>(previousArray.getElements());
            merged.addAll(value.expectArrayNode().getElements());
            data.put(key, new ArrayNode(merged, value.getSourceLocation()));
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
        return data;
    }
}
