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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class EnumTraitTest {
    @Test
    public void loadsTrait() {
        Node node = Node.parse("[{\"value\": \"foo\"}, "
                               + "{\"value\": \"bam\"}, "
                               + "{\"value\": \"boozled\"}]");
        EnumTrait trait = new EnumTrait.Provider().createTrait(ShapeId.from("ns.foo#baz"), node);

        assertThat(trait.toNode(), equalTo(node));
        assertThat(trait.toBuilder().build(), equalTo(trait));
        assertThat(trait.getEnumDefinitionValues(), contains("foo", "bam", "boozled"));
    }

    @Test
    public void expectsAtLeastOneConstant() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            provider.createTrait(ShapeId.from("smithy.api#enum"), ShapeId.from("ns.qux#foo"), Node.objectNode());
        });
    }

    @Test
    public void checksIfAllDefineNames() {
        Node node = Node.parse("[{\"value\": \"foo\", \"name\": \"FOO\"}, "
                               + "{\"value\": \"bam\", \"name\": \"BAM\"}]");
        EnumTrait trait = new EnumTrait.Provider().createTrait(ShapeId.from("ns.foo#baz"), node);

        assertThat(trait.hasNames(), is(true));
    }
}
