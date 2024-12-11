/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.SmithyCli;

/**
 * Hidden command that implements {@code smithy --version} to print the current version.
 */
final class VersionCommand implements Command {
    @Override
    public String getName() {
        return "--version";
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String getSummary() {
        return "";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        env.stdout().println(SmithyCli.getVersion());
        return 0;
    }
}
