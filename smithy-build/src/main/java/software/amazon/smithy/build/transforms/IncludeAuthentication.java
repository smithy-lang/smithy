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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Removes authentication schemes from shapes that do not match one of
 * the given arguments (a list of authentication schemes).
 */
public final class IncludeAuthentication implements ProjectionTransformer {
    @Override
    public String getName() {
        return "includeAuthentication";
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Set<String> includeNames = new HashSet<>(arguments);
        return (transformer, model) -> transformer.mapShapes(
                model,
                shape -> shape.getTrait(AuthenticationTrait.class)
                        .map(authTrait -> {
                            Set<String> names = getSchemeNameIntersection(authTrait, includeNames);
                            return names.size() == authTrait.getAuthenticationSchemes().size()
                                    ? shape
                                    : updateTrait(shape, authTrait, includeNames);
                        })
                        .orElse(shape));
    }

    private static Set<String> getSchemeNameIntersection(AuthenticationTrait endpoints, Set<String> includeNames) {
        Set<String> names = endpoints.getAuthenticationSchemes().keySet();
        names.retainAll(includeNames);
        return names;
    }

    private static Shape updateTrait(Shape shape, AuthenticationTrait trait, Set<String> names) {
        AuthenticationTrait.Builder builder = AuthenticationTrait.builder();
        builder.clearAuthenticationSchemes();
        trait.getAuthenticationSchemes().entrySet().stream()
                .filter(e -> names.contains(e.getKey()))
                .forEach(entry -> builder.putAuthenticationScheme(entry.getKey(), entry.getValue()));
        return Shape.shapeToBuilder(shape).addTrait(builder.build()).build();
    }
}
