/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

/**
 * Determines what is reserved and escapes reserved words.
 */
public interface ReservedWords {
    /**
     * Escapes a reserved word.
     *
     * @param word Word to escape.
     * @return Returns the converted value.
     */
    String escape(String word);

    /**
     * Checks if the given word is reserved.
     *
     * @param word Word to check.
     * @return Returns true if the word is reserved.
     */
    boolean isReserved(String word);

    /**
     * Creates a reserved word implementation that does not modify words.
     *
     * <pre>
     * {@code
     * ReservedWords reserved = ReservedWords.identity();
     * reserved.isReserved("foo"); // always returns false for anything.
     * }
     * </pre>
     *
     * @return Returns the identity implementation.
     */
    static ReservedWords identity() {
        return new ReservedWords() {
            @Override
            public String escape(String word) {
                return word;
            }

            @Override
            public boolean isReserved(String word) {
                return false;
            }
        };
    }

    /**
     * Composes multiple instance of {@code ReservedWords} into a
     * single implementation that delegates to them one after the
     * other.
     *
     * <p>Each reserved words implementation is invoked one after
     * the other until one of them returns true for
     * {@link ReservedWords#isReserved}.
     *
     * <pre>
     * {@code
     * ReservedWords a = MappedReservedWords.builder().put("void", "_void").build();
     * ReservedWords b = MappedReservedWords.builder().put("foo", "_foo").build();
     * ReservedWords composed = ReservedWords.compose(a, b);
     * String safeWordA = composed.escape("void");
     * String safeWordB = composed.escape("foo");
     * System.out.println(safeWordA + " " + safeWordB); // outputs "_void _foo"
     * }
     * </pre>
     *
     * @param delegates ReservedWords instances to delegate to.
     * @return Returns the created {@code ReservedWords} instance.
     */
    static ReservedWords compose(ReservedWords... delegates) {
        return new ReservedWords() {
            @Override
            public String escape(String word) {
                for (ReservedWords reservedWords : delegates) {
                    if (reservedWords.isReserved(word)) {
                        return reservedWords.escape(word);
                    }
                }

                return word;
            }

            @Override
            public boolean isReserved(String word) {
                for (ReservedWords reservedWords : delegates) {
                    if (reservedWords.isReserved(word)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }
}
