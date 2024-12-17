/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.pattern;

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Represents a contained pattern.
 *
 * <p>A pattern is a series of segments, some of which may be labels.
 *
 * <p>Labels may appear in the pattern in the form of "{label}". Labels must
 * not be repeated, must not contain other labels (e.g., "{fo{bar}oo}"),
 * and the label name must match the regex "^[a-zA-Z0-9_]+$". No labels can
 * appear after the query string.
 *
 * <p>Greedy labels, a specialized type of label, may be specified using
 * "{label+}". Only a single greedy label may appear in a pattern, and it
 * must be the last label in a pattern. Greedy labels may be disabled for a
 * pattern as part of the builder construction.
 */
public class SmithyPattern {

    private final String pattern;
    private final List<Segment> segments;

    protected SmithyPattern(Builder builder) {
        pattern = Objects.requireNonNull(builder.pattern);
        segments = Objects.requireNonNull(builder.segments);

        checkForDuplicateLabels();
        if (!builder.allowsGreedyLabels && segments.stream().anyMatch(Segment::isGreedyLabel)) {
            throw new InvalidPatternException("Pattern must not contain a greedy label. Found " + pattern);
        }
    }

    /**
     * Gets all segments, in order.
     *
     * @return All segments, in order, in an unmodifiable list.
     */
    public final List<Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Get a list of all segments that are labels.
     *
     * @return Label segments in an unmodifiable list.
     */
    public final List<Segment> getLabels() {
        return Collections.unmodifiableList(
                segments.stream().filter(Segment::isLabel).collect(Collectors.toList()));
    }

    /**
     * Get a label by case-insensitive name.
     *
     * @param name Name of the label to retrieve.
     * @return An optionally found label.
     */
    public final Optional<Segment> getLabel(String name) {
        String searchKey = name.toLowerCase(Locale.US);
        return segments.stream()
                .filter(Segment::isLabel)
                .filter(label -> label.getContent().toLowerCase(Locale.US).equals(searchKey))
                .findFirst();
    }

    /**
     * Gets the greedy label of the pattern, if present.
     *
     * @return Returns the optionally found segment that is a greedy label.
     */
    public final Optional<Segment> getGreedyLabel() {
        return segments.stream().filter(Segment::isGreedyLabel).findFirst();
    }

    @Override
    public String toString() {
        return pattern;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SmithyPattern && pattern.equals(((SmithyPattern) other).pattern);
    }

