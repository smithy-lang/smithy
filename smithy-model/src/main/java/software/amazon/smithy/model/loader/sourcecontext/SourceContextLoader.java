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

package software.amazon.smithy.model.loader.sourcecontext;

import java.util.Collection;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Loads lines of text from source location files to display in things like error messages.
 */
@SmithyUnstableApi
public interface SourceContextLoader {
    /**
     * Attempts to load a file and return contextual source lines for the given source location.
     *
     * @param location Source location to load.
     * @return Returns the loaded source lines.
     */
    Collection<Line> loadContext(FromSourceLocation location);

    /**
     * Load context and include {@code defaultCodeLines} lines leading up to the target line.
     *
     * @param defaultCodeLines Number of leading lines to include leading up to the target. Must be greater than 0.
     * @return Returns the loader.
     * @throws IllegalArgumentException if {@code defaultCodeLines} is less than 1.
     */
    static SourceContextLoader createLineBasedLoader(int defaultCodeLines) {
        return new DefaultSourceLoader(defaultCodeLines, null);
    }

    /**
     * Load context and include the most relevant information possible based on the kind of {@link FromSourceLocation}.
     *
     * @param defaultCodeLinesHint Limits the number of context lines in some cases. Must be greater than 0.
     * @return Returns the loader.
     * @throws IllegalArgumentException if {@code defaultCodeLinesHint} is less than 1.
     */
    static SourceContextLoader createModelAwareLoader(Model model, int defaultCodeLinesHint) {
        return new DefaultSourceLoader(defaultCodeLinesHint, model);
    }

    /**
     * A pair of line numbers to the contents of lines.
     */
    final class Line {

        private final int lineNumber;
        private final CharSequence content;

        public Line(int lineNumber, CharSequence content) {
            this.lineNumber = lineNumber;
            this.content = content;
        }

        /**
         * Get the line number of the line, starting at 1.
         *
         * @return Returns the 1-based line number.
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Returns the content as a CharSequence.
         *
         * <p>CharSequence is used here to allow implementations to potentially use things like CharBuffer slices.
         *
         * @return The content of the line.
         */
        public CharSequence getContent() {
            return content;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lineNumber, content);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Line line = (Line) o;
            return lineNumber == line.lineNumber && Objects.equals(content, line.content);
        }

        @Override
        public String toString() {
            return lineNumber + " | " + content;
        }
    }
}
