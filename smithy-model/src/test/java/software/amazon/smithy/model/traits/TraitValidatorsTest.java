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

public class TraitValidatorsTest {
    @Test
    public void convertsToNode() {
        SourceLocation s = new SourceLocation("foo.xml");
        TraitValidatorsTrait trait1 = TraitValidatorsTrait.builder()
                .sourceLocation(s)
                .putValidator("hi", new TraitValidatorsTrait.Validator(Selector.parse("*"), "Error!"))
                .putValidator("hi", new TraitValidatorsTrait.Validator(Selector.parse("string"),
                                                                        "Warning!",
                                                                        Severity.WARNING))
                .build();

        TraitValidatorsTrait.Provider p = new TraitValidatorsTrait.Provider();

        assertThat(p.createTrait(ShapeId.from("com.foo#Example"), trait1.toNode()), equalTo(trait1));
    }

    @Test
    public void convertsToBuilder() {
        SourceLocation s = new SourceLocation("foo.xml");
        TraitValidatorsTrait trait1 = TraitValidatorsTrait.builder()
                .sourceLocation(s)
                .putValidator("hi", new TraitValidatorsTrait.Validator(Selector.parse("*"),
                                                                        "Error!",
                                                                        Severity.DANGER))
                .build();

        assertThat(trait1.toBuilder().build(), equalTo(trait1));
    }
}
