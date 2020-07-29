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

package software.amazon.smithy.model.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * This validation event formatter outputs a validation event that points
 * to the source code line that triggered the event.
 *
 * <p>If the event does not have a source location, then this formatter
 * will not attempt to load the contents of the model.
 *
 * <p>This formatter outputs messages similar to the following text:</p>
 *
 * <pre>{@code
 * ERROR: aws.protocols.tests.ec2#IgnoresWrappingXmlName (Model)
 *     --> /foo/bar.smithy
 *      |
 *  403 | apply MyShape @httpResponseTests([
 *      |                                  ^
 *      = Unable to resolve trait `smithy.test#httpResponseTests`. If this is a custom trait, [...]
 * }</pre>
 */
public final class ContextualValidationEventFormatter implements ValidationEventFormatter {
    @Override
    public String format(ValidationEvent event) {
        StringWriter writer = new StringWriter();
        Formatter formatter = new Formatter(writer);
        formatter.format("%s: %s (%s)%n",
                         event.getSeverity(),
                         event.getShapeId().map(ShapeId::toString).orElse("-"),
                         event.getId());

        if (event.getSourceLocation() != SourceLocation.NONE) {
            String humanReadableFilename = getHumanReadableFilename(event.getSourceLocation());
            String contextualLine = null;
            try {
                contextualLine = loadContextualLine(event.getSourceLocation());
            } catch (IOException e) {
                // Do nothing.
            }

            if (contextualLine == null) {
                formatter.format("     @ %s%n", event.getSourceLocation());
            } else {
                // Show the filename.
                formatter.format("     @ %s%n", humanReadableFilename);
                formatter.format("     |%n");
                // Show the line number and source code line.
                formatter.format("%4d | %s%n", event.getSourceLocation().getLine(), contextualLine);
                // Add a carat to point to the column of the error.
                formatter.format("     | %" + event.getSourceLocation().getColumn() + "s%n", "^");
            }
        }

        // Add the message and indent each line.
        formatter.format("     = %s%n", event.getMessage().replace("\n", "       \n"));

        // Close up the formatter.
        formatter.flush();

        return writer.toString();
    }

    // Filenames might start with a leading file:/. Strip that.
    private String getHumanReadableFilename(SourceLocation source) {
        String filename = source.getFilename();

        if (filename.startsWith("file:")) {
            filename = filename.substring(5);
        }

        return filename;
    }

    // Attempts to load a specific line from the model.
    private String loadContextualLine(SourceLocation source) throws IOException {
        // Ensure that there's a scheme.
        String normalizedFile = source.getFilename();
        if (!source.getFilename().startsWith("file:") && !source.getFilename().startsWith("jar:")) {
            normalizedFile = "file:" + normalizedFile;
        }

        // Loading from a JAR needs special treatment, but this can
        // all actually be handled in a uniform way using URLs.
        URL url = new URL(normalizedFile);
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);

        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(source.getLine() - 1)
                    .findFirst()
                    .orElse(null);
        }
    }
}
