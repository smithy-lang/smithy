/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.IoUtils;

/** Document and streaming-payload I/O for {@code smithy call}. */
final class CallIo {
    private static final JsonCodec CODEC = JsonCodec.builder()
            .useStringForArbitraryPrecision(true)
            .build();

    private CallIo() {}

    static Document readInput(String input) {
        if (input == null) {
            return null;
        }
        byte[] bytes;
        try {
            if (input.equals("-")) {
                bytes = IoUtils.toByteArray(System.in);
            } else if (input.startsWith("@")) {
                bytes = Files.readAllBytes(Path.of(input.substring(1)));
            } else {
                bytes = input.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new CliError("Unable to read --input: " + e.getMessage());
        }
        if (bytes.length == 0) {
            return null;
        }
        try (var deserializer = CODEC.createDeserializer(bytes)) {
            return deserializer.readDocument();
        }
    }

    static String streamingMember(OperationShape operation, Model model, boolean input) {
        Shape container = model.getShape(input ? operation.getInputShape() : operation.getOutputShape()).orElse(null);
        if (container == null) {
            return null;
        }
        for (var member : container.getAllMembers().values()) {
            Shape target = model.getShape(member.getTarget()).orElse(null);
            if (target != null && target.hasTrait("smithy.api#streaming")) {
                return member.getMemberName();
            }
        }
        return null;
    }

    static Document withStreamingPayload(Document input, String member, String source) {
        DataStream stream;
        if (source.equals("-")) {
            stream = DataStream.ofInputStream(System.in);
        } else {
            Path path = Path.of(source);
            if (!Files.isRegularFile(path)) {
                throw new CliError("--input-payload file not found: " + source);
            }
            stream = DataStream.ofFile(path);
        }
        Map<String, Document> members = new LinkedHashMap<>();
        if (input != null && input.asStringMap() != null) {
            members.putAll(input.asStringMap());
        }
        members.put(member, Document.ofObject(stream));
        return Document.of(members);
    }

    static Document echoInput(Document input, String streamingMember) {
        if (input == null || streamingMember == null
                || input.asStringMap() == null
                || !input.asStringMap().containsKey(streamingMember)) {
            return input;
        }
        Map<String, Document> copy = new LinkedHashMap<>(input.asStringMap());
        copy.put(streamingMember, Document.of("<streaming payload>"));
        return Document.of(copy);
    }

    static Document writeStreamingOutput(Document result, String member, String outputFile) {
        Document body = result.getMember(member);
        if (body != null) {
            try (var out = Files.newOutputStream(Path.of(outputFile))) {
                body.asDataStream().writeTo(out);
            } catch (IOException e) {
                throw new CliError("Unable to write --output-payload " + outputFile + ": " + e.getMessage());
            }
        }
        Map<String, Document> output = new LinkedHashMap<>();
        if (result.asStringMap() != null) {
            result.asStringMap()
                    .forEach((name, value) -> output.put(name, name.equals(member) ? Document.of(outputFile) : value));
        }
        output.putIfAbsent(member, Document.of(outputFile));
        return Document.of(output);
    }

    static void warmup() {
        CODEC.createSerializer(new ByteArrayOutputStream()).flush();
    }
}
