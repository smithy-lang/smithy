/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * A reserved words implementation that maps known words to other words.
 *
 * <p>The following example shows how to use this class to make reserved
 * words safe for the targeted code:
 *
 * <pre>
 * {@code
 * ReservedWords reserved = MappedReservedWords.builder()
 *         .put("exception", "apiException")
 *         .put("void", "void_")
 *         .build();
 * String safeWord = reserved.escape("exception");
 * System.out.println(safeWord); // outputs "apiException"
 * }
 * </pre>
 *
 * <p>The detection of reserved words can be made case-insensitive such
 * that "bar", "BAR", "Bar", etc., can be detected as reserved words.
 *
 * <pre>
 * {@code
 * ReservedWords reserved = MappedReservedWords.builder()
 *         .put("foo", "Hi")
 *         .putCaseInsensitive("bar", "bam")
 *         .build();
 *
 * assert(reserved.escape("foo").equals("Hi"));
 * assert(reserved.escape("Foo").equals("Foo"));
 * assert(reserved.escape("BAR").equals("bam"));
 * }
 * </pre>
 */
public final class MappedReservedWords implements ReservedWords {
    private final Map<String, String> mappings;
    private final Map<String, String> caseInsensitiveMappings;

    /**
     * @param mappings Map of reserved word to replacement words.
     * @param caseInsensitiveMappings Map of case-insensitive reserved word to replacement words.
     */
    public MappedReservedWords(Map<String, String> mappings, Map<String, String> caseInsensitiveMappings) {
        this.mappings = MapUtils.copyOf(mappings);
        this.caseInsensitiveMappings = MapUtils.copyOf(caseInsensitiveMappings);
    }

    /**
     * @return Creates a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String escape(String word) {
        String result = mappings.get(word);

        if (result == null && !caseInsensitiveMappings.isEmpty()) {
            result = caseInsensitiveMappings.get(word.toLowerCase(Locale.US));
        }

        return result != null ? result : word;
    }

    @Override
    public boolean isReserved(String word) {
        if (mappings.containsKey(word)) {
            return true;
        }

        return !caseInsensitiveMappings.isEmpty() && caseInsensitiveMappings.containsKey(word.toLowerCase(Locale.US));
    }

    /**
     * Builder to create a new {@link MappedReservedWords} instance.
     */
    public static final class Builder implements SmithyBuilder<ReservedWords> {
        private final Map<String, String> mappings = new HashMap<>();
        private final Map<String, String> caseInsensitiveMappings = new HashMap<>();

        private Builder() {}

        /**
         * Add a new reserved words.
         *
         * @param reservedWord Reserved word to convert.
         * @param conversion Word to convert to.
         * @return Returns the builder.
         */
        public Builder put(String reservedWord, String conversion) {
            mappings.put(reservedWord, conversion);
            return this;
        }

        /**
         * Add a new case-insensitive reserved word that converts the given
         * reserved word to the given conversion string.
         *
         * <p>Note that the conversion string is used literally. The casing
         * of the original word has no effect on the conversion. Use
         * {@link ReservedWordsBuilder} for a case-insensitive reserved words
         * implementation that can take casing into account.
         *
         * @param reservedWord Case-insensitive reserved word to convert.
         * @param conversion Word to convert to.
         * @return Returns the builder.
         */
        public Builder putCaseInsensitive(String reservedWord, String conversion) {
            caseInsensitiveMappings.put(reservedWord.toLowerCase(Locale.US), conversion);
            return this;
        }

        /**
         * Builds the reserved words.
         *
         * @return Returns the created reserved words implementation.
         */
        @Override
        public ReservedWords build() {
            return new MappedReservedWords(mappings, caseInsensitiveMappings);
        }
    }
}
