/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import static software.amazon.smithy.model.node.Node.loadArrayOfString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.ValidatedResult;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.Suppression;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * For backward compatibility, this will load both smithy.validators and
 * validators; smithy.suppressions and suppressions.
 */
final class ValidationLoader {
    private static final Logger LOGGER = Logger.getLogger(ValidationLoader.class.getName());
    private static final List<String> SEVERITIES = ListUtils.of("DANGER", "WARNING", "NOTE");
    private static final List<String> SUPPRESSION_PROPERTIES = ListUtils.of("ids", "shapes", "reason");
    private static final List<String> VALIDATOR_PROPERTIES = ListUtils.of(
            "name", "id", "message", "severity", "namespaces", "configuration");

    private ValidationLoader() {}

    static ValidatedResult<List<ValidatorDefinition>> loadValidators(Map<String, Node> metadata) {
        return loadMultiple(metadata, "validators", "smithy.validators", ValidationLoader::loadSingleValidator);
    }

    static ValidatedResult<List<Suppression>> loadSuppressions(Map<String, Node> metadata) {
        return loadMultiple(metadata, "suppressions", "smithy.suppressions", ValidationLoader::loadSingleSuppression);
    }

    private static <T> ValidatedResult<List<T>> loadMultiple(
            Map<String, Node> metadata,
            String newKey,
            String oldKey,
            Function<ObjectNode, T> f
    ) {
        if (!metadata.containsKey(oldKey)) {
            return load(metadata, newKey, f);
        }

        LOGGER.warning(String.format("`%s` is deprecated. Use `%s` instead", oldKey, newKey));
        if (!metadata.containsKey(newKey)) {
            return load(metadata, oldKey, f);
        }

        ValidatedResult<List<T>> result1 = load(metadata, newKey, f);
        ValidatedResult<List<T>> result2 = load(metadata, oldKey, f);
        List<T> merged = new ArrayList<>(result1.getResult().orElse(ListUtils.of()));
        merged.addAll(result2.getResult().orElse(ListUtils.of()));
        List<ValidationEvent> events = new ArrayList<>(result1.getValidationEvents());
        events.addAll(result2.getValidationEvents());
        return new ValidatedResult<>(merged, events);
    }

    private static <T> ValidatedResult<List<T>> load(
            Map<String, Node> metadata,
            String key,
            Function<ObjectNode, T> f
    ) {
        if (!metadata.containsKey(key)) {
            return ValidatedResult.empty();
        }

        List<ValidationEvent> events = new ArrayList<>();
        List<T> result = new ArrayList<>();
        Node node = metadata.get(key);
        try {
            ArrayNode values = node.expectArrayNode(key + " must be an array. Found {type}.");
            for (Node element : values.getElements()) {
                try {
                    ObjectNode definition = element.expectObjectNode(
                            "Each element of `" + key + "` must be an object. Found {type}.");
                    result.add(f.apply(definition));
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
        String name = node.expectMember("name", "Validator is missing a required `name` property.")
                .expectStringNode().getValue();
        ValidatorDefinition def = new ValidatorDefinition(
                name, node.getStringMember("id").map(StringNode::getValue).orElse(name));
        def.message = node.getStringMember("message").map(StringNode::getValue).orElse(null);
        def.severity = node.getStringMember("severity")
                .map(value -> value.expectOneOf(SEVERITIES))
                .map(value -> Severity.fromString(value).get())
                .orElse(null);
        node.getMember("namespaces").ifPresent(value -> def.namespaces.addAll(loadArrayOfString("namespaces", value)));
        def.configuration = node.getObjectMember("configuration").orElse(Node.objectNode());
        return def;
    }

    private static Suppression loadSingleSuppression(ObjectNode node) {
        node.warnIfAdditionalProperties(SUPPRESSION_PROPERTIES);
        Suppression.Builder builder = Suppression.builder().sourceLocation(node.getSourceLocation());
        loadArrayOfString("ids", node.expectMember("ids", "Missing required validator `ids` property."))
                .forEach(builder::addValidatorId);
        node.getMember("shapes")
                .map(value -> loadArrayOfString("shapes", value))
                .ifPresent(values -> values.forEach(builder::addShape));
        node.getStringMember("reason").map(StringNode::getValue).ifPresent(builder::reason);
        return builder.build();
    }
}
