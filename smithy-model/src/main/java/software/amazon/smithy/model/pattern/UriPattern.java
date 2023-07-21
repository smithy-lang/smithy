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

package software.amazon.smithy.model.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.utils.Pair;

/**
 * Represents a URI pattern.
 *
 * <p>A URI pattern is an absolute path segment used to route to operations.
 * The pattern must start with "/", must not contain empty segments
 * (i.e., "//"), must not contain a fragment (i.e., "#"), and must not
 * end with "?".
 *
 * <p>Labels may appear in the pattern in the form of "{label}". Labels must
 * not be repeated, must make up an entire path segment (e.g., "/{foo}/baz"),
 * and the label name must match the regex "^[a-zA-Z0-9_]+$". No labels can
 * appear after the query string.
 *
 * <p>Greedy labels, a specialized type of label, may be specified using
 * "{label+}". Only a single greedy label may appear in a pattern, and it
 * must be the last label in a pattern.
 */
public final class UriPattern extends SmithyPattern {

    private final Map<String, String> queryLiterals;

    private UriPattern(Builder builder, Map<String, String> queryLiterals) {
        super(builder);
        this.queryLiterals = queryLiterals;
    }

    /**
     * Parse a URI pattern string into a UriPattern.
     *
     * <p>The provided value must match the origin-form request-target
     * grammar production in RFC 7230, section 5.3.1.
     *
     * @param uri URI pattern to parse.
     * @return Returns the parsed URI pattern.
     * @throws InvalidUriPatternException for invalid URI patterns.
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a>
     */
    public static UriPattern parse(String uri) {
        if (uri.endsWith("?")) {
            throw new InvalidUriPatternException("URI patterns must not end with '?'. Found " + uri);
        } else if (!uri.startsWith("/")) {
            throw new InvalidUriPatternException("URI pattern must start with '/'. Found " + uri);
        } else if (uri.contains("#")) {
            throw new InvalidUriPatternException("URI pattern must not contain a fragment. Found " + uri);
        }

        String[] parts = uri.split(java.util.regex.Pattern.quote("?"), 2);
        String[] unparsedSegments = parts[0].split(java.util.regex.Pattern.quote("/"));
        List<Segment> segments = new ArrayList<>();
        // Skip the first "/" segment, and thus assume offset of 1.
        int offset = 1;
        for (int i = 1; i < unparsedSegments.length; i++) {
            String segment = unparsedSegments[i];
            segments.add(Segment.parse(segment, offset));
            // Add one to account for `/`
            offset += segment.length() + 1;
        }

        Map<String, String> queryLiterals = new LinkedHashMap<>();
        // Parse the query literals outside of the general pattern
        if (parts.length == 2) {
            if (parts[1].contains("{") || parts[1].contains("}")) {
                throw new InvalidUriPatternException("URI labels must not appear in the query string. Found " + uri);
            }
            for (String kvp : parts[1].split(java.util.regex.Pattern.quote("&"))) {
                String[] parameterParts = kvp.split("=", 2);
                String actualKey = parameterParts[0];
                if (queryLiterals.containsKey(actualKey)) {
                    throw new InvalidUriPatternException("Literal query parameters must not be repeated: " + uri);
                }
                queryLiterals.put(actualKey, parameterParts.length == 2 ? parameterParts[1] : "");
            }
        }

        return new UriPattern(builder().pattern(uri).segments(segments), queryLiterals);
    }

    /**
     * Get an immutable map of query string literal key-value pairs.
     *
     * @return An immutable map of parsed query string literals.
     */
    public Map<String, String> getQueryLiterals() {
        return Collections.unmodifiableMap(queryLiterals);
    }

    /**
     * Gets a specific query string literal parameter value.
     *
     * @param parameter Case-sensitive name of the parameter to retrieve.
     * @return Returns the optionally found parameter value.
     */
    public Optional<String> getQueryLiteralValue(String parameter) {
        return Optional.ofNullable(queryLiterals.get(parameter));
    }

    /**
     * Determines if the pattern conflicts with another pattern.
     *
     * @param otherPattern SmithyPattern to check against.
     * @return Returns true if there is a conflict.
     */
    public boolean conflictsWith(UriPattern otherPattern) {
        if (!getConflictingLabelSegmentsMap(otherPattern).isEmpty()) {
            return true;
        }

        List<Segment> segments = getSegments();
        List<Segment> otherSegments = otherPattern.getSegments();

        // By now we know there are no label conflicts, so one uri has more
        // segments than the other then they don't conflict.
        if (segments.size() != otherSegments.size()) {
            return false;
        }

        // Now we need to check for the differences  in the static segments of the uri.
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            Segment otherSegment = otherSegments.get(i);
            // We've already checked for label conflicts, so we can skip them here.
            if (segment.isLabel() || otherSegment.isLabel()) {
                continue;
            }
            if (!segment.getContent().equals(otherSegment.getContent())) {
                return false;
            }
        }

        // At this point, the path portions are equivalent. If the query
        // string literals are the same, then the patterns conflict.
        return queryLiterals.equals(otherPattern.queryLiterals);
    }

    @Deprecated
    public List<Pair<Segment, Segment>> getConflictingLabelSegments(UriPattern otherPattern) {
        Map<Segment, Segment> conflictingSegments = getConflictingLabelSegmentsMap(otherPattern);
        return conflictingSegments.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UriPattern)) {
            return false;
        }
        UriPattern otherPattern = (UriPattern) other;
        return super.equals(other) && queryLiterals.equals(otherPattern.queryLiterals);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + queryLiterals.hashCode();
    }

}
