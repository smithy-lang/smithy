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
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.utils.SetUtils;

final class BddNodeHelpers {
    private static final int[] TERMINAL_NODE = new int[] {-1, 1, -1};
    private static final Set<String> ALLOWED_PROPERTIES = SetUtils.of(
            "parameters",
            "conditions",
            "results",
            "root",
            "nodes",
            "nodeCount");

    private BddNodeHelpers() {}

    static Node toNode(Bdd bdd) {
        ObjectNode.Builder builder = ObjectNode.builder();

        List<Node> conditions = new ArrayList<>();
        for (Condition c : bdd.getConditions()) {
            conditions.add(c.toNode());
        }

        List<Node> results = new ArrayList<>();
        if (!(bdd.getResults().get(0) instanceof NoMatchRule)) {
            throw new IllegalArgumentException("BDD must always have a NoMatchRule as the first result");
        }
        for (int i = 1; i < bdd.getResults().size(); i++) {
            Rule result = bdd.getResults().get(i);
            if (result instanceof NoMatchRule) {
                throw new IllegalArgumentException("NoMatch rules can only appear at rule index 0. Found at index" + i);
            }
            results.add(bdd.getResults().get(i).toNode());
        }

        return builder
                .withMember("parameters", bdd.getParameters().toNode())
                .withMember("conditions", Node.fromNodes(conditions))
                .withMember("results", Node.fromNodes(results))
                .withMember("root", bdd.getRootRef())
                .withMember("nodes", encodeNodes(bdd))
                .withMember("nodeCount", bdd.getNodes().length)
                .build();
    }

    static Bdd fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        obj.warnIfAdditionalProperties(ALLOWED_PROPERTIES);
        Parameters params = Parameters.fromNode(obj.expectObjectMember("parameters"));
        List<Condition> conditions = obj.expectArrayMember("conditions").getElementsAs(Condition::fromNode);

        // Read the results and prepend NoMatchRule at index 0
        List<Rule> serializedResults = obj.expectArrayMember("results").getElementsAs(Rule::fromNode);
        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE); // Always add no-match at index 0
        results.addAll(serializedResults);

        String nodesBase64 = obj.expectStringMember("nodes").getValue();
        int nodeCount = obj.expectNumberMember("nodeCount").getValue().intValue();
        int[][] nodes = decodeNodes(nodesBase64, nodeCount);
        int rootRef = obj.expectNumberMember("root").getValue().intValue();
        return new Bdd(params, conditions, results, nodes, rootRef);
    }

    static String encodeNodes(Bdd bdd) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos)) {
            int[][] nodes = bdd.getNodes();
            for (int[] node : nodes) {
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

    static int[][] decodeNodes(String base64, int nodeCount) {
        if (base64.isEmpty() || nodeCount == 0) {
            return new int[][] {TERMINAL_NODE};
        }

        byte[] data = Base64.getDecoder().decode(base64);
        int[][] nodes = new int[nodeCount][];

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais)) {
            for (int i = 0; i < nodeCount; i++) {
                int varIdx = readVarInt(dis);
                int high = readVarInt(dis);
                int low = readVarInt(dis);
                nodes[i] = new int[] {varIdx, high, low};
            }
            if (bais.available() > 0) {
                throw new IllegalArgumentException("Extra data found after decoding " + nodeCount +
                        " nodes. " + bais.available() + " bytes remaining.");
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
