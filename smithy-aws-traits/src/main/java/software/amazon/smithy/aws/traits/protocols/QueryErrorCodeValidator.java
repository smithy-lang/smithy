/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Emits a warning if two or more error shapes share the same error code.
 */
@SmithyInternalApi
public class QueryErrorCodeValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = Collections.emptyList();
        for (ServiceShape service : model.getServiceShapes()) {
            if (service.hasTrait(AwsQueryCompatibleTrait.class) || service.hasTrait(AwsQueryTrait.class)
                    || service.hasTrait(Ec2QueryTrait.class)) {
                List<ValidationEvent> serviceEvents = validateService(model, service);
                if (!serviceEvents.isEmpty()) {
                    if (events.isEmpty()) {
                        events = new ArrayList<>();
                    }
                    events.addAll(serviceEvents);
                }
            }
        }
        return events;
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        TopDownIndex index = TopDownIndex.of(model);
        Set<ShapeId> errors = new HashSet<>(service.getErrors());
        for (OperationShape operation : index.getContainedOperations(service)) {
            errors.addAll(operation.getErrors());
        }

        Map<String, Set<String>> errorCodeBindings = new HashMap<>();
        for (ShapeId errorId : errors) {
            String errorCode = errorId.getName();
            Shape errorShape = model.expectShape(errorId);
            if (errorShape.hasTrait(AwsQueryErrorTrait.class)) {
                AwsQueryErrorTrait trait = errorShape.expectTrait(AwsQueryErrorTrait.class);
                errorCode = trait.getCode();
            }
            errorCodeBindings.computeIfAbsent(errorCode, k -> new TreeSet<>()).add(errorId.toString());
        }
        List<ValidationEvent> events = Collections.emptyList();
        for (Map.Entry<String, Set<String>> entry : errorCodeBindings.entrySet()) {
            if (entry.getValue().size() == 1) {
                continue;
            }
            String errorCode = entry.getKey();
            String shapes = String.join(", ", entry.getValue());
            if (events.isEmpty()) {
                events = new ArrayList<>();
            }
            events.add(warning(service,
                    String.format("Multiple error shapes with the error code `%s` found: [%s]. This error code "
                            + "ambiguity will result in the wrong error shape being deserialized when running the "
                            + "query protocol. This pain will be carried forward when migrating to a new protocol "
                            + "when using the awsQueryCompatible trait.", errorCode, shapes)));
        }
        return events;
    }
}
