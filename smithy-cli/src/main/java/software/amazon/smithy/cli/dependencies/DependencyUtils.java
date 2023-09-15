/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.dependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class DependencyUtils {
    private DependencyUtils() {}

    /**
     * Computes the sha1 digest of a file.
     *
     * @param path Path to file to compute hash for.
     * @return sha1 digest string.
     * @throws UncheckedIOException if the specified file could not be read.
     */
    public static String computeSha1(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (DigestInputStream din = new DigestInputStream(in, md)) {
                byte[] buf = new byte[1024 * 32];
                int n;
                do {
                    n = din.read(buf);
                } while (n > 0);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                int decimal = (int) b & 0xff;
                String hex = Integer.toHexString(decimal);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
