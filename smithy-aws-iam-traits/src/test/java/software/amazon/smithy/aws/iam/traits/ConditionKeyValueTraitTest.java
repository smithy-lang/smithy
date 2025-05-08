/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ConditionKeyValueTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("condition-key-value.smithy"))
                .assemble()
                .unwrap();

        ServiceShape service = result.expectShape(ShapeId.from("smithy.example#MyService"), ServiceShape.class);
        Shape shape = result.expectShape(ShapeId.from("smithy.example#EchoInput$id1"));
        ConditionKeyValueTrait trait = shape.expectTrait(ConditionKeyValueTrait.class);
        assertThat(trait.getValue(), equalTo("smithy:ActionContextKey1"));
        assertThat(trait.resolveConditionKey(service), equalTo("smithy:ActionContextKey1"));

        Shape shape2 = result.expectShape(ShapeId.from("smithy.example#EchoInput$id2"));
        ConditionKeyValueTrait trait2 = shape2.expectTrait(ConditionKeyValueTrait.class);
        assertThat(trait2.getValue(), equalTo("AnotherContextKey"));
        assertThat(trait2.resolveConditionKey(service), equalTo("myservice:AnotherContextKey"));
    }
}
