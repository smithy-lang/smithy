/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ValidateCommand extends SimpleCommand {

    private static final Logger LOGGER = Logger.getLogger(ValidateCommand.class.getName());

    public ValidateCommand(String parentCommandName) {
        super(parentCommandName);
    }

    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getSummary() {
        return "Validates Smithy models";
    }

    @Override
    protected List<ArgumentReceiver> createArgumentReceivers() {
        return Collections.singletonList(new BuildOptions());
    }

    @Override
    protected int run(Arguments arguments, Env env, List<String> models) {
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        LOGGER.info(() -> "Validating Smithy model sources: " + models);
        CommandUtils.buildModel(arguments, models, env, env.stdout(), standardOptions.quiet());
        LOGGER.info("Smithy validation complete");
        return 0;
    }
}
