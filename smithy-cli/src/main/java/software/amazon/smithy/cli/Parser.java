/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Defines the CLI argument parser of a {@link Command}.
 */
@SmithyUnstableApi
public final class Parser {
    private String positionalName;
    private String positionalHelp;
    private List<Argument> argumentList;
    private Map<String, Argument> argumentMap = new HashMap<>();

    private Parser(Builder builder) {
        this.argumentList = builder.arguments;
        this.positionalName = builder.positionalName;
        this.positionalHelp = builder.positionalHelp;

        for (Parser.Argument argument : argumentList) {
            argument.getShortName().ifPresent(name -> argumentMap.put(name, argument));
            argument.getLongName().ifPresent(name -> argumentMap.put(name, argument));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getPositionalName() {
        return Optional.ofNullable(positionalName);
    }

    public Optional<String> getPositionalHelp() {
        return Optional.ofNullable(positionalHelp);
    }

    public List<Argument> getArgs() {
        return argumentList;
    }

    /**
     * Parses command line arguments in an {@link Arguments} object.
     *
     * <p>All arguments after the first argument that does not start with
     * "-" or after "--" are treated as positional options. Arguments that
     * come before this must be present in the argument spec returned by the
     * command. Any encountered unknown argument will throw an exception.
     * Arguments that expect a value and do not find one will throw an
     * exception.
     *
     * @param args Arguments to parse.
     * @return Returns the parsed arguments.
     * @throws CliError if the arguments are invalid.
     */
    public Arguments parse(String[] args) {
        return parse(args, 0);
    }

    /**
     * Parses command line arguments in an {@link Arguments} object.
     *
     * @param args Arguments to parse.
     * @param offset Number of arguments to skip before parsing.
     * @return Returns the parsed arguments.
     * @throws CliError if the arguments are invalid.
     * @see #parse(String[])
     */
    public Arguments parse(String[] args, int offset) {
        Objects.requireNonNull(args);
        String currentArg = null;
        boolean consumeRemaining = false;
        Map<String, List<String>> parsedArgs = new HashMap<>();
        List<String> parsedOpts = new ArrayList<>();

        for (int i = offset; i < args.length; i++) {
            String arg = args[i];
            if (consumeRemaining) {
                parsedOpts.add(arg);
                continue;
            } else if (currentArg != null) {
                // Expecting to shift a value for an argument.
                parsedArgs.get(currentArg).add(arg);
                currentArg = null;
                continue;
            } else if (arg.equals("--") || !arg.startsWith("-")) {
                // Start consuming options when "--" is encountered, or
                // treat the first arg that does not start with "-" as the
                // start of the options list.
                if (!arg.equals("--")) {
                    parsedOpts.add(arg);
                }
                consumeRemaining = true;
                currentArg = null;
                continue;
            } else if (!argumentMap.containsKey(arg)) {
                // Arguments that start with "-" must exist in the spec.
                throw new CliError(String.format(
                        "Invalid argument %s. Expected one of: %s",
                        arg, argumentMap.keySet().stream().sorted().collect(Collectors.toList())));
            }

            // Canonicalize and parse the argument.
            currentArg = argumentMap.get(arg).getCanonicalName();
            if (!parsedArgs.containsKey(currentArg)) {
                parsedArgs.put(currentArg, new ArrayList<>());
                if (argumentMap.get(currentArg).arity == Arity.NONE) {
                    // If the spec for this arg takes no value, skip and
                    // start parsing the next arg.
                    currentArg = null;
                }
            } else if (argumentMap.get(currentArg).arity != Arity.MANY) {
                throw new CliError("Conflicting arguments provided for " + currentArg);
            }
        }

        if (currentArg != null) {
            throw new CliError("Missing argument value for " + currentArg);
        }

        if (!parsedOpts.isEmpty() && positionalName == null) {
            throw new CliError("Unexpected options provided: [" + parsedOpts + "]");
        }

        return new Arguments(parsedArgs, parsedOpts);
    }

    /**
     * Defines the arity of an argument.
     */
    public enum Arity {
        /** The argument accepts no value. */
        NONE,

        /** The argument accepts a single value. */
        ONE,

        /** The argument accepts one or more values. */
        MANY
    }

    /**
     * A command line argument.
     */
    public static final class Argument {
        private String shortName;
        private String longName;
        private Arity arity;
        private String help;

        /**
         * @param longName Long name of the argument (e.g., --help).
         * @param shortName Short name of the argument (e.g., -h).
         * @param arity Arity / number of times a value can be provided.
         * @param help Help text of the argument.
         */
        public Argument(String longName, String shortName, Arity arity, String help) {
            if (shortName == null && longName == null) {
                throw new IllegalArgumentException("A CLI argument must have a long name, short name, or both");
            } else if (longName != null && !longName.startsWith("-")) {
                throw new IllegalArgumentException("longName must start with '-': " + longName);
            } else if (shortName != null && !shortName.startsWith("-")) {
                throw new IllegalArgumentException("shortName must start with '-': " + shortName);
            }

            this.longName = longName;
            this.shortName = shortName;
            this.arity = Objects.requireNonNull(arity);
            this.help = Objects.requireNonNull(help);
        }

        /**
         * Gets the short name of the argument (e.g., -h).
         *
         * @return Returns the optionally present short name.
         */
        public Optional<String> getShortName() {
            return Optional.ofNullable(shortName);
        }

        /**
         * Gets the long name of the argument (e.g., --help).
         *
         * @return Returns the optionally present long name.
         */
        public Optional<String> getLongName() {
            return Optional.ofNullable(longName);
        }

        /**
         * Gets the canonical name, which is the longName if present or the shortName.
         *
         * @return Returns the canonicalized argument name.
         */
        public String getCanonicalName() {
            return longName != null ? longName : shortName;
        }

        /**
         * Gets the arity (number of times an argument value can be provided).
         *
         * @return Returns the argument arity.
         */
        public Arity getArity() {
            return arity;
        }

        /**
         * Gets the argument help text.
         *
         * @return Returns the help text.
         */
        public String getHelp() {
            return help;
        }
    }

    /**
     * Builds an {@link Parser}.
     */
    public static final class Builder implements SmithyBuilder<Parser> {
        private String positionalName;
        private String positionalHelp;
        private List<Argument> arguments = new ArrayList<>();

        @Override
        public Parser build() {
            // Always include --help, --debug, --stacktrace, and --no-color options; and --logging X.
            // This is done during build to move them to the end. Note that this could duplicate
            // arguments if a builder is reused, but that seems highly unlikely.
            option(Cli.HELP, "-h", "Print this help");
            option(Cli.DEBUG, "Display debug information");
            option(Cli.STACKTRACE, "Display a stacktrace on error");
            option(Cli.NO_COLOR, "Explicitly disable ANSI colors");
            option(Cli.FORCE_COLOR, "Explicitly enables ANSI colors");
            parameter(Cli.LOGGING, "Sets the log level to one of OFF, SEVERE, WARNING, INFO, FINE, ALL");
            return new Parser(this);
        }

        /**
         * Configures the name of positional arguments that come after options
         * and parameters.
         *
         * @param name Name of the positional argument.
         * @param help Positional argument help text.
         * @return Returns the builder.
         */
        public Builder positional(String name, String help) {
            positionalName = name;
            positionalHelp = help;
            return this;
        }

        /**
         * Adds an argument. Arguments appear before positional arguments.
         *
         * @param argument Argument to add.
         * @return Returns the builder.
         */
        public Builder argument(Argument argument) {
            arguments.add(Objects.requireNonNull(argument));
            return this;
        }

        /**
         * Adds an option argument that accepts no value.
         *
         * @param shortName Short name (e.g., -o).
         * @param longName Long name (e.g., --output).
         * @param help Help text for the option.
         * @return Returns the builder.
         */
        public Builder option(String shortName, String longName, String help) {
            return argument(new Argument(shortName, longName, Arity.NONE, help));
        }

        /**
         * Adds an option argument that accepts no value.
         *
         * @param longName Long name (e.g., --output).
         * @param help Help text for the option.
         * @return Returns the builder.
         */
        public Builder option(String longName, String help) {
            return argument(new Argument(longName, null, Arity.NONE, help));
        }

        /**
         * Adds an argument that accepts a single value.
         *
         * @param shortName Short nam (e.g., -o).
         * @param longName Long name (e.g., --output).
         * @param help Help text for the parameter.
         * @return Returns the builder.
         */
        public Builder parameter(String shortName, String longName, String help) {
            return argument(new Argument(shortName, longName, Arity.ONE, help));
        }

        /**
         * Adds an argument that accepts a single value.
         *
         * @param longName Long name (e.g., --output).
         * @param help Help text for the parameter.
         * @return Returns the builder.
         */
        public Builder parameter(String longName, String help) {
            return argument(new Argument(null, longName, Arity.ONE, help));
        }

        /**
         * Adds an argument that accepts a value that can be repeated.
         *
         * @param shortName Short nam (e.g., -o).
         * @param longName Long name (e.g., --output).
         * @param help Help text for the parameter.
         * @return Returns the builder.
         */
        public Builder repeatedParameter(String shortName, String longName, String help) {
            return argument(new Argument(shortName, longName, Arity.MANY, help));
        }

        /**
         * Adds an argument that accepts a value that can be repeated.
         *
         * @param longName Long name (e.g., --output).
         * @param help Help text for the parameter.
         * @return Returns the builder.
         */
        public Builder repeatedParameter(String longName, String help) {
            return argument(new Argument(longName, null, Arity.MANY, help));
        }
    }
}
