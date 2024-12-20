/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.RuntimeType;

public class LiteralExpressionTest {
    @Test
    public void containsNullValues() {
        LiteralExpression node = new LiteralExpression(null);

        assertThat(node.isNullValue(), is(true));
        assertThat(node.getType(), equalTo(RuntimeType.NULL));
    }

    @Test
    public void throwsWhenNotString() {
        LiteralExpression node = new LiteralExpression(10);

        Assertions.assertThrows(JmespathException.class, node::expectStringValue);
    }

    @Test
    public void getsAsString() {
        LiteralExpression node = new LiteralExpression("foo");

        node.expectStringValue();
        assertThat(node.isStringValue(), is(true));
        assertThat(node.isNullValue(), is(false)); // not null
        assertThat(node.getType(), equalTo(RuntimeType.STRING));
    }

    @Test
    public void throwsWhenNotArray() {
        LiteralExpression node = new LiteralExpression("hi");

        Assertions.assertThrows(JmespathException.class, node::expectArrayValue);
    }

    @Test
    public void getsAsArray() {
        LiteralExpression node = new LiteralExpression(Collections.emptyList());

        node.expectArrayValue();
        assertThat(node.isArrayValue(), is(true));
        assertThat(node.getType(), equalTo(RuntimeType.ARRAY));
    }

    @Test
    public void getsNegativeArrayIndex() {
        LiteralExpression node = new LiteralExpression(Arrays.asList(1, 2, 3));

        assertThat(node.getArrayIndex(-1).getValue(), equalTo(3));
        assertThat(node.getArrayIndex(-2).getValue(), equalTo(2));
        assertThat(node.getArrayIndex(-3).getValue(), equalTo(1));
        assertThat(node.getArrayIndex(-4).getValue(), equalTo(null));
    }

    @Test
    public void throwsWhenNotNumber() {
        LiteralExpression node = new LiteralExpression("hi");

        Assertions.assertThrows(JmespathException.class, node::expectNumberValue);
    }

    @Test
    public void getsAsNumber() {
        LiteralExpression node = new LiteralExpression(10);

        node.expectNumberValue();
        assertThat(node.isNumberValue(), is(true));
        assertThat(node.getType(), equalTo(RuntimeType.NUMBER));
    }

    @Test
    public void throwsWhenNotBoolean() {
        LiteralExpression node = new LiteralExpression("hi");

        Assertions.assertThrows(JmespathException.class, node::expectBooleanValue);
    }

    @Test
    public void getsAsBoolean() {
        LiteralExpression node = new LiteralExpression(true);

        node.expectBooleanValue();
        assertThat(node.isBooleanValue(), is(true));
        assertThat(node.getType(), equalTo(RuntimeType.BOOLEAN));
    }

    @Test
    public void getsAsBoxedBoolean() {
        LiteralExpression node = new LiteralExpression(true);

        node.expectBooleanValue();
        assertThat(node.isBooleanValue(), is(true));
    }

    @Test
    public void throwsWhenNotMap() {
        LiteralExpression node = new LiteralExpression("hi");

        Assertions.assertThrows(JmespathException.class, node::expectObjectValue);
    }

    @Test
    public void getsAsMap() {
        LiteralExpression node = new LiteralExpression(Collections.emptyMap());

        node.expectObjectValue();
        assertThat(node.isObjectValue(), is(true));
        assertThat(node.getType(), equalTo(RuntimeType.OBJECT));
    }

    @Test
    public void expressionReferenceTypeIsExpref() {
        assertThat(LiteralExpression.EXPREF.getType(), equalTo(RuntimeType.EXPRESSION));
    }

    @Test
    public void anyValueIsAnyType() {
        assertThat(LiteralExpression.ANY.getType(), equalTo(RuntimeType.ANY));
    }

    @Test
    public void determinesTruthyValues() {
        assertThat(new LiteralExpression(0).isTruthy(), is(true));
        assertThat(new LiteralExpression(1).isTruthy(), is(true));
        assertThat(new LiteralExpression(true).isTruthy(), is(true));
        assertThat(new LiteralExpression("hi").isTruthy(), is(true));
        assertThat(new LiteralExpression(Arrays.asList(1, 2)).isTruthy(), is(true));
        assertThat(new LiteralExpression(Collections.singletonMap("a", "b")).isTruthy(), is(true));

        assertThat(new LiteralExpression(false).isTruthy(), is(false));
        assertThat(new LiteralExpression("").isTruthy(), is(false));
        assertThat(new LiteralExpression(Collections.emptyList()).isTruthy(), is(false));
        assertThat(new LiteralExpression(Collections.emptyMap()).isTruthy(), is(false));
    }
}
