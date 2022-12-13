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

package software.amazon.smithy.cli;

import java.util.function.Consumer;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A command line argument receiver.
 *
 * <p>All non-positional arguments of a {@link Command} need a
 * corresponding receiver to accept it through either
 * {@link #testOption(String)} or {@link #testParameter(String)}.
 * If a non-positional argument is not accepted by any receiver,
 * the CLI will exit with an error.
 */
@SmithyUnstableApi
public interface ArgumentReceiver {
    /**
     * Test if the given value-less option is accepted by the receiver.
     *
     * <p>If the option is accepted, the receiver should store a stateful
     * value to indicate the option was received, and the CLI will skip
     * the argument for further processing.
     *
     * @param name Name of the option to test.
     * @return Returns true if accepted.
     */
    default boolean testOption(String name) {
        return false;
    }

    /**
     * Test if the given parameter that requires a value is accepted by the receiver.
     *
     * <p>If the parameter is accepted, the receiver returns a Consumer that
     * receives the expected value, and it should store a stateful value to allow
     * the value to be later recalled. The CLI will skip the argument for further
     * processing.
     *
     * @param name Name of the parameter to test.
     * @return Returns a consumer if accepted or null if not accepted.
     */
    default Consumer<String> testParameter(String name) {
        return null;
    }

    /**
     * Registers help information to the given {@link HelpPrinter}.
     *
     * @param printer Printer to modify.
     */
    default void registerHelp(HelpPrinter printer) {
        // do nothing by default.
    }
}
