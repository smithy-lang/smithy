/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;

/**
 * Similar to {@link CommandAction} but is also provided a loaded {@link SmithyBuildConfig}.
 */
@FunctionalInterface
interface CommandActionWithConfig {
    int apply(SmithyBuildConfig config, Arguments arguments, Command.Env env);
}
