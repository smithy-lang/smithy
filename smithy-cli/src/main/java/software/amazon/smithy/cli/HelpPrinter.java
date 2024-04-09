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
import java.util.List;
import java.util.StringTokenizer;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates and prints structured help output.
 */
public final class HelpPrinter {
    private final String name;
    private int maxWidth = 80;
    private String summary;
    private String documentation;
    private final List<Arg> args = new ArrayList<>();
    private Arg positional;

    public HelpPrinter(String name) {
        this.name = name;
    }

    public HelpPrinter maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /**
     * Create a HelpPrinter that registers help options and parameters defined in the
     * given {@code arguments}.
     *
     * @param name Name of the command.
     * @param arguments Arguments to extract params from.
     * @return Returns the created HelpPrinter.
     */
    public static HelpPrinter fromArguments(String name, Arguments arguments) {
        HelpPrinter printer = new HelpPrinter(name);
        for (ArgumentReceiver receiver : arguments.getReceivers()) {
            receiver.registerHelp(printer);
        }
        return printer;
    }

    /**
     * Defines the summary text of the command that comes after the short argument
     * description and before the long argument descriptions.
     *
     * @param summary Summary text to display.
     * @return Returns the printer.
     */
    public HelpPrinter summary(String summary) {
        this.summary = summary;
        return this;
    }

    /**
     * Defines the optional long-form documentation that comes after long argument
     * descriptions.
     *
     * @param documentation Documentation to display.
     * @return Returns the printer.
     */
    public HelpPrinter documentation(String documentation) {
        this.documentation = documentation;
        return this;
    }

    /**
     * Adds an option that takes no argument value.
     *
     * @param longName Nullable argument long form (e.g., "--foo").
     * @param shortName Nullable argument short form (e.g., null).
     * @param description Description description of the argument.
     * @return Returns the printer.
     */
    public HelpPrinter option(String longName, String shortName, String description) {
        args.add(Arg.option(longName, shortName, description));
        return this;
    }

    /**
     * Adds a parameter that requires an argument value.
     *
     * @param longName Nullable argument long form (e.g., "--foo").
     * @param shortName Nullable argument short form (e.g., null).
     * @param exampleValue Example value of the parameter (e.g., "FOO").
     * @param description Description description of the argument.
     * @return Returns the printer.
     */
    public HelpPrinter param(String longName, String shortName, String exampleValue, String description) {
        args.add(Arg.parameter(longName, shortName, exampleValue, description));
        return this;
    }

    /**
     * Defines a positional argument.
     *
     * @param name Name of the positional argument (e.g., {@code <MODEL...>})
     * @param description Description of the argument.
     * @return Returns the printer.
     */
    public HelpPrinter positional(String name, String description) {
        positional = Arg.positional(name, description);
        return this;
    }

    /**
     * Prints the generated help to the given printer.
     *
     * @param colors Color formatter.
     * @param printer CliPrinter to write to.
     */
    public void print(ColorFormatter colors, CliPrinter printer) {
        LineWrapper builder = new LineWrapper(maxWidth);

        builder.appendWithinLine("Usage: ")
                .appendWithinLine(colors.style(name, ColorTheme.EM_UNDERLINE))
                .space();

        // Calculate the column manually to account for possible styles interfering with the current column number.
        builder.indent("Usage: ".length() + name.length() + 1);

        for (Arg arg : args) {
            builder.appendWithinLine(arg.toShortArgs(colors)).space();
        }

        if (positional != null) {
            builder.appendWithinLine(positional.toShortArgs(colors));
        }

        builder.indent(0).newLine();
        if (summary != null) {
            builder.newLine().appendText(summary, 0);
        }

        builder.indent(4);
        builder.newLine();

        for (Arg arg : args) {
            writeArgHelp(colors, builder, arg);
        }

        if (positional != null) {
            writeArgHelp(colors, builder, positional);
        }

        if (!StringUtils.isEmpty(documentation)) {
            builder.indent(0).newLine().appendText(documentation, 0);
        }

        printer.println(builder.toString());
    }

    private void writeArgHelp(ColorFormatter colors, LineWrapper builder, Arg arg) {
        if (arg.longName != null) {
            builder.appendWithinLine(colors.style(arg.longName, ColorTheme.LITERAL));
            if (arg.shortName != null) {
                builder.appendWithinLine(", ");
            }
        }
        if (arg.shortName != null) {
            builder.appendWithinLine(colors.style(arg.shortName, ColorTheme.LITERAL));
        }
        if (arg.exampleValue != null) {
            builder.space().appendWithinLine(arg.exampleValue);
        }
        builder.indent(8);
        builder.newLine();
        builder.appendText(arg.description, 4);
    }

    private static final class Arg {
        final String longName;
        final String shortName;
        final String exampleValue;
        final String description;

        private Arg(String longName, String shortName, String exampleValue, String description) {
            this.longName = longName;
            this.shortName = shortName;
            this.exampleValue = exampleValue;
            this.description = description;
        }

        static Arg option(String longName, String shortName, String description) {
            return new Arg(longName, shortName, null, description);
        }

        static Arg parameter(String longName, String shortName, String exampleValue, String description) {
            return new Arg(longName, shortName, exampleValue, description);
        }

        static Arg positional(String name, String description) {
            return new Arg(name, null, null, description);
        }

        String toShortArgs(ColorFormatter colors) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            if (longName != null) {
                builder.append(colors.style(longName, ColorTheme.LITERAL));
                if (shortName != null) {
                    builder.append(" | ");
                }
            }
            if (shortName != null) {
                builder.append(colors.style(shortName, ColorTheme.LITERAL));
            }
            if (exampleValue != null) {
                builder.append(' ').append(exampleValue);
            }
            builder.append(']');
            return builder.toString();
        }
    }

    private static final class LineWrapper {
        private final StringBuilder builder = new StringBuilder();
        private final int maxLength;
        private int column = 0;
        private int indent = 0;

        private LineWrapper(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        LineWrapper indent(int indent) {
            this.indent = indent;
            return this;
        }

        LineWrapper space() {
            if (column >= 0) {
                builder.append(' ');
            }
            column++;
            return this;
        }

        LineWrapper appendWithinLine(String text) {
            if (column + text.length() > maxLength) {
                newLine();
                // If the text starts with a space, then eat the space since it isn't needed to separate words now.
                if (text.startsWith(" ")) {
                    builder.append(text, 1, text.length());
                    column += text.length() - 1;
                    return this;
                }
            }

            builder.append(text);
            column += text.length();
            return this;
        }

        LineWrapper appendText(String text, int indentAfter) {
            StringTokenizer tokenizer = new StringTokenizer(text, " \t\n\r\f", true);
            boolean pendingCarriage = false;
            while (tokenizer.hasMoreElements()) {
                String next = tokenizer.nextToken();
                if (next.equals("\n")) {
                    pendingCarriage = false;
                    newLine();
                } else {
                    if (pendingCarriage) {
                        newLine();
                        pendingCarriage = false;
                    }
                    if (next.equals("\r")) {
                        pendingCarriage = true;
                    } else {
                        appendWithinLine(next);
                    }
                }
            }
            if (pendingCarriage) {
                newLine();
            }
            indent(indentAfter);
            newLine();
            return this;
        }

        LineWrapper newLine() {
            builder.append(System.lineSeparator());
            for (int i = 0; i < indent; i++) {
                builder.append(' ');
            }
            column = indent;
            return this;
        }
    }
}
