/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.MetadataTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates metadata entries against their declared types.
 */
@SmithyInternalApi
public final class TypedMetadataValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Map<String, Shape> metadataTypes = collectMetadataTypes(model);

        List<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<String, Shape> entry : metadataTypes.entrySet()) {
            String key = entry.getKey();
            Node value = model.getMetadata().get(key);
            if (value == null) {
                continue;
            }

            Shape shape = entry.getValue();
            NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                    .model(model)
                    .value(value)
                    .eventId("TypedMetadata." + key)
                    .startingContext("metadata." + key)
                    .addFeature(NodeValidationVisitor.Feature.REQUIRE_BASE_64_BLOB_VALUES)
                    .addFeature(NodeValidationVisitor.Feature.RANGE_TRAIT_ZERO_VALUE_WARNING)
                    .build();
            events.addAll(shape.accept(visitor));
        }
        return events;
    }

    private Map<String, Shape> collectMetadataTypes(Model model) {
        Map<String, Shape> metadataTypes = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        for (Shape shape : model.getShapesWithTrait(MetadataTrait.class)) {
            String key = shape.expectTrait(MetadataTrait.class).getKey();
            if (metadataTypes.containsKey(key)) {
                duplicates.add(key);
            } else {
                metadataTypes.put(key, shape);
            }
        }
        // If the type for the key is defined multiple times, don't try to
        // validate it. This invalid state will produce a validation event
        // in the validator for the metadata trait itself.
        duplicates.forEach(metadataTypes::remove);
        return metadataTypes;
    }
}
