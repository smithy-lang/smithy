/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;

public class ConstrainShapesTraitTest {
    @Test
    public void convertsToNode() {
        SourceLocation s = new SourceLocation("foo.xml");
        ConstrainShapesTrait trait1 = ConstrainShapesTrait.builder()
                .sourceLocation(s)
                .putDefinition("hi", new ConstrainShapesTrait.Definition(Selector.parse("*"), "Error!"))
                .putDefinition("hi", new ConstrainShapesTrait.Definition(Selector.parse("string"),
                                                                         "Warning!",
                                                                         Severity.WARNING))
                .build();

        ConstrainShapesTrait.Provider p = new ConstrainShapesTrait.Provider();

        assertThat(p.createTrait(ShapeId.from("com.foo#Example"), trait1.toNode()), equalTo(trait1));
    }

    @Test
    public void convertsToBuilder() {
        SourceLocation s = new SourceLocation("foo.xml");
        ConstrainShapesTrait trait1 = ConstrainShapesTrait.builder()
                .sourceLocation(s)
                .putDefinition("hi", new ConstrainShapesTrait.Definition(Selector.parse("*"),
                                                                         "Error!",
                                                                         Severity.DANGER))
                .build();

        assertThat(trait1.toBuilder().build(), equalTo(trait1));
    }
}
