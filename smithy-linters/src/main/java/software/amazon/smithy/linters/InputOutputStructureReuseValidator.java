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

package software.amazon.smithy.linters;

import static software.amazon.smithy.model.validation.ValidationUtils.tickedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.Pair;

/**
 * Checks if a structure is used as both input and output and if the same
 * input or output structures are used across multiple operations.
 */
public final class InputOutputStructureReuseValidator extends AbstractValidator {

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(InputOutputStructureReuseValidator.class, InputOutputStructureReuseValidator::new);
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Map<ShapeId, Set<ShapeId>> inputs = createStructureToOperation(model, OperationShape::getInput);
        Map<ShapeId, Set<ShapeId>> outputs = createStructureToOperation(model, OperationShape::getOutput);

        // Look for structures used as both input and output.
        Set<ShapeId> both = new HashSet<>(inputs.keySet());
        both.retainAll(outputs.keySet());
        both.stream().map(id -> emitWhenBothInputAndOutput(
                model, id, inputs.get(id), outputs.get(id))).forEach(events::add);

        // Look for shared usage across multiple operations.
        events.addAll(emitShared(model, inputs, "input"));
        events.addAll(emitShared(model, outputs, "output"));
        return events;
    }

    private List<ValidationEvent> emitShared(
            Model model,
            Map<ShapeId, Set<ShapeId>> mapping,
            String descriptor
    ) {
        return mapping.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> emitWhenShared(model, entry.getKey(), entry.getValue(), descriptor))
                .collect(Collectors.toList());
    }

    /**
     * Creates a map of structure ShapeId to a list of operations that reference it as input/output.
     *
     * @param model The model used to get all operations.
     * @param f The mapping function used to retrieve input out output structures.
     * @return Returns the mapping.
     */
    private static Map<ShapeId, Set<ShapeId>> createStructureToOperation(
            Model model,
            Function<OperationShape, Optional<ShapeId>> f
    ) {
        return model.shapes(OperationShape.class)
                .flatMap(shape -> Pair.flatMapStream(shape, f))
                .collect(Collectors.groupingBy(
                        Pair::getRight,
                        HashMap::new,
                        Collectors.mapping(pair -> pair.getLeft().getId(), Collectors.toSet())
                ));
    }

    private ValidationEvent emitWhenBothInputAndOutput(
            Model model,
            ShapeId structure,
            Set<ShapeId> inputs,
            Set<ShapeId> outputs
    ) {
        return emit(model, structure, String.format(
                "Using the same structure for both input and output can lead to backward-compatibility problems "
                + "in the future if the members or traits used in input needs to diverge from those used in "
                + "output. It is always better to use structures that are exclusively used as input or "
                + "exclusively used as output, and it is typically best to not share these structures across "
                + "multiple operations. This structure is used as input in the following operations: [%s]. It "
                + "is used as output in the following operations: [%s]",
                tickedList(inputs), tickedList(outputs)));
    }

    private ValidationEvent emitWhenShared(
            Model model,
            ShapeId structure,
            Set<ShapeId> operations,
            String descriptor
    ) {
        return emit(model, structure, String.format(
                "Referencing the same %1$s structure from multiple operations can lead to backward-compatibility "
                + "problems in the future if the %1$ss ever need to diverge. By using the same structure, you "
                + "are unnecessarily tying the interfaces of these operations together. This structure is"
                + "referenced as %1$s by the following operations: [%2$s]",
                descriptor, tickedList(operations)));
    }

    private ValidationEvent emit(Model model, ShapeId shape, String message) {
        ValidationEvent.Builder builder = ValidationEvent.builder()
                .id(getName())
                .severity(Severity.DANGER)
                .message(message)
                .shapeId(shape);
        model.getShape(shape).ifPresent(builder::shape);
        return builder.build();
    }
}
