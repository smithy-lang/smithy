/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class CliPrinterTest {
    @Test
    public void printsWithNewlineByDefault() {
        StringBuilder builder = new StringBuilder();
        CliPrinter printer = new CliPrinter() {
            @Override
            public CliPrinter append(char c) {
                builder.append(c);
                return this;
            }

            @Override
            public CliPrinter append(CharSequence csq, int start, int end) {
                builder.append(csq, start, end);
                return this;
            }
        };
        printer.println("Hi");

        assertThat(builder.toString(), equalTo("Hi" + System.lineSeparator()));
    }
}
