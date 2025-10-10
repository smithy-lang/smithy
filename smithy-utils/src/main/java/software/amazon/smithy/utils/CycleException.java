/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Signals that one or more cycles have been detected when attempting to topologically
 * sort shapes in a {@link DependencyGraph}.
 */
public class CycleException extends RuntimeException {
    private final List<?> sortedNodes;
    private final Set<?> cyclicNodes;
    private final Class<?> nodeType;

    /**
     * Constructs a CycleException.
     *
     * @param sortedNodes A list of the nodes that were sorted successfully.
     * @param cyclicNodes A set of nodes that could not be sorted due to being part of a cycle.
     * @param <T> The type of the node.
     */
    public <T> CycleException(List<T> sortedNodes, Set<T> cyclicNodes) {
        super(String.format("Cycle(s) detected amongst: [%s]",
                cyclicNodes.stream().map(Object::toString).collect(Collectors.joining(", "))));
        this.sortedNodes = ListUtils.copyOf(sortedNodes);
        this.cyclicNodes = SetUtils.orderedCopyOf(cyclicNodes);
        if (this.cyclicNodes.isEmpty()) {
            throw new IllegalArgumentException("Cyclic nodes cannot be empty");
        }
        this.nodeType = this.cyclicNodes.iterator().next().getClass();
    }

    /**
     * Gets the set of nodes that are part of a cycle.
     *
     * <p>This contains all nodes that are a part of any cycles. To see a list of
     * individual cycles, use {@link DependencyGraph#findCycles()}.
     *
     * @param expectedNodeType The expected type of the node, which will be checked to
     *        be compatible with the actual type. This is necessary because
     *        exceptions can't be generic.
     * @return Returns a set of cyclic nodes.
     * @param <T> The type of the graph's nodes.
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getCyclicNodes(Class<T> expectedNodeType) {
        if (expectedNodeType.isAssignableFrom(this.nodeType)) {
            return (Set<T>) cyclicNodes;
        }
        throw new IllegalArgumentException(String.format(
                "Expected node type %s is not assignable from actual node type %s",
                expectedNodeType.getName(),
                this.nodeType.getName()));
    }

    /**
     * Gets the list of nodes that could be sorted.
     *
     * @param expectedNodeType The expected type of the node, which will be checked to
     *        be compatible with the actual type. This is necessary because
     *        exceptions can't be generic.
     * @return Returns the sorted list of non-cyclic nodes.
     * @param <T> The type of the graph's nodes.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getSortedNodes(Class<T> expectedNodeType) {
        if (expectedNodeType.isAssignableFrom(this.nodeType)) {
            return (List<T>) sortedNodes;
        }
        throw new IllegalArgumentException(String.format(
                "Expected node type %s is not assignable from actual node type %s",
                expectedNodeType.getName(),
                this.nodeType.getName()));
    }
}
