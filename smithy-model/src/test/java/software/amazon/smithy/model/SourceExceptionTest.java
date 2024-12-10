/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;

import org.junit.jupiter.api.Test;

public class SourceExceptionTest {
    @Test
    public void addsSourceLocationIfNotInMessage() {
        SourceLocation sourceLocation = new SourceLocation("/file");
        SourceException e = new SourceException("foo", sourceLocation);

        assertThat(e.getMessage(), containsString(sourceLocation.toString()));
    }

    @Test
    public void omitsSourceLocationIfNotInMessage() {
        SourceLocation sourceLocation = new SourceLocation("/file");
        SourceException e = new SourceException("foo " + sourceLocation + " baz", sourceLocation);

        assertThat(e.getMessage(), endsWith(" baz"));
    }

    @Test
    public void omitsSourceLocationIfNone() {
        SourceException e = new SourceException("foo", SourceLocation.NONE);

        assertThat(e.getMessage(), endsWith("foo"));
    }
}
