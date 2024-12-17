/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.plugins;

import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.node.NodeMapper;

/**
 * An abstract class used to more easily implement a Smithy build plugin
 * that expects configuration input in a specific type, {@code T}.
 *
 * <p>This class will automatically deserialize the given {@code Node}
 * value in the {@code T} and invoke {@link #executeWithConfig(PluginContext, Object)}
 * with the deserialized configuration of type {@code T}.
 *
 * <p><strong>If your build plugin requires configuration, then you typically
 * should just extend this class.</strong></p>
 *
 * @param <T> The configuration setting type (e.g., a POJO).
 */
public abstract class ConfigurableSmithyBuildPlugin<T> implements SmithyBuildPlugin {
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
     * it that correspond to property names.
     *
     * @return Returns the configuration class (a POJO with setters/getters).
     */
    public abstract Class<T> getConfigType();

    @Override
    public void execute(PluginContext context) {
        NodeMapper mapper = new NodeMapper();
        T config = mapper.deserialize(context.getSettings(), getConfigType());
        executeWithConfig(context, config);
    }

    /**
     * Executes the plugin using the deserialized configuration object.
     *
     * @param context Plugin context.
     * @param config Deserialized configuration object.
     */
    protected abstract void executeWithConfig(PluginContext context, T config);
}
