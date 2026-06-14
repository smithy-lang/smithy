/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import com.code_intelligence.jazzer.junit.FuzzTest;
import java.nio.charset.StandardCharsets;
import software.amazon.smithy.model.node.Node;

/**
 * Fuzzes the JSON parser ({@link Node#parse}, backed by {@code JsonAstReader}).
 *
 * <p>Goal: no input — however malformed — should ever make the parser throw anything other than the
 * declared {@link ModelSyntaxException} (or terminate abnormally, hang, or OOM). Any other throwable
 * escaping the parser is a bug Jazzer will surface.
 */
class JsonParseFuzzTest {

    private static final int MAX_INPUT = 64 * 1024;

    @FuzzTest
    void parseString(byte[] data) {
        if (data.length > MAX_INPUT) {
            return;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        try {
            Node.parse(json);
        } catch (ModelSyntaxException expected) {
            // The only exception the parser is allowed to throw for bad input.
        }
    }

    @FuzzTest
    void parseStringWithComments(byte[] data) {
        if (data.length > MAX_INPUT) {
            return;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        try {
            Node.parseJsonWithComments(json);
        } catch (ModelSyntaxException expected) {
            // Allowed.
        }
    }

    @FuzzTest
    void roundTripParsedNode(byte[] data) {
        if (data.length > MAX_INPUT) {
            return;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        Node parsed;
        try {
            parsed = Node.parse(json);
        } catch (ModelSyntaxException expected) {
            return;
        }
        // A successfully-parsed node must serialize and re-parse to an equal node.
        String printed = Node.printJson(parsed);
        Node reparsed = Node.parse(printed);
        if (!parsed.equals(reparsed)) {
            throw new AssertionError("Round-trip mismatch.\n  input:    " + json
                    + "\n  printed:  " + printed);
        }
    }
}
