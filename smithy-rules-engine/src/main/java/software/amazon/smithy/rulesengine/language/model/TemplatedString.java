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

package software.amazon.smithy.rulesengine.language.model;

import java.util.List;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class TemplatedString {
    private final List<Part> parts;

    private TemplatedString(List<Part> parts) {
        this.parts = parts;
    }

    public List<Part> parts() {
        return parts;
    }

    public interface Part {
        String value();

        default boolean isPlaceholder() {
            return false;
        }
    }

//    private static TemplatedString parse(String template) {
//        ArrayList<Template.Part> out = new ArrayList<>();
//        Optional<Integer> templateStart = Optional.empty();
//        int depth = 0;
//        int templateEnd = 0;
//        for (int i = 0; i < template.length(); i++) {
//            if (template.substring(i).startsWith("{{")) {
//                i++;
//                continue;
//            }
//            if (template.substring(i).startsWith("}}")) {
//                i++;
//                continue;
//            }
//            if (template.charAt(i) == '{') {
//                if (depth == 0) {
//                    if (templateEnd != i) {
//                        out.add(Template.Literal.unescape(template.substring(templateEnd, i)));
//                    }
//                    templateStart = Optional.of(i + 1);
//                }
//                depth++;
//            }
//            if (template.charAt(i) == '}') {
//                depth--;
//                if (depth < 0) {
//                    throw new RuntimeException("unmatched `}` in template");
//                }
//                if (depth == 0) {
//                    int j = i + 1;
//                    while ()
//                    templateStart = Optional.empty();
//                }
//                templateEnd = i + 1;
//            }
//        }
//        if (depth != 0) {
//            throw new InnerParseError("unmatched `{` in template");
//        }
//        if (templateEnd < template.length()) {
//            out.add(Template.Literal.unescape(template.substring(templateEnd)));
//        }
//        return out;
//    }
//    }
//
//    private static class LiteralPart implements Part {
//        private final String value;
//
//        public LiteralPart(String value) {
//            this.value = value;
//        }
//
//        @Override
//        public String value() {
//            return value;
//        }
//    }
//
//    private static class PlaceholderPart implements Part {
//        public PlaceholderPart(String value) {
//            this.value = value;
//        }
//
//        private final String value;
//
//        @Override
//        public String value() {
//            return value;
//        }
//
//        @Override
//        public boolean isPlaceholder() {
//            return true;
//        }
//    }
}
