/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.utils.ListUtils;

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
 * Note: if you override {@link #getAdditionalProjectionsFunction()} and
 * do not override {@link #transform(TransformContext)}, the configuration for
 * your transformer will be deserialized twice during execution.
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

    @Override
    public List<String> getAdditionalProjections(TransformContext context) {
        return getAdditionalProjectionsFunction().map(fn -> {
            NodeMapper mapper = new NodeMapper();
            T config = mapper.deserialize(context.getSettings(), getConfigType());
            return fn.apply(context, config);
        }).orElseGet(ListUtils::of);
    }

    /**
     * Executes the transform using the deserialized configuration object.
     *
     * @param context Transform context.
     * @param config Deserialized configuration object.
     * @return Returns the transformed model.
     */
    protected abstract Model transformWithConfig(TransformContext context, T config);

    /**
     * @return an Optional of either a BiFunction that returns the additional
     *         projections to run after this one, or empty to indicate this
     *         projection will never compose other ones.
     */
    protected Optional<BiFunction<TransformContext, T, List<String>>> getAdditionalProjectionsFunction() {
        return Optional.empty();
    }
}