    /**
     * Gets a map of explicitly conflicting label segments between this
     * pattern and another.
     *
     * @param otherPattern SmithyPattern to check against.
     * @return A map of Segments where each pair represents a conflict
     *     and where the key is a segment from this pattern. This map is
     *     ordered so segments that appear first in this pattern appear
     *     first when iterating the map.
     */
    public Map<Segment, Segment> getConflictingLabelSegmentsMap(SmithyPattern otherPattern) {
        Map<Segment, Segment> conflictingSegments = new LinkedHashMap<>();

        List<Segment> segments = getSegments();
        List<Segment> otherSegments = otherPattern.getSegments();
        int minSize = Math.min(segments.size(), otherSegments.size());
        for (int i = 0; i < minSize; i++) {
            Segment thisSegment = segments.get(i);
            Segment otherSegment = otherSegments.get(i);
            if (thisSegment.isLabel() != otherSegment.isLabel()) {
                // The segments conflict if one is a literal and the other
                // is a label.
                conflictingSegments.put(thisSegment, otherSegment);
            } else if (thisSegment.isGreedyLabel() != otherSegment.isGreedyLabel()) {
                // The segments conflict if a greedy label is introduced at
                // or before segments in the other pattern.
                conflictingSegments.put(thisSegment, otherSegment);
            } else if (!thisSegment.isLabel()) {
                // Both are literals. They can only conflict if they are the
                // same exact string.
                if (!thisSegment.getContent().equals(otherSegment.getContent())) {
                    return conflictingSegments;
                }
            }
        }

        return conflictingSegments;
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    private void checkForDuplicateLabels() {
        Set<String> labels = new HashSet<>();
        segments.forEach(segment -> {
            if (segment.isLabel() && !labels.add(segment.getContent().toLowerCase(Locale.US))) {
                throw new InvalidPatternException(format("Label `%s` is defined more than once in pattern: %s",
                        segment.getContent(),
                        pattern));
            }
        });
    }

    /**
     * @return Returns a builder used to create a SmithyPattern.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a SmithyPattern.
     */
    public static final class Builder {
        private boolean allowsGreedyLabels = true;
        private String pattern;
        private List<Segment> segments;

        private Builder() {}

        public Builder allowsGreedyLabels(boolean allowsGreedyLabels) {
            this.allowsGreedyLabels = allowsGreedyLabels;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder segments(List<Segment> segments) {
            this.segments = segments;
            return this;
        }

        public SmithyPattern build() {
            return new SmithyPattern(this);
        }
    }

    /**
     * Segment within a SmithyPattern.
     */
    public static final class Segment {

        public enum Type {
            LITERAL, LABEL, GREEDY_LABEL
        }

        private final String asString;
        private final String content;
        private final Type segmentType;

        public Segment(String content, Type segmentType) {
            this(content, segmentType, null);
        }

        public Segment(String content, Type segmentType, Integer offset) {
            this.content = Objects.requireNonNull(content);
            this.segmentType = segmentType;

            checkForInvalidContents(offset);

            if (segmentType == Type.GREEDY_LABEL) {
                asString = "{" + content + "+}";
            } else if (segmentType == Type.LABEL) {
                asString = "{" + content + "}";
            } else {
                asString = content;
            }
        }

        private void checkForInvalidContents(Integer offset) {
            String offsetString = "";
            if (offset != null) {
                offsetString += " at index " + offset;
            }
            if (segmentType == Type.LITERAL) {
                if (content.isEmpty()) {
                    throw new InvalidPatternException("Segments must not be empty" + offsetString);
                } else if (content.contains("{") || content.contains("}")) {
                    throw new InvalidPatternException(
                            "Literal segments must not contain `{` or `}` characters. Found segment `"
                                    + content + "`" + offsetString);
                }
            } else if (content.isEmpty()) {
                throw new InvalidPatternException("Empty label declaration in pattern" + offsetString + ".");
            } else if (!ShapeId.isValidIdentifier(content)) {
                throw new InvalidPatternException(
                        "Invalid label name in pattern: '" + content + "'" + offsetString
                                + ". Labels must contain value identifiers.");
            }
        }

        /**
         * Parse a segment from the given offset.
         *
         * @param content Content of the segment.
         * @param offset Character offset where the segment starts in the containing pattern.
         * @return Returns the created segment.
         * @throws InvalidPatternException if the segment is invalid.
         */
        public static Segment parse(String content, int offset) {
            if (content.length() >= 2 && content.charAt(0) == '{' && content.charAt(content.length() - 1) == '}') {
                Type labelType = content.charAt(content.length() - 2) == '+' ? Type.GREEDY_LABEL : Type.LABEL;
                content = labelType == Type.GREEDY_LABEL
                        ? content.substring(1, content.length() - 2)
                        : content.substring(1, content.length() - 1);
                return new Segment(content, labelType, offset);
            } else {
                return new Segment(content, Type.LITERAL, offset);
            }
        }

        /**
         * Get the content of the segment.
         *
         * <p>The return value contains the segment in its entirety for
         * non-labels, and the label name for both labels and greedy labels.
         * For example, given a segment of "{label+}", the return value of
         * getContent would be "label".
         *
         * @return Content of the segment.
         */
        public String getContent() {
            return content;
        }

        /**
         * @return True if the segment is a non-label literal.
         */
        public boolean isLiteral() {
            return segmentType == Type.LITERAL;
        }

        /**
         * @return True if the segment is a label regardless of whether is greedy or not.
         */
        public boolean isLabel() {
            return segmentType != Type.LITERAL;
        }

        /**
         * @return True if the segment is a non-greedy label.
         */
        public boolean isNonGreedyLabel() {
            return segmentType == Type.LABEL;
        }

        /**
         * @return True if the segment is a greedy label.
         */
        public boolean isGreedyLabel() {
            return segmentType == Type.GREEDY_LABEL;
        }

        /**
         * Get the segment as a literal value to be used in a pattern.
         *
         * <p>Unlike the result of {@link #getContent}, the return value
         * of {@code toString} includes braces for labels and "+" for
         * greedy labels.
         *
         * @return The literal segment.
         */
        @Override
        public String toString() {
            return asString;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Segment && asString.equals(((Segment) other).asString);
        }

        @Override
        public int hashCode() {
            return asString.hashCode();
        }
    }
}
