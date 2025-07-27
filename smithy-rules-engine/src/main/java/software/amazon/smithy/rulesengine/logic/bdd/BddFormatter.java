/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Formats BDD node structures to a writer.
 */
public final class BddFormatter {

    private final Bdd bdd;
    private final Writer writer;
    private final String indent;

    /**
     * Creates a BDD formatter.
     *
     * @param bdd the BDD to format
     * @param writer the writer to format to
     * @param indent the indentation string
     */
    public BddFormatter(Bdd bdd, Writer writer, String indent) {
        this.bdd = bdd;
        this.writer = writer;
        this.indent = indent;
    }

    /**
     * Formats a BDD to a string.
     *
     * @param bdd the BDD to format
     * @return a formatted string representation
     */
    public static String format(Bdd bdd) {
        return format(bdd, "");
    }

    /**
     * Formats a BDD to a string with custom indent.
     *
     * @param bdd the BDD to format
     * @param indent the indentation string
     * @return a formatted string representation
     */
    public static String format(Bdd bdd, String indent) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            new BddFormatter(bdd, writer, indent).format();
            writer.flush();
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            // Should never happen with ByteArrayOutputStream
            throw new RuntimeException("Failed to format BDD", e);
        }
    }

    /**
     * Formats the BDD structure.
     *
     * @throws IOException if writing fails
     */
    public void format() throws IOException {
        // Calculate formatting widths
        FormatContext ctx = calculateFormatContext();

        // Write header
        writer.write(indent);
        writer.write("Bdd {\n");

        // Write counts
        writer.write(indent);
        writer.write("  conditions: ");
        writer.write(String.valueOf(bdd.getConditionCount()));
        writer.write("\n");

        writer.write(indent);
        writer.write("  results: ");
        writer.write(String.valueOf(bdd.getResultCount()));
        writer.write("\n");

        // Write root
        writer.write(indent);
        writer.write("  root: ");
        writer.write(formatReference(bdd.getRootRef()));
        writer.write("\n");

        // Write nodes
        writer.write(indent);
        writer.write("  nodes (");
        writer.write(String.valueOf(bdd.getNodeCount()));
        writer.write("):\n");

        for (int i = 0; i < bdd.getNodeCount(); i++) {
            writer.write(indent);
            writer.write("    ");
            writer.write(String.format("%" + ctx.indexWidth + "d", i));
            writer.write(": ");

            if (i == 0 && bdd.getVariable(0) == -1) {
                writer.write("terminal");
            } else {
                formatNode(i, ctx);
            }
            writer.write("\n");
        }

        writer.write(indent);
        writer.write("}");
    }

    private FormatContext calculateFormatContext() {
        int nodeCount = bdd.getNodeCount();
        int maxVarIdx = -1;

        // Scan nodes to find max variable index
        for (int i = 1; i < nodeCount; i++) {
            int varIdx = bdd.getVariable(i);
            if (varIdx >= 0) {
                maxVarIdx = Math.max(maxVarIdx, varIdx);
            }
        }

        // Calculate widths
        int conditionCount = bdd.getConditionCount();
        int resultCount = bdd.getResultCount();
        int conditionWidth = conditionCount > 0 ? String.valueOf(conditionCount - 1).length() + 1 : 2;
        int resultWidth = resultCount > 0 ? String.valueOf(resultCount - 1).length() + 1 : 2;
        int varWidth = Math.max(Math.max(conditionWidth, resultWidth), String.valueOf(maxVarIdx).length());
        int indexWidth = String.valueOf(nodeCount - 1).length();

        return new FormatContext(varWidth, indexWidth);
    }

    private void formatNode(int nodeIndex, FormatContext ctx) throws IOException {
        writer.write("[");

        // Variable reference
        int varIdx = bdd.getVariable(nodeIndex);
        String varRef = formatVariableIndex(varIdx);
        writer.write(String.format("%" + ctx.varWidth + "s", varRef));

        // High and low references
        writer.write(", ");
        writer.write(String.format("%6s", formatReference(bdd.getHigh(nodeIndex))));
        writer.write(", ");
        writer.write(String.format("%6s", formatReference(bdd.getLow(nodeIndex))));
        writer.write("]");
    }

    private String formatVariableIndex(int varIdx) {
        if (bdd.getConditionCount() > 0 && varIdx < bdd.getConditionCount()) {
            return "C" + varIdx;
        } else if (bdd.getConditionCount() > 0 && bdd.getResultCount() > 0) {
            return "R" + (varIdx - bdd.getConditionCount());
        } else {
            return String.valueOf(varIdx);
        }
    }

    /**
     * Formats a BDD reference (node pointer) to a human-readable string.
     *
     * @param ref the reference to format
     * @return the formatted reference string
     */
    public static String formatReference(int ref) {
        if (ref == 0) {
            return "INVALID";
        } else if (ref == 1) {
            return "TRUE";
        } else if (ref == -1) {
            return "FALSE";
        } else if (ref >= Bdd.RESULT_OFFSET) {
            return "R" + (ref - Bdd.RESULT_OFFSET);
        } else if (ref < 0) {
            return "!" + (-ref - 1);
        } else {
            return String.valueOf(ref - 1);
        }
    }

    private static class FormatContext {
        final int varWidth;
        final int indexWidth;

        FormatContext(int varWidth, int indexWidth) {
            this.varWidth = varWidth;
            this.indexWidth = indexWidth;
        }
    }
}
