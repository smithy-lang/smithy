/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;

/**
 * A pull-based cursor over a Smithy JSON AST value, used to load models without committing to a
 * single source representation.
 */
interface AstReader {

    /**
     * The kind of the value the cursor is currently positioned on.
     */
    enum Type {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }

    /**
     * Renders a {@link Type} using the same lowercase names {@code NodeType} uses in type-mismatch
     * messages, so reader errors read identically to the node-based loader's errors.
     *
     * @param type the type to describe (may be {@code null}).
     * @return the lowercase type name.
     */
    static String describe(Type type) {
        if (type == null) {
            return "null";
        }
        switch (type) {
            case OBJECT:
                return "object";
            case ARRAY:
                return "array";
            case STRING:
                return "string";
            case NUMBER:
                return "number";
            case BOOLEAN:
                return "boolean";
            case NULL:
            default:
                return "null";
        }
    }

    /**
     * @return the type of the current value.
     */
    Type currentType();

    /**
     * @return the source location of the current value.
     */
    SourceLocation currentLocation();

    /**
     * Begins reading the current value as an object. The cursor must be positioned on an
     * {@link Type#OBJECT}. After this call, use {@link #nextKey()} to iterate members.
     */
    void startObject();

    /**
     * Advances to the next member of the object started with {@link #startObject()} and positions the
     * cursor on that member's value.
     *
     * @return the member key, or {@code null} when the object has no more members
     */
    String nextKey();

    /**
     * @return the source location of the key most recently returned by {@link #nextKey()}.
     */
    SourceLocation lastKeyLocation();

    /**
     * Begins reading the current value as an array. The cursor must be positioned on an
     * {@link Type#ARRAY}. After this call, use {@link #nextElement()} to iterate elements.
     */
    void startArray();

    /**
     * Advances to the next element of the array started with {@link #startArray()}, positioning the
     * cursor on it.
     *
     * @return {@code true} if positioned on an element, {@code false} when the array is fully consumed.
     */
    boolean nextElement();

    /**
     * Reads the current value as a string.
     *
     * @return the string value.
     * @throws SourceException if the current value is not a string.
     */
    String expectStringValue(String label);

    /**
     * Materializes the current value (and any nested values) as a {@link Node}, advancing past it.
     *
     * <p>For {@link NodeAstReader} this returns the existing subtree without copying.
     *
     * @return the value as a Node.
     */
    Node readValueAsNode();

    /**
     * Materializes the object the cursor is currently iterating as a {@link Node}, given that its
     * opening and first member key have already been consumed (the cursor is on the first member's
     * value). Used by the loader to buffer a shape definition whose keys are out of order.
     *
     * @param objectLocation Source location of the object's opening.
     * @param firstKey The already-read first member name.
     * @param firstKeyLocation Source location of the first member name.
     * @return the completed object as a Node.
     */
    Node finishObjectAsNode(SourceLocation objectLocation, String firstKey, SourceLocation firstKeyLocation);

    /**
     * Skips the current value (and any nested values), advancing past it.
     */
    void skipValue();

    /**
     * @return the current container nesting depth (0 at the document root).
     */
    int depth();

    /**
     * Unwinds the cursor by closing containers until it returns to {@code targetDepth}. Used to
     * recover cleanly after an error thrown while iterating somewhere inside a shape definition.
     *
     * @param targetDepth the depth to unwind back to.
     */
    void skipToDepth(int targetDepth);
}
