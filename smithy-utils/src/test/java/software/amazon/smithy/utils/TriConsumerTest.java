/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Locale;
import org.junit.jupiter.api.Test;

public class TriConsumerTest {
    @Test
    public void composes() {
        StringBuilder result = new StringBuilder();

        TriConsumer<String, String, String> first = (a, b, c) -> {
            result.append(a).append(b).append(c);
        };

        TriConsumer<String, String, String> second = (d, e, f) -> {
            result.append(d.toUpperCase(Locale.ENGLISH))
                    .append(e.toUpperCase(Locale.ENGLISH))
                    .append(f.toUpperCase(Locale.ENGLISH));
        };

        TriConsumer<String, String, String> composed = first.andThen(second);

        composed.accept("a", "b", "c");

        assertThat(result.toString(), equalTo("abcABC"));
    }
}
