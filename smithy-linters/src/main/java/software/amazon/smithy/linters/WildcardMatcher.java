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

package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import software.amazon.smithy.utils.StringUtils;

final class WildcardMatcher implements Predicate<String> {

    private final List<Predicate<String>> predicates = new ArrayList<>();

    @Override
    public boolean test(String text) {
        if (StringUtils.isEmpty(text)) {
            return false;
        }

        text = text.toLowerCase(Locale.ENGLISH);
        for (Predicate<String> predicate : predicates) {
            if (predicate.test(text)) {
                return true;
            }
        }

        return false;
    }

    void addSearch(String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            throw new IllegalArgumentException("Invalid empty pattern");
        } else if (pattern.equals("*")) {
            throw new IllegalArgumentException("Invalid wildcard pattern: *");
        } else {
            predicates.add(parseWildcardPattern(pattern));
        }
    }

    private static Predicate<String> parseWildcardPattern(String pattern) {
        boolean suffix = false;
        boolean prefix = false;

        // Find any leading or ending star, ensure that no inner stars are used.
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (i == 0) {
                    suffix = true;
                } else if (i == pattern.length() - 1) {
                    prefix = true;
                } else {
                    throw new IllegalArgumentException("Invalid inner '*' in wildcard pattern: " + pattern);
                }
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        String needle = result.toString();
        if (suffix && prefix) {
            return text -> text.contains(needle);
        } else if (suffix) {
            return text -> text.endsWith(needle);
        } else if (prefix) {
            return text -> text.startsWith(needle);
        } else {
            return text -> text.equals(needle);
        }
    }
}
