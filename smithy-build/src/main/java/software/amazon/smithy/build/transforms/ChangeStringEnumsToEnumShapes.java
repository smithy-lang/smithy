/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;

/**
 * {@code changeStringEnumsToEnumShapes} is used to change string shapes
 * bearing the enum trait into enum shapes.
 */
public final class ChangeStringEnumsToEnumShapes
        extends ConfigurableProjectionTransformer<ChangeStringEnumsToEnumShapes.Config> {

    /**
     * {@code changeStringEnumsToEnumShapes} configuration settings.
     */
    public static final class Config {
        private boolean synthesizeNames = false;

        /**
         * @param synthesizeNames Whether enums without names should have names synthesized if possible.
         */
        public void setSynthesizeNames(boolean synthesizeNames) {
            this.synthesizeNames = synthesizeNames;
        }

        public boolean getSynthesizeNames() {
            return synthesizeNames;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    public String getName() {
        return "changeStringEnumsToEnumShapes";
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        return context.getTransformer()
                .changeStringEnumsToEnumShapes(
                        context.getModel(),
                        config.getSynthesizeNames());
    }
}
