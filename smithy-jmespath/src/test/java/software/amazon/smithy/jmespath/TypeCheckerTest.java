/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class TypeCheckerTest {

    private List<String> check(String expr) {
        return JmespathExpression.parse(expr)
                .lint()
                .getProblems()
                .stream()
                .map(ExpressionProblem::toString)
                .collect(Collectors.toList());
    }

    @Test
    public void detectsInvalidArrayProjectionLhs() {
        assertThat(check("{foo: `true`} | [*]"), contains("[DANGER] Array projection performed on object (1:18)"));
    }

    @Test
    public void detectsInvalidObjectProjectionLhs() {
        assertThat(check("[foo] | *"), contains("[DANGER] Object projection performed on array (1:9)"));
    }

    @Test
    public void detectsGettingFieldFromArray() {
        assertThat(check("[foo].baz"), contains("[DANGER] Object field 'baz' extraction performed on array (1:7)"));
    }

    @Test
    public void detectsFlatteningNonArray() {
        assertThat(check("`true` | []"), contains("[DANGER] Array flatten performed on boolean (1:10)"));
    }

    @Test
    public void detectsBadFlattenExpression() {
        assertThat(check("[].[`true` | foo]"),
                contains("[DANGER] Object field 'foo' extraction performed on boolean (1:14)"));
    }

    @Test
    public void detectsInvalidExpressionsInMultiSelectLists() {
        assertThat(check("`true` | [foo, [1], {bar: foo}]"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:11)",
                        "[DANGER] Array index '1' extraction performed on boolean (1:17)",
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:27)"));
    }

    @Test
    public void detectsInvalidExpressionsInMultiSelectHash() {
        assertThat(check("`true` | {foo: [1], bar: foo}"),
                containsInAnyOrder(
                        "[DANGER] Array index '1' extraction performed on boolean (1:17)",
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:26)"));
    }

    @Test
    public void detectsInvalidComparisonExpressions() {
        assertThat(check("`true` | foo == [1]"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:10)",
                        "[DANGER] Array index '1' extraction performed on boolean (1:18)"));
    }

    @Test
    public void detectsInvalidExpressionReferences() {
        assertThat(check("&(`true` | foo)"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:12)"));
    }

    @Test
    public void detectsValidIndex() {
        assertThat(check("`[1]` | [1]"), empty());
    }

    @Test
    public void detectsValidField() {
        assertThat(check("`{\"foo\": true}` | foo"), empty());
    }

    @Test
    public void detectsInvalidAndLhs() {
        assertThat(check("(`true` | foo) && baz"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:11)"));
    }

    @Test
    public void detectsInvalidAndRhs() {
        assertThat(check("foo && (`true` | foo)"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:18)"));
    }

    @Test
    public void detectsInvalidOrLhs() {
        assertThat(check("(`true` | foo) || baz"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:11)"));
    }

    @Test
    public void detectsInvalidOrRhs() {
        assertThat(check("foo || (`true` | foo)"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:18)"));
    }

    @Test
    public void detectsInvalidNot() {
        assertThat(check("`true` | !foo"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' extraction performed on boolean (1:11)"));
    }

    @Test
    public void detectsValidNot() {
        assertThat(check("`{\"foo\": true}` | !foo"), empty());
    }

    @Test
    public void detectsMissingProperty() {
        assertThat(check("`{}` | foo"),
                containsInAnyOrder(
                        "[DANGER] Object field 'foo' does not exist in object with properties [] (1:8)"));
    }

    @Test
    public void detectsInvalidSlice() {
        assertThat(check("`true` | [1:10]"),
                containsInAnyOrder(
                        "[DANGER] Slice performed on boolean (1:11)"));
    }

    @Test
    public void detectsValidSlice() {
        assertThat(check("`[]` | [1:10]"), empty());
    }

    @Test
    public void detectsInvalidFilterProjectionLhs() {
        assertThat(check("`true` | [?baz == bar]"),
                containsInAnyOrder(
                        "[DANGER] Filter projection performed on boolean (1:19)"));
    }

    @Test
    public void detectsInvalidFilterProjectionRhs() {
        assertThat(check("[?baz == bar].[`true` | bam]"),
                containsInAnyOrder(
                        "[DANGER] Object field 'bam' extraction performed on boolean (1:25)"));
    }

    @Test
    public void detectsInvalidFilterProjectionComparison() {
        assertThat(check("[?(`true` | baz) == bar]"),
                containsInAnyOrder(
                        "[DANGER] Object field 'baz' extraction performed on boolean (1:13)"));
    }

    @Test
    public void detectsInvalidFunction() {
        assertThat(check("does_not_exist(@)"), containsInAnyOrder("[ERROR] Unknown function: does_not_exist (1:1)"));
    }

    @Test
    public void detectsInvalidFunctionArity() {
        assertThat(check("length(@, @)"),
                containsInAnyOrder(
                        "[ERROR] length function expected 1 arguments, but was given 2 (1:1)"));
    }

    @Test
    public void detectsSuccessfulAnyArgument() {
        assertThat(check("length(@)"), empty());
        assertThat(check("starts_with(@, @)"), empty());
        assertThat(check("ends_with(@, @)"), empty());
        assertThat(check("avg(@)"), empty());
    }

    @Test
    public void detectsSuccessfulStaticArguments() {
        assertThat(check("length('foo')"), empty());
        assertThat(check("starts_with('foo', 'f')"), empty());
        assertThat(check("ends_with('foo', 'o')"), empty());
        assertThat(check("avg(`[10, 15]`)"), empty());
    }

    @Test
    public void detectsInvalidStaticArguments() {
        assertThat(check("length(`true`)"),
                containsInAnyOrder(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:8)"));
        assertThat(check("starts_with(`true`, `false`)"),
                containsInAnyOrder(
                        "[ERROR] starts_with function argument 0 error: Expected argument to be string, but found boolean (1:13)",
                        "[ERROR] starts_with function argument 1 error: Expected argument to be string, but found boolean (1:21)"));
        assertThat(check("avg(`[\"a\", false]`)"),
                containsInAnyOrder(
                        "[ERROR] avg function argument 0 error: Expected an array of number, but found string at index 0 (1:5)"));
    }

    @Test
    public void detectsInvalidArgumentThatExpectedArray() {
        assertThat(check("avg(`true`)"),
                containsInAnyOrder(
                        "[ERROR] avg function argument 0 error: Expected argument to be an array, but found boolean (1:5)"));
    }

    @Test
    public void detectsInvalidUseOfStaticObjects() {
        assertThat(check("{foo: `true`}.length(foo)"),
                containsInAnyOrder(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:22)"));
        assertThat(check("{foo: `true`} | floor(@)"),
                containsInAnyOrder(
                        "[ERROR] floor function argument 0 error: Expected argument to be number, but found object (1:23)"));
    }

    @Test
    public void detectsWhenTooFewArgumentsAreGiven() {
        assertThat(check("length()"),
                containsInAnyOrder(
                        "[ERROR] length function expected 1 arguments, but was given 0 (1:1)"));
    }

    @Test
    public void parsesVariadicFunctionsProperly() {
        assertThat(check("not_null(@, @, @, @, @)"), empty());
    }

    @Test
    public void unknownOrResultIsPermittedAsAny() {
        assertThat(check("length(a || b)"), empty());
        assertThat(check("length(a || `true`)"), empty());
    }

    @Test
    public void unknownAndResultIsPermittedAsAny() {
        assertThat(check("length(a && b)"), empty());
    }

    @Test
    public void detectsInvalidAndResult() {
        assertThat(check("length(a && `true`)"),
                contains(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:10)"));
    }

    @Test
    public void andForwardsTruthyValuesThrough() {
        assertThat(check("`true` && `true` == `true`"), empty());
    }

    @Test
    public void flattenFiltersOutNullValues() {
        assertThat(check("`[null, \"hello\", null, \"goodbye\"]`[] | length([0]) || length([1])"), empty());
    }

    @Test
    public void flattenFiltersOutNullValuesAndMergesArrays() {
        assertThat(check("`[null, [\"hello\"], null, [\"goodbye\"]]`[] | length([0]) || length([1])"), empty());
    }

    @Test
    public void canDetectInvalidIndexResultsStatically() {
        assertThat(check("`[null, true]` | length([0]) || length([1])"),
                containsInAnyOrder(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:26)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:41)"));
    }

    @Test
    public void analyzesValidObjectProjectionRhs() {
        assertThat(check("`{\"foo\": [\"hi\"]}`.*.nope"),
                containsInAnyOrder(
                        "[DANGER] Object field 'nope' extraction performed on array (1:21)"));
    }

    @Test
    public void detectsInvalidObjectProjectionRhs() {
        assertThat(check("`{\"foo\": [true]}`.*[0].length(@)"),
                containsInAnyOrder(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:31)"));
    }

    @Test
    public void detectsInvalidFilterProjectionRhsFunction() {
        assertThat(check("`[{\"foo\": true}, {\"foo\": false}]`[?foo == `true`].foo | length([0])"),
                containsInAnyOrder(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:65)"));
    }

    @Test
    public void comparesBooleans() {
        assertThat(check("`[{\"foo\": true}, {\"foo\": false}]`[?foo == `true`] | length(to_string([0]))"), empty());
        assertThat(check("`[{\"foo\": true}, {\"foo\": false}]`[?foo != `true`] | length(to_string([0]))"), empty());
        assertThat(check("`[{\"foo\": true}, {\"foo\": false}]`[?foo < `true`] | length([0])"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<' for boolean (1:42)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:60)"));
    }

    @Test
    public void comparesStrings() {
        assertThat(check("`[{\"foo\": \"a\"}, {\"foo\": \"b\"}]`[?foo == 'a'] | length(to_string([0]))"), empty());
        assertThat(check("`[{\"foo\": \"a\"}, {\"foo\": \"b\"}]`[?foo != 'a'] | length(to_string([0]))"), empty());
        assertThat(check("`[{\"foo\": \"a\"}, {\"foo\": \"b\"}]`[?foo > 'a'] | length([0])"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>' for string (1:39)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:54)"));
    }

    @Test
    public void comparesNumbers() {
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo == `1`].foo | abs([0])"), empty());
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo != `1`].foo | abs([0])"), empty());
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo > `1`].foo | abs([0])"), empty());
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo >= `1`].foo | abs([0])"), empty());
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo < `2`].foo | abs([0])"), empty());
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo <= `2`].foo | abs([0])"), empty());
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo < `0`].foo | abs([0])"),
                containsInAnyOrder(
                        "[ERROR] abs function argument 0 error: Expected argument to be number, but found null (1:51)"));
    }

    @Test
    public void comparisonsBetweenIncompatibleTypesIsFalse() {
        assertThat(check("`[{\"foo\": 1}, {\"foo\": 2}]`[?foo == `true`].foo | abs([0])"),
                containsInAnyOrder(
                        "[ERROR] abs function argument 0 error: Expected argument to be number, but found null (1:55)"));
    }

    @Test
    public void comparesNulls() {
        assertThat(check("length(`null` == `null` && 'hi')"), empty());
        assertThat(check("length(`null` != `null` || 'hi')"), empty());
        assertThat(check("length(`null` != `null` && 'hi')"),
                contains(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:25)"));
        assertThat(check("length(`null` > `null` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>' for null (1:17)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:24)"));
        assertThat(check("length(`null` >= `null` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>=' for null (1:18)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:25)"));
        assertThat(check("length(`null` < `null` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<' for null (1:17)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:24)"));
        assertThat(check("length(`null` <= `null` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<=' for null (1:18)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:25)"));
    }

    @Test
    public void cannotCompareExpref() {
        assertThat(check("(&foo) == (&foo)"), contains("[WARNING] Invalid comparator '==' for expression (1:11)"));
    }

    @Test
    public void comparesArrays() {
        assertThat(check("length(`[1,2]` == `[1,2]` && 'hi')"), empty());
        assertThat(check("length(`[1]` != `[1,2]` && 'hi')"), empty());
        assertThat(check("length(`[1]` != `[1]` && 'hi')"),
                contains(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:23)"));
        assertThat(check("length(`[1]` > `[2]` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>' for array (1:16)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:22)"));
        assertThat(check("length(`[1]` >= `[2]` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>=' for array (1:17)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:23)"));
        assertThat(check("length(`[1]` < `[2]` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<' for array (1:16)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:22)"));
        assertThat(check("length(`[1]` <= `[2]` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<=' for array (1:17)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:23)"));
    }

    @Test
    public void comparesObjects() {
        assertThat(check("length(`{}` == `{}` && 'hi')"), empty());
        assertThat(check("length(`{\"foo\":true}` != `{}` && 'hi')"), empty());
        assertThat(check("length(`[1]` != `[1]` && 'hi')"),
                contains(
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found boolean (1:23)"));
        assertThat(check("length(`{\"foo\":true}` > `{}` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>' for object (1:25)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:30)"));
        assertThat(check("length(`{\"foo\":true}` >= `{}` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '>=' for object (1:26)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:31)"));
        assertThat(check("length(`{\"foo\":true}` < `{}` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<' for object (1:25)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:30)"));
        assertThat(check("length(`{\"foo\":true}` <= `{}` && 'hi')"),
                containsInAnyOrder(
                        "[WARNING] Invalid comparator '<=' for object (1:26)",
                        "[ERROR] length function argument 0 error: Expected one of [string, array, object], but found null (1:31)"));
    }

    @Test
    public void falseyLhsIsReturnedFromAnd() {
        assertThat(check("ceil(`[]` && `0.9`)"),
                contains(
                        "[ERROR] ceil function argument 0 error: Expected argument to be number, but found array (1:11)"));
        assertThat(check("ceil(`{}` && `0.9`)"),
                contains(
                        "[ERROR] ceil function argument 0 error: Expected argument to be number, but found object (1:11)"));
        assertThat(check("ceil(`\"\"` && `0.9`)"),
                contains(
                        "[ERROR] ceil function argument 0 error: Expected argument to be number, but found string (1:11)"));
        assertThat(check("ceil(`false` && `0.9`)"),
                contains(
                        "[ERROR] ceil function argument 0 error: Expected argument to be number, but found boolean (1:14)"));
        assertThat(check("ceil(`null` && `0.9`)"),
                contains(
                        "[ERROR] ceil function argument 0 error: Expected argument to be number, but found null (1:13)"));
    }
}
