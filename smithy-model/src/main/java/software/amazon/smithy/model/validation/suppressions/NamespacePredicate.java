/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import java.util.function.Predicate;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;

final class NamespacePredicate implements Predicate<ValidationEvent> {

    private final String namespace;

    NamespacePredicate(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean test(ValidationEvent event) {
        ShapeId id = event.getShapeId().orElse(null);
        if (id == null) {
            return false;
        }

        return id.getNamespace().equals(namespace);
    }
}
