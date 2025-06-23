/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class QueryErrorCodeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.hasTrait(AwsQueryCompatibleTrait.class) || service.hasTrait(AwsQueryTrait.class)
                    || service.hasTrait(Ec2QueryTrait.class)) {
                events.addAll(validateService(model, service));
            }
        }
        return events;
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex index = TopDownIndex.of(model);
        List<ShapeId> errors = new ArrayList<>(service.getErrors());
        index.getContainedOperations(service).forEach(operation -> errors.addAll(operation.getErrors()));

        Map<String, Set<ShapeId>> errorCodeBindings = new HashMap<>();
        for (ShapeId errorId : errors) {
            String errorCode = errorId.getName();
            Shape errorShape = model.expectShape(errorId);
            if (errorShape.hasTrait(AwsQueryErrorTrait.class)) {
                AwsQueryErrorTrait trait = errorShape.expectTrait(AwsQueryErrorTrait.class);
                errorCode = trait.getCode();
            }
            errorCodeBindings.computeIfAbsent(errorCode, k -> new TreeSet<>()).add(errorId);
        }

        for (Map.Entry<String, Set<ShapeId>> entry : errorCodeBindings.entrySet()) {
            if (entry.getValue().size() == 1) {
                continue;
            }
            String errorCode = entry.getKey();
            String shapes = entry.getValue().stream().map(Objects::toString).collect(Collectors.joining(", "));
            events.add(danger(service,
                    String.format("Multiple error shapes with the error code `%s` found: [%s]. This error code "
                            + "ambiguity will result in the wrong error shape being deserialized when running the "
                            + "query protocol. This pain will be carried forward when migrating to a new protocol "
                            + "when using the awsQueryCompatible trait.", errorCode, shapes)));
        }
        return events;
    }
}
