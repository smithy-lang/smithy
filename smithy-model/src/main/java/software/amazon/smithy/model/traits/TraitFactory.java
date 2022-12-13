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

package software.amazon.smithy.model.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Creates traits using trait data from a model.
 */
@FunctionalInterface
public interface TraitFactory {
    /**
     * Creates and configures a trait using model trait data.
     *
     * @param id Shape ID of the trait.
     * @param target Shape that the trait is applied on.
     * @param value The Node value of the trait.
     * @return Returns the created trait wrapped in an Optional.
     * @throws SourceException on configuration error.
     * @throws RuntimeException if an error occurs while creating the trait.
     */
    Optional<Trait> createTrait(ShapeId id, ShapeId target, Node value);

    /**
     * Creates a TraitFactory that uses a List of TraitService provider instances.
     *
     * @param services List of TraitService provider instances.
     * @return Returns the created TraitFactory.
     */
    static TraitFactory createServiceFactory(Iterable<TraitService> services) {
        Map<ShapeId, TraitService> serviceMap = new HashMap<>();
        services.forEach(service -> serviceMap.put(service.getShapeId(), service));
        return (name, target, value) -> Optional.ofNullable(serviceMap.get(name))
                .map(provider -> provider.createTrait(target, value));
    }

    /**
     * Creates a TraitFactory that discovers TraitService providers using
     * the Thread context class loader.
     *
     * @return Returns the created TraitFactory.
     */
    static TraitFactory createServiceFactory() {
        return createServiceFactory(ServiceLoader.load(TraitService.class));
    }

    /**
     * Creates a TraitFactory that discovers TraitService providers using
     * the given ClassLoader.
     *
     * @param classLoader Class loader used to find TraitService providers.
     * @return Returns the created TraitFactory.
     */
    static TraitFactory createServiceFactory(ClassLoader classLoader) {
        return createServiceFactory(ServiceLoader.load(TraitService.class, classLoader));
    }
}
