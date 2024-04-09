$version: "2.0"

namespace ns.foo

use aws.api#clientEndpointDiscovery
use aws.api#clientDiscoveredEndpoint

@clientEndpointDiscovery(
    operation: DescribeEndpoints,
    error: InvalidEndpointError,
)
service BazService {
    version: "2021-06-29",
    operations: [DescribeEndpoints, GetObjectWithEndpointError],
}

operation DescribeEndpoints {
    input: DescribeEndpointsInput,
    output: DescribeEndpointsOutput,
}

@input
structure DescribeEndpointsInput {
    Operation: String,
    Identifiers: Identifiers,
}

map Identifiers {
    key: String,
    value: String,
}

@output
structure DescribeEndpointsOutput {
    Endpoints: Endpoints,
}

list Endpoints {
    member: Endpoint,
}

structure Endpoint {
    Address: String,
    CachePeriodInMinutes: Long,
}

operation GetObjectWithEndpointError {
    input: GetObjectWithEndpointErrorInput,
    output: GetObjectWithEndpointErrorOutput,
    errors: [InvalidEndpointError],
}

@input
structure GetObjectWithEndpointErrorInput {
    @required
    Id: String,
}

@output
structure GetObjectWithEndpointErrorOutput {
    Object: Blob,
}

@error("client")
@httpError(421)
structure InvalidEndpointError {}
