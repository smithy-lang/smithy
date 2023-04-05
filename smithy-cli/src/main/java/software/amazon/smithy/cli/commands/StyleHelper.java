/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import java.util.regex.Pattern;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.utils.StringUtils;

final class StyleHelper {

    private static final Pattern TICK_PATTERN = Pattern.compile("`(.*?)`");

    private StyleHelper() {}

    // Converts Markdown style ticks to use color highlights if colors are enabled.
    static String formatMessage(String message, int lineLength, ColorFormatter colors) {
        String content = StringUtils.wrap(message, lineLength, System.lineSeparator(), false);

        if (colors.isColorEnabled()) {
            content = markdownLiterals(content, colors);
        }

        return content;
    }

    static String markdownLiterals(String content, ColorFormatter colors) {
        if (colors.isColorEnabled()) {
            content = TICK_PATTERN.matcher(content).replaceAll(colors.style("$1", ColorTheme.LITERAL));
        }
        return content;
    }
}
