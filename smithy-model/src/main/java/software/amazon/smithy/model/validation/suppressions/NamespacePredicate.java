/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
