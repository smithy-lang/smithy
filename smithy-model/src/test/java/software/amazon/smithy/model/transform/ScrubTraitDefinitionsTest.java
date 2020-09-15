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

package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import java.util.function.Predicate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ScrubTraitDefinitionsTest {

    @Test
    public void removesTracesOfTraitDefinitions() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("scrub-trait-def.json"))
                .assemble()
                .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.scrubTraitDefinitions(model);

        assertThat(result.getShape(ShapeId.from("ns.foo#FooStructure")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#BarString")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#BarStringList")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#ComplexRemoved")), Matchers.is(Optional.empty()));

        assertThat(result.getShape(ShapeId.from("ns.foo#IpsumString")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#IpsumList")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#KeepStructure")), Matchers.not(Optional.empty()));

        // Make sure public prelude shapes weren't removed.
        assertThat(result.getShape(ShapeId.from("smithy.api#String")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Blob")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Boolean")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Byte")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Short")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Integer")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Long")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Float")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Double")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#BigInteger")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#BigDecimal")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Timestamp")), Matchers.not(Optional.empty()));

        // Make sure public prelude trait definition shapes were removed.
        assertThat(result.getShape(ShapeId.from("smithy.api#length")), Matchers.is(Optional.empty()));
    }

    @Test
    public void retainsSelectTraitDefinitions() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("scrub-trait-def.json"))
                .assemble()
                .unwrap();

        Predicate<Shape> keepFilter = shape -> !shape.getId().equals(ShapeId.from("ns.foo#bam"));

        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.scrubTraitDefinitions(model, keepFilter);

        assertThat(result.getShape(ShapeId.from("ns.foo#FooStructure")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#BarString")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#BarStringList")), Matchers.is(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#ComplexRemoved")), Matchers.is(Optional.empty()));

        assertThat(result.getShape(ShapeId.from("ns.foo#IpsumString")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#IpsumList")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("ns.foo#KeepStructure")), Matchers.not(Optional.empty()));

        // Make sure trait definitions that should be retained weren't removed.
        assertThat(result.getShape(ShapeId.from("ns.foo#bam")), Matchers.not(Optional.empty()));

        // Make sure other trait definitions were removed.
        assertThat(result.getShape(ShapeId.from("ns.foo#baz")), Matchers.is(Optional.empty()));

        // Make sure public prelude shapes weren't removed.
        assertThat(result.getShape(ShapeId.from("smithy.api#String")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Blob")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Boolean")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Byte")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Short")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Integer")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Long")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Float")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Double")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#BigInteger")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#BigDecimal")), Matchers.not(Optional.empty()));
        assertThat(result.getShape(ShapeId.from("smithy.api#Timestamp")), Matchers.not(Optional.empty()));

        // Make sure public prelude trait definition shapes were removed.
        assertThat(result.getShape(ShapeId.from("smithy.api#length")), Matchers.is(Optional.empty()));
    }
}
