/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

final class NewLineExpressionsDataSource {

    public Stream<String> validTests() {
        return readFile(getClass().getResourceAsStream("valid"));
    }

    public Stream<String> invalidTests() {
        return readFile(getClass().getResourceAsStream("invalid"));
    }

    private Stream<String> readFile(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .lines()
                .map(line -> {
                    if (line.endsWith(",")) {
                        return line.substring(0, line.length() - 1);
                    } else {
                        return line;
                    }
                })
                .map(line -> Lexer.tokenize(line).next().value.expectStringValue());
    }
}
