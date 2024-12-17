/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.util.function.Consumer;

/**
 * A command line argument receiver.
 *
 * <p>All non-positional arguments of a {@link Command} need a
 * corresponding receiver to accept it through either
 * {@link #testOption(String)} or {@link #testParameter(String)}.
 * If any receiver rejects a non-positional argument, the CLI will
 * exit with an error.
 */
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
     * @return Returns a consumer if accepted or null if rejected.
     */
    default Consumer<String> testParameter(String name) {
        return null;
    }

    /**
     * Registers help information to the given {@link HelpPrinter}.
     *
     * @param printer Printer to modify.
     */
    default void registerHelp(HelpPrinter printer) {}
}
