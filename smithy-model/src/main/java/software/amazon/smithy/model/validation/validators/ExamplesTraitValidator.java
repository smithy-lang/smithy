/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            // allowConstraintErrors only makes sense when the example demonstrates an error.
            if (example.getAllowConstraintErrors() && !example.getError().isPresent()) {
                events.add(error(shape,
                        trait,
                        String.format(
                                "Example: `%s` has allowConstraintErrors enabled, so error must be defined.",
                                example.getTitle())));
            }

            ObjectNode call = buildOperationCall(model, shape, example);
            events.addAll(createVisitor(call, model, shape, example).validateOperationCall(shape));
        }

        return events;
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

    private NodeValidationVisitor createVisitor(
            ObjectNode value,
            Model model,
            OperationShape shape,
            ExamplesTrait.Example example
    ) {
        NodeValidationVisitor.Builder builder = NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext("Example `" + example.getTitle() + "`")
                .eventId(getName());
        if (example.getAllowConstraintErrors()) {
            builder.addFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS);
        }
        return builder.build();
    }
}
