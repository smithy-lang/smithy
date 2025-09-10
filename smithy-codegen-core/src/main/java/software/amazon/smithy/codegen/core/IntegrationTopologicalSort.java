/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.utils.DependencyGraph;

final class IntegrationTopologicalSort<I extends SmithyIntegration<?, ?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(IntegrationTopologicalSort.class.getName());

    private final Map<String, I> integrationLookup = new LinkedHashMap<>();
    private final Map<String, Integer> insertionOrder = new HashMap<>();
    private final DependencyGraph<String> dependencyGraph = new DependencyGraph<>();

    IntegrationTopologicalSort(Iterable<I> integrations) {
        for (I integration : integrations) {
            I previous = this.integrationLookup.put(integration.name(), integration);
            if (previous != null) {
                throw new IllegalArgumentException(String.format(
                        "Conflicting SmithyIntegration names detected for '%s': %s and %s",
                        integration.name(),
                        integration.getClass().getCanonicalName(),
                        previous.getClass().getCanonicalName()));
            }
            insertionOrder.put(integration.name(), insertionOrder.size());
            dependencyGraph.add(integration.name());
        }
        for (I integration : integrations) {
            dependencyGraph.addDependencies(integration.name(), integration.runAfter());
            for (String dependant : integration.runBefore()) {
                dependencyGraph.addDependency(dependant, integration.name());
            }
        }
    }

    List<I> sort() {
        List<I> result = new ArrayList<>(dependencyGraph.size());
        List<String> sorted;
        try {
            sorted = dependencyGraph.toSortedList(this::compareIntegrations);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e);
        }
        for (String name : sorted) {
            I integration = integrationLookup.get(name);
            if (integration != null) {
                result.add(integration);
            } else {
                logMissingIntegration(name);
            }
        }
        return result;
    }

    private int compareIntegrations(String left, String right) {
        I leftIntegration = integrationLookup.get(left);
        I rightIntegration = integrationLookup.get(right);
        if (leftIntegration == null || rightIntegration == null) {
            return 0;
        }
        // Priority order is used to sort first.
        int byteResult = Byte.compare(rightIntegration.priority(), leftIntegration.priority());
        // If priority is a tie, then sort based on insertion order of integrations.
        // This makes the order deterministic.
        return byteResult == 0
                ? Integer.compare(insertionOrder.get(left), insertionOrder.get(right))
                : byteResult;
    }

    private void logMissingIntegration(String name) {
        StringBuilder message = new StringBuilder("Could not find SmithyIntegration named '");
        message.append(name).append('\'');
        if (!dependencyGraph.getDirectDependants(name).isEmpty()) {
            message.append(" that was supposed to run before integrations [");
            message.append(String.join(", ", dependencyGraph.getDirectDependants(name)));
            message.append("]");
        }
        if (!dependencyGraph.getDirectDependencies(name).isEmpty()) {
            if (!dependencyGraph.getDirectDependants(name).isEmpty()) {
                message.append(" and ");
            } else {
                message.append(" that ");
            }
            message.append("was supposed to run after integrations [");
            message.append(String.join(", ", dependencyGraph.getDirectDependencies(name)));
            message.append("]");
        }
        LOGGER.warning(message.toString());
    }
}
