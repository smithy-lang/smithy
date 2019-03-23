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

package software.amazon.smithy.model.shapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShapeVisitorBuilderTest {

    @Test
    public void throwsForUnexpectedCase() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            ShapeVisitor<String> cases = Shape.<String>visitor().build();
            BlobShape.builder().id("ns.foo#Bar").build().accept(cases);
        });
    }

    @Test
    public void throwsSpecificExceptionForUnexpectedCase() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            UnsupportedOperationException e = new UnsupportedOperationException();
            ShapeVisitor<String> cases = Shape.<String>visitor().orElseThrow(e);
            BlobShape.builder().id("ns.foo#Bar").build().accept(cases);
        });
    }

    @Test
    public void orElseReturnGetsEveryNonSpecificCase() {
        ShapeVisitor<Integer> cases = Shape.<Integer>visitor().when(BlobShape.class, shape -> 99).orElse(1);

        assertEquals(99, BlobShape.builder().id("ns.foo#Bar").build().accept(cases).intValue());
        assertEquals(1, BooleanShape.builder().id("ns.foo#Bar").build().accept(cases).intValue());
    }

    @Test
    public void orElseCallGetsEveryNonSpecificCase() {
        ShapeVisitor<Integer> cases = Shape.<Integer>visitor()
                .when(BlobShape.class, shape -> 99)
                .orElseGet(shape -> 1);

        assertEquals(99, BlobShape.builder().id("ns.foo#Bar").build().accept(cases).intValue());
        assertEquals(1, BooleanShape.builder().id("ns.foo#Bar").build().accept(cases).intValue());
    }
}
