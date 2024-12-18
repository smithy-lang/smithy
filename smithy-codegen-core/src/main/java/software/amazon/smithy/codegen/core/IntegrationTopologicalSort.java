/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

final class IntegrationTopologicalSort<I extends SmithyIntegration<?, ?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(IntegrationTopologicalSort.class.getName());

    private final Map<String, I> integrationLookup = new LinkedHashMap<>();
    private final Map<String, Integer> insertionOrder = new HashMap<>();
    private final Map<String, Set<String>> forwardDependencies = new LinkedHashMap<>();
    private final Map<String, Set<String>> reverseDependencies = new LinkedHashMap<>();

    private final Queue<String> satisfied = new PriorityQueue<>((left, right) -> {
        I leftIntegration = integrationLookup.get(left);
        I rightIntegration = integrationLookup.get(right);
        // Priority order is used to sort first.
        int byteResult = Byte.compare(rightIntegration.priority(), leftIntegration.priority());
        // If priority is a tie, then sort based on insertion order of integrations.
        // This makes the order deterministic.
        return byteResult == 0
                ? Integer.compare(insertionOrder.get(left), insertionOrder.get(right))
                : byteResult;
    });

    IntegrationTopologicalSort(Iterable<I> integrations) {
        // Validate name conflicts and register integrations with the lookup table + insertion order table.
        for (I integration : integrations) {
            addIntegration(integration);
        }

        // Validate missing dependencies and add found dependencies.
        for (I integration : integrations) {
            for (String before : getValidatedDependencies("before", integration.name(), integration.runBefore())) {
                addDependency(before, integration.name());
            }
            for (String after : getValidatedDependencies("after", integration.name(), integration.runAfter())) {
                addDependency(integration.name(), after);
            }
        }

        // Offer satisfied dependencies.
        for (I integration : integrations) {
            if (!forwardDependencies.containsKey(integration.name())) {
                satisfied.offer(integration.name());
            }
        }
    }

    private void addIntegration(I integration) {
        I previous = this.integrationLookup.put(integration.name(), integration);
        insertionOrder.put(integration.name(), insertionOrder.size());
        if (previous != null) {
            throw new IllegalArgumentException(String.format(
                    "Conflicting SmithyIntegration names detected for '%s': %s and %s",
                    integration.name(),
                    integration.getClass().getCanonicalName(),
                    previous.getClass().getCanonicalName()));
        }
    }

    private List<String> getValidatedDependencies(String descriptor, String what, List<String> dependencies) {
        if (dependencies.isEmpty()) {
            return dependencies;
        } else {
            List<String> filtered = new ArrayList<>(dependencies);
            filtered.removeIf(value -> {
                if (integrationLookup.containsKey(value)) {
                    return false;
                } else {
                    LOGGER.warning(what + " is supposed to run " + descriptor + " an integration that could "
                            + "not be found, '" + value + "'");
                    return true;
                }
            });
            return filtered;
        }
    }

    private void addDependency(String what, String dependsOn) {
        forwardDependencies.computeIfAbsent(what, n -> new LinkedHashSet<>()).add(dependsOn);
        reverseDependencies.computeIfAbsent(dependsOn, n -> new LinkedHashSet<>()).add(what);
    }

    List<I> sort() {
        List<I> result = new ArrayList<>();

        while (!satisfied.isEmpty()) {
            String current = satisfied.poll();
            forwardDependencies.remove(current);
            result.add(integrationLookup.get(current));

            for (String dependent : reverseDependencies.getOrDefault(current, Collections.emptySet())) {
                Set<String> dependentDependencies = forwardDependencies.get(dependent);
                dependentDependencies.remove(current);
                if (dependentDependencies.isEmpty()) {
                    satisfied.offer(dependent);
                }
            }
        }

        if (!forwardDependencies.isEmpty()) {
            throw new IllegalArgumentException("SmithyIntegration cycles detected among "
                    + forwardDependencies.keySet());
        }

        return result;
    }
}
