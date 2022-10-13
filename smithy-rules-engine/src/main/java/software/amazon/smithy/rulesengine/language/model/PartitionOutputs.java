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

package software.amazon.smithy.rulesengine.language.model;

import java.util.Arrays;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.util.SourceLocationTrackingBuilder;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * The outputs to be provided by the rule-set aws.partition function.
 */
@SmithyUnstableApi
public final class PartitionOutputs implements ToSmithyBuilder<PartitionOutputs>, FromSourceLocation, ToNode {
    private static final String DNS_SUFFIX = "dnsSuffix";
    private static final String DUAL_STACK_DNS_SUFFIX = "dualStackDnsSuffix";
    private static final String SUPPORTS_FIPS = "supportsFIPS";
    private static final String SUPPORTS_DUAL_STACK = "supportsDualStack";
    private static final String NAME = "name";

    private final String dnsSuffix;
    private final String dualStackDnsSuffix;
    private final boolean supportsFips;
    private final boolean supportsDualStack;

    private final SourceLocation sourceLocation;

    private PartitionOutputs(Builder builder) {
        this.dnsSuffix = builder.dnsSuffix;
        this.dualStackDnsSuffix = builder.dualStackDnsSuffix;
        this.supportsFips = builder.supportsFips;
        this.supportsDualStack = builder.supportsDualStack;
        this.sourceLocation = builder.getSourceLocation();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static PartitionOutputs fromNode(Node node) {
        ObjectNode objNode = node.expectObjectNode();

        objNode.expectNoAdditionalProperties(Arrays.asList(
                NAME, DNS_SUFFIX, DUAL_STACK_DNS_SUFFIX, SUPPORTS_FIPS, SUPPORTS_DUAL_STACK));

        Builder b = new Builder(node);

        objNode.getStringMember(DNS_SUFFIX).ifPresent(n -> b.dnsSuffix(n.getValue()));
        objNode.getStringMember(DUAL_STACK_DNS_SUFFIX).ifPresent(n -> b.dualStackDnsSuffix(n.getValue()));
        objNode.getBooleanMember(SUPPORTS_FIPS).ifPresent(n -> b.supportsFips(n.getValue()));
        objNode.getBooleanMember(SUPPORTS_DUAL_STACK).ifPresent(n -> b.supportsDualStack(n.getValue()));

        return b.build();
    }

    public String dnsSuffix() {
        return dnsSuffix;
    }

    public String dualStackDnsSuffix() {
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
                .dnsSuffix(dnsSuffix)
                .dualStackDnsSuffix(dualStackDnsSuffix)
                .supportsFips(supportsFips)
                .supportsDualStack(supportsDualStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dnsSuffix, dualStackDnsSuffix, supportsFips, supportsDualStack);
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
               && Objects.equals(dnsSuffix, partitionOutputs.dnsSuffix)
               && Objects.equals(dualStackDnsSuffix, partitionOutputs.dualStackDnsSuffix);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(DNS_SUFFIX, dnsSuffix)
                .withMember(DUAL_STACK_DNS_SUFFIX, dualStackDnsSuffix)
                .withMember(SUPPORTS_FIPS, supportsFips)
                .withMember(SUPPORTS_DUAL_STACK, supportsDualStack)
                .build();
    }

    public static class Builder extends SourceLocationTrackingBuilder<Builder, PartitionOutputs> {
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private boolean supportsFips;
        private boolean supportsDualStack;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
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
