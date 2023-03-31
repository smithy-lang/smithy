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

package software.amazon.smithy.cli;

import java.util.List;

/**
 * Command line arguments list to evaluate.
 *
 * <p>Arguments are parsed on demand. To finalize parsing arguments and force validation of remaining arguments,
 * call {@link #getPositional()}. Note that because arguments are parsed on demand and whole set of arguments isn't
 * known before {@link #getPositional()} is called.
 */
public interface Arguments {

    static Arguments of(String[] args) {
        return new DefaultArguments(args);
    }

    /**
     * Adds an argument receiver to the argument list.
     *
     * @param receiver Receiver to add.
     */
    void addReceiver(ArgumentReceiver receiver);

    /**
     * Check if this class contains a receiver.
     *
     * @param receiverClass Class of receiver to detect.
     * @return Returns true if found.
     */
    boolean hasReceiver(Class<? extends ArgumentReceiver> receiverClass);

    /**
     * Get a receiver by class.
     *
     * @param type Type of receiver to get.
     * @param <T> Type of receiver to get.
     * @return Returns the found receiver.
     * @throws NullPointerException if not found.
     */
    <T extends ArgumentReceiver> T getReceiver(Class<T> type);

    /**
     * Get the argument receivers registered with the Arguments list.
     *
     * @return Returns the receivers.
     */
    Iterable<ArgumentReceiver> getReceivers();

    /**
     * Checks if any arguments are remaining.
     *
     * @return Returns true if there are more arguments.
     */
    boolean hasNext();

    /**
     * Peeks at the next value in the argument list without shifting it.
     *
     * <p>Note that arguments consumed by a {@link ArgumentReceiver} are never peeked.
     *
     * @return Returns the next argument or null if no further arguments are present.
     */
    String peek();

    /**
     * Shifts off the next value in the argument list or returns null.
     *
     * @return Returns the next value or null.
     */
    String shift();

    /**
     * Expects an argument value for a parameter by name.
     *
     * @param parameter Name of the parameter to get the value of.
     * @return Returns the value of the parameter.
     * @throws CliError if the parameter is not present.
     */
    String shiftFor(String parameter);

    /**
     * Gets the positional arguments.
     *
     * <p>Expects that all remaining arguments are positional, and returns them.
     *
     * <p>If an argument is "--", then it's skipped and remaining arguments are considered positional. If any
     * argument is encountered that isn't valid for a registered Receiver, then an error is raised. Otherwise, all
     * remaining arguments are returned in a list.
     *
     * <p>Subscribers for different receivers are called when this method is first called.
     *
     * @return Returns remaining arguments.
     * @throws CliError if the next argument starts with "--" but isn't "--".
     */
    List<String> getPositional();
}
