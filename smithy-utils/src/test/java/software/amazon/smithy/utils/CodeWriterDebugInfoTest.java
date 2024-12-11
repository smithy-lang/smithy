/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class CodeWriterDebugInfoTest {
    @Test
    public void convertsToStringWhenEmpty() {
        CodeWriterDebugInfo info = new CodeWriterDebugInfo();

        assertThat(info.toString(), equalTo("(Debug Info {})"));
    }

    @Test
    public void convertsToString() {
        CodeWriterDebugInfo info = new CodeWriterDebugInfo();
        info.putMetadata("path", "ROOT/a");
        info.putMetadata("test", "true");

        assertThat(info.toString(), equalTo("(Debug Info {path=ROOT/a, test=true})"));
    }

    @Test
    public void returnsWellKnownPath() {
        CodeWriterDebugInfo info = new CodeWriterDebugInfo();
        info.putMetadata("path", "ROOT/a");

        assertThat(info.getStateDebugPath(), equalTo("ROOT/a"));
    }
}
