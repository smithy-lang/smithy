/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;
import software.amazon.smithy.protocoltests.traits.ProtocolTestValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates event stream test cases. It performs the following validations for each test case:
 *
 * <ul>
 *     <li>Validates that the initial request params match the modeled initial request.</li>
 *     <li>Validates that the initial request matches the initial request shape if set.</li>
 *     <li>Validates that the initial response params match the modeled initial response.</li>
 *     <li>Validates that the initial response matches the initial response shape if set.</li>
 *     <li>Validates that the vendor params match the vendor params shape if set.</li>
 *     <li>Validates that there is at least one event or initial message defined.</li>
 *     <li>Validates that the ID is unique.</li>
 * </ul>
 *
 * For each event in the test case, the following validations are performed:
 *
 * <ul>
 *     <li>If the event is a REQUEST event and it has params, validates that the operation has an input stream.</li>
 *     <li>If the event is a RESPONSE event and it has params, validates that the operation has an output stream.</li>
 *     <li>If the event has params, validates that it matches an event in the event stream.</li>
 *     <li>If the body media type is set to XML or JSON, validate that the body parseable.</li>\
 *     <li>Validates that the vendor params match the vendor params shape if set.</li>
 * </ul>
 */
public final class EventStreamTestsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape shape : model.getOperationShapesWithTrait(EventStreamTestsTrait.class)) {
            EventStreamTestsTrait trait = shape.expectTrait(EventStreamTestsTrait.class);
            List<EventStreamTestCase> cases = trait.getTestCases();
            for (int i = 0; i < cases.size(); i++) {
                EventStreamTestCase testCase = cases.get(i);
                events.addAll(validateTestCase(model, shape, trait, testCase, i));
            }
        }
        return events;
    }

    private List<ValidationEvent> validateTestCase(
            Model model,
            OperationShape operation,
            EventStreamTestsTrait trait,
            EventStreamTestCase testCase,
            int testCaseIndex
    ) {

        EventStreamIndex eventStreamIndex = EventStreamIndex.of(model);
        Optional<EventStreamInfo> inputStream = eventStreamIndex.getInputInfo(operation);
        Optional<EventStreamInfo> outputStream = eventStreamIndex.getOutputInfo(operation);

        List<ValidationEvent> validationEvents = new ArrayList<>();
        if (testCase.getInitialRequestParams().isPresent()) {
            NodeValidationVisitor inputValidator = createVisitor(
                    testCase.getInitialRequestParams().get(),
                    model,
                    operation,
                    testCaseIndex + ".initialRequestParams");
            validationEvents.addAll(model.expectShape(operation.getInputShape()).accept(inputValidator));
        }

        if (testCase.getInitialRequestShape().isPresent()) {
            NodeValidationVisitor initialRequestValidator = createVisitor(
                    testCase.getInitialRequest().orElseGet(Node::objectNode),
                    model,
                    operation,
                    testCaseIndex + ".initialRequest");
            validationEvents.addAll(model.expectShape(testCase.getInitialRequestShape().get())
                    .accept(initialRequestValidator));
        }

        if (testCase.getInitialResponseParams().isPresent()) {
            NodeValidationVisitor outputValidator = createVisitor(
                    testCase.getInitialResponseParams().get(),
                    model,
                    operation,
                    testCaseIndex + ".initialResponseParams");
            validationEvents.addAll(model.expectShape(operation.getOutputShape()).accept(outputValidator));
        }

        if (testCase.getInitialResponseShape().isPresent()) {
            NodeValidationVisitor initialResponseValidator = createVisitor(
                    testCase.getInitialResponse().orElseGet(Node::objectNode),
                    model,
                    operation,
                    testCaseIndex + ".initialResponse");
            validationEvents.addAll(model.expectShape(testCase.getInitialResponseShape().get())
                    .accept(initialResponseValidator));
        }

        if (testCase.getVendorParamsShape().isPresent()) {
            NodeValidationVisitor vendorParamsValidator = createVisitor(
                    testCase.getVendorParams().orElseGet(Node::objectNode),
                    model,
                    operation,
                    testCaseIndex + ".vendorParams");
            validationEvents.addAll(model.expectShape(testCase.getVendorParamsShape().get())
                    .accept(vendorParamsValidator));
        }

        List<Event> events = testCase.getEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            String eventContextSuffix = String.format("%s.events.%s", testCaseIndex, i);

            if (event.getParams().isPresent()) {
                String paramsContextSuffix = eventContextSuffix + ".params";
                NodeValidationVisitor paramsValidator = createVisitor(
                        event.getParams().get(),
                        model,
                        operation,
                        paramsContextSuffix);

                if (event.getType().equals(EventType.REQUEST)) {
                    if (!inputStream.isPresent()) {
                        String message = String.format(
                                "%s.%s: Invalid request event for operation %s that has no input stream.",
                                EventStreamTestsTrait.ID,
                                eventContextSuffix,
                                operation.getId());
                        validationEvents.add(error(operation, trait, message));
                    } else {
                        validationEvents.addAll(inputStream.get().getEventStreamTarget().accept(paramsValidator));
                    }
                } else if (event.getType().equals(EventType.RESPONSE)) {
                    if (!outputStream.isPresent()) {
                        String message = String.format(
                                "%s.%s: Invalid response event for operation %s that has no output stream.",
                                EventStreamTestsTrait.ID,
                                eventContextSuffix,
                                operation.getId());
                        validationEvents.add(error(operation, trait, message, eventContextSuffix));
                    } else {
                        validationEvents.addAll(outputStream.get().getEventStreamTarget().accept(paramsValidator));
                    }
                }
            }

            if (event.getBodyMediaType().isPresent()) {
                validationEvents.addAll(validateMediaType(operation, trait, event, eventContextSuffix));
            }

            if (event.getVendorParamsShape().isPresent()) {
                Shape vendorParamsShape = model.expectShape(event.getVendorParamsShape().get());
                String vendorParamsContextSuffix = String.format("%s.events.%s.vendorParams", testCaseIndex, i);
                NodeValidationVisitor vendorParamsValidator = createVisitor(
                        event.getVendorParams().orElseGet(Node::objectNode),
                        model,
                        operation,
                        vendorParamsContextSuffix);
                validationEvents.addAll(vendorParamsShape.accept(vendorParamsValidator));
            }
        }

        if (testCase.getEvents().isEmpty()
                && !testCase.getInitialRequestParams().isPresent()
                && !testCase.getInitialRequest().isPresent()
                && !testCase.getInitialResponseParams().isPresent()
                && !testCase.getInitialResponse().isPresent()) {
            validationEvents.add(error(
                    operation,
                    trait,
                    String.format(
                            "%s.%s: At least one event, an initial request, or an initial response must be set.",
                            EventStreamTestsTrait.ID,
                            testCaseIndex),
                    String.valueOf(testCaseIndex)));
        }

        return validationEvents;
    }

    private List<ValidationEvent> validateMediaType(
            OperationShape operation,
            Trait trait,
            Event event,
            String eventContextSuffix
    ) {
        if (!event.getBodyMediaType().isPresent()) {
            return Collections.emptyList();
        }

        return ProtocolTestValidationUtils.validateMediaType(event.getBody().orElse(""), event.getBodyMediaType().get())
                .map(e -> ListUtils.of(emitMediaTypeError(operation, trait, eventContextSuffix, event, e)))
                .orElse(Collections.emptyList());
    }

    private ValidationEvent emitMediaTypeError(
            OperationShape operation,
            Trait trait,
            String eventContextSuffix,
            Event test,
            Throwable e
    ) {
        return danger(operation,
                trait,
                String.format(
                        "%s.%s.body: Invalid %s content: %s",
                        EventStreamTestsTrait.ID,
                        eventContextSuffix,
                        test.getBodyMediaType().orElse(""),
                        e.getMessage()));
    }

    private NodeValidationVisitor createVisitor(
            ObjectNode value,
            Model model,
            Shape shape,
            String contextSuffix
    ) {
        return NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext(EventStreamTestsTrait.ID + "." + contextSuffix)
                .eventId(getName())
                .timestampValidationStrategy(TimestampValidationStrategy.FORMAT)
                .addFeature(NodeValidationVisitor.Feature.ALLOW_OPTIONAL_NULLS)
                .build();
    }
}
