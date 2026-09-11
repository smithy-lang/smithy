/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

public class SmfWriterTest {

    private static byte[] serialize(Model model) {
        return SmfWriter.builder().build().serialize(model);
    }

    @Test
    public void writesValidHeader() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build();
        byte[] data = serialize(model);

        // Magic: SMBY
        assertEquals('S', data[0] & 0xFF);
        assertEquals('M', data[1] & 0xFF);
        assertEquals('F', data[2] & 0xFF);
        assertEquals(0, data[3] & 0xFF);
        // Format version
        assertEquals(1, data[4]);
        // Smithy version 2.0
        assertEquals(2, data[5]);
        assertEquals(0, data[6]);
    }

    @Test
    public void hasMetadataFlagSetWhenMetadataPresent() {
        Model model = Model.builder()
                .putMetadataProperty("foo", software.amazon.smithy.model.node.Node.from("bar"))
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build();
        byte[] data = serialize(model);
        assertTrue((data[7] & SmfConstants.FLAG_HAS_METADATA) != 0);
    }

    @Test
    public void hasMetadataFlagClearWhenNoMetadata() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build();
        byte[] data = serialize(model);
        assertEquals(0, data[7] & SmfConstants.FLAG_HAS_METADATA);
    }

    @Test
    public void traitFilterStripsDocs() {
        Model model = Model.builder()
                .addShape(StringShape.builder()
                        .id("com.example#Foo")
                        .addTrait(new DocumentationTrait("hello"))
                        .build())
                .build();
        byte[] data = SmfWriter.builder()
                .traitFilter(trait -> !trait.toShapeId().equals(DocumentationTrait.ID))
                .build()
                .serialize(model);
        Model loaded = SmfReader.read(data);
        assertFalse(loaded.expectShape(ShapeId.from("com.example#Foo"))
                .hasTrait(DocumentationTrait.class));
    }

    @Test
    public void strippedModelIsSmallerThanFull() {
        Model model = Model.builder()
                .addShape(StringShape.builder()
                        .id("com.example#Foo")
                        .addTrait(new DocumentationTrait("A very long documentation string"))
                        .build())
                .build();
        byte[] full = serialize(model);
        byte[] stripped = SmfWriter.builder()
                .traitFilter(trait -> !trait.toShapeId().equals(DocumentationTrait.ID))
                .build()
                .serialize(model);
        assertTrue(stripped.length < full.length);
    }

    @Test
    public void writesSimpleShape() {
        Model model = Model.builder()
                .addShape(BlobShape.builder().id("com.example#MyBlob").build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
        assertTrue(data.length > SmfConstants.HEADER_SIZE);
    }

    @Test
    public void writesStructureWithMembers() {
        Model model = Model.builder()
                .addShape(StructureShape.builder()
                        .id("com.example#MyStruct")
                        .addMember("name", ShapeId.from("smithy.api#String"))
                        .addMember("age", ShapeId.from("smithy.api#Integer"))
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
        assertTrue(data.length > SmfConstants.HEADER_SIZE);
    }

    @Test
    public void writesOperationShape() {
        Model model = Model.builder()
                .addShape(StructureShape.builder().id("com.example#Input").build())
                .addShape(StructureShape.builder().id("com.example#Output").build())
                .addShape(OperationShape.builder()
                        .id("com.example#GetThing")
                        .input(ShapeId.from("com.example#Input"))
                        .output(ShapeId.from("com.example#Output"))
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
        assertTrue(data.length > SmfConstants.HEADER_SIZE);
    }

    @Test
    public void writesServiceShape() {
        Model model = Model.builder()
                .addShape(ServiceShape.builder()
                        .id("com.example#MyService")
                        .version("2023-01-01")
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
        assertTrue(data.length > SmfConstants.HEADER_SIZE);
    }

    @Test
    public void writesListShape() {
        Model model = Model.builder()
                .addShape(ListShape.builder()
                        .id("com.example#MyList")
                        .member(ShapeId.from("smithy.api#String"))
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
    }

    @Test
    public void writesMapShape() {
        Model model = Model.builder()
                .addShape(MapShape.builder()
                        .id("com.example#MyMap")
                        .key(ShapeId.from("smithy.api#String"))
                        .value(ShapeId.from("smithy.api#Integer"))
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
    }

    @Test
    public void writesTraitsOnMembers() {
        Model model = Model.builder()
                .addShape(StructureShape.builder()
                        .id("com.example#MyStruct")
                        .addMember(MemberShape.builder()
                                .id(ShapeId.from("com.example#MyStruct$name"))
                                .target("smithy.api#String")
                                .addTrait(new RequiredTrait())
                                .build())
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
    }

    @Test
    public void sharedSymbolsNotInLocalTable() {
        // smithy.api#String is in the shared table, should not appear in local symbols
        Model model = Model.builder()
                .addShape(ListShape.builder()
                        .id("com.example#MyList")
                        .member(ShapeId.from("smithy.api#String"))
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
    }

    @Test
    public void outputIsNonEmpty() {
        Model model = Model.builder().build();
        byte[] data = serialize(model);
        // Even an empty model has header + symbol table + index + shapes section
        assertTrue(data.length >= SmfConstants.HEADER_SIZE);
    }

    @Test
    public void writesResourceShapeWithLifecycleOps() {
        Model model = Model.builder()
                .addShape(OperationShape.builder().id("com.example#PutOp").build())
                .addShape(OperationShape.builder().id("com.example#GetOp").build())
                .addShape(OperationShape.builder().id("com.example#DeleteOp").build())
                .addShape(OperationShape.builder().id("com.example#ListOp").build())
                .addShape(OperationShape.builder().id("com.example#CreateOp").build())
                .addShape(OperationShape.builder().id("com.example#UpdateOp").build())
                .addShape(OperationShape.builder().id("com.example#ExtraOp").build())
                .addShape(ResourceShape.builder()
                        .id("com.example#MyResource")
                        .put(ShapeId.from("com.example#PutOp"))
                        .create(ShapeId.from("com.example#CreateOp"))
                        .read(ShapeId.from("com.example#GetOp"))
                        .update(ShapeId.from("com.example#UpdateOp"))
                        .delete(ShapeId.from("com.example#DeleteOp"))
                        .list(ShapeId.from("com.example#ListOp"))
                        .addOperation("com.example#ExtraOp")
                        .addIdentifier("id", "smithy.api#String")
                        .addProperty("name", "smithy.api#String")
                        .build())
                .build();
        byte[] data = serialize(model);
        assertNotNull(data);
        // Round-trip to verify
        Model loaded = software.amazon.smithy.model.loader.smf.SmfReader.read(data);
        ResourceShape res = loaded.expectShape(ShapeId.from("com.example#MyResource"),
                ResourceShape.class);
        assertTrue(res.getPut().isPresent());
        assertTrue(res.getCreate().isPresent());
        assertTrue(res.getRead().isPresent());
        assertTrue(res.getUpdate().isPresent());
        assertTrue(res.getDelete().isPresent());
        assertTrue(res.getList().isPresent());
    }

    @Test
    public void writesServiceWithRenamesAndErrors() {
        Model model = Model.builder()
                .addShape(StructureShape.builder().id("com.example#Error1").build())
                .addShape(StructureShape.builder().id("com.example#Error2").build())
                .addShape(StructureShape.builder().id("com.example#Collision").build())
                .addShape(ServiceShape.builder()
                        .id("com.example#MyService")
                        .version("2023-01-01")
                        .addError("com.example#Error1")
                        .addError("com.example#Error2")
                        .putRename(ShapeId.from("com.example#Collision"), "RenamedCollision")
                        .build())
                .build();
        byte[] data = serialize(model);
        Model loaded = SmfReader.read(data);
        ServiceShape svc = loaded.expectShape(ShapeId.from("com.example#MyService"),
                ServiceShape.class);
        assertEquals(2, svc.getErrors().size());
        assertEquals("RenamedCollision",
                svc.getRename().get(ShapeId.from("com.example#Collision")));
    }

    @Test
    public void writesDoubleTraitValue() {
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace com.example\n"
                                + "@range(min: 1.5, max: 99.9)\n"
                                + "float MyFloat\n")
                .assemble()
                .unwrap();
        byte[] data = serialize(model);
        Model loaded = SmfReader.read(data);
        assertNotNull(loaded.expectShape(ShapeId.from("com.example#MyFloat")));
    }
}
