/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Formats BDD node structures to a stream without building the entire representation in memory.
 */
public final class BddFormatter {

    private final Writer writer;
    private final int[][] nodes;
    private final int rootRef;
    private final int conditionCount;
    private final int resultCount;
    private final String indent;

    private BddFormatter(Builder builder) {
        this.writer = builder.writer;
        this.nodes = builder.nodes;
        this.rootRef = builder.rootRef;
        this.conditionCount = builder.conditionCount;
        this.resultCount = builder.resultCount;
        this.indent = builder.indent;
    }

    /**
     * Creates a builder for BddFormatter.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Formats the BDD node structure.
     */
    public void format() {
        try {
            // Calculate formatting widths
            FormatContext ctx = calculateFormatContext();

            // Write root
            writer.write(indent);
            writer.write("Root: ");
            writer.write(formatReference(rootRef));
            writer.write("\n");

            // Write nodes
            writer.write(indent);
            writer.write("Nodes:\n");

            for (int i = 0; i < nodes.length; i++) {
                writer.write(indent);
                writer.write("    ");
                writer.write(String.format("%" + ctx.indexWidth + "d", i));
                writer.write(": ");

                if (i == 0 && nodes[i][0] == -1) {
                    writer.write("terminal");
                } else {
                    formatNode(nodes[i], ctx);
                }
                writer.write("\n");
            }

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private FormatContext calculateFormatContext() {
        int maxVarIdx = -1;

        // Scan nodes to find max variable index
        for (int i = 1; i < nodes.length; i++) {
            int varIdx = nodes[i][0];
            if (varIdx >= 0) {
                maxVarIdx = Math.max(maxVarIdx, varIdx);
            }
        }

        // Calculate widths
        int conditionWidth = conditionCount > 0 ? String.valueOf(conditionCount - 1).length() + 1 : 2;
        int resultWidth = resultCount > 0 ? String.valueOf(resultCount - 1).length() + 1 : 2;
        int varWidth = Math.max(Math.max(conditionWidth, resultWidth), String.valueOf(maxVarIdx).length());
        int indexWidth = String.valueOf(nodes.length - 1).length();

        return new FormatContext(varWidth, indexWidth);
    }

    private void formatNode(int[] node, FormatContext ctx) throws IOException {
        writer.write("[");

        // Variable reference
        int varIdx = node[0];
        String varRef = formatVariableIndex(varIdx);
        writer.write(String.format("%" + ctx.varWidth + "s", varRef));

        // High and low references
        writer.write(", ");
        writer.write(String.format("%6s", formatReference(node[1])));
        writer.write(", ");
        writer.write(String.format("%6s", formatReference(node[2])));
        writer.write("]");
    }

    private String formatVariableIndex(int varIdx) {
        if (conditionCount > 0 && varIdx < conditionCount) {
            return "C" + varIdx;
        } else if (conditionCount > 0 && resultCount > 0) {
            return "R" + (varIdx - conditionCount);
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

    /**
     * Builder for BddFormatter.
     */
    public static final class Builder {
        private Writer writer;
        private int[][] nodes;
        private int rootRef;
        private int conditionCount = 0;
        private int resultCount = 0;
        private String indent = "";

        private Builder() {}

        public Builder writer(Writer writer) {
            this.writer = writer;
            return this;
        }

        public Builder writer(OutputStream out) {
            return writer(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        }

        public Builder nodes(int[][] nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder rootRef(int rootRef) {
            this.rootRef = rootRef;
            return this;
        }

        public Builder conditionCount(int conditionCount) {
            this.conditionCount = conditionCount;
            return this;
        }

        public Builder resultCount(int resultCount) {
            this.resultCount = resultCount;
            return this;
        }

        public Builder indent(String indent) {
            this.indent = indent;
            return this;
        }

        public BddFormatter build() {
            if (writer == null) {
                throw new IllegalStateException("writer is required");
            }
            if (nodes == null) {
                throw new IllegalStateException("nodes are required");
            }
            return new BddFormatter(this);
        }
    }
}
