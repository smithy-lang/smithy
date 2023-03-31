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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Command line arguments list to evaluate.
 *
 * <p>Arguments are parsed on demand. To finalize parsing arguments and
 * force validation of remaining arguments, call {@link #getPositional()}.
 * Note that because arguments are parsed on demand and whole set of
 * arguments isn't known before {@link #getPositional()} is called, you
 * may need to delay configuring parts of the CLI or commands by adding
 * subscribers via {@link #onComplete(BiConsumer)}. These subscribers are
 * invoked when all arguments have been parsed.
 */
public final class Arguments {

    private final String[] args;
    private final Map<Class<? extends ArgumentReceiver>, ArgumentReceiver> receivers = new LinkedHashMap<>();
    private final List<BiConsumer<Arguments, List<String>>> subscribers = new ArrayList<>();
    private List<String> positional;
    private boolean inPositional = false;
    private int position = 0;

    public Arguments(String[] args) {
        this.args = args;
    }

    /**
     * Adds an argument receiver to the argument list.
     *
     * @param receiver Receiver to add.
     */
    public <T extends ArgumentReceiver> void addReceiver(T receiver) {
        receivers.put(receiver.getClass(), receiver);
    }

    /**
     * Removes an argument receiver by class.
     *
     * @param receiverClass Class of receiver to remove.
     * @return Returns the removed receiver if found, or null.
     * @param <T> Kind of receiver to remove.
     */
    @SuppressWarnings("unchecked")
    public <T extends ArgumentReceiver> T removeReceiver(Class<T> receiverClass) {
        return (T) receivers.remove(receiverClass);
    }

    /**
     * Check if this class contains a receiver.
     *
     * @param receiverClass Class of receiver to detect.
     * @return Returns true if found.
     */
    public boolean hasReceiver(Class<? extends ArgumentReceiver> receiverClass) {
        return receivers.containsKey(receiverClass);
    }

    /**
     * Get a receiver by class.
     *
     * @param type Type of receiver to get.
     * @param <T> Type of receiver to get.
     * @return Returns the found receiver.
     * @throws NullPointerException if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ArgumentReceiver> T getReceiver(Class<T> type) {
        return (T) Objects.requireNonNull(receivers.get(type));
    }

    /**
     * Get the argument receivers registered with the Arguments list.
     *
     * @return Returns the receivers.
     */
    public Iterable<ArgumentReceiver> getReceivers() {
        return receivers.values();
    }

    /**
     * Adds a subscriber to the arguments that is invoked when arguments
     * have finished parsing.
     *
     * @param consumer Subscriber consumer to add.
     */
    public void onComplete(BiConsumer<Arguments, List<String>> consumer)  {
        subscribers.add(consumer);
    }

    /**
     * Checks if any arguments are remaining.
     *
     * @return Returns true if there are more arguments.
     */
    public boolean hasNext() {
        return position < args.length;
    }

    /**
     * Peeks at the next value in the argument list without shifting it.
     *
     * <p>Note that arguments consumed by a {@link ArgumentReceiver} are never
     * peeked.
     *
     * @return Returns the next argument or null if no further arguments are present.
     */
    public String peek() {
        String peek = hasNext() ? args[position] : null;

        if (peek != null && !inPositional) {
            // Automatically skip "--" and consider the remaining arguments positional.
            if (peek.equals("--")) {
                inPositional = true;
                position++;
                return peek();
            }

            for (ArgumentReceiver interceptor : receivers.values()) {
                if (interceptor.testOption(peek)) {
                    position++;
                    return peek();
                }

                Consumer<String> optionConsumer = interceptor.testParameter(peek);
                if (optionConsumer != null) {
                    position++;
                    optionConsumer.accept(shiftFor(peek));
                    return peek();
                }
            }
        }

        return peek;
    }

    /**
     * Shifts off the next value in the argument list or returns null.
     *
     * @return Returns the next value or null.
     */
    public String shift() {
        String peek = peek();

        if (peek != null) {
            position++;
        }

        return peek;
    }

    /**
     * Expects an argument value for a parameter by name.
     *
     * @param parameter Name of the parameter to get the value of.
     * @return Returns the value of the parameter.
     * @throws CliError if the parameter is not present.
     */
    public String shiftFor(String parameter) {
        if (!hasNext()) {
            throw new CliError("Expected argument for '" + parameter + "'");
        }

        String next = args[position];
        position++;
        return next;
    }

    /**
     * Gets the positional arguments.
     *
     * <p>Expects that all remaining arguments are positional, and returns them.
     *
     * <p>If an argument is "--", then it's skipped and remaining arguments are
     * considered positional. If any argument is encountered that isn't valid
     * for a registered Receiver, then an error is raised. Otherwise, all remaining
     * arguments are returned in a list.
     *
     * <p>Subscribers for different receivers are called when this method is first called.
     *
     * @return Returns remaining arguments.
     * @throws CliError if the next argument starts with "--" but isn't "--".
     */
    public List<String> getPositional() {
        if (positional == null) {
            positional = new ArrayList<>();

            while (hasNext()) {
                String next = shift();
                if (next != null) {
                    if (!inPositional && next.startsWith("-")) {
                        throw new CliError("Unexpected CLI argument: " + next);
                    } else {
                        inPositional = true;
                        positional.add(next);
                    }
                }
            }

            for (BiConsumer<Arguments, List<String>> subscriber : subscribers) {
                subscriber.accept(this, positional);
            }
        }

        return positional;
    }
}
