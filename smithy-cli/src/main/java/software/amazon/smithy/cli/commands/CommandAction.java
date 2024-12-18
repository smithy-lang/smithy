/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;

/**
 * A shared abstraction used to compose the {@link Command#execute(Arguments, Command.Env)} method of commands.
 */
@FunctionalInterface
interface CommandAction {
    int apply(Arguments arguments, Command.Env env);
}
