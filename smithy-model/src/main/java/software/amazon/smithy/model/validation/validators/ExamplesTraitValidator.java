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
import software.amazon.smithy.model.shapes.MemberShape;
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

    // Projects each named @references into a ghost resource handle reachable as
    // input.<name>, synthesized from the observable identifier members. A reference is
    // declared either on the structure (ids maps each identifier to a member) or directly
    // on a member's target string shape (the member's own value is the single identifier).
    private static ObjectNode withReferenceHandles(Model model, ShapeId structureId, ObjectNode values) {
        Shape shape = model.getShape(structureId).orElse(null);
        if (shape == null) {
            return values;
        }
        ObjectNode result = values;

        // Structure references: ids maps a resource identifier name to the member that
        // provides its value.
        if (shape.hasTrait(ReferencesTrait.class)) {
            for (ReferencesTrait.Reference reference : shape.expectTrait(ReferencesTrait.class).getReferences()) {
                if (!reference.getName().isPresent()) {
                    continue;
                }
                ObjectNode.Builder ids = Node.objectNodeBuilder();
                for (Map.Entry<String, String> entry : reference.getIds().entrySet()) {
                    Node value = values.getMember(entry.getValue()).orElse(Node.nullNode());
                    ids.withMember(entry.getKey(), value);
                }
                result = result.withMember(reference.getName().get(), buildHandle(model, reference, ids.build()));
            }
        }

        // String-shape references: the trait is on a member's target string shape, whose
        // own value is the referenced resource's single identifier.
        for (MemberShape member : shape.members()) {
            Shape target = model.getShape(member.getTarget()).orElse(null);
            if (target == null || !target.isStringShape() || !target.hasTrait(ReferencesTrait.class)) {
                continue;
            }
            for (ReferencesTrait.Reference reference : target.expectTrait(ReferencesTrait.class).getReferences()) {
                if (!reference.getName().isPresent()) {
                    continue;
                }
                Optional<String> identifier = singleIdentifierName(model, reference.getResource());
                if (!identifier.isPresent()) {
                    continue;
                }
                Node value = values.getMember(member.getMemberName()).orElse(Node.nullNode());
                ObjectNode ids = Node.objectNodeBuilder().withMember(identifier.get(), value).build();
                result = result.withMember(reference.getName().get(), buildHandle(model, reference, ids));
            }
        }

        return result;
    }

    // Builds the {service, resource, ids} handle object. The handle locates the resource
    // under a service shape in the world snapshot: the reference's explicit service, else
    // the single service that contains the resource; left unset when that is ambiguous.
    private static ObjectNode buildHandle(Model model, ReferencesTrait.Reference reference, ObjectNode ids) {
        ObjectNode.Builder handle = Node.objectNodeBuilder()
                .withMember("resource", reference.getResource().toString())
                .withMember("ids", ids);
        serviceName(model, reference).ifPresent(service -> handle.withMember("service", service));
        return handle.build();
    }

    // The single identifier name of a resource referenced from a string shape, or empty
    // when the resource is missing or does not have exactly one identifier.
    private static Optional<String> singleIdentifierName(Model model, ShapeId resourceId) {
        return model.getShape(resourceId)
                .flatMap(Shape::asResourceShape)
                .map(ResourceShape::getIdentifiers)
                .filter(ids -> ids.size() == 1)
                .map(ids -> ids.keySet().iterator().next());
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
