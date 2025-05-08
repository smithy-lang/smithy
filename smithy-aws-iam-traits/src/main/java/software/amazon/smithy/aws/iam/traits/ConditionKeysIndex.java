/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.aws.traits.ArnReferenceTrait;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.utils.MapUtils;
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
        for (ServiceShape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            // Defines the scoping of any derived condition keys.
            String arnNamespace = service.expectTrait(ServiceTrait.class).getArnNamespace();

            // Copy over the explicitly defined condition keys into the service map.
            // This will be mutated when adding inferred resource condition keys.
            Map<String, ConditionKeyDefinition> serviceKeys = new HashMap<>();
            if (service.hasTrait(DefineConditionKeysTrait.ID)) {
                DefineConditionKeysTrait trait = service.expectTrait(DefineConditionKeysTrait.class);
                for (Map.Entry<String, ConditionKeyDefinition> entry : trait.getConditionKeys().entrySet()) {
                    serviceKeys.put(resolveFullConditionKey(service, entry.getKey()), entry.getValue());
                }
            }
            serviceConditionKeys.put(service.getId(), serviceKeys);
            resourceConditionKeys.put(service.getId(), new HashMap<>());

            // Compute the keys of child resources.
            for (ShapeId resourceId : service.getResources()) {
                compute(model, service, arnNamespace, model.expectShape(resourceId, ResourceShape.class), null);
            }

            // Compute the keys of operations of the service.
            for (ShapeId operationId : service.getOperations()) {
                compute(model, service, arnNamespace, model.expectShape(operationId, OperationShape.class), null);
            }
        }
    }

    public static ConditionKeysIndex of(Model model) {
        return model.getKnowledge(ConditionKeysIndex.class, ConditionKeysIndex::new);
    }

    static String resolveFullConditionKey(ServiceShape service, String conditionKey) {
        // If no colon is present, we infer that this condition key is for the
        // current service and apply its ARN namespace.
        if (conditionKey.contains(":")) {
            return conditionKey;
        }
        return service.expectTrait(ServiceTrait.class).getArnNamespace() + ":" + conditionKey;
    }

    /**
     * Get all the explicit and inferred condition keys used in the entire service.
     *
     * <p>The result does not include global condition keys like "aws:accountId".
     * Use {@link #getConditionKeyNames} to find all the condition keys used
     * but not necessarily defined for a service.
     *
     * @param service Service shape/shapeId to get.
     * @return Returns the conditions keys of the service or an empty map when not found.
     */
    public Map<String, ConditionKeyDefinition> getDefinedConditionKeys(ToShapeId service) {
        return Collections.unmodifiableMap(serviceConditionKeys.getOrDefault(service.toShapeId(), MapUtils.of()));
    }

    /**
     * Get all the condition key names used in a service.
     *
     * @param service Service shape/shapeId use to scope the result.
     * @return Returns the conditions keys of the service or an empty map when not found.
     */
    public Set<String> getConditionKeyNames(ToShapeId service) {
        if (!resourceConditionKeys.containsKey(service.toShapeId())) {
            return SetUtils.of();
        }

        Set<String> names = new HashSet<>();
        for (Set<String> resourceKeyNames : resourceConditionKeys.get(service.toShapeId()).values()) {
            names.addAll(resourceKeyNames);
        }
        return names;
    }

    /**
     * Get all the defined condition keys used in an operation or resource, including
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
     * Get all the defined condition keys used in an operation or resource, including
     * any inferred keys and keys inherited by parent resource bindings.
     *
     * <p>The result does not include global condition keys like "aws:accountId".
     * Use {@link #getConditionKeyNames} to find all the condition keys used
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
            ServiceShape service,
            String arnRoot,
            Shape subject,
            ResourceShape parent
    ) {
        compute(model, service, arnRoot, subject, parent, SetUtils.of());
    }

    private void compute(
            Model model,
            ServiceShape service,
            String arnRoot,
            Shape subject,
            ResourceShape parent,
            Set<String> parentDefinitions
    ) {
        Set<String> definitions = new HashSet<>();
        if (!subject.hasTrait(IamResourceTrait.ID)
                || !subject.expectTrait(IamResourceTrait.class).isDisableConditionKeyInheritance()) {
            definitions.addAll(parentDefinitions);
        }
        resourceConditionKeys.get(service.getId()).put(subject.getId(), definitions);
        if (subject.hasTrait(ConditionKeysTrait.ID)) {
            definitions.addAll(subject.expectTrait(ConditionKeysTrait.class).resolveConditionKeys(service));
        }

        // Continue recursing into resources and computing keys.
        subject.asResourceShape().ifPresent(resource -> {
            boolean disableConditionKeyInference = resource.hasTrait(DisableConditionKeyInferenceTrait.ID)
                    || service.hasTrait(DisableConditionKeyInferenceTrait.ID);

            // Add any inferred resource identifiers to the resource and to the service-wide definitions.
            Map<String, String> childIdentifiers = !disableConditionKeyInference
                    ? inferChildResourceIdentifiers(model, service.getId(), arnRoot, resource, parent)
                    : MapUtils.of();

            // Compute the keys of each child operation, passing no keys.
            for (ShapeId operationId : resource.getOperations()) {
                Optional<Shape> operationOptional = model.getShape(operationId);
                if (operationOptional.isPresent()) {
                    compute(model, service, arnRoot, operationOptional.get(), resource);
                }
            }

            // Child resources always inherit the identifiers of the parent.
            definitions.addAll(childIdentifiers.values());

            // Compute the keys of each child resource.
            for (ShapeId resourceId : resource.getResources()) {
                Optional<Shape> resourceOptional = model.getShape(resourceId);
                if (resourceOptional.isPresent()) {
                    compute(model, service, arnRoot, resourceOptional.get(), resource, definitions);
                }
            }
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
                if (shape.hasTrait(ArnReferenceTrait.ID)) {
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
                // The identifier name consists of "[arn service]:[Resource name][uppercase identifier name]".
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
