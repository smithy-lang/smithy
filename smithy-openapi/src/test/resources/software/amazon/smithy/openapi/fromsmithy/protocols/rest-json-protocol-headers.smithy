$version: "1.0"

namespace smithy.example

use aws.protocols#restJson1
use aws.api#clientEndpointDiscovery
use aws.api#clientDiscoveredEndpoint

@clientEndpointDiscovery(operation: DescribeEndpoints, error: EmptyError)
@restJson1
service Service {
    version: "30-07-21",
    operations: [
        NoInputOrOutput,
        EmptyInputAndOutput,
        OnlyErrorOutput,
        HttpChecksumRequired,
        HttpChecksumInputOperation,
        HttpChecksumOutputOperation,
        DescribeEndpoints,
        HasDiscoveredEndpoint
    ]
}

// Since this lacks an input and an output, neither will have the content-type
// or content-length headers.
@http(method: "GET", uri: "/NoInputOrOutput")
operation NoInputOrOutput {}

// The input will still not have a content-type, but the output will due to
// there being a modeled output.
@http(method: "GET", uri: "/EmptyInputAndOutput")
operation EmptyInputAndOutput {
    input: EmptyStruct,
    output: EmptyStruct,
}

structure EmptyStruct {}

// Operations with errors will also have content headers in the response
@http(method: "GET", uri: "/OnlyErrorOutput")
operation OnlyErrorOutput {
    errors: [EmptyError]
}

@error("client")
@httpError(400)
structure EmptyError {}

// By default this makes the content-md5 header required
@httpChecksumRequired
@http(method: "GET", uri: "/HttpChecksumRequired")
operation HttpChecksumRequired {}

// The httpChecksum trait can add required headers. Its presence disables the
// content-md5 defaulting of httpChecksumRequired
@httpChecksumRequired
@httpChecksum(request: [
    {algorithm: "sha256", in: "header", name: "x-amz-checksum-sha256"},
    {algorithm: "sha1", in: "trailer", name: "x-amz-checksum-sha1"},
])
@http(method: "GET", uri: "/HttpChecksumInputOperation")
operation HttpChecksumInputOperation {
    input: EmptyStruct,
}

// The httpChecksum trait can add required headers.
@httpChecksum(response: [
    {algorithm: "sha256", in: "header", name: "x-amz-checksum-sha256"},
])
@http(method: "GET", uri: "/HttpChecksumOutputOperation")
operation HttpChecksumOutputOperation {
    output: EmptyStruct,
}

@http(method: "GET", uri: "/HasDiscoveredEndpoint")
@clientDiscoveredEndpoint(required: true)
operation HasDiscoveredEndpoint {
    errors: [EmptyError]
}

@http(method: "POST", uri: "/DescribeEndpoints")
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
  value: String
}

structure DescribeEndpointsOutput {
  Endpoints: Endpoints,
}

list Endpoints {
  member: Endpoint
}

structure Endpoint {
  Address: String,
  CachePeriodInMinutes: Long,
}
