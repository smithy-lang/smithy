$version: "1.0"

namespace ns.foo

use aws.api#clientEndpointDiscovery
use aws.api#clientDiscoveredEndpoint

// This deliberately doesn't have an endpoint error. This should result in
// a DANGER.
@clientEndpointDiscovery(operation: DescribeEndpoints)
service FooService {
    version: "2021-06-29",
    operations: [DescribeEndpoints, GetObject],
}

// This DOES have an error, but it's not bound to the operations. This should
// result in an ERROR.
@clientEndpointDiscovery(
    operation: DescribeEndpoints,
    error: InvalidEndpointError,
)
service BarService {
    version: "2021-06-29",
    operations: [DescribeEndpoints, GetObject],
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

structure DescribeEndpointsInput {
    Operation: String,
    Identifiers: Identifiers,
}

map Identifiers {
    key: String,
    value: String,
}

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

structure GetObjectInput {
    @required
    Id: String,
}

structure GetObjectOutput {
    Object: Blob,
}

@clientDiscoveredEndpoint(required: true)
operation GetObjectWithEndpointError {
    input: GetObjectInput,
    output: GetObjectOutput,
    errors: [InvalidEndpointError],
}

@error("client")
@httpError(421)
structure InvalidEndpointError {}
