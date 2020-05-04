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

import java.util.List;
import java.util.Set;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;

/**
 * Recursively applies transforms of other projections.
 *
 * <p>Note: this transform is special cased and not created using a
 * normal factory. This is because this transformer needs to
 * recursively transform models based on projections, and no other
 * transform needs this functionality. We could *maybe* address
 * this later if we really care that much.
 */
public final class Apply extends BackwardCompatHelper<Apply.Config> {

    /**
     * {@code apply} configuration.
     */
    public static final class Config {
        private List<String> projections;

        /**
         * Gets the ordered list of projections to apply by name.
         *
         * @return Returns the projection names to apply.
         */
        public List<String> getProjections() {
            return projections;
        }

        /**
         * Sets the ordered list of projection names to apply.
         *
         * @param projections Projection names to apply.
         */
        public void setProjections(List<String> projections) {
            this.projections = projections;
        }
    }

    @FunctionalInterface
    public interface ApplyCallback {
        Model apply(Model inputModel, String projectionName, Set<String> visited);
    }

    private final ApplyCallback applyCallback;

    /**
     * Sets the function used to apply projections.
     *
     * @param applyCallback Takes the projection name, model, and returns the updated model.
     */
    public Apply(ApplyCallback applyCallback) {
        this.applyCallback = applyCallback;
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "apply";
    }

    @Override
    String getBackwardCompatibleNameMapping() {
        return "projections";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        Model current = context.getModel();
        Set<String> visited = context.getVisited();

        for (String projection : config.getProjections()) {
            current = applyCallback.apply(current, projection, visited);
        }

        return current;
    }
}
