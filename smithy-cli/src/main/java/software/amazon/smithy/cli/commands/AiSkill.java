/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import software.amazon.smithy.ai.AiContent;
import software.amazon.smithy.cli.CliError;

/**
 * A skill that {@code smithy ai install} can write into a harness.
 *
 * <p>Two sources are supported: <em>bundled</em> skills discovered on the classpath through
 * {@link AiContent} (shipped by the {@code smithy-ai} module and any other JAR contributing
 * skills), and <em>external</em> skills the user points at with {@code --skill &lt;path&gt;} - either a
 * {@code SKILL.md} file or a directory containing one. Both resolve to a directory name and an
 * ordered list of relative file paths (SKILL.md plus any supporting files the skill bundles).
 */
final class AiSkill {

    private static final String SKILL_FILE = "SKILL.md";

    private final String name;
    private final List<String> files;
    private final software.amazon.smithy.ai.AiSkill bundled;
    private final Path externalRoot;

    private AiSkill(String name, List<String> files, software.amazon.smithy.ai.AiSkill bundled, Path externalRoot) {
        this.name = name;
        this.files = files;
        this.bundled = bundled;
        this.externalRoot = externalRoot;
    }

    String getName() {
        return name;
    }

    /** Relative paths of every file in this skill, in a stable order. */
    List<String> getFiles() {
        return files;
    }

    /** Reads the UTF-8 content of one relative file. */
    String readFile(String relativePath) {
        if (bundled != null) {
            return bundled.readFile(relativePath);
        }
        try {
            return new String(Files.readAllBytes(externalRoot.resolve(relativePath)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read skill file: " + externalRoot.resolve(relativePath), e);
        }
    }

    /** Every bundled skill on the given classloader. */
    static List<AiSkill> bundled(ClassLoader classLoader) {
        List<AiSkill> out = new ArrayList<>();
        for (software.amazon.smithy.ai.AiSkill s : AiContent.skills(classLoader)) {
            out.add(new AiSkill(s.getName(), s.getFiles(), s, null));
        }
        return out;
    }

    /** Comma-joined names of the bundled skills. */
    static String bundledNames(ClassLoader classLoader) {
        List<String> names = new ArrayList<>();
        for (software.amazon.smithy.ai.AiSkill s : AiContent.skills(classLoader)) {
            names.add(s.getName());
        }
        return String.join(", ", names);
    }

    /**
     * Resolves a {@code --skill} value: an existing filesystem path (a {@code SKILL.md} file or a
     * directory containing one) becomes an external skill; otherwise the value is looked up as a
     * bundled skill name on {@code classLoader}.
     */
    static AiSkill resolve(String value, ClassLoader classLoader) {
        Path path = Paths.get(value);
        if (Files.isRegularFile(path)) {
            return external(path);
        }
        if (Files.isDirectory(path)) {
            Path skillFile = path.resolve(SKILL_FILE);
            if (Files.isRegularFile(skillFile)) {
                return external(skillFile);
            }
            throw new CliError("No " + SKILL_FILE + " found in directory: " + path);
        }
        for (software.amazon.smithy.ai.AiSkill s : AiContent.skills(classLoader)) {
            if (s.getName().equals(value)) {
                return new AiSkill(s.getName(), s.getFiles(), s, null);
            }
        }
        throw new CliError("Unknown skill '" + value + "'. Pass a bundled skill name ("
                + bundledNames(classLoader) + ") or a path to a " + SKILL_FILE + ".");
    }

    /**
     * Builds an external skill from a {@code SKILL.md} on disk. The install directory takes its
     * name from the file's parent directory when it has one (e.g. {@code .../my-skill/SKILL.md ->
     * my-skill}), else from the file itself.
     *
     * <p>A skill is {@code SKILL.md} plus any supporting files a skill author bundles alongside it
     * (for example {@code reference.md}, {@code examples.md}, {@code scripts/}, {@code templates/}),
     * per the AIM skill convention. So the file list is the entire skill directory, not a fixed
     * {@code references/} subdirectory.
     */
    private static AiSkill external(Path skillFile) {
        Path parent = skillFile.toAbsolutePath().getParent();
        if (parent == null) {
            // A bare SKILL.md at the filesystem root: install just the file.
            return new AiSkill(SKILL_FILE,
                    Collections.singletonList(SKILL_FILE),
                    null,
                    skillFile.toAbsolutePath());
        }
        Path parentName = parent.getFileName();
        String dirName = parentName != null ? parentName.toString() : SKILL_FILE;
        List<String> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(parent)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> parent.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to walk external skill directory: " + parent, e);
        }
        return new AiSkill(dirName, files, null, parent);
    }

}
