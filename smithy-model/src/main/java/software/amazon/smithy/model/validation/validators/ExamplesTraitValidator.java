/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that examples traits are valid for their operations.
 */
public final class ExamplesTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(ExamplesTrait.class)) {
            events.addAll(validateExamples(model, operation, operation.expectTrait(ExamplesTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateExamples(Model model, OperationShape shape, ExamplesTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ExamplesTrait.Example example : trait.getExamples()) {
            boolean isOutputDefined = example.getOutput().isPresent();
            boolean isErrorDefined = example.getError().isPresent();

            model.getShape(shape.getInputShape()).ifPresent(input -> {
                if (example.getAllowConstraintErrors() && !isErrorDefined) {
                    events.add(error(shape,
                            trait,
                            String.format(
                                    "Example: `%s` has allowConstraintErrors enabled, so error must be defined.",
                                    example.getTitle())));
                }
                events.addAll(input.accept(createVisitor("input", example.getInput(), model, shape, example)));
            });

            if (isOutputDefined && isErrorDefined) {
                events.add(error(shape,
                        trait,
                        String.format(
                                "Example: `%s` has both output and error defined, only one should be present.",
                                example.getTitle())));
            } else if (isOutputDefined) {
                model.getShape(shape.getOutputShape())
                        .ifPresent(output -> events.addAll(
                                output.accept(
                                        createVisitor("output", example.getOutput().get(), model, shape, example))));
            } else if (isErrorDefined) {
                ExamplesTrait.ErrorExample errorExample = example.getError().get();
                Optional<Shape> errorShape = model.getShape(errorExample.getShapeId());
                if (errorShape.isPresent() && (
                // The error is directly bound to the operation.
                shape.getErrorsSet().contains(errorExample.getShapeId())
                        // The error is bound to all services that contain the operation.
                        || servicesContainError(model, shape, errorExample.getShapeId()))) {
                    events.addAll(errorShape.get()
                            .accept(createVisitor("error", errorExample.getContent(), model, shape, example)));
                } else {
                    events.add(error(shape,
                            trait,
                            String.format(
                                    "Error parameters provided for operation without the `%s` error: `%s`",
                                    errorExample.getShapeId(),
                                    example.getTitle())));
                }
            }

            // Per-section structural validation above mirrors a plain example. The call-level
            // invariants that only an operation call has (the ghost `before`/`after` world
            // snapshots, any unexpected call member, and dynamic `@contracts` evaluation) run
            // once over the assembled {input, output, error, before, after} call node, with
            // each declared `@references` projected onto the input as a resource handle.
            ObjectNode call = buildOperationCall(model, shape, example);
            events.addAll(callVisitor(call, model, shape, example).validateOperationCall(shape));
        }

        return events;
    }

    // A service that contains the operation must also declare the error for the error to
    // be a valid error of a call to that operation.
    private boolean servicesContainError(Model model, OperationShape shape, ShapeId errorId) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Set<ServiceShape> services = model.getServiceShapes();
        if (services.isEmpty()) {
            return false;
        }
        for (ServiceShape service : services) {
            // Skip if the service doesn't have the operation.
            if (!topDownIndex.getContainedOperations(service).contains(shape)) {
                continue;
            }
            // We've already checked if the operation contains the error,
            // so a service having no errors means we've failed.
            if (service.getErrorsSet().isEmpty() || !service.getErrorsSet().contains(errorId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assembles the {@code {input, output, error, before, after}} call node for an
     * example, projecting each declared {@code @references} on the input into a ghost
     * resource handle reachable as {@code input.<name>}.
     *
     * <p>Shared so that other validators (such as contract evaluation) check the same
     * call node this validator validates structurally.
     *
     * @param model The model the operation belongs to.
     * @param shape The operation the example is for.
     * @param example The example to assemble.
     * @return Returns the assembled call node.
     */
    public static ObjectNode buildOperationCall(Model model, OperationShape shape, ExamplesTrait.Example example) {
        ObjectNode input = withReferenceHandles(model, shape.getInputShape(), example.getInput());
        ObjectNode.Builder builder = Node.objectNodeBuilder().withMember("input", input);
        example.getOutput().ifPresent(output -> builder.withMember("output", output));
        example.getError()
                .ifPresent(error -> builder.withMember("error",
                        Node.objectNodeBuilder()
                                .withMember("shapeId", error.getShapeId().toString())
                                .withMember("content", error.getContent())
                                .build()));
        example.getBefore().ifPresent(before -> builder.withMember("before", before));
        example.getAfter().ifPresent(after -> builder.withMember("after", after));
        return builder.build();
    }

    // Projects each named @references on the structure into a ghost resource handle
    // reachable as input.<name>, synthesized from the observable identifier members.
    private static ObjectNode withReferenceHandles(Model model, ShapeId structureId, ObjectNode values) {
        Shape shape = model.getShape(structureId).orElse(null);
        if (shape == null || !shape.hasTrait(ReferencesTrait.class)) {
            return values;
        }
        ObjectNode result = values;
        for (ReferencesTrait.Reference reference : shape.expectTrait(ReferencesTrait.class).getReferences()) {
            if (!reference.getName().isPresent()) {
                continue;
            }
            ObjectNode.Builder ids = Node.objectNodeBuilder();
            for (Map.Entry<String, String> entry : reference.getIds().entrySet()) {
                // ids maps a resource identifier name to the member that provides its value.
                Node value = values.getMember(entry.getValue()).orElse(Node.nullNode());
                ids.withMember(entry.getKey(), value);
            }
            ObjectNode.Builder handle = Node.objectNodeBuilder()
                    .withMember("resource", reference.getResource().toString())
                    .withMember("ids", ids.build());
            // The handle locates the resource under a service shape in the world snapshot.
            // Use the reference's explicit service, else the single service that contains
            // the resource; leave it unset when that is ambiguous.
            serviceName(model, reference)
                    .ifPresent(service -> handle.withMember("service", service));
            result = result.withMember(reference.getName().get(), handle.build());
        }
        return result;
    }

    // The service shape name that locates the referenced resource: the reference's own
    // service when set, otherwise the sole service whose closure contains the resource.
    private static Optional<String> serviceName(Model model, ReferencesTrait.Reference reference) {
        if (reference.getService().isPresent()) {
            return Optional.of(reference.getService().get().getName());
        }
        TopDownIndex index = TopDownIndex.of(model);
        ShapeId resource = reference.getResource();
        ServiceShape found = null;
        for (ServiceShape service : model.getServiceShapes()) {
            boolean contains = false;
            for (ResourceShape contained : index.getContainedResources(service)) {
                if (contained.getId().equals(resource)) {
                    contains = true;
                    break;
                }
            }
            if (contains) {
                if (found != null) {
                    return Optional.empty();
                }
                found = service;
            }
        }
        return Optional.ofNullable(found).map(service -> service.getId().toString());
    }

    // Builds a visitor for one named section (input, output, error) of an example, so a
    // structural problem is reported as "Example <name> of `<title>`".
    private NodeValidationVisitor createVisitor(
            String name,
            ObjectNode value,
            Model model,
            OperationShape shape,
            ExamplesTrait.Example example
    ) {
        return visitorBuilder(value, model, shape, example)
                .startingContext("Example " + name + " of `" + example.getTitle() + "`")
                .build();
    }

    // Builds a visitor for the whole call node, used for the call-level invariants
    // (ghost before/after world snapshots, unexpected members, and @contracts).
    private NodeValidationVisitor callVisitor(
            ObjectNode value,
            Model model,
            OperationShape shape,
            ExamplesTrait.Example example
    ) {
        return visitorBuilder(value, model, shape, example)
                .startingContext("Example `" + example.getTitle() + "`")
                .build();
    }

    private NodeValidationVisitor.Builder visitorBuilder(
            ObjectNode value,
            Model model,
            OperationShape shape,
            ExamplesTrait.Example example
    ) {
        NodeValidationVisitor.Builder builder = NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .eventId(getName());
        if (example.getAllowConstraintErrors()) {
            builder.addFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS);
        }
        return builder;
    }
}
