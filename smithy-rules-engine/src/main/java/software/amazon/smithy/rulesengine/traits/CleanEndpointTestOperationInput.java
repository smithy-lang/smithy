/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public class CleanEndpointTestOperationInput implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        Set<Shape> servicesToUpdate = getServicesToUpdate(model, removed);
        return transformer.replaceShapes(model, servicesToUpdate);
    }

    private Set<Shape> getServicesToUpdate(Model model, Collection<Shape> removed) {
        // Precompute shape ids to operations that were removed.
        Map<String, OperationShape> removedOperationMap = new HashMap<>();
        for (Shape shape : removed) {
            if (shape.isOperationShape()) {
                removedOperationMap.put(shape.getId().getName(), shape.asOperationShape().get());
            }
        }

        Set<Shape> result = new HashSet<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            EndpointTestsTrait trait = serviceShape.expectTrait(EndpointTestsTrait.class);
            if (trait.getTestCases().isEmpty()) {
                continue;
            }

            List<EndpointTestCase> updatedTestCases = new ArrayList<>(trait.getTestCases());
            // Check each input to each test case and remove entries from the list.
            for (EndpointTestCase testCase : trait.getTestCases()) {
                for (EndpointTestOperationInput input : testCase.getOperationInputs()) {
                    if (removed.contains(removedOperationMap.get(input.getOperationName()))) {
                        updatedTestCases.remove(testCase);
                    }
                }
            }

            // Update the shape if the trait has changed.
            if (updatedTestCases.size() != trait.getTestCases().size()) {
                result.add(serviceShape.toBuilder()
                        .addTrait(trait.toBuilder().testCases(updatedTestCases).build())
                        .build());
            }
        }

        return result;
    }
}
