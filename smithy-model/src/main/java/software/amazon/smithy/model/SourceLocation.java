/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model;

import java.util.Objects;

/**
 * Represents the source location of a model component.
 */
public final class SourceLocation implements FromSourceLocation, Comparable<SourceLocation> {

    public static final SourceLocation NONE = new SourceLocation("N/A");

    private final String filename;
    private final int line;
    private final int column;
    private int hash;

    public SourceLocation(String filename, int line, int column) {
        this.filename = Objects.requireNonNull(filename);
        this.line = line;
        this.column = column;
    }

    public SourceLocation(String filename) {
        this(filename, 0, 0);
    }

    /**
     * Creates an empty source location.
     *
     * @return Returns the empty location.
     */
    public static SourceLocation none() {
        return NONE;
    }

    /**
     * @return Returns the source location filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return Returns the line number from which the component originated.
     */
    public int getLine() {
        return line;
    }

    /**
     * @return Returns the column from which the component originated.
     */
    public int getColumn() {
        return column;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return this;
    }

    @Override
    public String toString() {
        return filename.isEmpty()
               ? String.format("[%d, %d]", line, column)
               : String.format("%s [%d, %d]", filename, line, column);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SourceLocation && toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        int h = hash;

        if (h == 0) {
            h = 1 + filename.hashCode() + line * 17 + column;
            hash = h;
        }

        return h;
    }

    @Override
    public int compareTo(SourceLocation o) {
        if (!this.getFilename().equals(o.getFilename())) {
            return this.getFilename().compareTo(o.getFilename());
        }

        int lineComparison = Integer.compare(this.getLine(), o.getLine());
        if (lineComparison != 0) {
            return lineComparison;
        }

        return Integer.compare(this.getColumn(), o.getColumn());
    }
}
