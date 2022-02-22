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

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An enum definition for the enum trait.
 */
public final class EnumDefinition implements ToNode, ToSmithyBuilder<EnumDefinition>, Tagged {

    public static final String VALUE = "value";
    public static final String NAME = "name";
    public static final String DOCUMENTATION = "documentation";
    public static final String TAGS = "tags";
    public static final String DEPRECATED = "deprecated";

    private static final Pattern CONVERTABLE_VALUE = Pattern.compile("^[a-zA-Z-_.:/\\s]+[a-zA-Z_0-9-.:/\\s]*$");

    private final String value;
    private final String documentation;
    private final List<String> tags;
    private final String name;
    private final boolean deprecated;

    private EnumDefinition(Builder builder) {
        value = SmithyBuilder.requiredState("value", builder.value);
        name = builder.name;
        documentation = builder.documentation;
        tags = new ArrayList<>(builder.tags);
        deprecated = builder.deprecated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getValue() {
        return value;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.withMember(EnumDefinition.VALUE, getValue())
                .withOptionalMember(EnumDefinition.NAME, getName().map(Node::from))
                .withOptionalMember(EnumDefinition.DOCUMENTATION, getDocumentation().map(Node::from));

        if (!tags.isEmpty()) {
            builder.withMember(EnumDefinition.TAGS, Node.fromStrings(getTags()));
        }

        if (isDeprecated()) {
            builder.withMember(EnumDefinition.DEPRECATED, true);
        }

        return builder.build();
    }

    public static EnumDefinition fromNode(Node node) {
        ObjectNode value = node.expectObjectNode();
        EnumDefinition.Builder builder = EnumDefinition.builder()
                .value(value.expectStringMember(EnumDefinition.VALUE).getValue())
                .name(value.getStringMember(EnumDefinition.NAME).map(StringNode::getValue).orElse(null))
                .documentation(value.getStringMember(EnumDefinition.DOCUMENTATION)
                        .map(StringNode::getValue)
                        .orElse(null))
                .deprecated(value.getBooleanMemberOrDefault(EnumDefinition.DEPRECATED));

        value.getMember(EnumDefinition.TAGS).ifPresent(tags -> {
            builder.tags(Node.loadArrayOfString(EnumDefinition.TAGS, tags));
        });

        return builder.build();
    }

    /**
     * Converts an enum definition to the equivalent enum member shape.
     *
     * @param parentId The {@link ShapeId} of the enum shape.
     * @param synthesizeName Whether to synthesize a name if possible.
     * @return An optional member shape representing the enum definition,
     *         or empty if conversion is impossible.
     */
    public Optional<MemberShape> asMember(ShapeId parentId, boolean synthesizeName) {
        String name;
        if (!getName().isPresent()) {
            if (canConvertToMember(synthesizeName)) {
                name = getValue().replaceAll("[-.:/\\s]+", "_");
            } else {
                return Optional.empty();
            }
        } else {
            name = getName().get();
        }

        try {
            MemberShape.Builder builder = MemberShape.builder()
                    .id(parentId.withMember(name))
                    .target(UnitTypeTrait.UNIT)
                    .addTrait(EnumValueTrait.builder().stringValue(value).build());

            getDocumentation().ifPresent(docs -> builder.addTrait(new DocumentationTrait(docs)));
            if (!tags.isEmpty()) {
                builder.addTrait(TagsTrait.builder().values(tags).build());
            }
            if (deprecated) {
                builder.addTrait(DeprecatedTrait.builder().build());
            }

            return Optional.of(builder.build());
        } catch (ShapeIdSyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Converts an enum definition to the equivalent enum member shape.
     *
     * This is only possible if the enum definition has a name.
     *
     * @param parentId The {@link ShapeId} of the enum shape.
     * @return An optional member shape representing the enum definition,
     *         or empty if conversion is impossible.
     */
    public Optional<MemberShape> asMember(ShapeId parentId) {
        return asMember(parentId, false);
    }

    /**
     * Determines whether the definition can be converted to a member.
     *
     * @param withSynthesizedNames Whether to account for name synthesization.
     * @return Returns true if the definition can be converted.
     */
    public boolean canConvertToMember(boolean withSynthesizedNames) {
        return getName().isPresent() || (withSynthesizedNames && CONVERTABLE_VALUE.matcher(getValue()).find());
    }

    /**
     * Converts an enum member into an equivalent enum definition object.
     *
     * @param member The enum member to convert.
     * @return An {@link EnumDefinition} representing the given member.
     */
    public static EnumDefinition fromMember(MemberShape member) {
        EnumDefinition.Builder builder = EnumDefinition.builder().name(member.getMemberName());

        EnumValueTrait valueTrait = member.expectTrait(EnumValueTrait.class);
        if (valueTrait.getStringValue().isPresent()) {
            builder.value(valueTrait.getStringValue().get());
        } else {
            throw new IllegalStateException("Enum definitions can only be made for string enums.");
        }

        member.getTrait(DocumentationTrait.class).ifPresent(docTrait -> builder.documentation(docTrait.getValue()));
        member.getTrait(TagsTrait.class).ifPresent(tagsTrait -> builder.tags(tagsTrait.getValues()));
        member.getTrait(DeprecatedTrait.class).ifPresent(deprecatedTrait -> builder.deprecated(true));
        return builder.build();
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .name(name)
                .value(value)
                .tags(tags)
                .documentation(documentation)
                .deprecated(deprecated);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EnumDefinition)) {
            return false;
        }

        EnumDefinition otherEnum = (EnumDefinition) other;
        return value.equals(otherEnum.value)
                && Objects.equals(name, otherEnum.name)
                && Objects.equals(documentation, otherEnum.documentation)
                && tags.equals(otherEnum.tags)
                && deprecated == otherEnum.deprecated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, name, tags, documentation, deprecated);
    }

    /**
     * Builds a {@link EnumDefinition}.
     */
    public static final class Builder implements SmithyBuilder<EnumDefinition> {
        private String value;
        private String documentation;
        private String name;
        private boolean deprecated;
        private final List<String> tags = new ArrayList<>();

        @Override
        public EnumDefinition build() {
            return new EnumDefinition(this);
        }

        public Builder value(String value) {
            this.value = Objects.requireNonNull(value);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder tags(Collection<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public Builder clearTags() {
            tags.clear();
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }
    }
}

