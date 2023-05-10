/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
