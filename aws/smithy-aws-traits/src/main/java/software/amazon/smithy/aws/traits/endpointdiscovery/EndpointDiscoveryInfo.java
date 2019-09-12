package software.amazon.smithy.aws.traits.endpointdiscovery;

import java.util.List;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;

public final class EndpointDiscoveryInfo {

    private final ServiceShape service;
    private final OperationShape operation;
    private final OperationShape discoveryOperation;
    private final StructureShape error;
    private final List<MemberShape> discoveryIds;
    private final boolean required;

    EndpointDiscoveryInfo(
            ServiceShape service,
            OperationShape operation,
            OperationShape discoveryOperation,
            StructureShape error,
            List<MemberShape> discoveryIds,
            boolean required
    ) {
        this.service = service;
        this.operation = operation;
        this.discoveryOperation = discoveryOperation;
        this.error = error;
        this.discoveryIds = discoveryIds;
        this.required = required;
    }

    public ServiceShape getService() {
        return service;
    }

    public OperationShape getOperation() {
        return operation;
    }

    public OperationShape getDiscoveryOperation() {
        return discoveryOperation;
    }

    public StructureShape getError() {
        return error;
    }

    public List<MemberShape> getDiscoveryIds() {
        return discoveryIds;
    }

    public boolean isRequired() {
        return required;
    }
}
