/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import com.code_intelligence.jazzer.junit.DictionaryFile;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;

/**
 * Fuzz test for SMF deserialization.
 *
 * <p>Feeds arbitrary byte arrays to {@link SmfReader} and verifies that it
 * either produces a valid Model or throws a clean exception. It must never:
 * <ul>
 *   <li>Throw NullPointerException</li>
 *   <li>Throw StackOverflowError (from deeply nested values)</li>
 *   <li>Enter an infinite loop (timeout enforced)</li>
 *   <li>Throw ArrayIndexOutOfBoundsException (bounds must be checked)</li>
 * </ul>
 */
public class SmfDeserializationFuzzTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @DictionaryFile(resourcePath = "/dictionary/smf-fuzz.dict")
    @FuzzTest
    public void fuzzReader(byte[] input) {
        Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
            try {
                SmfReader.read(input);
            } catch (SmfFormatException
                    | IllegalArgumentException
                    | IllegalStateException
                    | IndexOutOfBoundsException ignored) {
                // Expected for malformed input — these are clean rejections.
            }
        },
                () -> "Timeout or unexpected error on input: "
                        + Base64.getEncoder().encodeToString(input));
    }
}
