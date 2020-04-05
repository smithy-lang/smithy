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

package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;

public class TraitDefinitionTest {
    @Test
    public void hasGetters() {
        TraitDefinition trait = TraitDefinition.builder().selector(Selector.parse("string")).build();

        assertThat(trait.getSelector().toString(), equalTo("string"));
        assertEquals(trait.toNode(), Node.objectNode().withMember("selector", Node.from("string")));
    }

    @Test
    public void isomorphicBuilder() {
        TraitDefinition trait = TraitDefinition.builder()
                .selector(Selector.parse("string"))
                .addConflict("com.foo#baz")
                .structurallyExclusive(TraitDefinition.StructurallyExclusive.MEMBER)
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void comparesAndHashCodeWhenShapeIsNull() {
        TraitDefinition a = TraitDefinition.builder().selector(Selector.parse("string")).build();
        TraitDefinition b = TraitDefinition.builder().selector(Selector.parse("string")).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
