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

package software.amazon.smithy.model.validation.builtins;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.UriPattern;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that no two URIs in a service conflict with each other.
 */
public final class HttpUriConflictValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(shape -> validateService(topDownIndex, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(TopDownIndex topDownIndex, ServiceShape service) {
        List<OperationShape> operations = topDownIndex.getContainedOperations(service)
                .stream()
                .filter(shape -> shape.getTrait(HttpTrait.class).isPresent())
                .collect(Collectors.toList());
        return operations.stream()
                .flatMap(shape -> Trait.flatMapStream(shape, HttpTrait.class))
                .flatMap(pair -> checkConflicts(pair, operations).stream())
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> checkConflicts(
            Pair<OperationShape, HttpTrait> pair,
            List<OperationShape> operations
    ) {
        OperationShape operation = pair.getLeft();
        String method = pair.getRight().getMethod();
        UriPattern pattern = pair.getRight().getUri();
        String conflicts = operations.stream()
                .filter(shape -> shape != operation)
                .flatMap(shape -> Trait.flatMapStream(shape, HttpTrait.class))
                .filter(other -> other.getRight().getMethod().equals(method))
                .filter(other -> other.getRight().getUri().conflictsWith(pattern))
                .map(other -> String.format("`%s` (%s)", other.getLeft().getId(), other.getRight().getUri()))
                .sorted()
                .collect(Collectors.joining(", "));

        if (conflicts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(operation, String.format(
                "Operation URI, `%s`, conflicts with other operation URIs in the same service: [%s]",
                pattern, conflicts)));
    }
}
