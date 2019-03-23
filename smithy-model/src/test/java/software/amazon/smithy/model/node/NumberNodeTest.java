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

package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;

public class NumberNodeTest {

    @Test
    public void zeroNodeIsEmpty() {
        NumberNode zero = Node.from(0);

        assertEquals(Integer.valueOf(0), zero.getValue());
        assertEquals(SourceLocation.none(), zero.getSourceLocation());
    }

    @Test
    public void getType() {
        NodeType type = Node.from(0).getType();

        assertEquals(NodeType.NUMBER, type);
        assertEquals("number", type.toString());
    }

    @Test
    public void isNumberNode() {
        NumberNode node = Node.from(0);

        assertEquals(false, node.isObjectNode());
        assertEquals(false, node.isArrayNode());
        assertEquals(false, node.isStringNode());
        assertEquals(true, node.isNumberNode());
        assertEquals(false, node.isBooleanNode());
        assertEquals(false, node.isNullNode());
    }

    @Test
    public void expectNumberNodeReturnsNumberNode() {
        Node node = Node.from(0);

        assertThat(node.expectNumberNode(), instanceOf(NumberNode.class));
    }

    @Test
    public void expectNumberNodeAcceptsErrorMessage() {
        Node node = Node.from(0);

        assertSame(node, node.expectNumberNode("does not raise"));
    }

    @Test
    public void hasSourceLocation() {
        SourceLocation loc = new SourceLocation("filename", 1, 10);
        NumberNode node = new NumberNode(0, loc);

        assertSame(loc, node.getSourceLocation());
    }

    @Test
    public void fromShort() {
        short number = 1;
        NumberNode node = Node.from(number);

        assertEquals(Short.valueOf(number), node.getValue());
    }

    @Test
    public void fromInteger() {
        int number = 1;
        NumberNode node = Node.from(number);

        assertEquals(Integer.valueOf(number), node.getValue());
    }

    @Test
    public void fromLong() {
        long number = 1;
        NumberNode node = Node.from(number);

        assertEquals(Long.valueOf(number), node.getValue());
    }

    @Test
    public void fromFloat() {
        float number = (float) 1.1;
        NumberNode node = Node.from(number);

        assertEquals(Float.valueOf(number), node.getValue());
    }

    @Test
    public void fromDouble() {
        double number = (float) 1.1;
        NumberNode node = Node.from(number);

        assertEquals(Double.valueOf(number), node.getValue());
    }

    @Test
    public void isNaturalNumber() {
        assertTrue((Node.from(Short.valueOf((short) 1))).isNaturalNumber());
        assertTrue((Node.from(Integer.valueOf(1))).isNaturalNumber());
        assertTrue((Node.from(Long.valueOf(1))).isNaturalNumber());
        assertFalse((Node.from(Float.valueOf((float) 1.0))).isNaturalNumber());
        assertFalse((Node.from(Double.valueOf(1.0))).isNaturalNumber());
    }

    @Test
    public void isFloatingPointNumber() {
        assertFalse((Node.from(Short.valueOf((short) 1))).isFloatingPointNumber());
        assertFalse((Node.from(Integer.valueOf(1))).isFloatingPointNumber());
        assertFalse((Node.from(Long.valueOf(1))).isFloatingPointNumber());
        assertTrue((Node.from(Float.valueOf((float) 1.0))).isFloatingPointNumber());
        assertTrue((Node.from(Double.valueOf(1.0))).isFloatingPointNumber());
    }

    @Test
    public void equalityAndHashCodeTest() {
        NumberNode a = Node.from(1);
        NumberNode b = Node.from(1);
        BooleanNode c = Node.from(false);

        assertTrue(a.equals(b));
        assertTrue(a.equals(a));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertTrue(a.hashCode() == b.hashCode());
        assertFalse(a.hashCode() == c.hashCode());
    }

    @Test
    public void convertsToNumberNode() {
        assertTrue(Node.from(10).asNumberNode().isPresent());
    }
}
