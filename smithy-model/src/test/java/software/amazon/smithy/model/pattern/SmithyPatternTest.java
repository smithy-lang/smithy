/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static software.amazon.smithy.model.pattern.SmithyPattern.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmithyPatternTest {

    private static List<Segment> parser(String target) {
        String[] unparsedSegments = target.split(java.util.regex.Pattern.quote("/"));
        List<Segment> segments = new ArrayList<>();
        int offset = 1;
        for (int i = 1; i < unparsedSegments.length; i++) {
            String segment = unparsedSegments[i];
            segments.add(Segment.parse(segment, offset));
            offset += segment.length();
        }
        return segments;
    }

    @Test
    public void parsesGreedyLabels() {
        String target = "/foo/baz/{bar+}";
        SmithyPattern pattern = SmithyPattern.builder()
                .segments(parser(target))
                .pattern(target)
                .build();

        assertThat(pattern.toString(), equalTo(target));
        assertThat(pattern.getGreedyLabel(),
                equalTo(Optional.of(
                        new Segment("bar", Segment.Type.GREEDY_LABEL))));
    }

    @Test
    public void computesHashAndEquals() {
        String target1 = "/foo";
        SmithyPattern pattern1 = SmithyPattern.builder()
                .segments(parser(target1))
                .pattern(target1)
                .build();
        String target2 = "/foo/{baz+}/";
        SmithyPattern pattern2 = SmithyPattern.builder()
                .segments(parser(target2))
                .pattern(target2)
                .build();

        assertThat(pattern1, equalTo(pattern1));
        assertThat(pattern1, not(equalTo(pattern2)));
        assertThat(pattern1.hashCode(), is(pattern1.hashCode()));
        assertThat(pattern1.hashCode(), not(pattern2.hashCode()));
        assertThat(pattern2, equalTo(pattern2));
        assertThat(pattern2.hashCode(), is(pattern2.hashCode()));
    }

    @Test
    public void labelsAreCaseInsensitive() {
        String target = "/foo/{baz}";
        SmithyPattern pattern = SmithyPattern.builder()
                .segments(parser(target))
                .pattern(target)
                .build();
        Segment segment = new Segment("baz", Segment.Type.LABEL);

        assertThat(pattern.getLabel("baz"), is(Optional.of(segment)));
        assertThat(pattern.getLabel("BAZ"), is(Optional.of(segment)));
    }

    @Test
    public void labelsMustNotIncludeEmptySegments() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            String target = "//baz";
            SmithyPattern.builder()
                    .segments(parser(target))
                    .pattern(target)
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Segments must not be empty at index 1"));
    }

    @Test
    public void labelsMustNotBeRepeated() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            String target = "/{foo}/{Foo}";
            SmithyPattern.builder()
                    .segments(parser(target))
                    .pattern(target)
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Label `Foo` is defined more than once"));
    }

    @Test
    public void restrictsGreedyLabels() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            String target = "/foo/{baz+}";
            SmithyPattern.builder()
                    .allowsGreedyLabels(false)
                    .segments(parser(target))
                    .pattern(target)
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Pattern must not contain a greedy label"));
    }

    @Test
    public void noEmptyLabels() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            String target = "/a/{}";
            SmithyPattern.builder()
                    .segments(parser(target))
                    .pattern(target)
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Empty label declaration in pattern at index 2"));
    }

    @Test
    public void labelsMustMatchRegex() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            String target = "/{!}";
            SmithyPattern.builder()
                    .segments(parser(target))
                    .pattern(target)
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Invalid label name"));
        assertThat(thrown.getMessage(), containsString("at index 1"));
    }

    @Test
    public void labelsMustSpanEntireSegment() {
        Throwable thrown = Assertions.assertThrows(InvalidPatternException.class, () -> {
            String target = "/{foo}baz";
            SmithyPattern.builder()
                    .segments(parser(target))
                    .pattern(target)
                    .build();
        });

        assertThat(thrown.getMessage(), containsString("Literal segments must not contain"));
        assertThat(thrown.getMessage(), containsString("at index 1"));
    }
}
