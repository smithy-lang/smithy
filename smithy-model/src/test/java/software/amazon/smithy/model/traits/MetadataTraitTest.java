/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

public class MetadataTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNodeBuilder().withMember("key", "suppressions").build();
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#metadata"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(MetadataTrait.class));
        MetadataTrait metadataTrait = (MetadataTrait) trait.get();

        assertEquals("suppressions", metadataTrait.getKey());
        assertEquals(MetadataTrait.ID, metadataTrait.toShapeId());
        assertThat(metadataTrait.toNode(), equalTo(node));
        assertThat(metadataTrait.toBuilder().build(), equalTo(metadataTrait));
    }

    @Test
    public void requiresKey() {
        assertThrows(IllegalStateException.class, () -> MetadataTrait.builder().build());
    }

    @Test
    public void equalityIsBasedOnKey() {
        MetadataTrait a = MetadataTrait.builder().key("suppressions").build();
        MetadataTrait b = MetadataTrait.builder().key("suppressions").build();
        MetadataTrait c = MetadataTrait.builder().key("other").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    public void loadsFromModel() {
        // End-to-end: a .smithy model assembles, the trait is applied via SPI, and
        // the declared type is reachable through the standard Shape API.
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2.0\"\n"
                                + "namespace smithy.example\n"
                                + "@metadata(key: \"auditLevel\")\n"
                                + "string AuditLevel\n")
                .assemble()
                .unwrap();

        StringShape shape = model.expectShape(ShapeId.from("smithy.example#AuditLevel"), StringShape.class);
        MetadataTrait trait = shape.expectTrait(MetadataTrait.class);
        assertEquals("auditLevel", trait.getKey());
    }
}
