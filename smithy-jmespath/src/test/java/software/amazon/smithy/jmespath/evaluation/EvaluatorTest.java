/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

public class EvaluatorTest {
    @Test
    public void sliceWithStartOutOfBoundsIsEmpty() {
        LiteralExpression array = new LiteralExpression(Arrays.asList(0, 1, 2));

        assertThat(JmespathExpression.parse("[5:]").evaluate(array).expectArrayValue(), empty());
        assertThat(JmespathExpression.parse("[-5::-1]").evaluate(array).expectArrayValue(), empty());

        LiteralExpression emptyArray = new LiteralExpression(Collections.emptyList());
        assertThat(JmespathExpression.parse("[0:]").evaluate(emptyArray).expectArrayValue(), empty());
    }
}
