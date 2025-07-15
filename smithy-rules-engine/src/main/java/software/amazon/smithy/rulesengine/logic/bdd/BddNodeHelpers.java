/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

final class BddNodeHelpers {
    private static final String LATEST_VERSION = "1.3";
    private static final int[] TERMINAL_NODE = new int[] {-1, 1, -1};

    private BddNodeHelpers() {}

    static Node toNode(Bdd bdd) {
        ObjectNode.Builder builder = ObjectNode.builder();
        builder.withMember("version", LATEST_VERSION);

        List<Node> conditions = new ArrayList<>();
        for (Condition c : bdd.getConditions()) {
            conditions.add(c.toNode());
        }

        List<Node> results = new ArrayList<>();
        for (Rule r : bdd.getResults()) {
            results.add(r.toNode());
        }

        return builder
                .withMember("parameters", bdd.getParameters().toNode())
                .withMember("conditions", Node.fromNodes(conditions))
                .withMember("results", Node.fromNodes(results))
                .withMember("root", bdd.getRootRef())
                .withMember("nodes", encodeNodes(bdd))
                .build();
    }

    static Bdd fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Parameters params = Parameters.fromNode(obj.expectObjectMember("parameters"));
        List<Condition> conditions = obj.expectArrayMember("conditions").getElementsAs(Condition::fromNode);
        List<Rule> results = obj.expectArrayMember("results").getElementsAs(Rule::fromNode);
        String nodesBase64 = obj.expectStringMember("nodes").getValue();
        List<int[]> nodes = decodeNodes(nodesBase64);
        int rootRef = obj.expectNumberMember("root").getValue().intValue();

        String version = obj.expectStringMember("version").getValue();
        if (!version.equals(LATEST_VERSION)) {
            throw new IllegalArgumentException("Unsupported BDD version: " + version);
        }

        return new Bdd(params, conditions, results, nodes, rootRef);
    }

    static String encodeNodes(Bdd bdd) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos)) {
            for (int[] node : bdd.getNodes()) {
                writeVarInt(dos, node[0]);
                writeVarInt(dos, node[1]);
                writeVarInt(dos, node[2]);
            }
            dos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode BDD nodes", e);
        }
    }

    static List<int[]> decodeNodes(String base64) {
        if (base64.isEmpty()) {
            return Collections.singletonList(TERMINAL_NODE);
        }

        byte[] data = Base64.getDecoder().decode(base64);
        List<int[]> nodes = new ArrayList<>();

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais)) {
            while (bais.available() > 0) {
                int varIdx = readVarInt(dis);
                int high = readVarInt(dis);
                int low = readVarInt(dis);
                nodes.add(new int[] {varIdx, high, low});
            }
            return nodes;
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode BDD nodes", e);
        }
    }

    // Zig-zag + varint encode of a signed int
    private static void writeVarInt(DataOutputStream dos, int value) throws IOException {
        int zz = (value << 1) ^ (value >> 31);
        while ((zz & ~0x7F) != 0) {
            dos.writeByte((zz & 0x7F) | 0x80);
            zz >>>= 7;
        }
        dos.writeByte(zz);
    }

    // Decode a signed int from varint + zig-zag.
    private static int readVarInt(DataInputStream dis) throws IOException {
        int shift = 0, result = 0;
        while (true) {
            byte b = dis.readByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                break;
            shift += 7;
        }
        // reverse zig-zag
        return (result >>> 1) ^ -(result & 1);
    }
}
