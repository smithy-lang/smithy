/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.Trait;

public class SmfRoundTripTest {

    @Test
    public void roundTripsSimpleString() {
        assertRoundTrip(Model.builder()
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build());
    }

    @Test
    public void roundTripsStructureWithMembers() {
        assertRoundTrip(Model.builder()
                .addShape(StructureShape.builder()
                        .id("com.example#MyStruct")
                        .addMember("name", ShapeId.from("smithy.api#String"))
                        .addMember("age", ShapeId.from("smithy.api#Integer"))
                        .build())
                .build());
    }

    @Test
    public void roundTripsTraits() {
        assertRoundTrip(Model.builder()
                .addShape(StructureShape.builder()
                        .id("com.example#MyStruct")
                        .addTrait(new DocumentationTrait("Hello world"))
                        .addTrait(new SensitiveTrait())
                        .addMember(MemberShape.builder()
                                .id(ShapeId.from("com.example#MyStruct$name"))
                                .target("smithy.api#String")
                                .addTrait(new RequiredTrait())
                                .addTrait(new DocumentationTrait("The name"))
                                .build())
                        .build())
                .build());
    }

    @Test
    public void roundTripsListShape() {
        assertRoundTrip(Model.builder()
                .addShape(ListShape.builder()
                        .id("com.example#MyList")
                        .member(ShapeId.from("smithy.api#String"))
                        .build())
                .build());
    }

    @Test
    public void roundTripsMapShape() {
        assertRoundTrip(Model.builder()
                .addShape(MapShape.builder()
                        .id("com.example#MyMap")
                        .key(ShapeId.from("smithy.api#String"))
                        .value(ShapeId.from("smithy.api#Integer"))
                        .build())
                .build());
    }

    @Test
    public void roundTripsOperationShape() {
        assertRoundTrip(Model.builder()
                .addShape(StructureShape.builder().id("com.example#Input").build())
                .addShape(StructureShape.builder().id("com.example#Output").build())
                .addShape(StructureShape.builder().id("com.example#Error").build())
                .addShape(OperationShape.builder()
                        .id("com.example#GetThing")
                        .input(ShapeId.from("com.example#Input"))
                        .output(ShapeId.from("com.example#Output"))
                        .addError("com.example#Error")
                        .build())
                .build());
    }

    @Test
    public void roundTripsServiceShape() {
        assertRoundTrip(Model.builder()
                .addShape(OperationShape.builder().id("com.example#GetThing").build())
                .addShape(ServiceShape.builder()
                        .id("com.example#MyService")
                        .version("2023-01-01")
                        .addOperation("com.example#GetThing")
                        .build())
                .build());
    }

    @Test
    public void roundTripsMetadata() {
        assertRoundTrip(Model.builder()
                .putMetadataProperty("validators",
                        Node.fromNodes(
                                Node.objectNode().withMember("name", Node.from("EmitEachSelector"))))
                .putMetadataProperty("simple", Node.from("value"))
                .addShape(StringShape.builder().id("com.example#Foo").build())
                .build());
    }

