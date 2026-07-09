/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;

/**
 * Lists the Smithy skills bundled with the CLI and the AI agent harnesses install supports,
 * marking which harnesses already have the skill installed under the checked root.
 *
 * <p>Discovery is a sibling verb of {@code install}, not a mode of it: overloading a
 * {@code --list} flag onto install would perform a read action under a command whose name
 * promises a write, which is a well-known CLI anti-pattern.
 */
final class AiListCommand implements Command {

    private final String parentCommandName;

    AiListCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getSummary() {
        return "List bundled Smithy skills and supported AI agent harnesses.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new Options());
        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, c -> {
            ColorBuffer buffer = ColorBuffer.of(c, new StringBuilder());
            buffer.println("Examples:");
            buffer.println("   smithy ai list", ColorTheme.LITERAL);
            buffer.println("   smithy ai list --dir .", ColorTheme.LITERAL);
            return buffer.toString();
        }, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Command.Env env) {
        Options options = arguments.getReceiver(Options.class);

        // Installed state is checked against the same root install writes to: home by default,
        // or --dir for a project-local view.
        Path root = AiHarness.resolveRoot(options.dir);

        env.stdout().println("Bundled skills:");
        for (AiSkill skill : AiSkill.bundled(env.classLoader())) {
            env.stdout().println("    " + skill.getName());
        }
        env.stdout().println("");
        env.stdout().println("Supported harnesses (installed state under " + root + "):");
        for (AiHarness h : AiHarness.SUPPORTED.values()) {
            env.stdout().println("    " + h.getName() + "  ->  " + h.displayPath());
            for (AiSkill skill : AiSkill.bundled(env.classLoader())) {
                String state = Files.exists(h.skillPath(root, skill.getName())) ? "installed" : "not installed";
                env.stdout().println("        " + skill.getName() + "  [" + state + "]");
            }
        }
        if (!AiHarness.UNSUPPORTED_REASONS.isEmpty()) {
            env.stdout().println("");
            env.stdout()
                    .println("Not yet supported (these read a single instructions file, not a SKILL.md "
                            + "skills directory; an adapter is planned):");
            for (Map.Entry<String, String> e : AiHarness.UNSUPPORTED_REASONS.entrySet()) {
                env.stdout().println("    " + e.getKey() + " - " + e.getValue());
            }
        }
        return 0;
    }

    private static final class Options implements ArgumentReceiver {
        private String dir;

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--dir":
                case "-C":
                    return value -> dir = value;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--dir",
                    "-C",
                    "~",
                    "Check installed state under this project root instead of the user home directory.");
        }
    }
}
