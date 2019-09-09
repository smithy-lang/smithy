package software.amazon.smithy.aws.traits.endpointdiscovery;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.BooleanTrait;

/**
 * Indicates members of the operation input which should be use to discover
 * endpoints.
 */
public final class EndpointDiscoveryIdTrait extends BooleanTrait {
    public static final ShapeId ID = ShapeId.from("aws.api#endpointDiscoveryId");

    public EndpointDiscoveryIdTrait(SourceLocation sourceLocation) {
        super(ID, sourceLocation);
    }

    public EndpointDiscoveryIdTrait() {
        this(SourceLocation.NONE);
    }

    public static final class Provider extends BooleanTrait.Provider<EndpointDiscoveryIdTrait> {
        public Provider() {
            super(ID, EndpointDiscoveryIdTrait::new);
        }
    }
}
