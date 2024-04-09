/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions.partition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * The outputs to be provided by the rule-set aws.partition function.
 */
@SmithyUnstableApi
public final class PartitionOutputs implements ToSmithyBuilder<PartitionOutputs>, FromSourceLocation, ToNode {
    private static final String NAME = "name";
    private static final String DNS_SUFFIX = "dnsSuffix";
    private static final String DUAL_STACK_DNS_SUFFIX = "dualStackDnsSuffix";
    private static final String SUPPORTS_FIPS = "supportsFIPS";
    private static final String SUPPORTS_DUAL_STACK = "supportsDualStack";
    private static final String IMPLICIT_GLOBAL_REGION = "implicitGlobalRegion";
    private static final List<String> PROPERTIES = ListUtils.of(NAME, DNS_SUFFIX, DUAL_STACK_DNS_SUFFIX,
            SUPPORTS_FIPS, SUPPORTS_DUAL_STACK, IMPLICIT_GLOBAL_REGION);

    private final String name;
    private final String dnsSuffix;
    private final String dualStackDnsSuffix;
    private final boolean supportsFips;
    private final boolean supportsDualStack;
    private final String implicitGlobalRegion;
    private final SourceLocation sourceLocation;

    private PartitionOutputs(Builder builder) {
        sourceLocation = builder.getSourceLocation();
        name = builder.name;
        dnsSuffix = builder.dnsSuffix;
        dualStackDnsSuffix = builder.dualStackDnsSuffix;
        supportsFips = builder.supportsFips;
        supportsDualStack = builder.supportsDualStack;
        implicitGlobalRegion = builder.implicitGlobalRegion;
    }

    /**
     * Builder to create a {@link PartitionOutputs} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    /**
     * Creates a {@link PartitionOutputs} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created PartitionOutputs.
     */
    public static PartitionOutputs fromNode(Node node) {
        Builder builder = new Builder(node);
        ObjectNode objectNode = node.expectObjectNode();
        objectNode.expectNoAdditionalProperties(PROPERTIES);

        objectNode.getStringMember(NAME, builder::name);
        objectNode.getStringMember(DNS_SUFFIX, builder::dnsSuffix);
        objectNode.getStringMember(DUAL_STACK_DNS_SUFFIX, builder::dualStackDnsSuffix);
        objectNode.getBooleanMember(SUPPORTS_FIPS, builder::supportsFips);
        objectNode.getBooleanMember(SUPPORTS_DUAL_STACK, builder::supportsDualStack);
        objectNode.getStringMember(IMPLICIT_GLOBAL_REGION, builder::implicitGlobalRegion);

        return builder.build();
    }

    /**
     * Gets this partition's name.
     *
     * @return returns the partition's name.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Gets this partition's default DNS suffix.
     *
     * @return returns the DNS suffix.
     */
    public String getDnsSuffix() {
        return dnsSuffix;
    }

    /**
     * Gets this partition's dual stack DNS suffix.
     *
     * @return returns the DNS suffix for dual stack endpoints.
     */
    public String getDualStackDnsSuffix() {
        return dualStackDnsSuffix;
    }

    /**
     * Returns true if the partition supports FIPS.
     *
     * @return returns true of FIPS is supported.
     */
    public boolean supportsFips() {
        return supportsFips;
    }

    /**
     * Returns true if the partition supports dual stack.
     *
     * @return returns true of dual stack is supported.
     */
    public boolean supportsDualStack() {
        return supportsDualStack;
    }

    /**
     * Gets this partition's implicit global region: the region that
     * non-regionalized (global) services should use for signing.
     *
     * @return returns the partition's implicit global region.
     */
    public String getImplicitGlobalRegion() {
        return implicitGlobalRegion;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public SmithyBuilder<PartitionOutputs> toBuilder() {
        return new Builder(getSourceLocation())
                .name(name)
                .dnsSuffix(dnsSuffix)
                .dualStackDnsSuffix(dualStackDnsSuffix)
                .supportsFips(supportsFips)
                .supportsDualStack(supportsDualStack)
                .implicitGlobalRegion(implicitGlobalRegion);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(DNS_SUFFIX, dnsSuffix)
                .withMember(DUAL_STACK_DNS_SUFFIX, dualStackDnsSuffix)
                .withMember(SUPPORTS_FIPS, supportsFips)
                .withMember(SUPPORTS_DUAL_STACK, supportsDualStack)
                .withMember(IMPLICIT_GLOBAL_REGION, implicitGlobalRegion);

        if (name != null) {
            builder.withMember(NAME, name);
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartitionOutputs partitionOutputs = (PartitionOutputs) o;
        return supportsFips == partitionOutputs.supportsFips && supportsDualStack == partitionOutputs.supportsDualStack
                && Objects.equals(name, partitionOutputs.name)
                && Objects.equals(dnsSuffix, partitionOutputs.dnsSuffix)
                && Objects.equals(dualStackDnsSuffix, partitionOutputs.dualStackDnsSuffix)
                && Objects.equals(implicitGlobalRegion, partitionOutputs.implicitGlobalRegion);

    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dnsSuffix, dualStackDnsSuffix,
                supportsFips, supportsDualStack, implicitGlobalRegion);
    }

    /**
     * A builder used to create a {@link Partition} class.
     */
    public static class Builder extends RulesComponentBuilder<Builder, PartitionOutputs> {
        private String name;
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private boolean supportsFips;
        private boolean supportsDualStack;
        private String implicitGlobalRegion;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
            return this;
        }

        public Builder dualStackDnsSuffix(String dualStackDnsSuffix) {
            this.dualStackDnsSuffix = dualStackDnsSuffix;
            return this;
        }

        public Builder supportsFips(boolean supportsFips) {
            this.supportsFips = supportsFips;
            return this;
        }

        public Builder supportsDualStack(boolean supportsDualStack) {
            this.supportsDualStack = supportsDualStack;
            return this;
        }

        public Builder implicitGlobalRegion(String implicitGlobalRegion) {
            this.implicitGlobalRegion = implicitGlobalRegion;
            return this;
        }

        @Override
        public PartitionOutputs build() {
            return new PartitionOutputs(this);
        }
    }
}
