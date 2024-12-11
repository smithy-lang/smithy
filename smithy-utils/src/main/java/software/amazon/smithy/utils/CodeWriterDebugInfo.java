/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Provides debug information about the current state of a CodeWriter.
 *
 * <p>The primary use case of this class is to be included in things like
 * exception messages thrown by {@link CodeWriter}. Additional metadata
 * can be appended to the debug info by calling {@link #putMetadata},
 * and this metadata will appear when {@link #toString()} is called.
 */
public final class CodeWriterDebugInfo {

    private final Map<String, String> metadata = new LinkedHashMap<>();

    /**
     * The currently package-private constructor. We can open this up later
     * if a use case arises. This is left package-private for now in case
     * we decide to refactor.
     */
    CodeWriterDebugInfo() {}

    /**
     * Get the CodeWriter state path from which the debug information was collected.
     *
     * @return Returns the path as returned by {@link AbstractCodeWriter#getDebugInfo()};
     */
    public String getStateDebugPath() {
        return getMetadata("path");
    }

    /**
     * Put additional debug metadata on the object.
     *
     * @param key Name of the value to set.
     * @param value Value to set that cannot be null.
     */
    public void putMetadata(String key, String value) {
        metadata.put(key, Objects.requireNonNull(value));
    }

    /**
     * Gets debug metadata by name.
     *
     * @param key Value to retrieve.
     * @return Returns the string value or null if not found.
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Returns a string representation that can be used in exception
     * messages and when debugging.
     *
     * @return Returns debug info as a string.
     */
    @Override
    public String toString() {
        StringJoiner result = new StringJoiner(", ", "(Debug Info {", "})");
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }
        return result.toString();
    }
}
