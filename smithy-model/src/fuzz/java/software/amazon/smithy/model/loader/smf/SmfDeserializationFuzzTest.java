/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import com.code_intelligence.jazzer.junit.DictionaryFile;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Fuzz test for SMF deserialization.
 *
 * <p>Feeds arbitrary byte arrays to every reader entry point and verifies that
 * each either produces a result or throws a clean {@link SmfFormatException}.
 * A well-behaved reader of untrusted input must NEVER:
 * <ul>
 *   <li>Throw NullPointerException</li>
 *   <li>Throw StackOverflowError (from deeply nested values)</li>
 *   <li>Throw ArrayIndexOutOfBoundsException or any other
 *       IndexOutOfBoundsException (bounds must be checked and surfaced as
 *       {@link SmfFormatException})</li>
 *   <li>Enter an infinite loop (timeout enforced)</li>
 * </ul>
 *
 * <p>{@code IndexOutOfBoundsException} is intentionally NOT in the accepted
 * set: the reader must translate any such condition into
 * {@link SmfFormatException}. Catching it here would defeat the purpose of the
 * fuzz test.
 */
public class SmfDeserializationFuzzTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @DictionaryFile(resourcePath = "/dictionary/smf-fuzz.dict")
    @FuzzTest
    public void fuzzReader(byte[] input) {
        run(input, () -> SmfReader.read(input));
    }

    @DictionaryFile(resourcePath = "/dictionary/smf-fuzz.dict")
    @FuzzTest
    public void fuzzReaderNoCrc(byte[] input) {
        run(input, () -> SmfReader.read(input, false));
    }

    @DictionaryFile(resourcePath = "/dictionary/smf-fuzz.dict")
    @FuzzTest
    public void fuzzReadInto(byte[] input) {
        run(input, () -> SmfReader.readInto(input, new CollectingHandler(), false));
    }

    @DictionaryFile(resourcePath = "/dictionary/smf-fuzz.dict")
    @FuzzTest
    public void fuzzReadSelective(byte[] input) {
        run(input,
                () -> SmfReader.readSelective(input,
                        SelectiveLoadRequest.builder()
                                .service(ShapeId.from("com.example#Service"))
                                .addOperation(ShapeId.from("com.example#Operation"))
                                .verifyCrc(false)
                                .build()));
    }

    private void run(byte[] input, Runnable body) {
        Assertions.assertTimeoutPreemptively(TIMEOUT, () -> {
            try {
                body.run();
            } catch (SmfFormatException
                    | IllegalArgumentException
                    | IllegalStateException
                    | software.amazon.smithy.model.SourceException ignored) {
                // Expected for malformed input — these are clean rejections.
                // SourceException covers structurally-invalid shapes that decode
                // successfully but fail Smithy's shape/model builders.
            }
            // NOTE: IndexOutOfBoundsException is deliberately not caught. If the
            // reader lets one escape, the test fails, because untrusted input
            // must never produce a raw bounds exception.
        },
                () -> "Timeout or unexpected error on input: "
                        + Base64.getEncoder().encodeToString(input));
    }

    private static final class CollectingHandler implements SmfReader.LoadHandler {
        private final List<AbstractShapeBuilder<?, ?>> builders = new ArrayList<>();

        @Override
        public void modelVersion() {}

        @Override
        public void metadata(String key, Node value) {}

        @Override
        public void defineShape(AbstractShapeBuilder<?, ?> builder, List<MemberShape.Builder> members) {
            builders.add(builder);
        }
    }
}
