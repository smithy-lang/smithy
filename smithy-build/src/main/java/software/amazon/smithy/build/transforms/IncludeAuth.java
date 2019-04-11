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

package software.amazon.smithy.build.transforms;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.ListUtils;

/**
 * Removes authentication schemes from "auth" traits and from the "auth"
 * property of protocols that do not match one of the given arguments
 * (a list of authentication schemes).
 */
public final class IncludeAuth implements ProjectionTransformer {
    @Override
    public String getName() {
        return "includeAuth";
    }

    @Override
    public List<String> getAliases() {
        return ListUtils.of("includeAuthentication");
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Set<String> includeNames = new HashSet<>(arguments);
        return (transformer, model) -> transformer.mapShapes(model, shape -> {
            // First update the auth trait on all shapes.
            Shape result = shape.getTrait(AuthTrait.class)
                    .map(authTrait -> updateShapeAuth(shape, authTrait, includeNames))
                    .orElse(shape);
            // Next update the protocols trait on service shapes.
            return result.getTrait(ProtocolsTrait.class)
                    .map(protocolsTrait -> updateProtocolsTrait(result, protocolsTrait, includeNames))
                    .orElse(result);
        });
    }

    private static Shape updateShapeAuth(Shape shape, AuthTrait authTrait, Set<String> includeNames) {
        Set<String> names = getIntersection(authTrait.getValues(), includeNames);

        // Only modify the shape if it actually changed the listed schemes.
        if (names.size() == authTrait.getValues().size()) {
            return shape;
        }

        AuthTrait.Builder builder = AuthTrait.builder();
        builder.clearValues();
        authTrait.getValues().stream().filter(names::contains).forEach(builder::addValue);
        return Shape.shapeToBuilder(shape).addTrait(builder.build()).build();
    }

    private static Set<String> getIntersection(Collection<String> values, Set<String> includeNames) {
        Set<String> names = new HashSet<>(values);
        names.retainAll(includeNames);
        return names;
    }

    private static Shape updateProtocolsTrait(Shape shape, ProtocolsTrait trait, Set<String> includeNames) {
        Set<String> schemes = new HashSet<>(trait.getAllAuthSchemes());
        schemes.removeAll(includeNames);

        // Return the shape as-is if it doesn't contain any additional schemes.
        if (schemes.isEmpty()) {
            return shape;
        }

        // Clear out and then re-add each updated protocol.
        ProtocolsTrait.Builder protocolTraitBuilder = trait.toBuilder().clearProtocols();

        for (Protocol protocol : trait.getProtocols()) {
            Protocol.Builder protocolBuilder = protocol.toBuilder();
            // Clear out and re-add any auth schemes that are permitted.
            protocolBuilder.clearAuth();
            for (String auth : protocol.getAuth()) {
                if (includeNames.contains(auth)) {
                    protocolBuilder.addAuth(auth);
                }
            }
            protocolTraitBuilder.addProtocol(protocolBuilder.build());
        }

        return Shape.shapeToBuilder(shape).addTrait(protocolTraitBuilder.build()).build();
    }
}
