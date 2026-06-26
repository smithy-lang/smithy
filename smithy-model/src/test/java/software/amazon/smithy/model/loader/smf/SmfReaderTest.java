/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

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
}
