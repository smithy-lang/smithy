/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static software.amazon.smithy.model.node.Node.loadArrayOfString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

final class ValidationLoader {

    private static final List<String> SEVERITIES = ListUtils.of("DANGER", "WARNING", "NOTE");
    private static final List<String> VALIDATOR_PROPERTIES = ListUtils.of(
            "name",
            "id",
            "message",
            "severity",
            "namespaces",
            "selector",
            "configuration");

    private ValidationLoader() {}

    static ValidatedResult<List<ValidatorDefinition>> loadValidators(Map<String, Node> metadata) {
        if (!metadata.containsKey("validators")) {
            return ValidatedResult.empty();
        }

        List<ValidationEvent> events = new ArrayList<>();
        List<ValidatorDefinition> result = new ArrayList<>();
        Node node = metadata.get("validators");
        try {
            ArrayNode values = node.expectArrayNode("validators must be an array. Found {type}.");
            for (Node element : values.getElements()) {
                try {
                    ObjectNode definition = element.expectObjectNode(
                            "Each element of `validators` must be an object. Found {type}.");
                    result.add(ValidationLoader.loadSingleValidator(definition));
                } catch (SourceException e) {
                    events.add(ValidationEvent.fromSourceException(e));
                }
            }
        } catch (SourceException e) {
            events.add(ValidationEvent.fromSourceException(e));
        }

        return new ValidatedResult<>(result, events);
    }

    private static ValidatorDefinition loadSingleValidator(ObjectNode node) {
        node.warnIfAdditionalProperties(VALIDATOR_PROPERTIES);
        String name = node.expectStringMember("name").getValue();
        String id = node.getStringMemberOrDefault("id", name);
        ValidatorDefinition def = new ValidatorDefinition(name, id);
        def.sourceLocation = node.getSourceLocation();
        def.message = node.getStringMemberOrDefault("message", null);
        def.severity = node.getStringMember("severity")
                .map(value -> value.expectOneOf(SEVERITIES))
                .map(value -> Severity.fromString(value).get())
                .orElse(null);
        node.getMember("namespaces").ifPresent(value -> def.namespaces.addAll(loadArrayOfString("namespaces", value)));
        def.configuration = node.getObjectMember("configuration").orElse(Node.objectNode());

        node.getStringMember("selector").ifPresent(selector -> {
            def.selector = Selector.fromNode(selector);
        });

        return def;
    }
}
