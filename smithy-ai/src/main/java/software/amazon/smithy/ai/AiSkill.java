/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.utils.IoUtils;

/**
 * One bundled skill: a named directory holding a {@code SKILL.md} plus any supporting files the
 * skill author bundles alongside it (for example {@code reference.md}, {@code examples.md},
 * {@code scripts/}, {@code templates/}), discovered via {@link AiContent}. Instances are
 * immutable; content is read lazily.
 */
public final class AiSkill {

    private final String name;
    private final String resourcePrefix;
    private final List<String> files;
    private final ClassLoader loader;

    AiSkill(String name, String resourcePrefix, List<String> files, ClassLoader loader) {
        this.name = name;
        this.resourcePrefix = resourcePrefix;
        this.files = AiContent.unmodifiable(files);
        this.loader = loader;
    }

    /** The skill's identifier, matching its directory name (e.g. {@code smithy-docs-navigator}). */
    public String getName() {
        return name;
    }

    /**
     * Relative paths of every file in this skill (e.g. {@code SKILL.md},
     * {@code reference.md}, {@code scripts/helper.py}), in index order. Unmodifiable.
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * Absolute classpath resource path for one relative file inside this skill. Useful when a
     * caller wants to open the resource itself instead of taking the returned string.
     */
    public String resourcePath(String relativePath) {
        return resourcePrefix + relativePath;
    }

    /** Reads the UTF-8 content of one relative file. */
    public String readFile(String relativePath) {
        if (!files.contains(relativePath)) {
            throw new AiContentException("Skill '" + name + "' has no file '" + relativePath + "'");
        }
        return readAt(resourcePath(relativePath));
    }

    /** Reads every file in this skill in index order, keyed by relative path. */
    public Map<String, String> readFiles() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String rel : files) {
            out.put(rel, readAt(resourcePath(rel)));
        }
        return out;
    }

    private String readAt(String path) {
        return IoUtils.readUtf8Url(AiContent.requireResource(loader, path));
    }

    @Override
    public String toString() {
        return "AiSkill{" + name + ", files=" + files.size() + "}";
    }
}
