/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

/**
 * CRC-32C (Castagnoli) checksum for SMF integrity verification.
 *
 * <p>Uses the iSCSI polynomial (0x1EDC6F41) in reflected form.
 */
final class Crc32C {

    private static final int[] TABLE = new int[256];

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

    /**
     * Computes CRC-32C over the given byte range.
     */
    static int compute(byte[] buf, int off, int len) {
        int crc = 0xFFFFFFFF;
        for (int i = off; i < off + len; i++) {
            crc = TABLE[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
        }
        return ~crc;
    }
}
