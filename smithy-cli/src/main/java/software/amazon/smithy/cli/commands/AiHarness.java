/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.cli.CliError;

/**
 * A target agent harness for {@code smithy ai install}, modeled as a format-aware profile.
 *
 * <p>Each profile knows the on-disk directory the harness reads skills from (relative to the
 * project root) and how a skill file is named inside it. This shape composes for future
 * harnesses whose native format is not Anthropic-style {@code SKILL.md}: adding a harness
 * that reads {@code AGENTS.md} or {@code GEMINI.md} means implementing a new profile, not
 * bolting a special case onto a directory-only map.
 *
 * <p>Only harnesses with a verified native project-local skills concept are exposed to
 * {@code --harness}. Harnesses that are documented but use a different mechanism
 * ({@code AGENTS.md}, {@code GEMINI.md}, {@code .github/instructions/}) are named in
 * {@link #UNSUPPORTED_REASONS} so the CLI can reject them with an honest message rather
 * than silently writing a file the harness would never read.
 */
final class AiHarness {

    /** Verified supported harnesses. Add only when the mapping is confirmed against the harness's docs. */
    static final Map<String, AiHarness> SUPPORTED = new LinkedHashMap<>();

    /** Named-but-unsupported harnesses, mapped to the reason we refuse rather than writing a no-op. */
    static final Map<String, String> UNSUPPORTED_REASONS = new LinkedHashMap<>();

    static {
        // Both harnesses auto-discover skills from their <root>/skills/<name>/SKILL.md directory.
        SUPPORTED.put("claude", new AiHarness("claude", ".claude/skills", "SKILL.md"));
        SUPPORTED.put("kiro", new AiHarness("kiro", ".kiro/skills", "SKILL.md"));

        // These harnesses have no native SKILL.md skills directory. They read a single always-loaded
        // instructions file instead; delivering to them needs an AGENTS.md-style adapter (future work),
        // not a skills-dir write, so reject rather than produce a silent no-op.
        UNSUPPORTED_REASONS.put(
                "codex",
                "codex CLI has no SKILL.md skills directory; it reads AGENTS.md project instructions");
        UNSUPPORTED_REASONS.put(
                "copilot",
                "GitHub Copilot has no SKILL.md skills directory; it reads "
                        + ".github/copilot-instructions.md, .github/instructions/*.instructions.md, or AGENTS.md");
        UNSUPPORTED_REASONS.put(
                "gemini",
                "Gemini CLI has no SKILL.md skills directory; it reads a GEMINI.md context file");
    }

    private final String name;
    private final String skillDir;
    private final String skillFileName;

    AiHarness(String name, String skillDir, String skillFileName) {
        this.name = name;
        this.skillDir = skillDir;
        this.skillFileName = skillFileName;
    }

    String getName() {
        return name;
    }

    /** Marker directory whose presence indicates a project uses this harness (e.g. {@code .claude}). */
    String getMarkerDir() {
        return skillDir.split("/", 2)[0];
    }

    /** Absolute path this harness reads a given skill's SKILL.md from, under {@code root}. */
    Path skillPath(Path root, String skillName) {
        return root.resolve(skillDir).resolve(skillName).resolve(skillFileName);
    }

    /**
     * Absolute directory a skill's whole tree (SKILL.md + any supporting files) is installed into.
     * The SKILL.md path used for existence/listing is {@link #skillPath}; the tree root is here.
     */
    Path skillTreeDir(Path root, String skillName) {
        return root.resolve(skillDir).resolve(skillName);
    }

    /** Human-readable relative layout for help/list output (e.g. {@code .claude/skills/<skill>/SKILL.md}). */
    String displayPath() {
        return skillDir + "/<skill>/" + skillFileName;
    }

    /**
     * Resolves the install root: the given directory, or the user home when {@code dir} is null.
     * The docs-navigator skill applies to every project, so the default is home.
     */
    static Path resolveRoot(String dir) {
        return dir == null ? Paths.get(System.getProperty("user.home")) : Paths.get(dir);
    }

    /** Resolves a supported harness by name, throwing an honest error otherwise. */
    static AiHarness require(String requested) {
        String key = requested.toLowerCase(Locale.ENGLISH);
        AiHarness harness = SUPPORTED.get(key);
        if (harness != null) {
            return harness;
        }
        String reason = UNSUPPORTED_REASONS.get(key);
        if (reason != null) {
            throw new CliError("Harness '" + requested + "' is not supported: " + reason
                    + ". Supported: " + supportedNames());
        }
        throw new CliError("Unknown harness '" + requested + "'. Supported: " + supportedNames());
    }

    /**
     * Detects which supported harnesses a project root already uses, based on the presence of
     * each harness's marker directory (parent of its skills directory).
     *
     * @return every matching harness (empty if none, more than one if ambiguous).
     */
    static List<AiHarness> detect(Path projectRoot) {
        List<AiHarness> hits = new ArrayList<>();
        for (AiHarness h : SUPPORTED.values()) {
            if (Files.isDirectory(projectRoot.resolve(h.getMarkerDir()))) {
                hits.add(h);
            }
        }
        return hits;
    }

    static String supportedNames() {
        return String.join(", ", SUPPORTED.keySet());
    }
}
