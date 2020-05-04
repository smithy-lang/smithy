/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Container for parsed command line arguments meant to be queried by Commands.
 *
 * <p>Values parsed for command line arguments are canonicalized to the
 * long-form of an argument if available. This means that an argument
 * with a short name of "-h" and a long name of "--help" would be made
 * available in an {@code Arguments} instance as "--help" and not "-h".
 */
@SmithyUnstableApi
public final class Arguments {
    private final Map<String, List<String>> arguments;
    private final List<String> positionalArguments;

    public Arguments(Map<String, List<String>> arguments, List<String> positionalArguments) {
        this.arguments = Collections.unmodifiableMap(arguments);
        this.positionalArguments = Collections.unmodifiableList(positionalArguments);
    }

    /**
     * Checks if a canonicalized argument name was provided.
     *
     * <p>This method should be used for checking if a option is set rather
     * than calling {@link #parameter} since that method throws when an
     * argument cannot be found.
     *
     * @param arg Argument to check for (e.g., "--help").
     * @return Returns true if the argument was set.
     */
    public boolean has(String arg) {
        return arguments.containsKey(arg);
    }

    /**
     * Gets an argument by name or throws if not present.
     *
     * <p>Returns the first value if the argument is repeated.
     *
     * @param arg Argument to get (e.g., "-h", "--help").
     * @return Returns the value of the matching argument.
     * @throws CliError if the argument cannot be found or if the arg is a option.
     */
    public String parameter(String arg) {
        return repeatedParameter(arg).get(0);
    }

    /**
     * Gets an argument by name or return a default value if not found.
     *
     * @param arg Argument to get (e.g., "-h", "--help").
     * @param defaultValue Default value to return if not found.
     * @return Returns the value of the matching argument.
     */
    public String parameter(String arg, String defaultValue) {
        List<String> values = arguments.get(arg);
        return values == null || values.isEmpty() ? defaultValue : values.get(0);
    }

    /**
     * Gets a repeated argument by name or throws if not present.
     *
     * @param arg Argument to retrieve (e.g., "--help").
     * @return Returns a list of values for the argument.
     * @throws CliError if the argument cannot be found or if the arg is a option.
     */
    public List<String> repeatedParameter(String arg) {
        if (arguments.containsKey(arg)) {
            List<String> argVal = arguments.get(arg);
            if (argVal == null || argVal.isEmpty()) {
                throw new CliError("Argument " + arg + " was provided no value");
            }
            return argVal;
        }

        throw new CliError("Missing required argument: " + arg);
    }

    /**
     * Gets a repeated argument by name or returns a default list if not found.
     *
     * @param arg Argument to retrieve (e.g., "--help").
     * @param defaultValues Default list of values to return if not found.
     * @return Returns a list of values for the argument.
     */
    public List<String> repeatedParameter(String arg, List<String> defaultValues) {
        List<String> values = arguments.get(arg);
        return values == null || values.isEmpty() ? defaultValues : values;
    }

    /**
     * Gets the list of positional arguments that came after named arguments.
     *
     * @return Returns the trailing positional arguments.
     */
    public List<String> positionalArguments() {
        return positionalArguments;
    }

    @Override
    public String toString() {
        return "Arguments{" + "arguments=" + arguments + ", positionalArguments=" + positionalArguments + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Arguments)) {
            return false;
        }
        Arguments other = (Arguments) o;
        return arguments.equals(other.arguments) && positionalArguments.equals(other.positionalArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arguments, positionalArguments);
    }
}
