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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;

public class TraitDefinitionTest {
    @Test
    public void requiresTraitName() {
        Assertions.assertThrows(IllegalStateException.class, () -> TraitDefinition.builder().build());
    }

    @Test
    public void requiresNamespace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            TraitDefinition.builder().name("foo");
        });
    }

    @Test
    public void supportsBooleanTrait() {
        TraitDefinition trait = TraitDefinition.builder().name("foo.bar#baz").build();

        assertThat(trait.getFullyQualifiedName(), equalTo("foo.bar#baz"));
        assertThat(trait.getName(), equalTo("baz"));
        assertThat(trait.getNamespace(), equalTo("foo.bar"));
        assertTrue(trait.isAnnotationTrait());
        assertFalse(trait.getShape().isPresent());
        assertEquals(trait.toNode(), Node.objectNode());
    }

    @Test
    public void supportsShapeTrait() {
        TraitDefinition trait = TraitDefinition.builder()
                .name("foo.bar#baz").shape(ShapeId.from("foo.bar#Baz")).build();

        assertFalse(trait.isAnnotationTrait());
        assertTrue(trait.getShape().isPresent());
        assertEquals(trait.toNode(), Node.objectNode()
                .withMember("shape", Node.from("foo.bar#Baz")));
    }

    @Test
    public void hasGetters() {
        TraitDefinition trait = TraitDefinition.builder()
                .name("foo.bar#baz")
                .addTag("abc")
                .selector(Selector.parse("string"))
                .build();

        assertThat(trait.getNamespace(), equalTo("foo.bar"));
        assertThat(trait.getTags(), hasSize(1));
        assertThat(trait.getSelector().toString(), equalTo("string"));
        assertThat(trait.getTags(), contains("abc"));
        assertEquals(trait.toNode(), Node.objectNode()
                .withMember("tags", Node.fromStrings("abc"))
                .withMember("selector", Node.from("string")));
    }

    @Test
    public void comparesAndHashCodeWhenShapeIsNull() {
        TraitDefinition a = TraitDefinition.builder().name("foo.bar#baz").build();
        TraitDefinition b = TraitDefinition.builder().name("foo.bar#baz").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalityIncludesTags() {
        TraitDefinition a = TraitDefinition.builder().name("foo.bar#baz").addTag("a").build();
        TraitDefinition b = TraitDefinition.builder().name("foo.bar#baz").addTag("b").build();

        assertThat(a, not(equalTo(b)));
        assertThat(a.toBuilder().build(), equalTo(a));
    }

    @Test
    public void canSetDocumentationValues() {
        TraitDefinition def = TraitDefinition.builder()
                .name("foo.bar#baz")
                .documentation("docs")
                .externalDocumentation("link")
                .build();

        assertThat(def.getDocumentation().get(), equalTo("docs"));
        assertThat(def.getExternalDocumentation().get(), equalTo("link"));
        assertThat(def.toBuilder().build(), equalTo(def));
    }

    @Test
    public void canSetStructurallyExclusive() {
        TraitDefinition def = TraitDefinition.builder()
                .name("foo.bar#baz")
                .structurallyExclusive(true)
                .build();

        assertThat(def.isStructurallyExclusive(), is(true));
        assertThat(def.toBuilder().build(), equalTo(def));
    }

    @Test
    public void canSetDeprecatedAndReason() {
        TraitDefinition def = TraitDefinition.builder()
                .name("foo.bar#baz")
                .deprecated(true)
                .deprecationReason("foo baz")
                .build();

        assertThat(def.toBuilder().build(), equalTo(def));
        assertThat(def.isDeprecated(), is(true));
        assertThat(def.getDeprecationReason(), equalTo(Optional.of("foo baz")));
    }

    @Test
    public void mustSetDeprecatedIfDeprecationReasonIsSet() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitDefinition.builder()
                    .name("foo.bar#baz")
                    .deprecationReason("foo baz")
                    .build();
        });
    }
}
