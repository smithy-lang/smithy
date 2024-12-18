/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class AnsiColorFormatterTest {
    @Test
    public void detectsIfColorIsEnabled() {
        assertThat(AnsiColorFormatter.NO_COLOR.isColorEnabled(), is(false));
        assertThat(AnsiColorFormatter.FORCE_COLOR.isColorEnabled(), is(true));
    }
}
