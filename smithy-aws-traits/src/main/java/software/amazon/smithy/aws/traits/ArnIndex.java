/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static java.util.Collections.unmodifiableMap;
import static software.amazon.smithy.model.knowledge.IdentifierBindingIndex.BindingType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.Pair;

/**
 * Resolves and indexes the ARN templates for each resource in a service.
 */
public final class ArnIndex implements KnowledgeIndex {
    private final Map<ShapeId, String> arnServices = new HashMap<>();
    private final Map<ShapeId, Map<ShapeId, ArnTrait>> templates;
    private final Map<ShapeId, Map<ShapeId, ArnTrait>> effectiveArns = new HashMap<>();

    public ArnIndex(Model model) {
        // Pre-compute the ARN services.
        for (Shape service : model.getServiceShapesWithTrait(ServiceTrait.class)) {
            arnServices.put(service.getId(), service.expectTrait(ServiceTrait.class).getArnNamespace());
        }

        // Pre-compute all of the ARN templates in a service shape.
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        List<ServiceShape> services = model.shapes(ServiceShape.class)
                .filter(shape -> shape.hasTrait(ServiceTrait.class))
                .collect(Collectors.toList());

        templates = unmodifiableMap(services.stream()
                .map(service -> compileServiceArns(topDownIndex, service))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));

        // Pre-compute all effective ARNs in each service.
        IdentifierBindingIndex bindingIndex = IdentifierBindingIndex.of(model);
        for (ServiceShape service : services) {
            compileEffectiveArns(topDownIndex, bindingIndex, service);
        }
    }

    public static ArnIndex of(Model model) {
        return model.getKnowledge(ArnIndex.class, ArnIndex::new);
    }

    private Pair<ShapeId, Map<ShapeId, ArnTrait>> compileServiceArns(TopDownIndex index, ServiceShape service) {
        Map<ShapeId, ArnTrait> mapping = new HashMap<>();
        for (ResourceShape resource : index.getContainedResources(service.getId())) {
            resource.getTrait(ArnTrait.class).ifPresent(arnTrait -> {
                mapping.put(resource.getId(), arnTrait);
            });
        }

        return Pair.of(service.getId(), Collections.unmodifiableMap(mapping));
    }

    private void compileEffectiveArns(
            TopDownIndex index,
            IdentifierBindingIndex bindings,
            ServiceShape service
    ) {
        Map<ShapeId, ArnTrait> operationMappings = new HashMap<>();
        effectiveArns.put(service.getId(), operationMappings);

        // Iterate over every resource that has an ARN trait.
        for (Map.Entry<ShapeId, ArnTrait> entry : templates.get(service.getId()).entrySet()) {
            ShapeId resourceId = entry.getKey();
            ArnTrait arnTrait = entry.getValue();
            // Find all of the operations contained within the resource and determine the
            // kind of identifier binding of the operation. This dictates if the effective
            // ARN is the ARN on the resource or the parent of the resource.
            for (OperationShape operation : index.getContainedOperations(resourceId)) {
                BindingType bindingType = bindings.getOperationBindingType(resourceId, operation);
                if (bindingType == BindingType.INSTANCE) {
                    operationMappings.put(operation.getId(), arnTrait);
                } else if (bindingType == BindingType.COLLECTION) {
                    for (ResourceShape resource : index.getContainedResources(service)) {
                        if (resource.getResources().contains(entry.getKey())) {
                            resource.getTrait(ArnTrait.class)
                                    .ifPresent(trait -> operationMappings.put(operation.getId(), trait));
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the ARN service namespace of a service shape.
     *
     * @param serviceId Service shape to get ARN namespace of.
     * @return Returns the resolved ARN service namespace, defaulting to the
     *   lowercase shape name if not known.
     */
    public String getServiceArnNamespace(ToShapeId serviceId) {
        return arnServices.containsKey(serviceId.toShapeId())
                ? arnServices.get(serviceId.toShapeId())
                : serviceId.toShapeId().getName().toLowerCase(Locale.US);
    }

    /**
     * Gets all of the mappings of resources within a service to its
     * arn trait.
     *
     * @param service Service to retrieve.
     * @return Returns the mapping of resource ID to arn traits.
     */
    public Map<ShapeId, ArnTrait> getServiceResourceArns(ToShapeId service) {
        return templates.getOrDefault(service.toShapeId(), Collections.emptyMap());
    }

    /**
     * Gets the effective ARN of an operation based on the identifier bindings
     * of the operation bound to a resource contained within a service.
     *
     * <p>An operation bound to a resource using a collection binding has an
     * effective ARN of the parent of the resource. An operation bound to a
     * resource using an instance binding uses the ARN of the resource as
     * its effective ARN.
     *
     * @param service Service the operation is bound within.
     * @param operation Operation shape for which to find the effective ARN.
     * @return Returns the optionally found effective ARN.
     */
    public Optional<ArnTrait> getEffectiveOperationArn(ToShapeId service, ToShapeId operation) {
        return Optional.ofNullable(effectiveArns.get(service.toShapeId()))
                .flatMap(operationArns -> Optional.ofNullable(operationArns.get(operation.toShapeId())));
    }

    /**
     * Expands the relative ARN of a resource with the service name to form a
     * full ARN template.
     *
     * <p>For relative ARNs, the returned template string is in the format of
     * <code>arn:{AWS::Partition}:service:{AWS::Region}:{AWS::AccountId}:resource</code>
     * where "service" is the resolved ARN service name of the service and
     * "resource" is the resource part of the arn template.
     * "{AWS::Region}" is added to the template if the arn "noRegion"
     * value is not set to true. "{AWS::AccountId}" is added to the template if
     * the arn "noAccount" value is not set to true.
     *
     * <p>For example, if both "noAccount" and "noRegion" are set to true,
     * the resolved ARN template might look like "arn:{AWS::Partition}:service:::resource".
     *
     * <p>Absolute ARN templates are returned as-is.
     *
     * @param service Service shape ID.
     * @param resource Resource shape ID.
     * @return Returns the optionally found ARN template for a resource.
     */
    public Optional<String> getFullResourceArnTemplate(ToShapeId service, ToShapeId resource) {
        return Optional.ofNullable(getServiceResourceArns(service).get(resource.toShapeId()))
                .map(trait -> {
                    StringBuilder result = new StringBuilder();
                    if (!trait.isAbsolute()) {
                        result.append("arn:")
                                .append("{AWS::Partition}:")
                                .append(getServiceArnNamespace(service))
                                .append(":");
                        if (!trait.isNoRegion()) {
                            result.append("{AWS::Region}");
                        }
                        result.append(":");
                        if (!trait.isNoAccount()) {
                            result.append("{AWS::AccountId}");
                        }
                        result.append(":");
                    }

                    return result.append(trait.getTemplate()).toString();
                });
    }
}
