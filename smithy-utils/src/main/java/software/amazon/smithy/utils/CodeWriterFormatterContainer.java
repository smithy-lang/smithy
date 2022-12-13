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

package software.amazon.smithy.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A container for formatters registered with CodeWriter.
 */
@SmithyInternalApi
final class CodeWriterFormatterContainer {

    // Must be sorted for binary search to work.
    static final char[] VALID_FORMATTER_CHARS = {
            '!', '%', '&', '*', '+', ',', '-', '.', ';', '=', '@',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_', '`'};

    private final Map<Character, BiFunction<Object, String, String>> formatters = new HashMap<>();
    private final CodeWriterFormatterContainer parent;

    CodeWriterFormatterContainer() {
        this(null);
    }

    CodeWriterFormatterContainer(CodeWriterFormatterContainer parent) {
        this.parent = parent;
    }

    void putFormatter(Character identifier, BiFunction<Object, String, String> formatFunction) {
        if (Arrays.binarySearch(VALID_FORMATTER_CHARS, identifier) < 0) {
            throw new IllegalArgumentException("Invalid formatter identifier: " + identifier);
        }
        formatters.put(identifier, formatFunction);
    }

    BiFunction<Object, String, String> getFormatter(char identifier) {
        BiFunction<Object, String, String> result = formatters.get(identifier);

        if (result == null && parent != null) {
            result = parent.getFormatter(identifier);
        }

        return result;
    }
}
