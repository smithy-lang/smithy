/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;

/**
 * Installs Smithy skills into an AI agent harness.
 *
 * <p>Each skill is a {@code SKILL.md} directory: the bundled {@code smithy-docs-navigator} skill
 * ships as a classpath resource, and {@code --skill <path>} installs a user-authored one from
 * disk. The bundled skill is a thin pointer that teaches an agent to read the published
 * {@code llms.txt} index and fetch each page's {@code .rst} source with a non-summarizing tool.
 *
 * <p>By default skills install under the user's home directory (for example
 * {@code ~/.claude/skills}) so they apply to every project; {@code --dir} opts into a
 * project-local install instead. The target harness is either passed explicitly with
 * {@code --harness} or detected from marker directories in the install root (for example
 * {@code .claude/} for Claude Code); with no {@code --harness}, install writes to every detected
 * harness. With no {@code --skill}, every bundled skill is installed.
 */
final class AiInstallCommand implements Command {

    private final String parentCommandName;

    AiInstallCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "install";
    }

    @Override
    public String getSummary() {
        return "Install a Smithy skill into an AI agent harness.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new Options());
        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, c -> {
            ColorBuffer buffer = ColorBuffer.of(c, new StringBuilder());
            buffer.println("Examples:");
            buffer.println("   smithy ai install                                  # into ~ (all projects)",
                    ColorTheme.LITERAL);
            buffer.println("   smithy ai install --harness claude", ColorTheme.LITERAL);
            buffer.println("   smithy ai install --harness claude -C .            # this project only",
                    ColorTheme.LITERAL);
            buffer.println("   smithy ai install --dry-run", ColorTheme.LITERAL);
            return buffer.toString();
        }, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);

        // Home by default (the skill applies to every project); --dir opts into project-local.
        Path root = AiHarness.resolveRoot(options.dir);

        // An explicit --harness targets exactly that harness; otherwise install into every
        // harness detected under the root, so a user with both Claude Code and Kiro gets both.
        List<AiHarness> targets;
        if (options.harness != null) {
            targets = Collections.singletonList(AiHarness.require(options.harness));
        } else {
            targets = AiHarness.detect(root);
            if (targets.isEmpty()) {
                throw new CliError("Could not detect an AI agent harness in " + root.toAbsolutePath()
                        + "; pass --harness (supported: " + AiHarness.supportedNames() + ")");
            }
            if (!standardOptions.quiet()) {
                List<String> names = new ArrayList<>();
                for (AiHarness h : targets) {
                    names.add(h.getName());
                }
                env.stdout()
                        .println("Detected harness(es): " + String.join(", ", names)
                                + " in " + root.toAbsolutePath());
            }
        }

        // --skill selects specific skills by bundled name or filesystem path; omitted installs
        // every bundled skill. Resolve each value up front so a bad name/path fails before writing.
        List<AiSkill> skills = new ArrayList<>();
        if (options.skills.isEmpty()) {
            skills.addAll(AiSkill.bundled(env.classLoader()));
        } else {
            for (String value : options.skills) {
                skills.add(AiSkill.resolve(value, env.classLoader()));
            }
        }

        for (AiSkill skill : skills) {
            for (AiHarness harness : targets) {
                installOne(skill, harness, root, options, standardOptions, env);
            }
        }
        return 0;
    }

    /**
     * Installs one skill's whole tree (SKILL.md + any supporting files) under a harness.
     * "Already installed" is decided by the presence of the tree's SKILL.md; {@code --force}
     * overwrites every file. Content is read per file only when a write is going to happen.
     */
    private void installOne(
            AiSkill skill,
            AiHarness harness,
            Path root,
            Options options,
            StandardOptions standardOptions,
            Env env
    ) {
        Path treeDir = harness.skillTreeDir(root, skill.getName());
        Path skillMd = harness.skillPath(root, skill.getName());
        boolean exists = Files.exists(skillMd);
        List<String> files = skill.getFiles();

        if (options.dryRun) {
            if (!standardOptions.quiet()) {
                if (exists && !options.force) {
                    env.stdout()
                            .println(skill.getName() + " already installed at " + skillMd
                                    + "; would skip (use --force to overwrite)");
                } else {
                    env.stdout()
                            .println("Would " + (exists ? "overwrite " : "install ") + skill.getName()
                                    + " (" + files.size() + " file" + (files.size() == 1 ? "" : "s")
                                    + ") at " + skillMd);
                }
            }
            return;
        }

        if (exists && !options.force) {
            if (!standardOptions.quiet()) {
                env.stdout()
                        .println(skill.getName() + " already installed at " + skillMd
                                + " (use --force to overwrite)");
            }
            return;
        }

        for (String rel : files) {
            Path dest = treeDir.resolve(rel);
            Path parent = dest.getParent();
            try {
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(dest, skill.readFile(rel).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to write skill file: " + dest, e);
            }
        }

        if (!standardOptions.quiet()) {
            env.stdout().println("Installed " + skill.getName() + " to " + skillMd);
        }
    }

    private static final class Options implements ArgumentReceiver {
        private String harness;
        private String dir;
        private final List<String> skills = new ArrayList<>();
        private boolean force;
        private boolean dryRun;

        @Override
        public boolean testOption(String name) {
            switch (name) {
                case "--force":
                case "-f":
                    force = true;
                    return true;
                case "--dry-run":
                    dryRun = true;
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--harness":
                case "-H":
                    return value -> harness = value;
                case "--dir":
                case "-C":
                    return value -> dir = value;
                case "--skill":
                    return skills::add;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--harness",
                    "-H",
                    "<harness>",
                    "Target AI agent harness. Supported: " + AiHarness.supportedNames()
                            + ". If omitted, install detects a harness from marker directories in --dir "
                            + "(for example .claude/ for Claude Code) and refuses if it cannot decide.");
            printer.param("--skill",
                    null,
                    "<name|path>",
                    "Skill to install: a bundled name (" + AiSkill.bundledNames(
                            Thread.currentThread().getContextClassLoader()) + ") or a path to a "
                            + "SKILL.md (or a directory containing one). Repeatable. If omitted, all "
                            + "bundled skills are installed.");
            printer.param("--dir",
                    "-C",
                    "~",
                    "Install into this project root instead of the user home directory. "
                            + "By default the skill installs under your home (e.g. ~/.claude/skills) "
                            + "so it applies to every project.");
            printer.option("--force", "-f", "Overwrite the skill file if it already exists");
            printer.option("--dry-run", null, "Print the target path without writing any files");
        }
    }
}
