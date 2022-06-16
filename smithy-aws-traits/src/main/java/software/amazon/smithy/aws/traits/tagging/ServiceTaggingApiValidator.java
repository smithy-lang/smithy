/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.traits.tagging;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates service satisfies AWS tagging requirements.
 */
public final class ServiceTaggingApiValidator extends AbstractValidator {
    private static final String TAG_RESOURCE_OPNAME = "TagResource";
    private static final String UNTAG_RESOURCE_OPNAME = "UntagResource";
    private static final String LISTTAGS_OPNAME = "ListTagsForResource";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new LinkedList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(TagEnabledTrait.class)) {
            events.addAll(validateService(model, service));
        }
        return events;
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        List<ValidationEvent> events = new LinkedList<>();
        //Check for standard operation presence in non-resource bound operations.
        //This is intentional as conventional AWS tag APIs should not be resource-bound.
        Set<ShapeId> operations = service.getOperations();
        OperationIndex operationIndex = OperationIndex.of(model);

        Predicate<ServiceShape> verifyTagResourceOp = (s) -> {
            return TaggingShapeUtils.verifyTagResourceOperation(model, s, operationIndex);
        };
        addEventIfMissingOperation(events, operations, TAG_RESOURCE_OPNAME, service, verifyTagResourceOp);

        Predicate<ServiceShape> verifyUntagResourceOp = (s) -> {
            return TaggingShapeUtils.verifyUntagResourceOperation(model, s, operationIndex);
        };
        addEventIfMissingOperation(events, operations, UNTAG_RESOURCE_OPNAME, service, verifyUntagResourceOp);

        Predicate<ServiceShape> verifyListTagsOp = (s) -> {
            return TaggingShapeUtils.verifyListTagsOperation(model, s, operationIndex);
        };
        addEventIfMissingOperation(events, operations, LISTTAGS_OPNAME, service, verifyListTagsOp);

        return events;
    }

    private void addEventIfMissingOperation(
        List<ValidationEvent> events,
        Set<ShapeId> operationIds,
        String operationName,
        ServiceShape service,
        Predicate<ServiceShape> operationValidator
    ) {
        if (!operationIds.contains(ShapeId.fromOptionalNamespace(service.getId().getNamespace(), operationName))
                || !operationValidator.test(service)) {
            events.add(warning(service, "Service shape annotated with `aws.api#TagEnabled` trait does not have a "
                                            + "qualifying '" + operationName + "' operation."));
        }
    }
}
