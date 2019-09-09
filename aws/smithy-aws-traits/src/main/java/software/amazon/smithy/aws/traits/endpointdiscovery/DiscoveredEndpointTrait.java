package software.amazon.smithy.aws.traits.endpointdiscovery;

import java.util.Collections;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that the target operation should use the SDK's endpoint discovery
 * logic.
 */
public final class DiscoveredEndpointTrait extends AbstractTrait implements ToSmithyBuilder<DiscoveredEndpointTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#discoveredEndpoint");

    private static final String REQUIRED = "required";

    private final boolean required;

    private DiscoveredEndpointTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.required = builder.required;
    }

    /**
     * @return Returns a builder used to create {@link DiscoveredEndpointTrait}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Returns whether or not the service requires endpoint discovery.
     */
    public boolean isRequired() {
        return required;
    }

    @Override
    protected Node createNode() {
        return Node.objectNode().withMember(REQUIRED, Node.from(isRequired()));
    }

    @Override
    public SmithyBuilder<DiscoveredEndpointTrait> toBuilder() {
        return builder().required(required);
    }

    /** Builder for {@link DiscoveredEndpointTrait}. */
    public static final class Builder extends AbstractTraitBuilder<DiscoveredEndpointTrait, Builder> {
        private boolean required;

        private Builder() {}

        @Override
        public DiscoveredEndpointTrait build() {
            return new DiscoveredEndpointTrait(this);
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public DiscoveredEndpointTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.warnIfAdditionalProperties(Collections.singletonList(REQUIRED));

            return builder()
                    .required(objectNode.getBooleanMemberOrDefault(REQUIRED, true))
                    .build();
        }
    }
}
