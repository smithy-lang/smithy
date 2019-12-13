/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CaseInsensitiveReservedWords implements ReservedWords {

    private final Set<String> words;
    private final Function<String, String> escaper;

    CaseInsensitiveReservedWords(Set<String> words, Function<String, String> escaper) {
        this.words = words.stream().map(word -> word.toLowerCase(Locale.ENGLISH)).collect(Collectors.toSet());
        this.escaper = escaper;
    }

    @Override
    public String escape(String word) {
        return isReserved(word) ? escaper.apply(word) : word;
    }

    @Override
    public boolean isReserved(String word) {
        return words.contains(word.toLowerCase(Locale.ENGLISH));
    }
}
