/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

public class SmfReaderTest {

    @Test
    public void rejectsEmptyInput() {
        assertThrows(SmfFormatException.class, () -> SmfReader.read(new byte[0]));
    }

    @Test
    public void rejectsWrongMagic() {
        byte[] data = new byte[8];
        data[0] = 'X';
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data));
    }

    @Test
    public void rejectsUnsupportedFormatVersion() {
        byte[] data = new byte[8];
        data[0] = 'S';
        data[1] = 'M';
        data[2] = 'F';
        data[3] = 0;
        data[4] = 99; // unsupported version
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data));
    }

    @Test
    public void rejectsTruncatedFile() {
        byte[] data = new byte[4]; // too short for header
        data[0] = 'S';
        data[1] = 'M';
        data[2] = 'F';
        data[3] = 0;
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data));
    }

    // Builds an 8-byte header with the given flags followed by trailing bytes,
    // plus a 4-byte CRC region so the reader's limit is well-defined. CRC is
    // not verified in these tests (read with verifyCrc=false).
    private static byte[] header(int flags, byte... trailing) {
        byte[] data = new byte[8 + trailing.length + 4];
        data[0] = 'S';
        data[1] = 'M';
        data[2] = 'F';
        data[3] = 0;
        data[4] = 0x01; // format version
        data[5] = 2;
        data[6] = 0;
        data[7] = (byte) flags;
        System.arraycopy(trailing, 0, data, 8, trailing.length);
        return data;
    }

    @Test
    public void rejectsMissingTraitValueOffsetsFlag() {
        // has-metadata|has-shape-index set, but NOT has-trait-value-offsets (0x04).
        byte[] data = header(0x01 | 0x02);
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data, false));
    }

    @Test
    public void rejectsTruncatedVarUInt() {
        // Flags = trait-value-offsets only. Then a symbol table that starts with
        // a VarUInt whose continuation bit is set but the buffer ends.
        byte[] data = header(0x04, (byte) 0x80); // 0x80 = continuation, no next byte before limit
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data, false));
    }

    @Test
    public void rejectsVarUIntExceeding32Bits() {
        // A 5-byte VarUInt whose 5th byte sets bits above the 32-bit range.
        byte[] data = header(0x04,
                (byte) 0x80,
                (byte) 0x80,
                (byte) 0x80,
                (byte) 0x80,
                (byte) 0x7F,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0,
                (byte) 0);
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data, false));
    }

    @Test
    public void rejectsOversizedShapeIndexCounts() {
        // sharedTableId=0, localSymbolCount=0, then trait value tables + a shape
        // index whose entryCount is huge. The reader must reject via bounds
        // checks rather than overflow or AIOOBE. Flags: shape-index + offsets.
        // Layout after header: symbolTable(sharedId=0, localCount=0),
        // traitValueTable(count=0,dataLen=0), then index entryCount=0xFFFFFFF.
        byte[] data = header(0x02 | 0x04,
                (byte) 0x00, // sharedTableId = 0
                (byte) 0x00, // localSymbolCount = 0
                (byte) 0x00,
                (byte) 0x00, // trait value table: count=0, dataLen=0
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0x7F, // entryCount huge
                (byte) 0x00); // neighborCount=0
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data, false));
    }

    @Test
    public void selectiveRejectsMalformedInputCleanly() {
        // Random-ish bytes with a valid header must not crash the selective path
        // with AIOOBE; a clean SmfFormatException is required.
        byte[] data = header(0x02 | 0x04,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x05,
                (byte) 0x05,
                (byte) 0x05,
                (byte) 0x05);
        assertThrows(SmfFormatException.class,
                () -> SmfReader.readSelective(data,
                        SelectiveLoadRequest.builder()
                                .service(ShapeId.from("com.example#S"))
                                .verifyCrc(false)
                                .build()));
    }

    @Test
    public void readsWriterOutput() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build();
        byte[] data = SmfWriter.builder().build().serialize(model);
        Model result = SmfReader.read(data);
        assertNotNull(result);
        assertNotNull(result.expectShape(ShapeId.from("com.example#Foo")));
    }

    @Test
    public void roundTripsUnicodeMetadataAndTraitValues() {
        Model model = Model.builder()
                .putMetadataProperty("cafe", Node.from("naive"))
                .putMetadataProperty("caf\u00E9",
                        Node.from("na\u00EFve \u4F60\u597D"))
                .addShape(StringShape.builder()
                        .id("com.example#Foo")
                        .addTrait(new DocumentationTrait("R\u00E9sum\u00E9 \u4F60\u597D"))
                        .build())
                .build();
        byte[] data = SmfWriter.builder().build().serialize(model);

        Model result = SmfReader.read(data);
        assertEquals("naive", result.getMetadata().get("cafe").expectStringNode().getValue());
        assertEquals("na\u00EFve \u4F60\u597D", result.getMetadata().get("caf\u00E9").expectStringNode().getValue());
        assertEquals("R\u00E9sum\u00E9 \u4F60\u597D",
                result.expectShape(ShapeId.from("com.example#Foo"))
                        .expectTrait(DocumentationTrait.class)
                        .getValue());
    }

    @Test
    public void loadsViaModelAssembler() throws Exception {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.example#Bar").build())
                .build();
        byte[] data = SmfWriter.builder().build().serialize(model);
        Path tempFile = Files.createTempFile("test", ".smf");
        try {
            Files.write(tempFile, data);
            Model loaded = Model.assembler()
                    .addImport(tempFile)
                    .assemble()
                    .unwrap();
            assertNotNull(loaded.expectShape(ShapeId.from("com.example#Bar")));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void rejectsCorruptedFile() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build();
        byte[] data = SmfWriter.builder().build().serialize(model);
        // Corrupt a byte in the middle of the data
        data[data.length / 2] ^= 0xFF;
        assertThrows(SmfFormatException.class, () -> SmfReader.read(data));
    }

    @Test
    public void rejectsTruncatedCrc() {
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build();
        byte[] data = SmfWriter.builder().build().serialize(model);
        // Chop off the CRC bytes
        byte[] truncated = new byte[data.length - 4];
        System.arraycopy(data, 0, truncated, 0, truncated.length);
        assertThrows(SmfFormatException.class, () -> SmfReader.read(truncated));
    }

    @Test
    public void selectiveLoadingIncludesTraitDefinitions() {
        // Model with a custom protocol trait definition applied to a service
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace com.example\n"
                                + "@trait\n"
                                + "@protocolDefinition\n"
                                + "structure myProtocol {}\n"
                                + "@myProtocol\n"
                                + "service MyService {\n"
                                + "    version: \"2024-01-01\"\n"
                                + "    operations: [GetThing]\n"
                                + "}\n"
                                + "operation GetThing {\n"
                                + "    input := { id: String }\n"
                                + "    output := { name: String }\n"
                                + "}\n")
                .assemble()
                .unwrap();

        byte[] data = SmfWriter.builder().build().serialize(model);

        // Selective load via SelectiveLoadRequest (service + operation)
        Model selective = SmfReader.readSelective(data,
                SelectiveLoadRequest.builder()
                        .service(ShapeId.from("com.example#MyService"))
                        .addOperation(ShapeId.from("com.example#GetThing"))
                        .build());

        // The trait definition shape must be present
        assertTrue(selective.getShape(ShapeId.from("com.example#myProtocol")).isPresent(),
                "Trait definition shape should be included in selective load");
    }

    @Test
    public void selectiveProfileUsesTraitValueOffsets() {
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace com.example\n"
                                + "@documentation(\"doc\")\n"
                                + "service MyService {\n"
                                + "    version: \"2024-01-01\"\n"
                                + "    operations: [GetThing]\n"
                                + "}\n"
                                + "@documentation(\"operation docs\")\n"
                                + "operation GetThing {\n"
                                + "    input := {\n"
                                + "        @documentation(\"member docs\")\n"
                                + "        id: String\n"
                                + "    }\n"
                                + "    output := { name: String }\n"
                                + "}\n")
                .assemble()
                .unwrap();

        byte[] data = SmfWriter.builder().build().serialize(model);
        SmfSelectiveLoadResult result = SmfReader.readSelectiveWithProfile(data,
                SelectiveLoadRequest.builder()
                        .service(ShapeId.from("com.example#MyService"))
                        .addOperation(ShapeId.from("com.example#GetThing"))
                        .verifyCrc(false)
                        .build());

        assertNotNull(result.getModel().expectShape(ShapeId.from("com.example#GetThing")));
        assertTrue(result.getProfile().getTraitValueDataBytes() > result.getProfile().getTraitValueBytesScanned(),
                "Trait offset table should reduce selective setup bytes below total trait payload bytes");
    }

    @Test
    public void splitColdTraitsPreserveFullReadAndSkipSelectiveDocs() {
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace com.example\n"
                                + "@documentation(\"service docs\")\n"
                                + "service MyService {\n"
                                + "    version: \"2024-01-01\"\n"
                                + "    operations: [GetThing]\n"
                                + "}\n"
                                + "@readonly\n"
                                + "@documentation(\"operation docs\")\n"
                                + "operation GetThing {\n"
                                + "    input := {\n"
                                + "        @required\n"
                                + "        @documentation(\"member docs\")\n"
                                + "        id: String\n"
                                + "    }\n"
                                + "    output := { value: String }\n"
                                + "}\n")
                .assemble()
                .unwrap();

        byte[] flatData = SmfWriter.builder().build().serialize(model);
        byte[] data = SmfWriter.builder()
                .splitColdTraits(true)
                .coldTraitFilter(trait -> trait.toShapeId().equals(DocumentationTrait.ID))
                .build()
                .serialize(model);

        ShapeId serviceId = ShapeId.from("com.example#MyService");
        ShapeId operationId = ShapeId.from("com.example#GetThing");
        ShapeId inputId = model.expectShape(operationId, OperationShape.class).getInputShape();
        ShapeId memberId = inputId.withMember("id");

        Model full = SmfReader.read(data, false);
        assertTrue(full.expectShape(serviceId).hasTrait(DocumentationTrait.class));
        assertTrue(full.expectShape(operationId).hasTrait("smithy.api#readonly"));
        assertTrue(full.expectShape(operationId).hasTrait(DocumentationTrait.class));
        assertTrue(full.expectShape(memberId).hasTrait(RequiredTrait.class));
        assertTrue(full.expectShape(memberId).hasTrait(DocumentationTrait.class));

        SmfSelectiveLoadResult result = SmfReader.readSelectiveWithProfile(data,
                SelectiveLoadRequest.builder()
                        .service(serviceId)
                        .addOperation(operationId)
                        .verifyCrc(false)
                        .build());
        SmfSelectiveLoadResult flatResult = SmfReader.readSelectiveWithProfile(flatData,
                SelectiveLoadRequest.builder()
                        .service(serviceId)
                        .addOperation(operationId)
                        .verifyCrc(false)
                        .build());
        Model selective = result.getModel();

        assertFalse(selective.expectShape(serviceId).hasTrait(DocumentationTrait.class));
        assertTrue(selective.expectShape(operationId).hasTrait("smithy.api#readonly"));
        assertFalse(selective.expectShape(operationId).hasTrait(DocumentationTrait.class));
        assertTrue(selective.expectShape(memberId).hasTrait(RequiredTrait.class));
        assertFalse(selective.expectShape(memberId).hasTrait(DocumentationTrait.class));
        assertTrue(result.getProfile().getTraitValueDataBytes() < flatResult.getProfile().getTraitValueDataBytes());
        assertTrue(result.getProfile().getTraitValueCount() < flatResult.getProfile().getTraitValueCount());
    }

    @Test
    public void smfLoadedViaAssemblerMergesWithIdenticalShapes() throws Exception {
        // Create a model, serialize to SMF, then load both the SMF and
        // the original source together — should not conflict.
        String smithy = "$version: \"2\"\n"
                + "namespace com.example\n"
                + "structure Foo {\n"
                + "    @required\n"
                + "    name: String\n"
                + "}\n";

        Model original = Model.assembler()
                .addUnparsedModel("test.smithy", smithy)
                .assemble()
                .unwrap();

        byte[] smfData = SmfWriter.builder().build().serialize(original);
        Path smfFile = Files.createTempFile("test", ".smf");
        try {
            Files.write(smfFile, smfData);

            // Load SMF alongside the same source — no conflicts expected
            Model combined = Model.assembler()
                    .addUnparsedModel("test.smithy", smithy)
                    .addImport(smfFile)
                    .assemble()
                    .unwrap();

            assertNotNull(combined.expectShape(ShapeId.from("com.example#Foo")));
            assertTrue(combined.expectShape(ShapeId.from("com.example#Foo$name"))
                    .hasTrait("smithy.api#required"));
        } finally {
            Files.deleteIfExists(smfFile);
        }
    }

    @Test
    public void smfLoadedViaAssemblerHandlesMixinShapes() throws Exception {
        // Model with mixins: SMF has flattened members. Loading SMF alone
        // through the assembler should produce a valid model.
        String smithy = "$version: \"2\"\n"
                + "namespace com.example\n"
                + "@mixin\n"
                + "structure Base {\n"
                + "    id: String\n"
                + "}\n"
                + "structure Concrete with [Base] {\n"
                + "    name: String\n"
                + "}\n";

        Model original = Model.assembler()
                .addUnparsedModel("test.smithy", smithy)
                .assemble()
                .unwrap();

        byte[] smfData = SmfWriter.builder().build().serialize(original);
        Path smfFile = Files.createTempFile("test", ".smf");
        try {
            Files.write(smfFile, smfData);

            // Load SMF alone through the assembler
            Model loaded = Model.assembler()
                    .addImport(smfFile)
                    .assemble()
                    .unwrap();

            assertNotNull(loaded.expectShape(ShapeId.from("com.example#Concrete")));
            assertTrue(loaded.getShape(ShapeId.from("com.example#Concrete$id")).isPresent());
            assertTrue(loaded.getShape(ShapeId.from("com.example#Concrete$name")).isPresent());
        } finally {
            Files.deleteIfExists(smfFile);
        }
    }
}
