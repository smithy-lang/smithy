/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class ServiceResolvedConditionKeysTraitTest {

    @Test
    public void testBuilder() {
        ServiceResolvedConditionKeysTrait.Builder builder = ServiceResolvedConditionKeysTrait.builder();
        builder.addValue("one");
        builder.addValue("two");
        ServiceResolvedConditionKeysTrait trait = builder.build();
        assertEquals(2, trait.getValues().size());
        assertThat(trait.getValues(),
                equalTo(ListUtils.of("one", "two")));
    }

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("service-resolved-condition-keys.smithy"))
                .assemble()
                .unwrap();

        ServiceShape shape = result.expectShape(ShapeId.from("smithy.example#MyService"), ServiceShape.class);
        ServiceResolvedConditionKeysTrait trait = shape.expectTrait(ServiceResolvedConditionKeysTrait.class);
        assertThat(trait.getValues(),
                equalTo(ListUtils.of("myservice:ServiceResolvedContextKey", "AnotherResolvedContextKey")));
        assertThat(trait.resolveConditionKeys(shape),
                equalTo(ListUtils.of("myservice:ServiceResolvedContextKey", "myservice:AnotherResolvedContextKey")));
    }
}
