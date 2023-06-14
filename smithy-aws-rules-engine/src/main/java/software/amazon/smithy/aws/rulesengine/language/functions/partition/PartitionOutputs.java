/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.rulesengine.language.functions.partition;

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
    private static final List<String> PROPERTIES = ListUtils.of(NAME, DNS_SUFFIX, DUAL_STACK_DNS_SUFFIX,
            SUPPORTS_FIPS, SUPPORTS_DUAL_STACK);

    private final String name;
    private final String dnsSuffix;
    private final String dualStackDnsSuffix;
    private final boolean supportsFips;
    private final boolean supportsDualStack;
    private final SourceLocation sourceLocation;

    private PartitionOutputs(Builder builder) {
        sourceLocation = builder.getSourceLocation();
        name = builder.name;
        dnsSuffix = builder.dnsSuffix;
        dualStackDnsSuffix = builder.dualStackDnsSuffix;
        supportsFips = builder.supportsFips;
        supportsDualStack = builder.supportsDualStack;
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static PartitionOutputs fromNode(Node node) {
        Builder builder = new Builder(node);
        ObjectNode objectNode = node.expectObjectNode();
        objectNode.expectNoAdditionalProperties(PROPERTIES);

        objectNode.getStringMember(NAME, builder::name);
        objectNode.getStringMember(DNS_SUFFIX, builder::dnsSuffix);
        objectNode.getStringMember(DUAL_STACK_DNS_SUFFIX, builder::dualStackDnsSuffix);
        objectNode.getBooleanMember(SUPPORTS_FIPS, builder::supportsFips);
        objectNode.getBooleanMember(SUPPORTS_DUAL_STACK, builder::supportsDualStack);

        return builder.build();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public String getDnsSuffix() {
        return dnsSuffix;
    }

    public String getDualStackDnsSuffix() {
        return dualStackDnsSuffix;
    }

    public boolean supportsFips() {
        return supportsFips;
    }

    public boolean supportsDualStack() {
        return supportsDualStack;
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
                .supportsDualStack(supportsDualStack);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(DNS_SUFFIX, dnsSuffix)
                .withMember(DUAL_STACK_DNS_SUFFIX, dualStackDnsSuffix)
                .withMember(SUPPORTS_FIPS, supportsFips)
                .withMember(SUPPORTS_DUAL_STACK, supportsDualStack);

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
                && Objects.equals(dualStackDnsSuffix, partitionOutputs.dualStackDnsSuffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dnsSuffix, dualStackDnsSuffix, supportsFips, supportsDualStack);
    }

    public static class Builder extends RulesComponentBuilder<Builder, PartitionOutputs> {
        private String name;
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private boolean supportsFips;
        private boolean supportsDualStack;

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

        @Override
        public PartitionOutputs build() {
            return new PartitionOutputs(this);
        }
    }
}
