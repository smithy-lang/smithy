/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.zip.Checksum;

/**
 * CRC-32C (Castagnoli) checksum for SMF integrity verification.
 *
 * <p>Uses the iSCSI polynomial (0x1EDC6F41) in reflected form.
 *
 * <p>When running on JDK 9+ this delegates to {@code java.util.zip.CRC32C},
 * which HotSpot intrinsifies to the CPU CRC32 instruction (SSE 4.2 on x86,
 * the CRC extension on ARM). That is roughly 24x faster than the portable
 * software table below on the models we benchmarked, and it is what makes
 * full-file verification cheap on the loading path.
 *
 * <p>Because this module still compiles against the Java 8 API (where
 * {@code CRC32C} does not exist), the platform implementation is bound
 * reflectively at class-initialization time. Only the constructor needs a
 * {@link MethodHandle}; the resulting object is used through the Java 8
 * {@link Checksum} interface, so the hot {@code update} calls dispatch
 * normally and the intrinsic still applies. If the platform class is
 * unavailable (a genuine Java 8 runtime), we fall back to the software table.
 */
final class Crc32C {

    private static final int[] TABLE = new int[256];

    // Reflectively-bound no-arg constructor of java.util.zip.CRC32C, or null
    // when running on a runtime that does not provide it (Java 8).
    private static final MethodHandle CRC32C_CONSTRUCTOR = findCrc32cConstructor();

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0x82F63B78;
                } else {
                    crc >>>= 1;
                }
            }
            TABLE[i] = crc;
        }
    }

    private Crc32C() {}

    private static MethodHandle findCrc32cConstructor() {
        try {
            Class<?> cls = Class.forName("java.util.zip.CRC32C");
            // Return type is the concrete class; callers cast the result to the
            // Java 8-visible Checksum interface that CRC32C implements.
            return MethodHandles.publicLookup().findConstructor(cls, MethodType.methodType(void.class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Computes CRC-32C over the given byte range.
     */
    static int compute(byte[] buf, int off, int len) {
        if (CRC32C_CONSTRUCTOR != null) {
            try {
                Checksum checksum = (Checksum) CRC32C_CONSTRUCTOR.invoke();
                checksum.update(buf, off, len);
                return (int) checksum.getValue();
            } catch (Throwable t) {
                // Fall through to the software implementation on any unexpected
                // failure so verification never breaks because of the fast path.
            }
        }
        return computeSoftware(buf, off, len);
    }

    // Portable fallback used when java.util.zip.CRC32C is not available.
    static int computeSoftware(byte[] buf, int off, int len) {
        int crc = 0xFFFFFFFF;
        for (int i = off; i < off + len; i++) {
            crc = TABLE[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
        }
        return ~crc;
    }
}
