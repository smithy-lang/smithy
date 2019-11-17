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

package software.amazon.smithy.model.validation.validators;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Validates that resource references do not introduce circular hierarchies.
 */
public final class ResourceCycleValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ResourceShape.class)
                .flatMap(shape -> OptionalUtils.stream(detectCycles(model, shape, new LinkedHashSet<>())))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> detectCycles(Model model, ResourceShape resource, Set<ShapeId> visited) {
        if (visited.contains(resource.getId())) {
            return Optional.of(cycle(resource, visited));
        }

        visited.add(resource.getId());
        for (ShapeId child : resource.getResources()) {
            ResourceShape childResource = model.getShape(child).flatMap(Shape::asResourceShape).orElse(null);
            if (childResource != null) {
                Optional<ValidationEvent> error = detectCycles(model, childResource, visited);
                if (error.isPresent()) {
                    return error;
                }
            }
        }

        return Optional.empty();
    }

    private ValidationEvent cycle(ResourceShape shape, Set<ShapeId> parents) {
        String chain = parents.stream().map(ShapeId::toString).collect(Collectors.joining(" -> "));
        return error(shape, String.format("Circular resource hierarchy found: %s -> %s", chain, shape.getId()));
    }
}
