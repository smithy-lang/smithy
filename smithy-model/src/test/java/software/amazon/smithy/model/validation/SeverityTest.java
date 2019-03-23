/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
