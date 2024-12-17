/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

/**
 * Filters metadata key-value pairs out of a model that do not match
 * a predicate.
 *
 * @see ModelTransformer#filterMetadata
 */
final class FilterMetadata {
    private Predicate<Map.Entry<String, Node>> predicate;

    FilterMetadata(BiPredicate<String, Node> predicate) {
        this.predicate = (entry) -> predicate.test(entry.getKey(), entry.getValue());
    }

    Model transform(Model model) {
        Model.Builder builder = model.toBuilder();
        builder.clearMetadata();
        model.getMetadata()
                .entrySet()
                .stream()
                .filter(predicate)
                .forEach(e -> builder.putMetadataProperty(e.getKey(), e.getValue()));
        return builder.build();
    }
}
