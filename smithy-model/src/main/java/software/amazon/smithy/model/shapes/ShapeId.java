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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String IDENTIFIER = "[a-zA-Z_][a-zA-Z0-9_]*";
    public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^" + IDENTIFIER + "$");
    public static final Pattern NAMESPACE = Pattern.compile(String.format("%1$s(?:\\.%1$s)*", IDENTIFIER));
    public static final Pattern VALID_NAMESPACE = Pattern.compile("^" + NAMESPACE.pattern() + "$");
    // abc (invalid), abc.123 (invalid), abc.def (valid).
    private static final Pattern ID_PATTERN = Pattern.compile(
            String.format("^(?:(%s)#)?(%s)(?:\\$(%s))?$", NAMESPACE, IDENTIFIER, IDENTIFIER));

    private final String namespace;
    private final String name;
    private final String member;
    private final String absoluteName;
    private final int hash;

    private ShapeId(String absoluteName, String namespace, String name, String member) {
        this.namespace = namespace;
        this.name = name;
        this.member = member;
        this.absoluteName = absoluteName;
        this.hash = 17 + 31 * namespace.hashCode() * 31 + name.hashCode() * 17 + Objects.hashCode(member);
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
        Matcher matcher = ID_PATTERN.matcher(absoluteShapeId);
        if (!matcher.matches()) {
            throw new ShapeIdSyntaxException("Invalid shape ID: " + absoluteShapeId);
        } else if (matcher.group(1) == null) {
            throw new ShapeIdSyntaxException("Shape ID must contain a namespace: " + absoluteShapeId);
        }

        return new ShapeId(absoluteShapeId, matcher.group(1), matcher.group(2), matcher.group(3));
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
        return from(buildAbsoluteIdFromParts(namespace, name, member));
    }

    private static String buildAbsoluteIdFromParts(String namespace, String name, String member) {
        StringBuilder builder = new StringBuilder(namespace).append('#').append(name);
        if (member != null) {
            builder.append('$').append(member);
        }
        return builder.toString();
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
     *  is malformed.
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
     *  does not contain a namespace.
     * @param shapeName A relative or absolute shape reference.
     * @return Returns a {@code Id} extracted from shape reference.
     * @throws ShapeIdSyntaxException when the namespace or shape reference
     *  is malformed.
     */
    public static ShapeId fromOptionalNamespace(String defaultNamespace, String shapeName) {
        Objects.requireNonNull(shapeName, "Shape name must not be null");
        Objects.requireNonNull(defaultNamespace, "Default namespace must not be null");
        return shapeName.contains("#")
               ? ShapeId.from(shapeName)
               : fromRelative(defaultNamespace, shapeName);
    }

    /**
     * Creates a new Shape.Id with a member add to it.
     *
     * @param member Member to set.
     * @return returns a new Shape.Id
     * @throws ShapeIdSyntaxException if the member name syntax is invalid.
     */
    public ShapeId withMember(String member) {
        if (!IDENTIFIER_PATTERN.matcher(member).matches()) {
            throw new ShapeIdSyntaxException("Invalid shape ID member: " + member);
        }

        return new ShapeId(namespace, name, member);
    }

    public static boolean isValidNamespace(String namespace) {
        return VALID_NAMESPACE.matcher(namespace).find();
    }

    @Override
    public ShapeId toShapeId() {
        return this;
    }

    @Override
    public int compareTo(ShapeId other) {
        return toString().compareTo(other.toString());
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
        return hash;
    }
}
