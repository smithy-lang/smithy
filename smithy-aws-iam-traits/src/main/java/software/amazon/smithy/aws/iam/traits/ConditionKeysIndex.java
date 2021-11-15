/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.iam.traits;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.aws.traits.ArnReferenceTrait;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Provides an index of condition keys for a service, including any condition
 * keys inferred from resource identifiers.
 */
public final class ConditionKeysIndex implements KnowledgeIndex {
    private static final String STRING_TYPE = "String";
    private static final String ARN_TYPE = "ARN";

    private final Map<ShapeId, Map<String, ConditionKeyDefinition>> serviceConditionKeys = new HashMap<>();
    private final Map<ShapeId, Map<ShapeId, Set<String>>> resourceConditionKeys = new HashMap<>();

    public ConditionKeysIndex(Model model) {
        model.shapes(ServiceShape.class).forEach(service -> {
            service.getTrait(ServiceTrait.class).ifPresent(trait -> {
                // Copy over the explicitly defined condition keys into the service map.
                // This will be mutated when adding inferred resource condition keys.
                serviceConditionKeys.put(service.getId(), new HashMap<>(
                        service.getTrait(DefineConditionKeysTrait.class)
                                .map(DefineConditionKeysTrait::getConditionKeys)
                                .orElse(MapUtils.of())));
                resourceConditionKeys.put(service.getId(), new HashMap<>());

                // Defines the scoping of any derived condition keys.
                String arnRoot = trait.getArnNamespace();

                // Compute the keys of child resources.
                service.getResources().stream()
                        .flatMap(id -> OptionalUtils.stream(model.getShape(id)))
                        .forEach(resource -> {
                            compute(model, service.getId(), arnRoot, resource, null);
                        });

                // Compute the keys of operations of the service.
                service.getOperations().stream()
                        .flatMap(id -> OptionalUtils.stream(model.getShape(id)))
                        .forEach(operation -> {
                            compute(model, service.getId(), arnRoot, operation, null);
                        });
            });
        });
    }

    public static ConditionKeysIndex of(Model model) {
        return model.getKnowledge(ConditionKeysIndex.class, ConditionKeysIndex::new);
    }

    /**
     * Get all of the explicit and inferred condition keys used in the entire service.
     *
     * <p>The result does not include global condition keys like "aws:accountId".
     * Use {@link #getConditionKeyNames} to find all of the condition keys used
     * but not necessarily defined for a service.
     *
     * @param service Service shape/shapeId to get.
     * @return Returns the conditions keys of the service or an empty map when not found.
     */
    public Map<String, ConditionKeyDefinition> getDefinedConditionKeys(ToShapeId service) {
        return Collections.unmodifiableMap(serviceConditionKeys.getOrDefault(service.toShapeId(), MapUtils.of()));
    }

    /**
     * Get all of the condition key names used in a service.
     *
     * @param service Service shape/shapeId use to scope the result.
     * @return Returns the conditions keys of the service or an empty map when not found.
     */
    public Set<String> getConditionKeyNames(ToShapeId service) {
        return resourceConditionKeys.getOrDefault(service.toShapeId(), MapUtils.of())
                .values().stream()
                .flatMap(Set::stream)
                .collect(SetUtils.toUnmodifiableSet());
    }

    /**
     * Get all of the defined condition keys used in an operation or resource, including
     * any inferred keys and keys inherited by parent resource bindings.
     *
     * @param service Service shape/shapeId use to scope the result.
     * @param resourceOrOperation Resource or operation shape/shapeId
     * @return Returns the conditions keys of the service or an empty map when not found.
     */
    public Set<String> getConditionKeyNames(ToShapeId service, ToShapeId resourceOrOperation) {
        ShapeId serviceId = service.toShapeId();
        ShapeId subjectId = resourceOrOperation.toShapeId();
        return Collections.unmodifiableSet(
                resourceConditionKeys.getOrDefault(serviceId, MapUtils.of()).getOrDefault(subjectId, SetUtils.of()));
    }

    /**
     * Get all of the defined condition keys used in an operation or resource, including
     * any inferred keys and keys inherited by parent resource bindings.
     *
     * <p>The result does not include global condition keys like "aws:accountId".
     * Use {@link #getConditionKeyNames} to find all of the condition keys used
     * but not necessarily defined for a resource or operation.
     *
     * @param service Service shape/shapeId use to scope the result.
     * @param resourceOrOperation Resource or operation shape/shapeId
     * @return Returns the conditions keys of the service or an empty map when not found.
     */
    public Map<String, ConditionKeyDefinition> getDefinedConditionKeys(
            ToShapeId service,
            ToShapeId resourceOrOperation
    ) {
        Map<String, ConditionKeyDefinition> serviceDefinitions = getDefinedConditionKeys(service);
        Map<String, ConditionKeyDefinition> definitions = new HashMap<>();

        for (String name : getConditionKeyNames(service, resourceOrOperation)) {
            if (serviceDefinitions.containsKey(name)) {
                definitions.put(name, serviceDefinitions.get(name));
            }
        }

        return definitions;
    }

