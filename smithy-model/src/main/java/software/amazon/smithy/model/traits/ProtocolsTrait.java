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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the protocols supported by a service.
 */
public final class ProtocolsTrait extends AbstractTrait implements ToSmithyBuilder<ProtocolsTrait> {
    public static final String NAME = "smithy.api#protocols";
    public static final String NONE_AUTH = "none";
    private static final List<String> PROPERTIES = ListUtils.of("name", "auth", "tags");

    private final List<Protocol> protocols;

    private ProtocolsTrait(Builder builder) {
        super(NAME, builder.sourceLocation);
        this.protocols = ListUtils.copyOf(builder.protocols);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(NAME);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);

            for (ObjectNode protocol : value.expectArrayNode().getElementsAs(ObjectNode.class)) {
                protocol.warnIfAdditionalProperties(PROPERTIES);
                Protocol.Builder protocolBuilder = Protocol.builder();
                protocolBuilder.name(protocol.expectMember("name").expectStringNode().getValue());
                protocol.getMember("tags").map(Node::expectArrayNode).ifPresent(tagsNode -> {
                    tagsNode.getElements().stream()
                            .map(Node::expectStringNode)
                            .map(StringNode::getValue)
                            .forEach(protocolBuilder::addTag);
                });
                protocol.getArrayMember("auth").ifPresent(auth -> {
                    Node.loadArrayOfString("auth", auth).forEach(protocolBuilder::addAuth);
                });
                builder.addProtocol(protocolBuilder.build());
            }

            return builder.build();
        }
    }

    /**
     * Gets the list of protocols.
     *
     * @return Returns the protocols
     */
    public List<Protocol> getProtocols() {
        return protocols;
    }

    /**
     * Gets the list of protocol names.
     *
     * @return Returns the list of supported protocol names.
     */
    public List<String> getProtocolNames() {
        return protocols.stream().map(Protocol::getName).collect(Collectors.toList());
    }

    /**
     * Gets a protocol by name.
     *
     * @param name Name of the protocol to get.
     * @return Returns the optionally found protocol.
     */
    public Optional<Protocol> getProtocol(String name) {
        return protocols.stream().filter(p -> p.getName().equals(name)).findFirst();
    }

    /**
     * Checks if the trait contains a protocol of the given name.
     *
     * @param name Name of the protocol to check.
     * @return Returns true if the protocol exists for this name.
     */
    public boolean hasProtocol(String name) {
        return getProtocol(name).isPresent();
    }

    /**
     * Gets a set of all the authentication schemes listed in each protocol.
     *
     * <p>Iteration over the returned set provides priority order based on the
     * order of the protocol followed by the order of the auth entry in a
     * protocols list.
     *
     * @return Returns all supported authentication schemes.
     */
    public Set<String> getAllAuthSchemes() {
        return protocols.stream()
                .flatMap(protocol -> protocol.getAuth().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    protected Node createNode() {
        return protocols.stream().collect(ArrayNode.collect());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder();
        protocols.forEach(builder::addProtocol);
        return builder;
    }

    /**
     * @return Returns an protocols trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds the protocols trait.
     */
    public static final class Builder extends AbstractTraitBuilder<ProtocolsTrait, Builder> {
        private List<Protocol> protocols = new ArrayList<>();

        private Builder() {}

        @Override
        public ProtocolsTrait build() {
            return new ProtocolsTrait(this);
        }

        /**
         * Adds a protocol to the trait.
         *
         * @param protocol Protocol to add.
         * @return Returns the builder.
         */
        public Builder addProtocol(Protocol protocol) {
            protocols.add(Objects.requireNonNull(protocol));
            return this;
        }

        /**
         * Removes a protocol by name from the builder.
         *
         * @param protocolName Protocol to remove.
         * @return Returns the builder.
         */
        public Builder removeProtocol(String protocolName) {
            protocols.removeIf(p -> p.getName().equals(protocolName));
            return this;
        }

        /**
         * Clears all protocols from the trait.
         *
         * @return Returns the builder.
         */
        public Builder clearProtocols() {
            protocols.clear();
            return this;
        }
    }
}
