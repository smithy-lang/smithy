/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers Smithy AI skills bundled on the classpath.
 *
 * <p>Every JAR that ships AI content places its files under {@code META-INF/smithy-ai/skills/} at
 * build time, and beside them an index file ({@code skills.index}) listing every file leaf. This
 * class enumerates the indexes across all JARs via {@link ClassLoader#getResources(String)} and
 * groups the listed files by their first path segment (the skill name) - the same idiom
 * {@code ModelDiscovery} uses for {@code META-INF/smithy/manifest}, chosen because a JAR cannot
 * list a directory at runtime.
 */
public final class AiContent {

    static final String ROOT = "META-INF/smithy-ai/";
    static final String SKILLS_INDEX = ROOT + "skills.index";
    static final String SKILLS_PREFIX = ROOT + "skills/";

    private AiContent() {}

    /** Returns every bundled skill discovered on the thread context classloader. */
    public static List<AiSkill> skills() {
        return skills(Thread.currentThread().getContextClassLoader());
    }

    /** Returns every bundled skill discovered on {@code loader}. */
    public static List<AiSkill> skills(ClassLoader loader) {
        List<AiSkill> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : discover(loader, SKILLS_INDEX, SKILLS_PREFIX).entrySet()) {
            result.add(new AiSkill(e.getKey(), SKILLS_PREFIX + e.getKey() + "/", e.getValue(), loader));
        }
        return result;
    }

    /** Finds one bundled skill by name, if present. */
    public static Optional<AiSkill> skill(ClassLoader loader, String name) {
        for (AiSkill s : skills(loader)) {
            if (s.getName().equals(name)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /**
     * Reads every JAR's copy of {@code indexResource} on the classpath, parses its file-leaf lines,
     * and groups them by first path segment (the skill name). Order matches the sorted
     * index; duplicate names across JARs merge their file lists.
     */
    private static Map<String, List<String>> discover(ClassLoader loader, String indexResource, String prefix) {
        Map<String, List<String>> byName = new LinkedHashMap<>();
        Enumeration<URL> indexes;
        try {
            indexes = loader.getResources(indexResource);
        } catch (IOException e) {
            throw new AiContentException("Error locating Smithy AI index " + indexResource, e);
        }
        while (indexes.hasMoreElements()) {
            URL indexUrl = indexes.nextElement();
            for (String line : readIndex(indexUrl)) {
                validateIndexLine(indexUrl, line);
                int slash = line.indexOf('/');
                if (slash <= 0) {
                    // Bare file at the root - no owning skill; skip with a clear rule.
                    throw new AiContentException("Illegal entry in " + indexUrl + ": " + line
                            + " (each entry must be <name>/<relative-file>)");
                }
                String name = line.substring(0, slash);
                String rel = line.substring(slash + 1);
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(rel);
            }
        }
        return byName;
    }

    private static List<String> readIndex(URL url) {
        List<String> lines = new ArrayList<>();
        try {
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            try (InputStream in = conn.getInputStream();
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isEmpty() && line.charAt(0) != '#') {
                        lines.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new AiContentException("Error reading Smithy AI index " + url, e);
        }
        return lines;
    }

    /**
     * Same segment-safety rules {@code ModelDiscovery} enforces: no empty, no "." / "..", no
     * leading or trailing slash. Prevents an index from pointing outside its prefix.
     */
    private static void validateIndexLine(URL url, String line) {
        if (line.startsWith("/") || line.endsWith("/")) {
            throw new AiContentException("Illegal entry in " + url + ": " + line
                    + " (must not start or end with '/')");
        }
        for (String segment : line.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new AiContentException("Illegal entry in " + url + ": " + line);
            }
        }
    }

    /** Package-private: resolve a resource path to a URL on {@code loader}, or throw. */
    static URL requireResource(ClassLoader loader, String path) {
        URL url = loader.getResource(path);
        if (url == null) {
            throw new AiContentException("Bundled AI resource not found: " + path);
        }
        return url;
    }

    /** Package-private: defensive copy of a file list, exposed to consumers as unmodifiable. */
    static List<String> unmodifiable(List<String> src) {
        return Collections.unmodifiableList(new ArrayList<>(src));
    }
}
