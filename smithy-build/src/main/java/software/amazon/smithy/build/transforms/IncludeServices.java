/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * {@code includeServices} filters out service shapes that are not
 * included in the list of shape IDs contained in the
 * {@code services} property.
 */
public final class IncludeServices extends BackwardCompatHelper<IncludeServices.Config> {

    /**
     * {@code includeServices} configuration.
     */
    public static final class Config {
        private Set<ShapeId> services = Collections.emptySet();

        /**
         * @return Gets the list of service shapes IDs to include.
         */
        public Set<ShapeId> getServices() {
            return services;
        }

        /**
         * Sets the list of service shapes IDs to include.
         *
         * @param services Services to include by shape ID.
         */
        public void setServices(Set<ShapeId> services) {
            this.services = services;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "includeServices";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "services";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Set<ShapeId> services = config.getServices();
        Model model = context.getModel();
        ModelTransformer transformer = context.getTransformer();
        return transformer.filterShapes(model, shape -> !shape.isServiceShape() || services.contains(shape.getId()));
    }
}
