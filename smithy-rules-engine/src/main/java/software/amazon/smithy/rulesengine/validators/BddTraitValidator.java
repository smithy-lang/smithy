/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.logic.bdd.Bdd;
import software.amazon.smithy.rulesengine.logic.bdd.EndpointBddTrait;

public final class BddTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(EndpointBddTrait.class)) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(EndpointBddTrait.class)) {
            validateService(events, service, service.expectTrait(EndpointBddTrait.class));
        }

        return events;
    }

    private void validateService(List<ValidationEvent> events, ServiceShape service, EndpointBddTrait trait) {
        Bdd bdd = trait.getBdd();

        // Validate root reference
        int rootRef = bdd.getRootRef();
        if (Bdd.isComplemented(rootRef) && rootRef != -1) {
            events.add(error(service, trait, "Root reference cannot be complemented: " + rootRef));
        }
        validateReference(events, service, trait, "Root", rootRef, bdd, trait);

        // Validate that condition and result counts match what's in the trait
        if (bdd.getConditionCount() != trait.getConditions().size()) {
            events.add(error(service,
                    trait,
                    String.format("BDD condition count (%d) doesn't match trait conditions (%d)",
                            bdd.getConditionCount(),
                            trait.getConditions().size())));
        }

        if (bdd.getResultCount() != trait.getResults().size()) {
            events.add(error(service,
                    trait,
                    String.format("BDD result count (%d) doesn't match trait results (%d)",
                            bdd.getResultCount(),
                            trait.getResults().size())));
        }

        // Validate nodes
        int nodeCount = bdd.getNodeCount();

        for (int i = 0; i < nodeCount; i++) {
            // Skip terminal node at index 0
            if (i == 0) {
                continue;
            }

            int varIdx = bdd.getVariable(i);
            int highRef = bdd.getHigh(i);
            int lowRef = bdd.getLow(i);

            if (varIdx < 0 || varIdx >= bdd.getConditionCount()) {
                events.add(error(service,
                        trait,
                        String.format(
                                "Node %d has invalid variable index %d (condition count: %d)",
                                i,
                                varIdx,
                                bdd.getConditionCount())));
            }

            validateReference(events, service, trait, String.format("Node %d high", i), highRef, bdd, trait);
            validateReference(events, service, trait, String.format("Node %d low", i), lowRef, bdd, trait);
        }
    }

    private void validateReference(
            List<ValidationEvent> events,
            ServiceShape service,
            EndpointBddTrait trait,
            String context,
            int ref,
            Bdd bdd,
            EndpointBddTrait bddTrait
    ) {
        if (ref == 0) {
            events.add(error(service, trait, String.format("%s has invalid reference: 0", context)));
        } else if (Bdd.isNodeReference(ref)) {
            int nodeIndex = Math.abs(ref) - 1;
            int nodeCount = bdd.getNodeCount();
            if (nodeIndex >= nodeCount) {
                events.add(error(service,
                        trait,
                        String.format(
                                "%s reference %d points to non-existent node %d (node count: %d)",
                                context,
                                ref,
                                nodeIndex,
                                nodeCount)));
            }
        } else if (Bdd.isResultReference(ref)) {
            int resultIndex = ref - Bdd.RESULT_OFFSET;
            if (resultIndex >= bddTrait.getResults().size()) {
                events.add(error(service,
                        trait,
                        String.format(
                                "%s reference %d points to non-existent result %d (result count: %d)",
                                context,
                                ref,
                                resultIndex,
                                bddTrait.getResults().size())));
            }
        }
    }
}
