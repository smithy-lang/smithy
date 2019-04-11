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

package software.amazon.smithy.aws.traits;

import static java.util.Collections.unmodifiableMap;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Resolves and indexes the ARN templates for each resource in a service.
 */
public final class ArnIndex implements KnowledgeIndex {
    private final Map<ShapeId, String> arnServices;
    private final Map<ShapeId, Map<ShapeId, ArnTrait>> templates;

    public ArnIndex(Model model) {
        // Pre-compute the ARN services.
        arnServices = unmodifiableMap(model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, ServiceTrait.class))
                .map(pair -> Pair.of(pair.getLeft().getId(), resolveServiceArn(pair)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));

        // Pre-compute all of the ArnTemplates in a service shape.
        var topDownIndex = model.getKnowledge(TopDownIndex.class);
        templates = unmodifiableMap(model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, ServiceTrait.class))
                .map(pair -> compileServiceArns(topDownIndex, pair.getLeft()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
    }

    private static String resolveServiceArn(Pair<ServiceShape, ServiceTrait> pair) {
        return pair.getRight().getArnNamespace();
    }

    private Pair<ShapeId, Map<ShapeId, ArnTrait>> compileServiceArns(
            TopDownIndex index,
            ServiceShape service
    ) {
        return Pair.of(service.getId(), unmodifiableMap(index.getContainedResources(service.getId()).stream()
                .flatMap(resource -> Trait.flatMapStream(resource, ArnTrait.class))
                .map(pair -> Pair.of(pair.getLeft().getId(), pair.getRight()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight))));
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
     * arnTemplate trait.
     *
     * @param service Service to retrieve.
     * @return Returns the mapping of resource ID to arnTemplate traits.
     */
    public Map<ShapeId, ArnTrait> getServiceResourceArns(ToShapeId service) {
        return templates.getOrDefault(service.toShapeId(), Collections.emptyMap());
    }

    /**
     * Expands the relative ARN of a resource with the service name to form a
     * full ARN template.
     *
     * <p>For relative ARNs, the returned template string is in the format of
     * <code>arn:{AWS::Partition}:service:{AWS::Region}:{AWS::AccountId}:resource</code>
     * where "service" is the resolved ARN service name of the service and
     * "resource" is the resource part of the arnTemplate template.
     * "{AWS::Region}" is added to the template if the arnTemplate "noRegion"
     * value is not set to true. "{AWS::AccountId}" is added to the template if
     * the arnTemplate "noAccount" value is not set to true.
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
