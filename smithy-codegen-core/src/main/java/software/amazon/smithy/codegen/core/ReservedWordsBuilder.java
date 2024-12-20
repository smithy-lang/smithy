/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.utils.StringUtils;

/**
 * Builds a {@link ReservedWords} implementation from explicit
 * mappings and from line-delimited files that contain reserved words.
 */
public final class ReservedWordsBuilder {

    private final Map<String, String> mappings = new HashMap<>();
    private final List<ReservedWords> delegates = new ArrayList<>();

    /**
     * Builds the reserved words.
     *
     * @return Returns the created reserved words implementation.
     */
    public ReservedWords build() {
        ReservedWords[] words = new ReservedWords[1 + delegates.size()];
        words[0] = new MappedReservedWords(mappings, Collections.emptyMap());
        for (int i = 0; i < delegates.size(); i++) {
            words[i + 1] = delegates.get(i);
        }

        return ReservedWords.compose(words);
    }

    /**
     * Add a new reserved words.
     *
     * @param reservedWord Reserved word to convert.
     * @param conversion Word to convert to.
     * @return Returns the builder.
     */
    public ReservedWordsBuilder put(String reservedWord, String conversion) {
        mappings.put(reservedWord, conversion);
        return this;
    }

    /**
     * Load a list of case-sensitive, line-delimited reserved words from a file.
     *
     * <p>This method will escape words by prefixing them with "_". Use
     * {@link #loadWords(URL, Function)} to customize how words are escaped.
     *
     * <p>Blank lines and lines that start with # are ignored.
     *
     * @param location URL of the file to load.
     * @return Returns the builder.
     */
    public ReservedWordsBuilder loadWords(URL location) {
        return loadWords(location, ReservedWordsBuilder::escapeWithUnderscore);
    }

    /**
     * Load a list of case-sensitive, line-delimited reserved words from a file.
     *
     * <p>Blank lines and lines that start with # are ignored.
     *
     * @param location URL of the file to load.
     * @param escaper Function used to escape reserved words.
     * @return Returns the builder.
     */
    public ReservedWordsBuilder loadWords(URL location, Function<String, String> escaper) {
        for (String word : readNonBlankNonCommentLines(location)) {
            put(word, escaper.apply(word));
        }
        return this;
    }

    /**
     * Load a list of case-insensitive, line-delimited reserved words from a file.
     *
     * <p>This method will escape words by prefixing them with "_". Use
     * {@link #loadCaseInsensitiveWords(URL, Function)} to customize how words
     * are escaped.
     *
     * <p>Blank lines and lines that start with # are ignored.
     *
     * @param location URL of the file to load.
     * @return Returns the builder.
     */
    public ReservedWordsBuilder loadCaseInsensitiveWords(URL location) {
        return loadCaseInsensitiveWords(location, ReservedWordsBuilder::escapeWithUnderscore);
    }

    /**
     * Load a list of case-insensitive, line-delimited reserved words from a file.
     *
     * <p>Blank lines and lines that start with # are ignored.
     *
     * @param location URL of the file to load.
     * @param escaper Function used to escape reserved words.
     * @return Returns the builder.
     */
    public ReservedWordsBuilder loadCaseInsensitiveWords(URL location, Function<String, String> escaper) {
        delegates.add(new CaseInsensitiveReservedWords(readNonBlankNonCommentLines(location), escaper));
        return this;
    }

    private static String escapeWithUnderscore(String word) {
        return "_" + word;
    }

    private static Set<String> readNonBlankNonCommentLines(URL url) {
        try (InputStream is = url.openConnection().getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(StringUtils::isNotBlank)
                    .filter(line -> !line.startsWith("#"))
                    .map(word -> StringUtils.stripEnd(word, null))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Error loading reserved words from " + url + ": " + e.getMessage(), e);
        }
    }
}
