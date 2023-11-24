$version: "2.0"

namespace ns.foo

use aws.api#clientEndpointDiscovery
use aws.api#clientDiscoveredEndpoint

// This DOES have an error, but it's not bound to the operations. This should
// result in an ERROR.
@clientEndpointDiscovery(
    operation: DescribeEndpoints,
    error: InvalidEndpointError,
)
service BarService {
    version: "2021-06-29",
    operations: [GetObject],
}

// This DOES have an error, and it IS bound to the operations. This should
// not produce any validation events.
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

@clientDiscoveredEndpoint(required: true)
operation GetObject {
    input: GetObjectInput,
    output: GetObjectOutput,
}

@input
structure GetObjectInput {
    @required
    Id: String,
}

@output
structure GetObjectOutput {
    Object: Blob,
}

@clientDiscoveredEndpoint(required: true)
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
