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

package software.amazon.smithy.codegen.core;

import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Shades and renames shape IDs into a target namespace.
 *
 * <p>This abstraction can be used when generating code so that code is
 * generated into a consolidated namespace rather than spread across
 * all of the namespaces used in a Smithy model. {@link SymbolProvider}
 * implementations can use the {@code ShapeIdShader} to handle renaming
 * shape IDs before they convert the shape ID into a symbol appropriate
 * for that language. This abstraction essentially normalizes shape IDs
 * before the ID is converted into a language-specific symbol.
 *
 * <p>For example, if you want to generate code into the "EC2" namespace,
 * could set the {@code targetNamespace} to "EC2". If the service shape
 * that is being generated is in the "com.amazon.ec2" namespace, then you
 * could set the {@code rootNamespace} to "com.amazon.ec2" to completely
 * elide that namespace so that "com.amazon.ec2" is mapped to "EC2".
 *
 * {@link #APPEND} appends namespaces outside of the root to the target.
 * Namespaces that are not inside of the given {@code rootNamespace}
 * are concatenated with the {@code targetNamespace} and then merged like
 * a sub-namespace. For example, given a "example.foo#Hi", the resulting
 * shape ID is "EC2.example.foo#Hi".
 *
 * <p>{@link #MERGE_NAME} combines all trailing namespaces into the shaded
 * shape ID's name. For example, given "com.amazon.nested.ns#Hello", the
 * shaded shape ID will become "EC2#NestedNsHello". The first character
 * of each namespace segment is capitalized and prefixed on the original
 * shape ID name.
 *
 * <p>{@link #MERGE_NAMESPACE} combines all trailing namespaces into the
 * shaded shape ID's namespace. For example, given
 * "com.amazon.nested.ns#Hello", the shaded shape ID will become
 * "EC2.NestedNs#Hello". The first character of each namespace segment is
 * capitalized and appended as additional segments after the target namespace.
 *
 * <p>{@link #FLATTEN} ignores given namespaces and changes them to the target
 * namespace. Warning: this runs a high risk of shape name conflicts across
 * namespaces! For example, given "com.amazon.nested.ns#Hello", the shaded
 * shape ID will become "EC2#Hello".
 */
public enum ShapeIdShader {
    /**
     * Appends additional namespace segments outside of the root namespace
     * to the end of the target namespace.
     *
     * <p>For example, given a target of "Foo", a root of "foo.bar", and an ID of
     * of "com.example#Baz", the resulting namespace of "Foo.com.example#Baz".
     */
    APPEND {
        @Override
        ShapeId applyShade(ShapeId id, String targetNamespace, String shadedNamespace) {
            shadedNamespace = targetNamespace + "." + shadedNamespace;
            return ShapeId.fromParts(shadedNamespace, id.getName(), id.getMember().orElse(null));
        }
    },

    /**
     * Merges additional namespace segments outside of the root namespace into
     * the name of the shape.
     *
     * <p>For example, given a target of "Foo", a root of "foo.bar", and an ID of
     * of "com.example#Baz", the resulting namespace of "Foo#ComExampleBaz". The
     * first character of the generated shape name is always capitalized.
     */
    MERGE_NAME {
        @Override
        ShapeId applyShade(ShapeId id, String targetNamespace, String shadedNamespace) {
            String name = mergeName(shadedNamespace + "." + id.getName());
            return ShapeId.fromParts(targetNamespace, name, id.getMember().orElse(null));
        }
    },

    /**
     * Merges additional namespace segments outside of the root namespace into
     * a single second-level namespace.
     *
     * <p>For example, given a target of "Foo", a root of "foo.bar", and a
     * namespace of "com.example", the resulting namespace of "Foo.ComExample".
     * The generated namespace will capitalize the first character of every
     * appended namespace segment.
     */
    MERGE_NAMESPACE {
        @Override
        ShapeId applyShade(ShapeId id, String targetNamespace, String shadedNamespace) {
            shadedNamespace = targetNamespace + "." + mergeName(shadedNamespace);
            return ShapeId.fromParts(shadedNamespace, id.getName(), id.getMember().orElse(null));
        }
    },

    /**
     * Flattens all namespaces into a single namespace.
     *
     * <p>This shader simply changes every namespace to the target namespace.
     */
    FLATTEN {
        @Override
        ShapeId applyShade(ShapeId id, String targetNamespace, String shadedNamespace) {
            return ShapeId.fromParts(targetNamespace, id.getName(), id.getMember().orElse(null));
        }
    };

    private static final Logger LOGGER = Logger.getLogger(ShapeIdShader.class.getName());
    private static final Pattern SANITIZE_CHARS = Pattern.compile("([^A-Za-z0-9_.]+)");
    private static final Pattern SANITIZE_DOTS = Pattern.compile("(\\.{2,})");

    /**
     * Shades the given shape ID into the target namespace stripping the given root.
     *
     * @param id The shape ID to shade.
     * @param rootNamespace Namespace to use as the "root".
     * @param targetNamespace Namespace to shade into.
     * @return Returns the shaded shape ID.
     */
    public ShapeId shade(ShapeId id, String rootNamespace, String targetNamespace) {
        String namespace = id.getNamespace();

        // If the namespace equals the root namespace, then just return the shape
        // ID equivalent in the target namespace with the same name + member.
        if (namespace.equals(rootNamespace)) {
            return targetNamespace.equals(rootNamespace)
                   ? id
                   : ShapeId.fromParts(targetNamespace, id.getName(), id.getMember().orElse(null));
        }

        if (namespace.startsWith(rootNamespace + ".")) {
            // Strip out the root namespace.
            namespace = namespace.substring(rootNamespace.length() + 1);
        }

        return applyShade(id, targetNamespace, namespace);
    }

    abstract ShapeId applyShade(ShapeId id, String targetNamespace, String shadedNamespace);

    /**
     * Sanitizes a given string to be used as a namespace.
     *
     * <p>Note that this value represents a ShapeID namespace, not a
     * namespace for a target programming language. The target namespace
     * is sanitized so that it is compatible with a Smithy shape ID. Special
     * characters that don't match the ShapeID syntax are replaced with "_".
     * Multiple successive dots are replaced with a single dot.
     *
     * @param targetNamespace Shape ID to target.
     * @return Returns the sanitized target namespace.
     */
    public static String sanitizeTargetNamespace(String targetNamespace) {
        String updated = targetNamespace;
        if (!ShapeId.isValidNamespace(targetNamespace)) {
            updated = SANITIZE_CHARS.matcher(targetNamespace).replaceAll("_");
            updated = SANITIZE_DOTS.matcher(updated).replaceAll(".");
            LOGGER.info(String.format("Sanitized target namespace `%s` to `%s`", targetNamespace, updated));
        }
        return updated;
    }

    /**
     * Creates a function that is used to shade shape IDs with a specific strategy
     * from a root namespace into a sanitized target namespace.
     *
     * <p>The provided {@code targetNamespace} is sanitized using
     * {@link  #sanitizeTargetNamespace}.
     *
     * @param rootNamespace Root namespace that is trimmed from IDs.
     * @param targetNamespace Target namespace to generate IDs into.
     * @param shader Shade strategy to use.
     * @return Returns the created shader function.
     */
    public static Function<ShapeId, ShapeId> createShader(
            String rootNamespace, String targetNamespace, ShapeIdShader shader) {
        String sanitizeTargetNamespace = sanitizeTargetNamespace(targetNamespace);
        return id -> shader.shade(id, rootNamespace, sanitizeTargetNamespace);
    }

    private static String mergeName(String name) {
        StringBuilder builder = new StringBuilder();
        boolean afterDot = true;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') {
                afterDot = true;
            } else if (afterDot) {
                builder.append(Character.toUpperCase(c));
                afterDot = false;
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
