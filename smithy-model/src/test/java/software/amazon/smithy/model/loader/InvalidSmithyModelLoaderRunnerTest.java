/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.IoUtils;

/**
 * Loads all of the ".smithy" files contained in idl/invalid.
 *
 * Each file must contain an invalid model. The file must start with a comment
 * that defines the expected parser error. The value of the expected error can
 * either be equal to the full parse error message or can be contain the
 * given error message.
 */
public class InvalidSmithyModelLoaderRunnerTest {
    @ParameterizedTest
    @MethodSource("data")
    public void testParserRunner(String file) {
        String contents = IoUtils.readUtf8File(file);
        String expectedError = contents.split("\\R")[0].substring(3);

        try {
            Model.assembler()
                    .addImport(file)
                    .assemble()
                    .unwrap();
            throw new IllegalStateException("Expected a parse error for " + file);
        } catch (RuntimeException e) {
            String actualMessage = cleanErrorMessage(e.getMessage());
            String expectedMessage = cleanErrorMessage(expectedError);
            if (!actualMessage.contains(expectedMessage)) {
                throw new IllegalStateException(
                        String.format("Expected a different parse error for %s.\nExpected (%s)\nFound (%s)",
                                file,
                                expectedMessage,
                                actualMessage),
                        e);
            }
        }
    }

    private String cleanErrorMessage(String errorMessage) {
        return errorMessage
                // We'll never see EOF on Windows since we only get 2 context characters and those
                // will be taken up by the line separator characters.
                .replace("[EOF]", "")
                // Make sure the line separators and representations of them are consistent across
                // operating systems.
                .replace("\r\n", "\\n")
                .replace("\r", "\\n")
                .replace("\n", "\\n");
    }

    public static Collection<String> data() throws Exception {
        try (Stream<Path> paths = Files.walk(Paths.get(
                ValidSmithyModelLoaderRunnerTest.class.getResource("invalid").toURI()))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".smithy"))
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
