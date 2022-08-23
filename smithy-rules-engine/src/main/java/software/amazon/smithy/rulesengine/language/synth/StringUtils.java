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

package software.amazon.smithy.rulesengine.language.synth;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class StringUtils {
    private StringUtils() {
    }


    // TODO(dongie): this is a little janky but fulfills its purpose...
    public static List<String> splitTemplatedString(String s) {
        List<String> parts = new ArrayList<>();

        int i = 0;
        int j = 0;

        while (i < s.length()) {
            if (s.charAt(i) == '{') {
                // entering a template. Add the current non-template substring, if any, to the list
                if (j < i) {
                    parts.add(s.substring(j, i));
                }
                j = i;
            } else if (s.charAt(i) == '}') {
                parts.add(s.substring(j, i + 1));
                j = i + 1;
            }
            ++i;
        }

        if (j < i) {
            parts.add(s.substring(j, i));
        }

        return parts;
    }

    public static boolean isTemplated(String s) {
        return s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}';
    }
}
