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

package software.amazon.smithy.model.knowledge;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.builtins.PaginatedTraitValidator;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Index of operation shapes to paginated trait information.
 *
 * <p>This index makes it easy to slice up paginated operations and
 * get the resolved members. This index performs some basic validation
 * of the paginated trait like ensuring that the operation has input
 * and output, and that the members defined in the paginated trait can
 * be found in the input or output of the operation. Additional
 * validation is performed in the {@link PaginatedTraitValidator}
 * (which makes use of this index).
 */
public final class PaginatedIndex implements KnowledgeIndex {
    private final Map<ShapeId, PaginationInfo> idToTraits;
    private final List<ValidationEvent> events = new ArrayList<>();

    public PaginatedIndex(Model model) {
        ShapeIndex index = model.getShapeIndex();
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);
        idToTraits = Collections.unmodifiableMap(index.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, PaginatedTrait.class))
                .flatMap(pair -> OptionalUtils.stream(create(opIndex, pair.getLeft(), pair.getRight())))
                .collect(Collectors.toMap(info -> info.getOperation().getId(), Function.identity())));
    }

    private Optional<PaginationInfo> create(OperationIndex opIndex, OperationShape operation, PaginatedTrait trait) {
        if (!opIndex.getInput(operation.getId()).isPresent()) {
            events.add(emit(operation, trait, "`paginated` operations require an input"));
            return Optional.empty();
        } else if (!opIndex.getOutput(operation.getId()).isPresent()) {
            events.add(emit(operation, trait, "`paginated` operations require an output"));
            return Optional.empty();
        }

        StructureShape input = opIndex.getInput(operation.getId()).get();
        StructureShape output = opIndex.getOutput(operation.getId()).get();
        if (!input.getMember(trait.getInputToken()).isPresent()) {
            events.add(emit(operation, trait, format(
                    "`paginated` trait `inputToken` property references a non-existent member named `%s`",
                    trait.getInputToken())));
            return Optional.empty();
        } else if (!output.getMember(trait.getOutputToken()).isPresent()) {
            events.add(emit(operation, trait, format(
                    "`paginated` trait `outputToken` property references a non-existent member named `%s`",
                    trait.getOutputToken())));
            return Optional.empty();
        }

        return Optional.of(new PaginationInfo(
                operation, input, output, trait,
                input.getMember(trait.getInputToken()).get(),
                output.getMember(trait.getOutputToken()).get()));
    }

    public List<ValidationEvent> getValidationEvents() {
        return Collections.unmodifiableList(events);
    }

    public Optional<PaginationInfo> getPaginationInfo(ToShapeId operation) {
        return Optional.ofNullable(idToTraits.get(operation.toShapeId()));
    }

    public List<PaginationInfo> getAllPaginatedInfo() {
        return new ArrayList<>(idToTraits.values());
    }

    private ValidationEvent emit(Shape shape, PaginatedTrait trait, String message) {
        return ValidationEvent.builder()
                .eventId("PaginatedTrait")
                .shapeId(shape.getId())
                .sourceLocation(trait)
                .severity(Severity.ERROR)
                .message(message).build();
    }

    /**
     * Resolved and valid pagination information about an operation.
     */
    public static final class PaginationInfo {

        private final OperationShape operation;
        private final StructureShape input;
        private final StructureShape output;
        private final PaginatedTrait paginatedTrait;
        private final MemberShape inputToken;
        private final MemberShape outputToken;

        private PaginationInfo(
                OperationShape operation,
                StructureShape input,
                StructureShape output,
                PaginatedTrait paginatedTrait,
                MemberShape inputToken,
                MemberShape outputToken
        ) {
            this.operation = operation;
            this.input = input;
            this.output = output;
            this.paginatedTrait = paginatedTrait;
            this.inputToken = inputToken;
            this.outputToken = outputToken;
        }

        public OperationShape getOperation() {
            return operation;
        }

        public StructureShape getInput() {
            return input;
        }

        public StructureShape getOutput() {
            return output;
        }

        public PaginatedTrait getPaginatedTrait() {
            return paginatedTrait;
        }

        public MemberShape getInputTokenMember() {
            return inputToken;
        }

        public MemberShape getOutputTokenMember() {
            return outputToken;
        }

        public Optional<MemberShape> getItemsMember() {
            return paginatedTrait.getItems().flatMap(output::getMember);
        }

        public Optional<MemberShape> getPageSizeMember() {
            return paginatedTrait.getPageSize().flatMap(input::getMember);
        }
    }
}
