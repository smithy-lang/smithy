/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class Crc32CTest {

    // CRC-32C of the ASCII string "123456789" is a well-known check value.
    private static final int CHECK_VALUE = 0xE3069283;

    @Test
    public void matchesKnownCheckVector() {
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        assertEquals(CHECK_VALUE, Crc32C.compute(data, 0, data.length));
        assertEquals(CHECK_VALUE, Crc32C.computeSoftware(data, 0, data.length));
    }

    @Test
    public void hardwareAndSoftwarePathsAgree() {
        // If the platform CRC32C is present, Crc32C.compute uses it; verify it
        // produces identical results to the portable software table across a
        // range of sizes and offsets. This guards cross-implementation
        // compatibility of the SMF trailer.
        Random random = new Random(42);
        for (int size : new int[] {0, 1, 2, 7, 8, 15, 16, 255, 4096, 65_537}) {
            byte[] data = new byte[size + 8];
            random.nextBytes(data);
            for (int off : new int[] {0, 1, 3}) {
                if (off + size > data.length) {
                    continue;
                }
                assertEquals(
                        Crc32C.computeSoftware(data, off, size),
                        Crc32C.compute(data, off, size),
                        "mismatch at size=" + size + " off=" + off);
            }
        }
    }

    @Test
    public void emptyInputIsZeroComplement() {
        // CRC of empty input: init 0xFFFFFFFF, complemented => 0.
        assertEquals(0, Crc32C.compute(new byte[0], 0, 0));
        assertEquals(0, Crc32C.computeSoftware(new byte[0], 0, 0));
    }
}
