/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.diff;

import static software.amazon.smithy.model.validation.ValidationUtils.orderedTickedList;
import static software.amazon.smithy.rulesengine.language.EndpointRuleSet.EndpointPathCollector;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.diff.evaluators.AbstractDiffEvaluator;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Emit diff validation on the `@smithy.rules#endpointRuleSet` trait for the endpoint `authSchemes` property.
 *
 * @see <a href="https://smithy.io/2.0/additional-specs/rules-engine/specification.html#endpoint-authschemes-list-property">Endpoint `authSchemes` list property</a>
 */
@SmithyInternalApi
public final class EndpointAuthSchemes extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = new ArrayList<>();

        // Validate service shape `@smithy.rules#endpointRuleSet` changes
        List<ChangedShape<ServiceShape>> serviceChanges = differences
            .changedShapes(ServiceShape.class)
            .collect(Collectors.toList());
        for (ChangedShape<ServiceShape> change : serviceChanges) {
            Optional<Pair<EndpointRuleSetTrait, EndpointRuleSetTrait>> endpointRuleSetOpt =
                change.getChangedTrait(EndpointRuleSetTrait.class);
            if (!endpointRuleSetOpt.isPresent()) {
                continue;
            }
            Map<String, Endpoint> oldEndpoints =
                new EndpointPathCollector(endpointRuleSetOpt.get().getLeft()).collect();
            Map<String, Endpoint> newEndpoints =
                new EndpointPathCollector(endpointRuleSetOpt.get().getRight()).collect();
            ServiceShape service = change.getNewShape();

            // Endpoint entry removed and `authSchemes` was not empty
            Map<String, Endpoint> removedEndpoints = oldEndpoints.entrySet().stream()
                .filter(e -> !newEndpoints.containsKey(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            for (Entry<String, Endpoint> entry : removedEndpoints.entrySet()) {
                String jsonPath = entry.getKey();
                Endpoint endpoint = entry.getValue();
                List<String> endpointAuthSchemes = endpoint.getEndpointAuthSchemes().stream()
                    .map(Endpoint::getEndpointAuthSchemeName)
                    .collect(Collectors.toList());
                if (endpointAuthSchemes.size() != 0) {
                    events.add(danger(
                        service,
                        "The endpoint with an `authSchemes` property of [" + orderedTickedList(endpointAuthSchemes)
                        + "] was removed from `@smithy.rules#endpointRuleSet` applied to `" + service.getId() + "` at "
                        + "the path: `" + jsonPath + "`. "
                        + "The removal may not be backward compatible since the authentication schemes defined in the "
                        + "endpoint will no longer override the modeled authentication schemes. "
                        + "Ensure authentication succeeds for the endpoint with clients built from both the current "
                        + "and new models."));
                }
            }

            // Endpoint entry added and `authSchemes` is not empty
            Map<String, Endpoint> addedEndpoints = newEndpoints.entrySet().stream()
                .filter(e -> !oldEndpoints.containsKey(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            for (Entry<String, Endpoint> entry : addedEndpoints.entrySet()) {
                String jsonPath = entry.getKey();
                Endpoint endpoint = entry.getValue();
                List<String> endpointAuthSchemes = endpoint.getEndpointAuthSchemes().stream()
                    .map(Endpoint::getEndpointAuthSchemeName)
                    .collect(Collectors.toList());
                if (endpointAuthSchemes.size() != 0) {
                    events.add(warning(
                        service,
                        "The endpoint with an `authSchemes` property of [" + orderedTickedList(endpointAuthSchemes)
                        + "] was added to `@smithy.rules#endpointRuleSet` applied to `" + service.getId()
                        + "` at the path: `" + jsonPath + "`. "
                        + "The authentication schemes defined in the endpoint authentication schemes will override "
                        + "the modeled authentication schemes, and cannot be easily modified without breaking "
                        + "changes. "
                        + "Ensure authentication succeeds for the new endpoint with clients built from the new "
                        + "model."));
                }
            }

            // Endpoint entries that exist in both the old and new model and have been modified
            Map<String, Pair<Endpoint, Endpoint>> modifiedEndpoints = newEndpoints.entrySet().stream()
                .filter(e -> oldEndpoints.containsKey(e.getKey()))
                .map(e -> new SimpleEntry<String, Pair<Endpoint, Endpoint>>(
                    e.getKey(),
                    Pair.of(oldEndpoints.get(e.getKey()), e.getValue())))
                .filter(e -> !e.getValue().getLeft().equals(e.getValue().getRight()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            for (Entry<String, Pair<Endpoint, Endpoint>> entry : modifiedEndpoints.entrySet()) {
                String jsonPath = entry.getKey();
                Endpoint oldEndpoint = entry.getValue().getLeft();
                Endpoint newEndpoint = entry.getValue().getRight();
                if (oldEndpoint.getEndpointAuthSchemes().equals(newEndpoint.getEndpointAuthSchemes())) {
                    continue;
                }
                List<String> oldEndpointAuthSchemes = oldEndpoint.getEndpointAuthSchemes().stream()
                    .map(Endpoint::getEndpointAuthSchemeName)
                    .collect(Collectors.toList());
                List<String> newEndpointAuthSchemes = newEndpoint.getEndpointAuthSchemes().stream()
                    .map(Endpoint::getEndpointAuthSchemeName)
                    .collect(Collectors.toList());
                // Endpoint entry authSchemes removed
                if (oldEndpointAuthSchemes.size() != 0 && newEndpointAuthSchemes.size() == 0) {
                   events.add(danger(
                        service,
                        "The `authSchemes` property [" + orderedTickedList(oldEndpointAuthSchemes) + "] was removed "
                        + "from an endpoint in `@smithy.rules#endpointRuleSet` applied to `" + service.getId()
                        + "` at the path: `" + jsonPath + "`. "
                        + "The removal may not be backward compatible since the authentication schemes defined in the "
                        + "endpoint will no longer override the modeled authentication schemes. "
                        + "Ensure authentication succeeds for the endpoint with clients built from both the current "
                        + "and new models."));
                // Endpoint entry authSchemes added
                } else if (oldEndpointAuthSchemes.size() == 0 && newEndpointAuthSchemes.size() != 0) {
                    events.add(danger(
                        service,
                        "The `authSchemes` property [" + orderedTickedList(newEndpointAuthSchemes) + "] was added "
                        + "to an endpoint in `@smithy.rules#endpointRuleSet` applied to `" + service.getId()
                        + "` at the path: `" + jsonPath + "`. "
                        + "The addition may not be backward compatible since the modeled authentication schemes "
                        + "defined in the endpoint will now be overriden by the endpoint authentication schemes. "
                        + "Ensure authentication succeeds for the endpoint with clients built from both the current "
                        + "and new models."));
                // Endpoint entry authSchemes modified
                } else {
                    events.add(danger(
                        service,
                        "The `authSchemes` property was modified in an endpoint in `@smithy.rules#endpointRuleSet` "
                        + "applied to `" + service.getId() + "` at the path: `" + jsonPath + "`. "
                        + "The change may not be backward compatible since the endpoint authentication schemes have "
                        + "been modified. "
                        + "Ensure authentication succeeds for the endpoint with clients built from both the current "
                        + "and new models."));
                }
            }
        }

        return events;
    }
}
