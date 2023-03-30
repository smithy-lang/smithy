/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnsiColorFormatterTest {
    @Test
    public void detectsIfColorIsEnabled() {
        assertThat(AnsiColorFormatter.NO_COLOR.isColorEnabled(), is(false));
        assertThat(AnsiColorFormatter.FORCE_COLOR.isColorEnabled(), is(true));
    }

    @Test
    public void wrapsConsumerWithColor() {
        ColorFormatter formatter = AnsiColorFormatter.FORCE_COLOR;
        StringBuilder builder = new StringBuilder();

        formatter.style(builder, b -> {
            b.append("Hello");
        }, Style.RED);

        String result = builder.toString();

        assertThat(result, equalTo("\033[31mHello\033[0m"));
    }

    @Test
    public void wrapsConsumerWithColorAndClosesColorIfThrows() {
        ColorFormatter formatter = AnsiColorFormatter.FORCE_COLOR;
        StringBuilder builder = new StringBuilder();

        Assertions.assertThrows(RuntimeException.class, () -> {
            formatter.style(builder, b -> {
                b.append("Hello");
                throw new RuntimeException("A");
            }, Style.RED);
        });

        String result = builder.toString();

        assertThat(result, equalTo("\033[31mHello\033[0m"));
    }
}
