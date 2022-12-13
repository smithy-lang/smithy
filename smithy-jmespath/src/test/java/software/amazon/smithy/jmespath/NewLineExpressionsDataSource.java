/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
