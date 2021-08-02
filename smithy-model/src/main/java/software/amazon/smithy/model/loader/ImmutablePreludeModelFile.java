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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * A model file that contains the immutable prelude. Traits cannot be added
 * to the prelude outside of the prelude's definition.
 *
 * @see Prelude#getPreludeModel()
 */
final class ImmutablePreludeModelFile implements ModelFile {
    private final Model prelude;
    private final List<ValidationEvent> events = new ArrayList<>();

    ImmutablePreludeModelFile(Model prelude) {
        this.prelude = prelude;
    }

    @Override
    public Set<ShapeId> shapeIds() {
        return prelude.getShapeIds();
    }

    @Override
    public Map<String, Node> metadata() {
        return prelude.getMetadata();
    }

    @Override
    public TraitContainer resolveShapes(Set<ShapeId> ids, Function<ShapeId, ShapeType> typeProvider) {
        return TraitContainer.EMPTY;
    }

    @Override
    public CreatedShapes createShapes(TraitContainer resolvedTraits) {
        // Create error events for each trait applied outside of the prelude.
        Map<ShapeId, Map<ShapeId, Trait>> invalidTraits = resolvedTraits.getTraitsAppliedToPrelude();

        for (Map.Entry<ShapeId, Map<ShapeId, Trait>> entry : invalidTraits.entrySet()) {
            for (Map.Entry<ShapeId, Trait> trait : entry.getValue().entrySet()) {
                String message = String.format(
                        "Cannot apply `%s` to an immutable prelude shape defined in `smithy.api`.",
                        trait.getKey());
                events.add(ValidationEvent.builder()
                        .severity(Severity.ERROR)
                        .id(Validator.MODEL_ERROR)
                        .sourceLocation(trait.getValue().getSourceLocation())
                        .shapeId(entry.getKey())
                        .message(message)
                        .build());
            }
        }

        return new CreatedShapes(prelude.toSet());
    }

    @Override
    public List<ValidationEvent> events() {
        return events;
    }

    @Override
    public ShapeType getShapeType(ShapeId id) {
        return prelude.getShape(id).map(Shape::getType).orElse(null);
    }
}
