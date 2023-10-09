/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.smoketests.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;

/**
 * Validates the following:
 *
 * <ul>
 *     <li>vendorParamsShape is a valid shape.</li>
 *     <li>vendorParams is compatible with vendorParamsShape.</li>
 *     <li>input is valid for the operation under test.</li>
 * </ul>
 */
public class SmokeTestCaseValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(SmokeTestsTrait.class)) {
            SmokeTestsTrait trait = shape.expectTrait(SmokeTestsTrait.class);
            List<SmokeTestCase> testCases = trait.getTestCases();

            for (int i = 0; i < testCases.size(); i++) {
                SmokeTestCase testCase = testCases.get(i);

                // Validate vendor params shape
                Optional<ShapeId> vendorParamsShapeOptional = testCase.getVendorParamsShape();
                Optional<ObjectNode> vendorParamsOptional = testCase.getVendorParams();
                if (vendorParamsShapeOptional.isPresent()) {
                    if (!vendorParamsOptional.isPresent()) {
                        events.add(warning(shape, trait,
                                "Smoke test case defined a `vendorParamsShape` but no `vendorParams`"));
                    } else {
                        Shape vendorParamsShape = model.expectShape(vendorParamsShapeOptional.get());
                        NodeValidationVisitor vendorParamsValidator = createVisitor(vendorParamsOptional.get(), model,
                                shape, i, ".vendorParams");
                        events.addAll(vendorParamsShape.accept(vendorParamsValidator));
                    }
                }

                // Validate input params
                StructureShape input = operationIndex.expectInputShape(shape);
                if (input != null && testCase.getParams().isPresent()) {
                    NodeValidationVisitor paramsValidator = createVisitor(testCase.getParams().get(), model, shape, i,
                            ".params");
                    events.addAll(input.accept(paramsValidator));
                } else if (testCase.getParams().isPresent()) {
                    events.add(error(shape, trait, String.format(
                            "Smoke test parameters provided for operation with no input: `%s`",
                            Node.printJson(testCase.getParams().get())

                    )));
                }
            }
        }
        return events;
    }

    private NodeValidationVisitor createVisitor(
            ObjectNode node,
            Model model,
            Shape shape,
            int position,
            String contextSuffix
    ) {
        return NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(node)
                .startingContext(SmokeTestsTrait.ID + "." + position + contextSuffix)
                .eventId(getName())
                .timestampValidationStrategy(TimestampValidationStrategy.EPOCH_SECONDS)
                .allowOptionalNull(true)
                .build();
    }
}
