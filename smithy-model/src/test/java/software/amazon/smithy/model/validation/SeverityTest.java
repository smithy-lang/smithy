/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SeverityTest {
    @Test
    public void checksIfCanSuppress() {
        assertTrue(Severity.NOTE.canSuppress());
        assertTrue(Severity.WARNING.canSuppress());
        assertTrue(Severity.DANGER.canSuppress());
        assertFalse(Severity.SUPPRESSED.canSuppress());
        assertFalse(Severity.ERROR.canSuppress());
    }

    @Test
    public void createsFromString() {
        assertFalse(Severity.fromString("FOO").isPresent());
        assertSame(Severity.NOTE, Severity.fromString("NOTE").get());
        assertSame(Severity.WARNING, Severity.fromString("WARNING").get());
        assertSame(Severity.ERROR, Severity.fromString("ERROR").get());
        assertSame(Severity.DANGER, Severity.fromString("DANGER").get());
    }
}
