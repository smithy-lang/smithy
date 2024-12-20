/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CaseInsensitiveReservedWords implements ReservedWords {

    private final Set<String> words;
    private final Function<String, String> escaper;

    CaseInsensitiveReservedWords(Set<String> words, Function<String, String> escaper) {
        this.words = words.stream().map(word -> word.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
        this.escaper = escaper;
    }

    @Override
    public String escape(String word) {
        return isReserved(word) ? escaper.apply(word) : word;
    }

    @Override
    public boolean isReserved(String word) {
        return words.contains(word.toLowerCase(Locale.ENGLISH));
    }
}
