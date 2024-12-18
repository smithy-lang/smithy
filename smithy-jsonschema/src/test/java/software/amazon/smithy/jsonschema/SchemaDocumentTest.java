/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class SchemaDocumentTest {
    @Test
    public void canSetIdKeyword() {
        SchemaDocument document = SchemaDocument.builder().idKeyword("foo").build();
        ObjectNode node = document.toNode().expectObjectNode();

        assertThat(document.getIdKeyword().get(), equalTo("foo"));
        assertThat(node.size(), is(1));
        assertThat(node.getStringMember("$id").get().getValue(), equalTo("foo"));
        assertThat(document.toBuilder().build(), equalTo(document));
    }

    @Test
    public void canSetSchemaKeyword() {
        SchemaDocument document = SchemaDocument.builder().schemaKeyword("foo").build();
        ObjectNode node = document.toNode().expectObjectNode();

        assertThat(document.getSchemaKeyword().get(), equalTo("foo"));
        assertThat(node.size(), is(1));
        assertThat(node.getStringMember("$schema").get().getValue(), equalTo("foo"));
        assertThat(document.toBuilder().build(), equalTo(document));
    }

    @Test
    public void canSetRootSchema() {
        SchemaDocument document = SchemaDocument.builder()
                .rootSchema(Schema.builder().type("string").build())
                .build();
        ObjectNode node = document.toNode().expectObjectNode();

        assertThat(document.getRootSchema().getType().get(), equalTo("string"));
        assertThat(node.getStringMember("type").get().getValue(), equalTo("string"));
        assertThat(document.toBuilder().build(), equalTo(document));
    }

    @Test
    public void canAddExtensions() {
        ObjectNode extensions = Node.objectNode().withMember("foo", Node.from("bar"));
        SchemaDocument document = SchemaDocument.builder().extensions(extensions).build();
        ObjectNode node = document.toNode().expectObjectNode();

        assertThat(document.getExtensions(), equalTo(extensions));
        assertThat(document.getExtension("foo").get(), equalTo(Node.from("bar")));
        assertThat(node.getStringMember("foo").get().getValue(), equalTo("bar"));
        assertThat(document.toBuilder().build(), equalTo(document));
    }

    @Test
    public void canAddDefinitions() {
        SchemaDocument document = SchemaDocument.builder()
                .putDefinition("#/definitions/foo", Schema.builder().type("string").build())
                .putDefinition("#/definitions/bar", Schema.builder().type("string").build())
                .build();
        ObjectNode node = document.toNode().expectObjectNode();

        assertThat(document.getDefinitions().values(), hasSize(2));
        assertTrue(document.getDefinition("#/definitions/foo").isPresent());
        assertTrue(document.getDefinition("#/definitions/bar").isPresent());
        assertThat(node.getObjectMember("definitions").get().getMembers().keySet(),
                containsInAnyOrder(Node.from("bar"), Node.from("foo")));
        assertThat(document.toBuilder().build(), equalTo(document));
    }

    @Test
    public void skipsDefinitionsNotRelative() {
        SchemaDocument document = SchemaDocument.builder()
                .putDefinition("http://foo.com/bar", Schema.builder().type("string").build())
                .build();
        ObjectNode node = document.toNode().expectObjectNode();

        assertThat(document.getDefinitions().values(), hasSize(1));
        assertFalse(node.getMember("definitions").isPresent());
        assertThat(document.toBuilder().build(), equalTo(document));
    }

    @Test
    public void unescapesJsonPointers() {
        Schema schema = Schema.builder().type("string").build();
        SchemaDocument document = SchemaDocument.builder()
                .putDefinition("#/definitions/~foo", schema)
                .build();

        assertThat(document.getDefinition("#/definitions/~0foo"), equalTo(Optional.of(schema)));
    }

    @Test
    public void retrievesNestedSchemas() {
        Schema string = Schema.builder().type("string").build();
        Schema array = Schema.builder().items(string).build();
        Schema complex = Schema.builder().putProperty("foo", array).build();
        SchemaDocument document = SchemaDocument.builder()
                .putDefinition("#/definitions/complex", complex)
                .build();

        assertThat(document.getDefinition("#/definitions/complex"), equalTo(Optional.of(complex)));
        assertThat(document.getDefinition("#/definitions/complex/properties/foo"), equalTo(Optional.of(array)));
        assertThat(document.getDefinition("#/definitions/complex/properties/foo/items"), equalTo(Optional.of(string)));
    }

    @Test
    public void emptyPointerReturnsRootSchema() {
        Schema string = Schema.builder().type("string").build();
        SchemaDocument document = SchemaDocument.builder()
                .rootSchema(string)
                .build();

        assertThat(document.getDefinition(""), equalTo(Optional.of(string)));
        assertThat(document.getDefinition("#"), equalTo(Optional.of(string)));
    }
}
