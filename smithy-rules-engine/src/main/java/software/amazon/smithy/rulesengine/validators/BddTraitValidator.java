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
import software.amazon.smithy.rulesengine.traits.BddTrait;

public final class BddTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(BddTrait.class)) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(BddTrait.class)) {
            validateService(events, service, service.expectTrait(BddTrait.class));
        }

        return events;
    }

    private void validateService(List<ValidationEvent> events, ServiceShape service, BddTrait trait) {
        Bdd bdd = trait.getBdd();

        // Validate root reference
        int rootRef = bdd.getRootRef();
        if (Bdd.isComplemented(rootRef)) {
            events.add(error(service, trait, "Root reference cannot be complemented: " + rootRef));
        }
        validateReference(events, service, trait, "Root", rootRef, bdd);

        // Validate node references
        int[][] nodes = bdd.getNodes();
        for (int i = 0; i < nodes.length; i++) {
            // Skip terminal node at index 0
            if (i == 0) {
                continue;
            }

            // Guard against malformed nodes array
            if (nodes[i] == null || nodes[i].length != 3) {
                events.add(error(service, trait, String.format("Node %d is malformed", i)));
                continue;
            }

            int[] node = nodes[i];
            int varIdx = node[0];
            int highRef = node[1];
            int lowRef = node[2];

            if (varIdx < 0 || varIdx >= bdd.getConditionCount()) {
                events.add(error(service,
                        trait,
                        String.format(
                                "Node %d has invalid variable index %d (condition count: %d)",
                                i,
                                varIdx,
                                bdd.getConditionCount())));
            }

            validateReference(events, service, trait, String.format("Node %d high", i), highRef, bdd);
            validateReference(events, service, trait, String.format("Node %d low", i), lowRef, bdd);
        }
    }

    private void validateReference(
            List<ValidationEvent> events,
            ServiceShape service,
            BddTrait trait,
            String context,
            int ref,
            Bdd bdd
    ) {
        if (ref == 0) {
            events.add(error(service, trait, String.format("%s has invalid reference: 0", context)));
        } else if (Bdd.isNodeReference(ref)) {
            int nodeIndex = Math.abs(ref) - 1;
            if (nodeIndex >= bdd.getNodes().length) {
                events.add(error(service,
                        trait,
                        String.format(
                                "%s reference %d points to non-existent node %d (node count: %d)",
                                context,
                                ref,
                                nodeIndex,
                                bdd.getNodes().length)));
            }
        } else if (Bdd.isResultReference(ref)) {
            int resultIndex = ref - Bdd.RESULT_OFFSET;
            if (resultIndex >= bdd.getResults().size()) {
                events.add(error(service,
                        trait,
                        String.format(
                                "%s reference %d points to non-existent result %d (result count: %d)",
                                context,
                                ref,
                                resultIndex,
                                bdd.getResults().size())));
            }
        }
    }
}
