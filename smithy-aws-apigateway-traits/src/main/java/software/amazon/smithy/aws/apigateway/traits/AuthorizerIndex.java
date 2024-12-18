/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;

/**
 * Computes the effective authorizers of each resource and
 * operation in a service.
 *
 * <p>An effective authorizer of an operation first checks if the
 * operation has the authorizer trait, then the resource an operation
 * is bound to, then the service.
 */
public final class AuthorizerIndex implements KnowledgeIndex {

    private static final Selector SELECTOR = Selector.parse("operation");

    /** Mapping of service shapes to a map of shape -> authorizer name. */
    private final Map<ShapeId, Map<ShapeId, String>> authorizers = new HashMap<>();

    /** Mapping of service shapes to a authorizer traits. */
    private final Map<ShapeId, AuthorizersTrait> authorizerTraits = new HashMap<>();

    public AuthorizerIndex(Model model) {
        PathFinder finder = PathFinder.create(model);

        model.shapes(ServiceShape.class).forEach(service -> {
            service.getTrait(AuthorizersTrait.class).ifPresent(trait -> authorizerTraits.put(service.getId(), trait));
            Map<ShapeId, String> serviceMap = new HashMap<>();
            authorizers.put(service.getId(), serviceMap);

            // Account for the edge case of no operations on the service.
            service.getTrait(AuthorizerTrait.class)
                    .ifPresent(trait -> serviceMap.put(service.getId(), trait.getValue()));

            for (PathFinder.Path path : finder.search(service, SELECTOR)) {
                String effectiveAuthorizer = null;
                for (Shape shape : path.getShapes()) {
                    if (shape.isServiceShape() || shape.isResourceShape() || shape.isOperationShape()) {
                        effectiveAuthorizer = getNullableAuthorizerValue(shape, effectiveAuthorizer);
                        if (effectiveAuthorizer != null) {
                            serviceMap.put(shape.getId(), effectiveAuthorizer);
                        }
                    }
                }
            }
        });
    }

    public static AuthorizerIndex of(Model model) {
        return model.getKnowledge(AuthorizerIndex.class, AuthorizerIndex::new);
    }

    private static String getNullableAuthorizerValue(Shape shape, String previous) {
        return shape.getTrait(AuthorizerTrait.class).map(AuthorizerTrait::getValue).orElse(previous);
    }

    /**
     * Gets the effective authorizer name of a specific resource or operation
     * within a service.
     *
     * @param service Service shape to query.
     * @param shape Resource or operation shape ID to query.
     * @return Returns the optionally resolved authorizer name.
     */
    public Optional<String> getAuthorizer(ToShapeId service, ToShapeId shape) {
        return Optional.ofNullable(authorizers.get(service.toShapeId()))
                .flatMap(mappings -> Optional.ofNullable(mappings.get(shape.toShapeId())));
    }

    /**
     * Gets the effective authorizer name of a service.
     *
     * @param service Service shape to query.
     * @return Returns the optionally resolved authorizer name.
     */
    public Optional<String> getAuthorizer(ToShapeId service) {
        return getAuthorizer(service, service);
    }

    /**
     * Gets the effective authorizer structure value of a service.
     *
     * @param service Service shape to query.
     * @return Returns the optionally resolved authorizer name.
     */
    public Optional<AuthorizerDefinition> getAuthorizerValue(ToShapeId service) {
        return getAuthorizerValue(service, service);
    }

    /**
     * Gets the effective authorizer structure value of a shape in a service.
     *
     * @param service Service shape to query.
     * @param shape Shape to get the authorizer of within the service.
     * @return Returns the optionally resolved authorizer name.
     */
    public Optional<AuthorizerDefinition> getAuthorizerValue(ToShapeId service, ToShapeId shape) {
        return getAuthorizer(service, shape)
                .flatMap(name -> Optional.ofNullable(authorizerTraits.get(service.toShapeId()))
                        .flatMap(trait -> trait.getAuthorizer(name)));
    }
}
