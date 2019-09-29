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

import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.StringUtils;

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
 * <p>Any namespaces underneath the root namespace are then combined
 * with the target namespace. By default, they are just merged using
 * {@link ShadeOption#APPEND}. For example, given
 * "com.amazon.ec2.nested.ns#Hello", the resulting shape ID is
 * "EC2.nested.ns#Hello". The merge behavior can be customized by passing
 * a {@link ShadeOption} to the {@link #shade(ShapeId, ShadeOption)} method.
 *
 * <p>A shade option of {@link ShadeOption#MERGE_NAME} combines
 * all trailing namespaces into the shaded shape ID's name. For example,
 * given "com.amazon.nested.ns#Hello", the shaded shape ID will become
 * "EC2#NestedNsHello". The first character of each namespace segment is
 * capitalized and prefixed on the original shape ID name.
 *
 * <p>A shade option of {@link ShadeOption#MERGE_NAMESPACE} combines
 * all trailing namespaces into the shaded shape ID's namespace. For example,
 * given "com.amazon.nested.ns#Hello", the shaded shape ID will become
 * "EC2.NestedNs#Hello". The first character of each namespace segment is
 * capitalized and appended as additional segments after the target namespace.
 *
 * <p>Namespaces that are not inside of the given {@code rootNamespace}
 * are concatenated with the {@code targetNamespace} and then merged like
 * a sub-namespace. For example, given a "example.foo#Hi", the resulting
 * shape ID is "EC2.example.foo#Hi". When using {@code MERGE_NAME},
 * the resulting namespace is "EC2#ExampleFooHi". When using
 * {@code MERGE_NAMESPACE}, the resulting namespace is
 * "EC2.ExampleFoo#Hi".
 */
public final class ShapeIdShader {

    public static final String ROOT_NAMESPACE = "rootNamespace";
    public static final String TARGET_NAMESPACE = "targetNamespace";

    private static final Logger LOGGER = Logger.getLogger(ShapeIdShader.class.getName());
    private static final Pattern DOT = Pattern.compile("\\.");
    private static final Pattern SANITIZE_CHARS = Pattern.compile("([^A-Za-z0-9_.]+)");
    private static final Pattern SANITIZE_DOTS = Pattern.compile("(\\.+)");

    private final String rootNamespace;
    private final String targetNamespace;

    private ShapeIdShader(Builder builder) {
        String target = SmithyBuilder.requiredState(TARGET_NAMESPACE, builder.targetNamespace);

        if (!ShapeId.isValidNamespace(target)) {
            target = SANITIZE_CHARS.matcher(target).replaceAll("_");
            target = SANITIZE_DOTS.matcher(target).replaceAll(".");
            LOGGER.info(String.format("Sanitized %s `%s` to `%s`", TARGET_NAMESPACE, builder.targetNamespace, target));
        }

        rootNamespace = SmithyBuilder.requiredState(ROOT_NAMESPACE, builder.rootNamespace);
        targetNamespace = target;
    }

    /**
     * Builds a new {@code ShapeIdShader}.
     *
     * @return Returns the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a shaded version of the given shape ID.
     *
     * @param id Shape ID to shade.
     * @return Returns the shaded shape ID.
     */
    public ShapeId shade(ShapeId id) {
        return shade(id, ShadeOption.APPEND);
    }

    /**
     * Creates a shaded version of the given shape ID and combines
     * any additional namespace segments using the provided
     * {@code ShadeOption} strategy.
     *
     * @param id Shape ID to shade.
     * @param option The shade strategy to use.
     * @return Returns the shaded shape ID.
     */
    public ShapeId shade(ShapeId id, ShadeOption option) {
        String name = id.getName();
        String namespace = shadeNamespace(id);

        if (namespace.isEmpty()) {
            namespace = targetNamespace;
        } else {
            switch (option) {
                case APPEND:
                    namespace = targetNamespace + "." + namespace;
                    break;
                case MERGE_NAME:
                    name = squashName(namespace + "." + name);
                    namespace = targetNamespace;
                    break;
                case MERGE_NAMESPACE:
                    namespace = targetNamespace + "." + squashName(namespace);
                    break;
                default:
                    throw new UnsupportedOperationException("Unreachable shade option: " + option);
            }
        }

        if (namespace.equals(id.getNamespace()) && name.equals(id.getName())) {
            return id;
        }

        return ShapeId.fromParts(namespace, name, id.getMember().orElse(null));
    }

    private String squashName(String name) {
        StringBuilder builder = new StringBuilder();
        for (String identifier : DOT.split(name)) {
            builder.append(StringUtils.capitalize(identifier));
        }
        return builder.toString();
    }

    private String shadeNamespace(ShapeId id) {
        String namespace = id.getNamespace();

        if (namespace.equals(rootNamespace)) {
            return "";
        }

        if (namespace.startsWith(rootNamespace + ".")) {
            // Strip out the root namespace.
            namespace = namespace.substring(rootNamespace.length() + 1);
        }

        return namespace;
    }

    /**
     * Controls how shading is handled when there are namespace segments found
     * outside of the {@code rootNamespace}.
     */
    public enum ShadeOption {
        /**
         * Appends additional namespace segments outside of the root namespace
         * to the end of the target namespace.
         *
         * <p>For example, given a target of "Foo", a root of "foo.bar", and an ID of
         * of "com.example#Baz", the resulting namespace of "Foo.com.example#Baz".
         */
        APPEND,

        /**
         * Merges additional namespace segments outside of the root namespace into
         * the name of the shape.
         *
         * <p>For example, given a target of "Foo", a root of "foo.bar", and an ID of
         * of "com.example#Baz", the resulting namespace of "Foo#ComExampleBaz".
         */
        MERGE_NAME,

        /**
         * Merges additional namespace segments outside of the root namespace into
         * a single second-level namespace.
         *
         * <p>For example, given a target of "Foo", a root of "foo.bar", and a namespace
         * of "com.example", the resulting namespace of "Foo.ComExample".
         */
        MERGE_NAMESPACE
    }

    /**
     * Builds the shape ID shader.
     */
    public static final class Builder implements SmithyBuilder<ShapeIdShader> {
        private String rootNamespace;
        private String targetNamespace;

        @Override
        public ShapeIdShader build() {
            return new ShapeIdShader(this);
        }

        /**
         * Root namespace that is removed from the targeted namespace.
         *
         * @param rootNamespace Root namespace to set.
         * @return Returns the builder.
         */
        public Builder rootNamespace(String rootNamespace) {
            this.rootNamespace = rootNamespace;
            return this;
        }

        /**
         * Target namespace where all namespaces will be generated into.
         *
         * <p>Note that this value represents a ShapeID namespace, not a
         * namespace for a target programming language. The target namespace
         * is sanitized so that it is compatible with a Smithy shape ID.
         * Special characters that don't match the ShapeID syntax are
         * replaced with "_". Multiple successive dots are replaced
         * with a single dot.
         *
         * @param targetNamespace Namespace to generate into.
         * @return Returns the builder.
         */
        public Builder targetNamespace(String targetNamespace) {
            this.targetNamespace = targetNamespace;
            return this;
        }

        /**
         * Loads configuration settings from a {@link Node} configuration.
         *
         * @param node Node to load settings from.
         * @return Returns the builder.
         */
        public Builder fromNode(Node node) {
            ObjectNode value = node.expectObjectNode();

            if (value.getMember(ROOT_NAMESPACE).isPresent()) {
                rootNamespace = value.expectStringMember(ROOT_NAMESPACE).getValue();
            }

            if (value.getMember(TARGET_NAMESPACE).isPresent()) {
                targetNamespace = value.expectStringMember(TARGET_NAMESPACE).getValue();
            }

            return this;
        }
    }
}
