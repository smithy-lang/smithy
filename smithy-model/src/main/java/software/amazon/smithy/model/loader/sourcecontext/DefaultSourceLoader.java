/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.sourcecontext;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * This class loads model files into memory, one at a time, and either shows leading lines up to a source location or
 * shows lines that are relevant to a source location use context from a {@link Model}.
 *
 * <p>Sort sequences of {@link SourceLocation}s before calling {@link #loadContext(FromSourceLocation)} to avoid
 * needing to open and parse the same file more than once.
 *
 * @see SourceContextLoader#createLineBasedLoader
 * @see SourceContextLoader#createModelAwareLoader
 */
final class DefaultSourceLoader implements SourceContextLoader {

    private static final Logger LOGGER = Logger.getLogger(DefaultSourceLoader.class.getName());
    private final List<Line> lines = new ArrayList<>();
    private final Model model;
    private final int defaultCodeLines;
    private SourceLocation lastLoadedLocation;
    private Collection<Line> lastLoadedLocationLines;

    DefaultSourceLoader(int defaultCodeLines, Model model) {
        if (defaultCodeLines < 1) {
            throw new IllegalArgumentException("Must allow at least one code hint line: " + defaultCodeLines);
        }

        this.defaultCodeLines = defaultCodeLines;
        this.model = model;
    }

    @Override
    public Collection<Line> loadContext(FromSourceLocation source) {
        SourceLocation location = source.getSourceLocation();

        // Ignore the components that were generated and have no location.
        if (location == SourceLocation.NONE) {
            return Collections.emptyList();
        }

        // Use the cache if possible (e.g., multiple validation events for the same shape).
        if (location.equals(lastLoadedLocation)) {
            return lastLoadedLocationLines;
        }

        // Open a new file if no file is open, or it differs from the source location file.
        if (lastLoadedLocation == null || !lastLoadedLocation.getFilename().equals(location.getFilename())) {
            loadNextFile(location);
        }

        int line = location.getLine();

        if (!isValidLine(line)) {
            LOGGER.finer(() -> "Attempted to load context for an invalid source location: " + location);
            lastLoadedLocationLines = Collections.emptyList();
        } else if (model == null) {
            int start = Math.max(0, line - defaultCodeLines);
            lastLoadedLocationLines = lines.subList(start, line);
        } else {
            lastLoadedLocationLines = parseLines(source);
        }

        return lastLoadedLocationLines;
    }

    private void loadNextFile(SourceLocation source) {
        lines.clear();
        lastLoadedLocation = source;
        LOGGER.finer(() -> "Opening source location file for " + source);

        try (LineNumberReader reader = openSourceLocation(source)) {
            String lineString;
            while ((lineString = reader.readLine()) != null) {
                lines.add(new Line(reader.getLineNumber(), lineString));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private LineNumberReader openSourceLocation(FromSourceLocation source) {
        try {
            // Ensure that there's a scheme.
            SourceLocation location = source.getSourceLocation();
            String normalizedFile = location.getFilename();

            // Refuse to open URLs that are not files or JARs by forcing the file protocol.
            if (!location.getFilename().startsWith("file:") && !location.getFilename().startsWith("jar:")) {
                normalizedFile = "file:" + normalizedFile;
            }

            // Loading from a JAR needs special treatment, but this can
            // all actually be handled in a uniform way using URLs.
            URL url = new URL(normalizedFile);
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            return new LineNumberReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load source location context for " + source, e);
        }
    }

    private boolean isValidLine(int line) {
        line--;
        return line >= 0 && line < lines.size();
    }

    private void addLineIfValid(int line, List<Line> mutate) {
        if (isValidLine(line)) {
            mutate.add(lines.get(line - 1));
        }
    }

    private List<Line> parseLines(FromSourceLocation source) {
        SourceLocation location = source.getSourceLocation();
        int line = location.getLine();

        if (source instanceof ValidationEvent) {
            ValidationEvent event = (ValidationEvent) source;
            Shape targetShape = event.getShapeId().flatMap(model::getShape).orElse(null);
            if (targetShape != null) {
                if (targetShape.getSourceLocation().equals(location)) {
                    // The validation event is on a shape since the location is equal to the shape location.
                    source = targetShape;
                } else if (targetShape.getSourceLocation().getLine() > location.getLine()) {
                    // Assume it's a trait in the Smithy IDL. Show the trait definition followed by shape definition.
                    return parseTraitBeforeShape(location, targetShape);
                } else if (location.getFilename().endsWith(".json")) {
                    // Assume it's a trait defined in JSON. Show the trait definition followed by shape definition.
                    return parseTraitAfterShape(location, targetShape);
                } else {
                    // It's a trait that uses "apply" or the location is invalid, so just show the trait line.
                    return Collections.singletonList(lines.get(line - 1));
                }
            }
        }

        if (source instanceof MemberShape) {
            return parseMemberShape(location, (MemberShape) source);
        }

        return parseOtherComponents(location);
    }

    private List<Line> parseTraitBeforeShape(SourceLocation location, Shape targetShape) {
        List<Line> result = new ArrayList<>(2);
        addLineIfValid(location.getLine(), result);
        addLineIfValid(targetShape.getSourceLocation().getLine(), result);
        return result;
    }

    private List<Line> parseTraitAfterShape(SourceLocation location, Shape targetShape) {
        List<Line> result = new ArrayList<>(2);
        addLineIfValid(targetShape.getSourceLocation().getLine(), result);
        addLineIfValid(location.getLine(), result);
        return result;
    }

    private List<Line> parseMemberShape(SourceLocation location, MemberShape member) {
        // Members should always crawl up to the defining shape in both the IDL and JSON.
        Shape container = model.getShape(member.getContainer()).orElse(null);

        // This should never be null, but guard here just in case.
        if (container == null) {
            LOGGER.warning(() -> "Member container not found: " + member.getId() + " -> " + member.getTarget());
        } else {
            SourceLocation containerLocation = container.getSourceLocation();
            // Some basic checking to ensure the member after the container in the same file.
            if (containerLocation.getFilename().equals(location.getFilename())
                    && containerLocation.getLine() < location.getLine()) {
                List<Line> result = new ArrayList<>(2);
                addLineIfValid(containerLocation.getLine(), result);
                addLineIfValid(location.getLine(), result);
                return result;
            }
        }

        return Collections.emptyList();
    }

    private List<Line> parseOtherComponents(SourceLocation location) {
        // Show context lines up from the shape until maxCode or until an empty line is encountered.
        int line = location.getLine();
        int lowestPossibleLine = Math.max(0, line - 1 - defaultCodeLines);
        int foundStart = line - 1;

        for (int i = line - 1; i > lowestPossibleLine; i--) {
            Line foundLine = lines.get(i);
            if (foundLine.getContent().length() > 0) {
                foundStart = i;
            } else {
                break;
            }
        }

        return lines.subList(foundStart, line);
    }
}
