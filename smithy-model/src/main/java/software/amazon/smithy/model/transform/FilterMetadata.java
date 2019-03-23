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
        model.getMetadata().entrySet().stream()
                .filter(predicate)
                .forEach(e -> builder.putMetadataProperty(e.getKey(), e.getValue()));
        return builder.build();
    }
}
