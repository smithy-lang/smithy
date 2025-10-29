/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates the following:
 *
 * <ul>
 *     <li>XML and JSON bodyMediaTypes contain valid content.</li>
 *     <li>vendorParamsShape is a valid shape.</li>
 *     <li>Vendor params are compatible with any referenced shape.</li>
 *     <li>Params for a test case are valid for the model.</li>
 * </ul>
 *
 * @param <T> Type of test case to validate.
 */
abstract class ProtocolTestCaseValidator<T extends Trait> extends AbstractValidator {

    private final Class<T> traitClass;
    private final ShapeId traitId;
    private final String descriptor;

    ProtocolTestCaseValidator(ShapeId traitId, Class<T> traitClass, String descriptor) {
        this.traitId = traitId;
        this.traitClass = traitClass;
        this.descriptor = descriptor;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(traitClass)) {
            events.addAll(validateShape(model, operationIndex, shape, shape.expectTrait(traitClass)));
        }

        return events;
    }

    abstract StructureShape getStructure(Shape shape, OperationIndex operationIndex);

    abstract List<? extends HttpMessageTestCase> getTestCases(T trait);

    boolean isValidatedBy(Shape shape) {
        return shape instanceof OperationShape;
    }

    private List<ValidationEvent> validateShape(
            Model model,
            OperationIndex operationIndex,
            Shape shape,
            T trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        List<? extends HttpMessageTestCase> testCases = getTestCases(trait);

        for (int i = 0; i < testCases.size(); i++) {
            HttpMessageTestCase testCase = testCases.get(i);

            // Validate the syntax of known media types like XML and JSON.
            events.addAll(validateMediaType(shape, trait, testCase));

            // Validate the vendorParams for the test case if we have a shape defined.
            Optional<ShapeId> vendorParamsShapeOptional = testCase.getVendorParamsShape();
            ObjectNode vendorParams = testCase.getVendorParams();
            if (vendorParamsShapeOptional.isPresent() && isValidatedBy(shape)) {
                if (vendorParams.isEmpty()) {
                    // Warn if vendorParamsShape is set on the case and no vendorParams is set.
                    events.add(warning(shape,
                            trait,
                            "Protocol test case defined a `vendorParamsShape` but no `vendorParams`"));
                } else {
                    // Otherwise, validate the params against the shape.
                    Shape vendorParamsShape = model.expectShape(vendorParamsShapeOptional.get());
                    NodeValidationVisitor vendorParamsValidator = createVisitor(vendorParams,
                            model,
                            shape,
                            i,
                            ".vendorParams");
                    events.addAll(vendorParamsShape.accept(vendorParamsValidator));
                }
            }

            StructureShape struct = getStructure(shape, operationIndex);
            if (struct != null) {
                // Validate the params for the test case.
                NodeValidationVisitor validator = createVisitor(testCase.getParams(), model, shape, i, ".params");
                events.addAll(struct.accept(validator));
            } else if (!testCase.getParams().isEmpty() && isValidatedBy(shape)) {
                events.add(error(shape,
                        trait,
                        String.format(
                                "Protocol test %s parameters provided for operation with no %s: `%s`",
                                descriptor,
                                descriptor,
                                Node.printJson(testCase.getParams()))));
            }
        }

        return events;
    }

    private NodeValidationVisitor createVisitor(
            ObjectNode value,
            Model model,
            Shape shape,
            int position,
            String contextSuffix
    ) {
        return NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext(traitId + "." + position + contextSuffix)
                .eventId(getName())
                .timestampValidationStrategy(TimestampValidationStrategy.EPOCH_SECONDS)
                .addFeature(NodeValidationVisitor.Feature.ALLOW_OPTIONAL_NULLS)
                .build();
    }

    private List<ValidationEvent> validateMediaType(Shape shape, Trait trait, HttpMessageTestCase test) {
        if (!test.getBodyMediaType().isPresent()) {
            return Collections.emptyList();
        }

        return ProtocolTestValidationUtils.validateMediaType(test.getBody().orElse(""), test.getBodyMediaType().get())
                .map(e -> ListUtils.of(emitMediaTypeError(shape, trait, test, e)))
                .orElse(Collections.emptyList());
    }

    private ValidationEvent emitMediaTypeError(Shape shape, Trait trait, HttpMessageTestCase test, Throwable e) {
        return danger(shape,
                trait,
                String.format(
                        "Invalid %s content in `%s` protocol test case `%s`: %s",
                        test.getBodyMediaType().orElse(""),
                        trait.toShapeId(),
                        test.getId(),
                        e.getMessage()));
    }
}
