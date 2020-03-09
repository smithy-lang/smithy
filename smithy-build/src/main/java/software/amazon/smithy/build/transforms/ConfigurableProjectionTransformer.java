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

package software.amazon.smithy.build.transforms;

import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;

/**
 * An abstract class used to more easily implement a Smithy build projection
 * transformer that expects configuration input in a specific type, {@code T}.
 *
 * <p>This class will automatically deserialize the given {@code Node}
 * value in the {@code T} and invoke {@link #transformWithConfig(TransformContext, Object)}
 * with the deserialized configuration of type {@code T}.
 *
 * <p><strong>If your build transformer requires configuration, then you typically
 * should just extend this class.</strong></p>
 *
 * @param <T> The configuration setting type (e.g., a POJO).
 */
public abstract class ConfigurableProjectionTransformer<T> implements ProjectionTransformer {
    /**
     * Gets the configuration class type.
     *
     * <p>The referenced {@code configType} class must be a public POJO with a
     * public, zero-arg constructor, getters, and setters. If the POJO has a
     * public static {@code fromNode} method, it will be invoked and is
     * expected to deserialize the Node. If the POJO has a public static
     * {@code builder} method, it will be invoked, setters will be called
     * on the builder POJO, and finally the result of calling the
     * {@code build} method is used as the configuration type. Finally,
     * the deserializer will attempt to create the type and call setters on
     * the instantiated object that correspond to property names (either named
     * "set" + property name, or just property name).
     *
     * @return Returns the configuration class (a POJO with setters/getters).
     */
    public abstract Class<T> getConfigType();

    @Override
    public Model transform(TransformContext context) {
        NodeMapper mapper = new NodeMapper();
        T config = mapper.deserialize(context.getSettings(), getConfigType());
        return transformWithConfig(context, config);
    }

    /**
     * Executes the transform using the deserialized configuration object.
     *
     * @param context Transform context.
     * @param config Deserialized configuration object.
     * @return Returns the transformed model.
     */
    protected abstract Model transformWithConfig(TransformContext context, T config);
}
