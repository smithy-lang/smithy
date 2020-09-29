/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
