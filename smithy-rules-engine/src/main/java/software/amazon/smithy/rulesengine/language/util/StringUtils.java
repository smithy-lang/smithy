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

package software.amazon.smithy.rulesengine.language.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * String utilities.
 */
@SmithyUnstableApi
public final class StringUtils {
    private StringUtils() {
    }

    /**
     * Indents the provided string by the provided number of spaces.
     *
     * @param s      the string to indent.
     * @param spaces the number of spaces.
     * @return the indented string.
     */
    public static String indent(String s, int spaces) {
        StringBuilder sb = new StringBuilder(spaces);
        for (int i = 0; i < spaces; ++i) {
            sb.append(" ");
        }
        String whitespace = sb.toString();
        return Stream.of(s.split("\n"))
                .map(ss -> whitespace + ss + "\n")
                .collect(Collectors.joining());
    }

    /**
     * Splits the given string by the UTF-8 LF U+000A character.
     *
     * @param s the string to split.
     * @return the list of string lines.
     */
    public static List<String> lines(String s) {
        String[] lines = s.split("\n");
        return ListUtils.of(lines);
    }
}
