/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class DependencyUtilsTest {
    @Test
    public void computesCorrectShaHash() throws URISyntaxException {
        // Expected sha was computed using the `sha1sum` command line utility
        String expectedSha = "543d822a441db77f32309650a1b3c1510c9be392";
        String shaHash = DependencyUtils.computeSha1(Paths.get(Objects.requireNonNull(
                getClass().getResource("sha-test.txt")).toURI()));
        assertEquals(shaHash, expectedSha);
    }
}