    private void compute(
            Model model,
            ShapeId service,
            String arnRoot,
            Shape subject,
            ResourceShape parent
    ) {
        compute(model, service, arnRoot, subject, parent, SetUtils.of());
    }

    private void compute(
            Model model,
            ShapeId service,
            String arnRoot,
            Shape subject,
            ResourceShape parent,
            Set<String> parentDefinitions
    ) {
        Set<String> definitions = new HashSet<>(parentDefinitions);
        resourceConditionKeys.get(service).put(subject.getId(), definitions);
        subject.getTrait(ConditionKeysTrait.class).ifPresent(trait -> definitions.addAll(trait.getValues()));

        // Continue recursing into resources and computing keys.
        subject.asResourceShape().ifPresent(resource -> {
            // Add any inferred resource identifiers to the resource and to the service-wide definitions.
            Map<String, String> childIdentifiers = !resource.hasTrait(DisableConditionKeyInferenceTrait.class)
                    ? inferChildResourceIdentifiers(model, service, arnRoot, resource, parent)
                    : MapUtils.of();

            // Compute the keys of each child operation, passing no keys.
            resource.getAllOperations().stream().flatMap(id -> OptionalUtils.stream(model.getShape(id)))
                    .forEach(child -> compute(model, service, arnRoot, child, resource));

            // Child resources always inherit the identifiers of the parent.
            definitions.addAll(childIdentifiers.values());

            // Compute the keys of each child resource.
            resource.getResources().stream().flatMap(id -> OptionalUtils.stream(model.getShape(id))).forEach(child -> {
                compute(model, service, arnRoot, child, resource, definitions);
            });
        });
    }

    private Map<String, String> inferChildResourceIdentifiers(
            Model model,
            ShapeId service,
            String arnRoot,
            ResourceShape resource,
            ResourceShape parent
    ) {
        Map<String, String> result = new HashMap<>();

        // We want child resources to reuse parent resource context keys, so
        // extract out identifiers that were introduced by the child resource.
        Set<String> parentIds = parent == null ? SetUtils.of() : parent.getIdentifiers().keySet();
        Set<String> childIds = new HashSet<>(resource.getIdentifiers().keySet());
        childIds.removeAll(parentIds);

        for (String childId : childIds) {
            model.getShape(resource.getIdentifiers().get(childId)).ifPresent(shape -> {
                // Only infer identifiers introduced by a child. Children should
                // use their parent identifiers and not duplicate them.
                ConditionKeyDefinition.Builder builder = ConditionKeyDefinition.builder();
                if (shape.hasTrait(ArnReferenceTrait.class)) {
                    // Use an ARN type if the targeted shape has the arnReference trait.
                    builder.type(ARN_TYPE);
                } else {
                    // Fall back to a string type otherwise.
                    builder.type(STRING_TYPE);
                }

                // Inline provided documentation or compute a simple string.
                builder.documentation(shape.getTrait(DocumentationTrait.class)
                        .map(DocumentationTrait::getValue)
                        .orElse(computeIdentifierDocs(resource, childId)));
                // The identifier name is comprised of "[arn service]:[Resource name][uppercase identifier name]
                String computeIdentifierName = computeIdentifierName(arnRoot, resource, childId);
                // Add the computed identifier binding and resolved context key to the result map.
                result.put(childId, computeIdentifierName);
                // Register the newly inferred context key definition with the service.
                serviceConditionKeys.get(service).put(computeIdentifierName, builder.build());
            });
        }

        return result;
    }

    private static String computeIdentifierDocs(ResourceShape resource, String identifierName) {
        return getContextKeyResourceName(resource) + " resource " + identifierName + " identifier";
    }

    private static String computeIdentifierName(String arnRoot, ResourceShape resource, String identifierName) {
        return arnRoot + ":" + getContextKeyResourceName(resource) + StringUtils.capitalize(identifierName);
    }

    private static String getContextKeyResourceName(ResourceShape resource) {
        return resource.getTrait(IamResourceTrait.class)
                       .flatMap(IamResourceTrait::getName)
                       .orElse(resource.getId().getName());
    }
}
