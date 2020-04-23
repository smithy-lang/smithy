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

package software.amazon.smithy.model.shapes;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable identifier for each shape in a model.
 *
 * <p>A shape ID is constructed from an absolute or relative shape
 * reference. A shape reference has the following structure:
 *
 * {@code NAMESPACE#NAME$MEMBER}
 *
 * <p>An absolute reference contains a namespace and a pound sign.
 * A relative reference omits the namespace and pound sign prefix. In
 * both absolute and relative shape references, the member is optional.
 *
 * <ul>
 *   <li>Relative path : {@code ShapeName}</li>
 *   <li>Relative path with a member : {@code ShapeName$memberName}</li>
 *   <li>Absolute path : {@code name.space#ShapeName}</li>
 *   <li>Absolute path with a member : {@code name.space#ShapeName$memberName}</li>
 * </ul>
 */
public final class ShapeId implements ToShapeId, Comparable<ShapeId> {

    private final String namespace;
    private final String name;
    private final String member;
    private final String absoluteName;
    private int hash;

    private ShapeId(String absoluteName, String namespace, String name, String member) {
        this.namespace = namespace;
        this.name = name;
        this.member = member;
        this.absoluteName = absoluteName;
    }

    private ShapeId(String namespace, String name, String member) {
        this(buildAbsoluteIdFromParts(namespace, name, member), namespace, name, member);
    }

    /**
     * Creates an absolute shape ID from the given string.
     *
     * @param absoluteShapeId Shape ID to parse.
     * @return The parsed ID.
     * @throws ShapeIdSyntaxException when the ID is malformed.
     */
    public static ShapeId from(String absoluteShapeId) {
        int namespacePosition = absoluteShapeId.indexOf('#');
        if (namespacePosition <= 0 || namespacePosition == absoluteShapeId.length() - 1) {
            throw new ShapeIdSyntaxException("Invalid shape ID: " + absoluteShapeId);
        }

        String namespace = absoluteShapeId.substring(0, namespacePosition);
        String name;
        String memberName = null;

        int memberPosition = absoluteShapeId.indexOf('$');
        if (memberPosition == -1) {
            name = absoluteShapeId.substring(namespacePosition + 1);
        } else if (memberPosition < namespacePosition) {
            throw new ShapeIdSyntaxException("Invalid shape ID: " + absoluteShapeId);
        } else {
            name = absoluteShapeId.substring(namespacePosition + 1, memberPosition);
            memberName = absoluteShapeId.substring(memberPosition + 1);
        }

        validateParts(absoluteShapeId, namespace, name, memberName);
        return new ShapeId(absoluteShapeId, namespace, name, memberName);
    }

    /**
     * Checks if the given string is a valid namespace.
     *
     * @param namespace Namespace value to check.
     * @return Returns true if this is a valid namespace.
     */
    public static boolean isValidNamespace(CharSequence namespace) {
        if (namespace == null) {
            return false;
        }

        boolean start = true;
        for (int position = 0; position < namespace.length(); position++) {
            char c = namespace.charAt(position);
            if (start) {
                start = false;
                if (!isValidIdentifierStart(c)) {
                    return false;
                }
            } else if (c == '.') {
                start = true;
            } else if (!isValidIdentifierAfterStart(c)) {
                return false;
            }
        }

        return !start;
    }

    /**
     * Checks if the given string is a valid identifier.
     *
     * @param identifier Identifier value to check.
     * @return Returns true if this is a valid identifier.
     */
    public static boolean isValidIdentifier(CharSequence identifier) {
        if (identifier == null || identifier.length() == 0) {
            return false;
        }

        if (!isValidIdentifierStart(identifier.charAt(0))) {
            return false;
        }

        for (int i = 1; i < identifier.length(); i++) {
            if (!isValidIdentifierAfterStart(identifier.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidIdentifierStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private static boolean isValidIdentifierAfterStart(char c) {
        return isValidIdentifierStart(c) || (c >= '0' && c <= '9');
    }

    /**
     * Creates an absolute shape ID from parts of a shape ID.
     *
     * @param namespace Namespace of the shape.
     * @param name Name of the shape.
     * @param member Optional/nullable member name.
     * @return The parsed ID.
     * @throws ShapeIdSyntaxException when the ID is malformed.
     */
    public static ShapeId fromParts(String namespace, String name, String member) {
        String idFromParts = buildAbsoluteIdFromParts(namespace, name, member);
        validateParts(idFromParts, namespace, name, member);
        return new ShapeId(idFromParts, namespace, name, member);
    }

    private static void validateParts(String absoluteId, String namespace, String name, String member) {
        if (!isValidNamespace(namespace)
                || !isValidIdentifier(name)
                || (member != null && !isValidIdentifier(member))) {
            throw new ShapeIdSyntaxException("Invalid shape ID: " + absoluteId);
        }
    }

    private static String buildAbsoluteIdFromParts(String namespace, String name, String member) {
        if (member != null) {
            return namespace + '#' + name + '$' + member;
        } else {
            return namespace + '#' + name;
        }
    }

    /**
     * Creates an absolute shape ID from parts of a shape ID.
     *
     * @param namespace Namespace of the shape.
     * @param name Name of the shape.
     * @return The parsed ID.
     * @throws ShapeIdSyntaxException when the ID is malformed.
     */
    public static ShapeId fromParts(String namespace, String name) {
        return fromParts(namespace, name, null);
    }

    /**
     * Builds a {@code Id} from a relative shape reference.
     *
     * <p>The given shape reference must not contain a namespace prefix.
     * It may contain a member.
     *
     * @param namespace The namespace.
     * @param relativeName A relative shape reference.
     * @return Returns a {@code Id} extracted from {@code relativeName}.
     * @throws ShapeIdSyntaxException when the namespace or shape reference
     * is malformed.
     */
    public static ShapeId fromRelative(String namespace, String relativeName) {
        Objects.requireNonNull(namespace, "Shape ID namespace must not be null");
        Objects.requireNonNull(relativeName, "Shape ID relative name must not be null");
        if (relativeName.contains("#")) {
            throw new ShapeIdSyntaxException("Relative shape ID must not contain a namespace: " + relativeName);
        }
        return from(namespace + "#" + relativeName);
    }

    /**
     * Builds a {@code Id} from the given reference.
     *
     * <p>If the shape reference contains a namespace, it is treated as an
     * absolute reference. If it does not contain a namespace prefix, it is
     * treated as a relative shape reference and the given default namespace
     * is used.
     *
     * @param defaultNamespace The namespace to use when the shape reference
     * does not contain a namespace.
     * @param shapeName A relative or absolute shape reference.
     * @return Returns a {@code Id} extracted from shape reference.
     * @throws ShapeIdSyntaxException when the namespace or shape reference
     * is malformed.
     */
    public static ShapeId fromOptionalNamespace(String defaultNamespace, String shapeName) {
        Objects.requireNonNull(shapeName, "Shape name must not be null");
        if (defaultNamespace == null || shapeName.contains("#")) {
            return ShapeId.from(shapeName);
        } else {
            return fromRelative(defaultNamespace, shapeName);
        }
    }

    /**
     * Creates a new Shape.Id with a member add to it.
     *
     * @param member Member to set.
     * @return returns a new Shape.Id
     * @throws ShapeIdSyntaxException if the member name syntax is invalid.
     */
    public ShapeId withMember(String member) {
        if (!isValidIdentifier(member)) {
            throw new ShapeIdSyntaxException("Invalid shape ID member: " + member);
        }

        return new ShapeId(namespace, name, member);
    }

    @Override
    public ShapeId toShapeId() {
        return this;
    }

    @Override
    public int compareTo(ShapeId other) {
        int outcome = toString().compareToIgnoreCase(other.toString());
        if (outcome == 0) {
            // If they're case-insensitively equal, use a case-sensitive comparison as a tie-breaker.
            return toString().compareTo(other.toString());
        }
        return outcome;
    }

    /**
     * Creates a new Shape.Id with no member.
     *
     * @return returns a new Shape.Id
     */
    public ShapeId withoutMember() {
        return new ShapeId(namespace, name, null);
    }

    /**
     * Get the namespace of the shape.
     *
     * @return Returns the namespace.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the name of the shape.
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the optional member of the shape.
     *
     * @return Returns the optional member.
     */
    public Optional<String> getMember() {
        return Optional.ofNullable(member);
    }

    /**
     * Creates a string that contains a relative reference to the ID.
     *
     * @return Returns a relative shape ID string with no namespace.
     */
    public String asRelativeReference() {
        return member == null ? name : name + "$" + member;
    }

    /**
     * Converts the {@code Id} into a shape ID string.
     *
     * For example: "com.foo.bar#Baz$member".
     *
     * @return Returns a shape ID as a string.
     */
    @Override
    public String toString() {
        return absoluteName;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShapeId && other.toString().equals(this.toString());
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = 17 + 31 * namespace.hashCode() * 31 + name.hashCode() * 17 + Objects.hashCode(member);
        }
        return hash;
    }
}
