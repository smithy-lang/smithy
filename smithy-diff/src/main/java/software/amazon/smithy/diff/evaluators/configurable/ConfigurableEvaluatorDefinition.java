/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.diff.evaluators.configurable;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.ListUtils;

/**
 * Definition of a diff evaluator which can be configured within a Smithy model.
 */
final class ConfigurableEvaluatorDefinition {
    private static final String ID = "id";
    private static final String MESSAGE = "message";
    private static final String EMIT_CONDITION = "emitCondition";
    private static final String APPLIES_TO = "appliesTo";
    private static final String SEVERITY = "severity";
    private static final String FILTER = "filter";
    private static final String SELECTOR = "selector";
    private static final List<String> ALLOWED_PROPERTIES = ListUtils.of(
            ID, MESSAGE, EMIT_CONDITION, APPLIES_TO, SEVERITY, FILTER, SELECTOR);


    private final String id;
    private final String message;
    private final EmitCondition emitCondition;
    private final AppliesTo appliesTo;
    private final Severity severity;
    private final Selector filter;
    private final Selector selector;

    private ConfigurableEvaluatorDefinition(
            String id,
            String message,
            EmitCondition emitCondition,
            AppliesTo appliesTo,
            Severity severity,
            Selector filter,
            Selector selector
    ) {
        this.id = id;
        this.message = message;
        this.emitCondition = emitCondition;
        this.appliesTo = appliesTo;
        this.severity = severity;
        this.filter = filter;
        this.selector = selector;
    }

    static ConfigurableEvaluatorDefinition fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode("Configurable diff evaluators must be objects.");
        objectNode.warnIfAdditionalProperties(ALLOWED_PROPERTIES);
        String id = objectNode.expectStringMember(ID).getValue();
        String message = objectNode.expectStringMember(MESSAGE).getValue();
        EmitCondition emitCondition = EmitCondition.fromNode(objectNode.expectStringMember(EMIT_CONDITION));
        AppliesTo appliesTo = AppliesTo.fromNode(objectNode.expectStringMember(APPLIES_TO));
        Severity severity = Severity.fromNode(objectNode.expectStringMember(SEVERITY));
        Selector filter = objectNode.getStringMember(FILTER).map(Selector::fromNode).orElse(null);
        Selector selector = Selector.fromNode(objectNode.expectStringMember(SELECTOR));
        return new ConfigurableEvaluatorDefinition(
                id,
                message,
                emitCondition,
                appliesTo,
                severity,
                filter,
                selector
        );
    }

    /**
     * @return The id of the event the diff evaluator will emit.
     */
    String getId() {
        return id;
    }

    /**
     * @return The message in the event that the diff evaluator will emit.
     */
    String getMessage() {
        return message;
    }

    /**
     * @return The condition on which the diff evaluator will emit an event.
     */
    EmitCondition getEmitCondition() {
        return emitCondition;
    }

    /**
     * @return What kind of diff the evaluator applies to.
     */
    AppliesTo getAppliesTo() {
        return appliesTo;
    }

    /**
     * @return The severity of the events emitted by the diff evaluator.
     */
    Severity getSeverity() {
        return severity;
    }

    /**
     * @return An optional selector used to filter the subset of shapes configured by {@link #getAppliesTo()}.
     */
    Optional<Selector> getFilter() {
        return Optional.ofNullable(filter);
    }

    /**
     * @return The selector that chooses which shapes the diff evaluator should apply to.
     */
    Selector getSelector() {
        return selector;
    }
}
