/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class VersionTest {
    @Test
    public void resolvesLatestMajorVersion() {
        assertEquals(Version.VERSION_2_1, Version.fromString("2.x"));
        assertEquals(Version.VERSION_2_1, Version.fromString("2.1"));
        assertEquals(Version.VERSION_2_0, Version.fromString("2"));
        assertEquals(Version.VERSION_2_0, Version.fromString("2.0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.x", "3.x", "2.X", "2.*", "2.x.0"})
    public void rejectsUnsupportedVersions(String version) {
        assertNull(Version.fromString(version));
    }
}
