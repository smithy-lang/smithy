/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.smithy.model.loader.ParserUtils;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;

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

    /** LRA (least recently added) cache of parsed shape IDs. */
    private static final ShapeIdFactory FACTORY = new ShapeIdFactory();

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
     * @param id Shape ID to parse.
     * @return The parsed ID.
     * @throws ShapeIdSyntaxException when the ID is malformed.
     */
    public static ShapeId from(String id) {
        return FACTORY.create(id);
    }

    public static ShapeId fromNode(Node node) {
        return node.expectStringNode().expectShapeId();
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
        } else if (namespace.equals(Prelude.NAMESPACE)) {
            // Shortcut for prelude namespaces.
            return true;
        }

        int length = namespace.length();
        if (length == 0) {
            return false;
        }

        int position = 0;
        while (true) {
            position = parseIdentifier(namespace, position);
            if (position == -1) { // Bad: did not parse a valid identifier.
                return false;
            } else if (position == length) { // Good: parsed and reached the end.
                return true;
            } else if (namespace.charAt(position) != '.') { // Bad: invalid character.
                return false;
            } else if (++position >= length) { // Bad: trailing '.'
                return false;
            } // continue parsing after '.', expecting an identifier.
        }
    }

    /**
     * Checks if the given string is a valid identifier.
     *
     * @param identifier Identifier value to check.
     * @return Returns true if this is a valid identifier.
     */
    public static boolean isValidIdentifier(CharSequence identifier) {
        return parseIdentifier(identifier, 0) == identifier.length();
    }

    private static int parseIdentifier(CharSequence identifier, int offset) {
        if (identifier == null || identifier.length() <= offset) {
            return -1;
        }

        // Parse the required IdentifierStart production.
        char startingChar = identifier.charAt(offset);
        if (startingChar == '_') {
            while (offset < identifier.length() && identifier.charAt(offset) == '_') {
                offset++;
            }
            if (offset == identifier.length()) {
                return -1;
            }
            char current = identifier.charAt(offset);
            if (!ParserUtils.isAlphabetic(current) && !ParserUtils.isDigit(current)) {
                return -1;
            }
            offset++;
        } else if (!ParserUtils.isAlphabetic(startingChar)) {
            return -1;
        }

        // Parse the optional IdentifierChars production.
        while (offset < identifier.length()) {
            if (!ParserUtils.isValidIdentifierCharacter(identifier.charAt(offset))) {
                // Return the position of the character that stops the identifier.
                // This is either an invalid case (e.g., isValidIdentifier), or
                // just the marker needed for isValidNamespace to find '.'.
                return offset;
            }
            offset++;
        }

        return offset;
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
     * @return returns a new Shape.Id, or the existing shape if it has no member.
     */
    public ShapeId withoutMember() {
        if (member == null) {
            return this;
        } else {
            return new ShapeId(namespace, name, null);
        }
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
     * <p>Use {@link #getName(ServiceShape)} when performing transformations
     * like code generation of the shapes used in services or clients.
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the shape when it is used within the contextual
     * closure of a service.
     *
     * <p>This method should be used when performing transformations like
     * code generation of a Smithy model. Service shapes can rename shapes
     * used within the closure of a service to give shapes unambiguous names
     * independent of a namespace.
     *
     * <p>This is a mirror of {@link ServiceShape#getContextualName(ToShapeId)}
     * that serves to make this functionality more discoverable.
     *
     * @param service Service shape used to contextualize the name.
     * @return Returns the contextualized shape name when used in a service.
     */
    public String getName(ServiceShape service) {
        return service.getContextualName(this);
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
     * Checks if the ID has a member set.
     *
     * @return Returns true if the ID has a member.
     */
    public boolean hasMember() {
        return member != null;
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
     * Creates a shape ID that uses a different namespace than the current ID.
     *
     * @param namespace Namespace to use.
     * @return Returns the shape ID with the changed namespace.
     * @throws ShapeIdSyntaxException if the namespace is invalid.
     */
    public ShapeId withNamespace(String namespace) {
        if (this.namespace.equals(namespace)) {
            return this;
        } else if (!isValidNamespace(namespace)) {
            throw new ShapeIdSyntaxException("Invalid shape ID: " + namespace);
        } else {
            return new ShapeId(namespace, name, member);
        }
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
        int h = hash;

        if (h == 0) {
            h = 17 + 31 * namespace.hashCode() * 31 + name.hashCode() * 17 + Objects.hashCode(member);
            hash = h;
        }

        return h;
    }

    /**
     * A least-recently used flyweight factory that creates shape IDs.
     *
     * <p>Prelude IDs are stored separately from non-prelude IDs because we can make a reasonable estimate about the
     * size of the prelude and stop caching IDs when that size is exceeded. Prelude shapes are stored in a
     * ConcurrentHashMap with a bounded size. Once the size exceeds 500, then items are no longer stored in the cache.
     * Non-prelude shapes are stored in a bounded, synchronized LRU cache based on {@link LinkedHashMap}.
     */
    private static final class ShapeIdFactory {
        private static final int NON_PRELUDE_MAX_SIZE = 8192;
        private static final int PRELUDE_MAX_SIZE = 500;
        private static final String PRELUDE_PREFIX = Prelude.NAMESPACE + '#';

        private final Map<String, ShapeId> nonPreludeCache;
        private final ConcurrentMap<String, ShapeId> preludeCache;

        ShapeIdFactory() {
            preludeCache = new ConcurrentHashMap<>(PRELUDE_MAX_SIZE);

            // A simple LRU cache based on LinkedHashMap, wrapped in a synchronized map.
            nonPreludeCache = Collections.synchronizedMap(new LinkedHashMap<String, ShapeId>(
                    NON_PRELUDE_MAX_SIZE + 1,
                    1.0f,
                    true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ShapeId> eldest) {
                    return this.size() > NON_PRELUDE_MAX_SIZE;
                }
            });
        }

        ShapeId create(final String key) {
            if (key.startsWith(PRELUDE_PREFIX)) {
                return getPreludeId(key);
            } else {
                return getNonPreludeId(key);
            }
        }

        private ShapeId getPreludeId(String key) {
            // computeIfAbsent isn't used here since we need to limit the cache size and creating multiple IDs
            // simultaneously isn't an issue.
            ShapeId result = preludeCache.get(key);
            if (result != null) {
                return result;
            }

            // The ID wasn't found so build it, and add it to the cache if the cache isn't too big already.
            result = buildShapeId(key);

            if (preludeCache.size() <= PRELUDE_MAX_SIZE) {
                preludeCache.putIfAbsent(key, result);
            }

            return result;
        }

        private ShapeId getNonPreludeId(String key) {
            return nonPreludeCache.computeIfAbsent(key, ShapeIdFactory::buildShapeId);
        }

        private static ShapeId buildShapeId(String absoluteShapeId) {
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
    }
}
