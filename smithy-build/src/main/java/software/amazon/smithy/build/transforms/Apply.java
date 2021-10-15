/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;

/**
 * Applies transforms of other projections.
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
        for (String projection : config.getProjections()) {
            context.enqueueProjection(projection);
        }

        return context.getModel();
    }
}
