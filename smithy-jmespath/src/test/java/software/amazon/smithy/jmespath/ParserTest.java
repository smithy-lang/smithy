/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.ComparatorType;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;

public class ParserTest {
    @Test
    public void throwsOnInvalidNudToken() {
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> JmespathExpression.parse("|| a"));

        assertThat(e.getMessage(), containsString("but found '||'"));
    }

    @Test
    public void parsesNudField() {
        assertThat(JmespathExpression.parse("foo"), equalTo(new FieldExpression("foo")));
    }

    @Test
    public void parsesFunctionExpression() {
        assertThat(JmespathExpression.parse("length(@)"),
                equalTo(
                        new FunctionExpression("length", Collections.singletonList(new CurrentExpression()))));
    }

    @Test
    public void parsesFunctionWithMultipleArguments() {
        assertThat(JmespathExpression.parse("starts_with(@, 'foo')"),
                equalTo(
                        new FunctionExpression("starts_with",
                                Arrays.asList(
                                        new CurrentExpression(),
                                        new LiteralExpression("foo")))));
    }

    @Test
    public void detectsIllegalTrailingCommaInFunctionExpression() {
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> JmespathExpression.parse("lenght(@,)"));

        assertThat(e.getMessage(), containsString("Invalid token after ',': ')'"));
    }

    @Test
    public void parsesNudWildcardIndex() {
        assertThat(JmespathExpression.parse("[*]"),
                equalTo(
                        new ProjectionExpression(
                                new CurrentExpression(),
                                new CurrentExpression())));
    }

    @Test
    public void parsesNudStar() {
        assertThat(JmespathExpression.parse("*"),
                equalTo(
                        new ObjectProjectionExpression(
                                new CurrentExpression(),
                                new CurrentExpression())));
    }

    @Test
    public void parsesNudLiteral() {
        assertThat(JmespathExpression.parse("`true`"), equalTo(new LiteralExpression(true)));
    }

    @Test
    public void detectsTrailingLiteralTick() {
        Assertions.assertThrows(JmespathException.class, () -> JmespathExpression.parse("`true``"));
    }

    @Test
    public void parsesNudIndex() {
        assertThat(JmespathExpression.parse("[1]"), equalTo(new IndexExpression(1)));
    }

    @Test
    public void parsesNudFlatten() {
        assertThat(JmespathExpression.parse("[].foo"),
                equalTo(
                        new ProjectionExpression(
                                new FlattenExpression(new CurrentExpression()),
                                new FieldExpression("foo"))));
    }

    @Test
    public void parsesNudMultiSelectList() {
        assertThat(JmespathExpression.parse("[foo, bar]"),
                equalTo(
                        new MultiSelectListExpression(Arrays.asList(
                                new FieldExpression("foo"),
                                new FieldExpression("bar")))));
    }

    @Test
    public void detectsIllegalTrailingCommaInNudMultiSelectList() {
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> JmespathExpression.parse("[foo,]"));

        assertThat(e.getMessage(), containsString("Invalid token after ',': ']'"));
    }

    @Test
    public void parsesNudMultiSelectHash() {
        Map<String, JmespathExpression> expressionMap = new LinkedHashMap<>();
        expressionMap.put("foo", new FieldExpression("bar"));
        expressionMap.put("baz", new Subexpression(new FieldExpression("bam"), new FieldExpression("boo")));

        assertThat(JmespathExpression.parse("{foo: bar, baz: bam.boo}"),
                equalTo(
                        new MultiSelectHashExpression(expressionMap)));
    }

    @Test
    public void parsesNudAmpersand() {
        assertThat(JmespathExpression.parse("&foo[1]"),
                equalTo(
                        new ExpressionTypeExpression(
                                new Subexpression(
                                        new FieldExpression("foo"),
                                        new IndexExpression(1)))));
    }

    @Test
    public void parsesNudNot() {
        assertThat(JmespathExpression.parse("!foo[1]"),
                equalTo(
                        new NotExpression(
                                new Subexpression(
                                        new FieldExpression("foo"),
                                        new IndexExpression(1)))));
    }

    @Test
    public void parsesNudFilter() {
        assertThat(JmespathExpression.parse("[?foo == `true`]"),
                equalTo(
                        new FilterProjectionExpression(
                                new CurrentExpression(),
                                new ComparatorExpression(
                                        ComparatorType.EQUAL,
                                        new FieldExpression("foo"),
                                        new LiteralExpression(true)),
                                new CurrentExpression())));
    }

    @Test
    public void parsesNudFilterWithComparators() {
        for (ComparatorType type : ComparatorType.values()) {
            assertThat(JmespathExpression.parse("[?foo " + type + " `true`]"),
                    equalTo(
                            new FilterProjectionExpression(
                                    new CurrentExpression(),
                                    new ComparatorExpression(
                                            type,
                                            new FieldExpression("foo"),
                                            new LiteralExpression(true)),
                                    new CurrentExpression())));
        }
    }

    @Test
    public void parsesNudLparen() {
        assertThat(JmespathExpression.parse("(foo | bar)"),
                equalTo(
                        new Subexpression(
                                new FieldExpression("foo"),
                                new FieldExpression("bar"))));
    }

    @Test
    public void parsesSubexpressions() {
        assertThat(JmespathExpression.parse("foo.bar.baz"),
                equalTo(
                        new Subexpression(
                                new Subexpression(
                                        new FieldExpression("foo"),
                                        new FieldExpression("bar")),
                                new FieldExpression("baz"))));
    }

    @Test
    public void parsesSubexpressionsWithQuotedIdentifier() {
        assertThat(JmespathExpression.parse("foo.\"1\""),
                equalTo(
                        new Subexpression(new FieldExpression("foo"), new FieldExpression("1"))));
    }

    @Test
    public void parsesMultiSelectHashAfterDot() {
        assertThat(JmespathExpression.parse("foo.{bar: baz}"),
                equalTo(
                        new Subexpression(
                                new FieldExpression("foo"),
                                new MultiSelectHashExpression(
                                        Collections.singletonMap("bar", new FieldExpression("baz"))))));
    }

    @Test
    public void parsesMultiSelectListAfterDot() {
        assertThat(JmespathExpression.parse("foo.[bar]"),
                equalTo(
                        new Subexpression(
                                new FieldExpression("foo"),
                                new MultiSelectListExpression(
                                        Collections.singletonList(new FieldExpression("bar"))))));
    }

    @Test
    public void requiresExpressionToFollowDot() {
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> JmespathExpression.parse("foo."));

        assertThat(e.getMessage(), containsString("but found EOF"));
    }

    @Test
    public void parsesPipeExpressions() {
        assertThat(JmespathExpression.parse("foo.bar.baz"),
                equalTo(
                        new Subexpression(
                                new Subexpression(
                                        new FieldExpression("foo"),
                                        new FieldExpression("bar")),
                                new FieldExpression("baz"))));
    }

    @Test
    public void parsesOrExpressions() {
        assertThat(JmespathExpression.parse("foo || bar || baz"),
                equalTo(
                        new OrExpression(
                                new OrExpression(
                                        new FieldExpression("foo"),
                                        new FieldExpression("bar")),
                                new FieldExpression("baz"))));
    }

    @Test
    public void parsesAndExpressions() {
        assertThat(JmespathExpression.parse("foo && bar && baz"),
                equalTo(
                        new AndExpression(
                                new AndExpression(
                                        new FieldExpression("foo"),
                                        new FieldExpression("bar")),
                                new FieldExpression("baz"))));
    }

    @Test
    public void parsesProjections() {
        assertThat(JmespathExpression.parse("foo.*.bar[*] || baz"),
                equalTo(
                        new OrExpression(
                                new ObjectProjectionExpression(
                                        new FieldExpression("foo"),
                                        new ProjectionExpression(
                                                new FieldExpression("bar"),
                                                new CurrentExpression())),
                                new FieldExpression("baz"))));
    }

    @Test
    public void parsesLedFlattenProjection() {
        assertThat(JmespathExpression.parse("a[].b"),
                equalTo(
                        new ProjectionExpression(
                                new FlattenExpression(new FieldExpression("a")),
                                new FieldExpression("b"))));
    }

    @Test
    public void parsesLedFilterProjection() {
        assertThat(JmespathExpression.parse("a[?b > c].d"),
                equalTo(
                        new FilterProjectionExpression(
                                new FieldExpression("a"),
                                new ComparatorExpression(
                                        ComparatorType.GREATER_THAN,
                                        new FieldExpression("b"),
                                        new FieldExpression("c")),
                                new FieldExpression("d"))));
    }

    @Test
    public void parsesLedProjectionIntoIndex() {
        assertThat(JmespathExpression.parse("a.*[1].b"),
                equalTo(
                        new ObjectProjectionExpression(
                                new FieldExpression("a"),
                                new Subexpression(
                                        new IndexExpression(1),
                                        new FieldExpression("b")))));
    }

    @Test
    public void parsesLedProjectionIntoFilterProjection() {
        assertThat(JmespathExpression.parse("a.*[?foo == bar]"),
                equalTo(
                        new ObjectProjectionExpression(
                                new FieldExpression("a"),
                                new FilterProjectionExpression(
                                        new CurrentExpression(),
                                        new ComparatorExpression(
                                                ComparatorType.EQUAL,
                                                new FieldExpression("foo"),
                                                new FieldExpression("bar")),
                                        new CurrentExpression()))));
    }

    @Test
    public void validatesValidLedProjectionRhs() {
        JmespathException e = Assertions.assertThrows(
                JmespathException.class,
                () -> JmespathExpression.parse("a.**"));

        assertThat(e.getMessage(), containsString("Invalid projection"));
    }

    @Test
    public void parsesSlices() {
        assertThat(JmespathExpression.parse("[1:3].foo"),
                equalTo(
                        new ProjectionExpression(
                                new SliceExpression(1, 3, 1),
                                new FieldExpression("foo"))));
    }

    @Test
    public void parsesSlicesWithStep() {
        assertThat(JmespathExpression.parse("[5:10:2]"),
                equalTo(
                        new ProjectionExpression(
                                new SliceExpression(5, 10, 2),
                                new CurrentExpression())));
    }

    @Test
    public void parsesSlicesWithNegativeStep() {
        assertThat(JmespathExpression.parse("[10:5:-1]"),
                equalTo(
                        new ProjectionExpression(
                                new SliceExpression(10, 5, -1),
                                new CurrentExpression())));
    }

    @Test
    public void parsesSlicesWithStepAndNoStop() {
        assertThat(JmespathExpression.parse("[10::5]"),
                equalTo(
                        new ProjectionExpression(
                                new SliceExpression(10, null, 5),
                                new CurrentExpression())));
    }

    @Test
    public void parsesSlicesWithStartAndNoStepOrEnd() {
        assertThat(JmespathExpression.parse("[10::]"),
                equalTo(
                        new ProjectionExpression(
                                new SliceExpression(10, null, 1),
                                new CurrentExpression())));

        assertThat(JmespathExpression.parse("[10:]"),
                equalTo(
                        new ProjectionExpression(
                                new SliceExpression(10, null, 1),
                                new CurrentExpression())));
    }

    @Test
    public void validatesTooManyColonsInSlice() {
        Assertions.assertThrows(JmespathException.class, () -> JmespathExpression.parse("[10:::]"));
    }
}