    @Test
    public void roundTripsMultipleShapeTypes() {
        assertRoundTrip(Model.builder()
                .addShape(StringShape.builder().id("com.example#S").build())
                .addShape(ListShape.builder()
                        .id("com.example#L")
                        .member(ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(MapShape.builder()
                        .id("com.example#M")
                        .key(ShapeId.from("smithy.api#String"))
                        .value(ShapeId.from("smithy.api#Integer"))
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#St")
                        .addMember("x", ShapeId.from("smithy.api#Integer"))
                        .build())
                .addShape(OperationShape.builder().id("com.example#Op").build())
                .addShape(ServiceShape.builder()
                        .id("com.example#Svc")
                        .version("1.0")
                        .addOperation("com.example#Op")
                        .build())
                .build());
    }

    @Test
    public void roundTripsRecursiveShapes() {
        // AttributeValue pattern: structure -> map -> structure (cycle)
        Model model = Model.builder()
                .addShape(MapShape.builder()
                        .id("com.example#ValueMap")
                        .key(ShapeId.from("smithy.api#String"))
                        .value(ShapeId.from("com.example#Recursive"))
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#Recursive")
                        .addMember("children", ShapeId.from("com.example#ValueMap"))
                        .addMember("value", ShapeId.from("smithy.api#String"))
                        .build())
                .build();
        assertRoundTrip(model);
    }

    @Test
    public void selectiveLoadingHandlesRecursiveShapes() {
        Model model = Model.builder()
                .addShape(MapShape.builder()
                        .id("com.example#ValueMap")
                        .key(ShapeId.from("smithy.api#String"))
                        .value(ShapeId.from("com.example#Recursive"))
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#Recursive")
                        .addMember("children", ShapeId.from("com.example#ValueMap"))
                        .addMember("value", ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#Unrelated")
                        .build())
                .build();

        byte[] data = SmfWriter.builder().build().serialize(model);
        Set<ShapeId> roots = Collections.singleton(
                ShapeId.from("com.example#Recursive"));
        Model selective = SmfReader.readSelective(data, roots);

        // Both recursive shapes should be loaded
        assertNotNull(selective.getShape(ShapeId.from("com.example#Recursive")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#ValueMap")).orElse(null));
        // Unrelated should not
        assertFalse(selective.getShape(ShapeId.from("com.example#Unrelated")).isPresent());
    }

    @Test
    public void roundTripPreservesMixins() {
        // Build a model with a mixin applied
        Model model = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace com.example\n"
                                + "@mixin\n"
                                + "structure MixinStruct {\n"
                                + "    mixedMember: String\n"
                                + "}\n"
                                + "structure MyStruct with [MixinStruct] {\n"
                                + "    localMember: String\n"
                                + "}\n")
                .assemble()
                .unwrap();

        byte[] data = SmfWriter.builder().build().serialize(model);
        Model loaded = SmfReader.read(data);

        Shape myStruct = loaded.expectShape(ShapeId.from("com.example#MyStruct"));
        // Verify mixin relationship is preserved
        assertTrue(myStruct.getMixins().contains(ShapeId.from("com.example#MixinStruct")),
                "Mixin relationship should be preserved");
        // Verify members are present (flattened)
        assertTrue(myStruct.getMemberNames().contains("mixedMember"),
                "Mixin member should be present");
        assertTrue(myStruct.getMemberNames().contains("localMember"),
                "Local member should be present");
    }

    @Test
    public void roundTripPreservesMemberOrder() {
        Model model = Model.builder()
                .addShape(StructureShape.builder()
                        .id("com.example#Ordered")
                        .addMember("zebra", ShapeId.from("smithy.api#String"))
                        .addMember("alpha", ShapeId.from("smithy.api#String"))
                        .addMember("middle", ShapeId.from("smithy.api#String"))
                        .build())
                .build();
        byte[] data = SmfWriter.builder().build().serialize(model);
        Model result = SmfReader.read(data);

        Shape original = model.expectShape(ShapeId.from("com.example#Ordered"));
        Shape loaded = result.expectShape(ShapeId.from("com.example#Ordered"));

        assertEquals(
                original.getMemberNames(),
                loaded.getMemberNames(),
                "Member order must be preserved");
    }

    @Test
    public void selectiveLoadingLoadsOnlyTransitiveClosure() {
        Model model = Model.builder()
                .addShape(StructureShape.builder()
                        .id("com.example#Input")
                        .addMember("id", ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#Output")
                        .addMember("name", ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(StructureShape.builder().id("com.example#Error").build())
                .addShape(OperationShape.builder()
                        .id("com.example#GetThing")
                        .input(ShapeId.from("com.example#Input"))
                        .output(ShapeId.from("com.example#Output"))
                        .addError("com.example#Error")
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#Unrelated")
                        .addMember("x", ShapeId.from("smithy.api#Integer"))
                        .build())
                .addShape(OperationShape.builder().id("com.example#OtherOp").build())
                .build();

        byte[] data = SmfWriter.builder().build().serialize(model);

        // Selectively load only GetThing
        Set<ShapeId> roots = Collections.singleton(
                ShapeId.from("com.example#GetThing"));
        Model selective = SmfReader.readSelective(data, roots);

        // Should have GetThing + Input + Output + Error (transitive closure)
        assertNotNull(selective.getShape(ShapeId.from("com.example#GetThing")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Input")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Output")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Error")).orElse(null));

        // Should NOT have Unrelated or OtherOp
        assertFalse(selective.getShape(ShapeId.from("com.example#Unrelated")).isPresent());
        assertFalse(selective.getShape(ShapeId.from("com.example#OtherOp")).isPresent());
    }

    private void assertRoundTrip(Model original) {
        byte[] data = SmfWriter.builder().build().serialize(original);
        Model loaded = SmfReader.read(data);

        // Compare shape IDs (excluding prelude shapes)
        Set<ShapeId> originalIds = original.toSet()
                .stream()
                .map(Shape::getId)
                .filter(id -> !id.getNamespace().equals("smithy.api"))
                .collect(Collectors.toSet());
        Set<ShapeId> loadedIds = loaded.toSet()
                .stream()
                .map(Shape::getId)
                .filter(id -> !id.getNamespace().equals("smithy.api"))
                .collect(Collectors.toSet());
        assertEquals(originalIds, loadedIds, "Shape IDs must match");

        // Compare each shape's traits
        for (ShapeId id : originalIds) {
            Shape origShape = original.expectShape(id);
            Shape loadedShape = loaded.expectShape(id);
            assertEquals(origShape.getType(),
                    loadedShape.getType(),
                    "Shape type mismatch for " + id);

            // Compare trait values via Node serialization
            Map<String, Node> origTraits = new HashMap<>();
            for (Map.Entry<ShapeId, Trait> e : origShape.getAllTraits().entrySet()) {
                origTraits.put(e.getKey().toString(), e.getValue().toNode());
            }
            Map<String, Node> loadedTraits = new HashMap<>();
            for (Map.Entry<ShapeId, Trait> e : loadedShape.getAllTraits().entrySet()) {
                loadedTraits.put(e.getKey().toString(), e.getValue().toNode());
            }
            assertEquals(origTraits,
                    loadedTraits,
                    "Trait values mismatch for " + id);
        }

        // Compare metadata
        assertEquals(original.getMetadata(), loaded.getMetadata());
    }

    @Test
    public void readAndReadIntoProduceEquivalentModels() {
        // Build a non-trivial model
        Model original = Model.assembler()
                .addUnparsedModel("test.smithy",
                        "$version: \"2\"\n"
                                + "namespace com.example\n"
                                + "service MyService {\n"
                                + "    version: \"1.0\"\n"
                                + "    operations: [GetThing]\n"
                                + "}\n"
                                + "@documentation(\"Gets a thing\")\n"
                                + "operation GetThing {\n"
                                + "    input := {\n"
                                + "        @required\n"
                                + "        id: String\n"
                                + "    }\n"
                                + "    output := {\n"
                                + "        name: String\n"
                                + "    }\n"
                                + "}\n")
                .assemble()
                .unwrap();

        byte[] data = SmfWriter.builder().build().serialize(original);

        // Path 1: SmfReader.read() (direct)
        Model viaRead = SmfReader.read(data);

        // Path 2: SmfReader.readInto() via ModelAssembler (simulates ModelLoader path)
        try {
            Path tempFile = Files.createTempFile("equiv-test", ".smf");
            Files.write(tempFile, data);
            Model viaAssembler = Model.assembler()
                    .addImport(tempFile)
                    .assemble()
                    .unwrap();
            Files.deleteIfExists(tempFile);

            // Compare non-prelude shapes
            Set<ShapeId> readIds = viaRead.toSet()
                    .stream()
                    .map(Shape::getId)
                    .filter(id -> !id.getNamespace().equals("smithy.api"))
                    .collect(Collectors.toSet());
            Set<ShapeId> assemblerIds = viaAssembler.toSet()
                    .stream()
                    .map(Shape::getId)
                    .filter(id -> !id.getNamespace().equals("smithy.api"))
                    .collect(Collectors.toSet());
            assertEquals(readIds, assemblerIds, "Shape IDs must match between read() and readInto() paths");

            // Compare traits on each shape
            for (ShapeId id : readIds) {
                Shape fromRead = viaRead.expectShape(id);
                Shape fromAssembler = viaAssembler.expectShape(id);
                Map<String, Node> readTraits = new TreeMap<>();
                fromRead.getAllTraits().forEach((k, v) -> readTraits.put(k.toString(), v.toNode()));
                Map<String, Node> assemblerTraits = new TreeMap<>();
                fromAssembler.getAllTraits().forEach((k, v) -> assemblerTraits.put(k.toString(), v.toNode()));
                assertEquals(readTraits, assemblerTraits, "Traits mismatch for " + id);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void memberOrderPreservedAcrossManyStructures() {
        // Generate multiple structures with intentionally non-alphabetical member orders
        String[][] memberSets = {
                {"zulu", "alpha", "mike", "bravo", "xray"},
                {"omega", "delta", "gamma", "alpha", "beta", "epsilon"},
                {"c", "a", "b"},
                {"z", "y", "x", "w", "v", "u", "t", "s", "r", "q", "p"},
                {"middle", "first", "last", "between"},
        };

        Model.Builder mb = Model.builder();
        for (int i = 0; i < memberSets.length; i++) {
            StructureShape.Builder sb = StructureShape.builder()
                    .id("com.example#Struct" + i);
            for (String member : memberSets[i]) {
                sb.addMember(member, ShapeId.from("smithy.api#String"));
            }
            mb.addShape(sb.build());
        }
        Model original = mb.build();

        byte[] data = SmfWriter.builder().build().serialize(original);
        Model loaded = SmfReader.read(data);

        for (int i = 0; i < memberSets.length; i++) {
            ShapeId id = ShapeId.from("com.example#Struct" + i);
            Shape origShape = original.expectShape(id);
            Shape loadedShape = loaded.expectShape(id);
            assertEquals(
                    origShape.getMemberNames(),
                    loadedShape.getMemberNames(),
                    "Member order not preserved for " + id);
        }
    }

    @Test
    public void selectiveLoadWithServiceAndOperations() {
        Model model = Model.builder()
                .putMetadataProperty("validators", Node.fromNodes())
                .addShape(StructureShape.builder()
                        .id("com.example#CommonError")
                        .addMember("message", ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(StructureShape.builder()
                        .id("com.example#Input1")
                        .addMember("id", ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(StructureShape.builder().id("com.example#Output1").build())
                .addShape(StructureShape.builder()
                        .id("com.example#Input2")
                        .addMember("name", ShapeId.from("smithy.api#String"))
                        .build())
                .addShape(StructureShape.builder().id("com.example#Output2").build())
                .addShape(StructureShape.builder().id("com.example#Input3").build())
                .addShape(StructureShape.builder().id("com.example#Output3").build())
                .addShape(OperationShape.builder()
                        .id("com.example#Op1")
                        .input(ShapeId.from("com.example#Input1"))
                        .output(ShapeId.from("com.example#Output1"))
                        .build())
                .addShape(OperationShape.builder()
                        .id("com.example#Op2")
                        .input(ShapeId.from("com.example#Input2"))
                        .output(ShapeId.from("com.example#Output2"))
                        .build())
                .addShape(OperationShape.builder()
                        .id("com.example#Op3")
                        .input(ShapeId.from("com.example#Input3"))
                        .output(ShapeId.from("com.example#Output3"))
                        .build())
                .addShape(ServiceShape.builder()
                        .id("com.example#MyService")
                        .version("1.0")
                        .addOperation("com.example#Op1")
                        .addOperation("com.example#Op2")
                        .addOperation("com.example#Op3")
                        .addError("com.example#CommonError")
                        .build())
                .build();

        byte[] data = SmfWriter.builder().build().serialize(model);

        // Load only the service + Op1 — should get service, Op1, Input1, Output1, CommonError
        Model selective = SmfReader.readSelective(data,
                SelectiveLoadRequest.builder()
                        .service(ShapeId.from("com.example#MyService"))
                        .addOperation(ShapeId.from("com.example#Op1"))
                        .build());

        // Service shape present
        assertNotNull(selective.getShape(ShapeId.from("com.example#MyService")).orElse(null));
        // Requested operation and its closure
        assertNotNull(selective.getShape(ShapeId.from("com.example#Op1")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Input1")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Output1")).orElse(null));
        // Common error included
        assertNotNull(selective.getShape(ShapeId.from("com.example#CommonError")).orElse(null));
        // Unrequested operations and their shapes NOT included
        assertFalse(selective.getShape(ShapeId.from("com.example#Op2")).isPresent());
        assertFalse(selective.getShape(ShapeId.from("com.example#Op3")).isPresent());
        assertFalse(selective.getShape(ShapeId.from("com.example#Input2")).isPresent());
        assertFalse(selective.getShape(ShapeId.from("com.example#Input3")).isPresent());
    }

    @Test
    public void selectiveLoadWithMetadataAndManyShapes() {
        Model.Builder mb = Model.builder();
        mb.putMetadataProperty("suppressions",
                Node.fromNodes(
                        Node.objectNode()
                                .withMember("id", Node.from("SomeRule"))
                                .withMember("namespace", Node.from("*"))));

        ServiceShape.Builder svc = ServiceShape.builder()
                .id("com.example#BigService")
                .version("1.0");
        for (int i = 0; i < 50; i++) {
            mb.addShape(StructureShape.builder()
                    .id("com.example#In" + i)
                    .addMember("id", ShapeId.from("smithy.api#String"))
                    .build());
            mb.addShape(StructureShape.builder().id("com.example#Out" + i).build());
            mb.addShape(OperationShape.builder()
                    .id("com.example#Op" + i)
                    .input(ShapeId.from("com.example#In" + i))
                    .output(ShapeId.from("com.example#Out" + i))
                    .build());
            svc.addOperation("com.example#Op" + i);
        }
        mb.addShape(StructureShape.builder()
                .id("com.example#Err")
                .addMember("msg", ShapeId.from("smithy.api#String"))
                .build());
        svc.addError("com.example#Err");
        mb.addShape(svc.build());

        Model model = mb.build();
        byte[] data = SmfWriter.builder().build().serialize(model);

        Model selective = SmfReader.readSelective(data,
                SelectiveLoadRequest.builder()
                        .service(ShapeId.from("com.example#BigService"))
                        .addOperation(ShapeId.from("com.example#Op3"))
                        .build());

        assertNotNull(selective.getShape(ShapeId.from("com.example#BigService")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Op3")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#In3")).orElse(null));
        assertNotNull(selective.getShape(ShapeId.from("com.example#Err")).orElse(null));
        assertFalse(selective.getShape(ShapeId.from("com.example#Op10")).isPresent());
    }
}
