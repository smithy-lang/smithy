$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.protocols#rpcv2Cbor

/// This operation has three possible return values:
///
/// 1. A successful response in the form of GreetingWithErrorsOutput
/// 2. An InvalidGreeting error.
/// 3. A ComplexError error.
///
/// Implementations must be able to successfully take a response and
/// properly deserialize successful and error responses.
@idempotent
operation GreetingWithErrors {
    output: GreetingWithErrorsOutput,
    errors: [InvalidGreeting, ComplexError]
}

structure GreetingWithErrorsOutput {
    greeting: String,
}

/// This error is thrown when an invalid greeting value is provided.
@error("client")
structure InvalidGreeting {
    Message: String,
}

apply InvalidGreeting @httpResponseTests([
    {
        id: "RpcV2CborInvalidGreetingError",
        documentation: "Parses simple RpcV2 Cbor errors",
        protocol: rpcv2Cbor,
        params: {
            Message: "Hi"
        },
        code: 400,
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        body: "v2ZfX3R5cGV4J2F3cy5wcm90b2NvbHRlc3RzLnJwY3YyI0ludmFsaWRHcmVldGluZ2dNZXNzYWdlYkhp/w==",
        bodyMediaType: "application/cbor",
    },
])

/// This error is thrown when a request is invalid.
@error("client")
structure ComplexError {
    TopLevel: String,
    Nested: ComplexNestedErrorData,
}

structure ComplexNestedErrorData {
    Foo: String,
}

apply ComplexError @httpResponseTests([
    {
        id: "RpcV2CborComplexError",
        documentation: "Parses a complex error with no message member",
        protocol: rpcv2Cbor,
        params: {
            TopLevel: "Top level",
            Nested: {
                Foo: "bar"
            }
        },
        code: 400,
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        body: "v2ZfX3R5cGV4JGF3cy5wcm90b2NvbHRlc3RzLnJwY3YyI0NvbXBsZXhFcnJvcmZOZXN0ZWS/Y0Zvb2NiYXL/aFRvcExldmVsaVRvcCBsZXZlbP8=",
        bodyMediaType: "application/cbor"
    },
    {
        id: "RpcV2CborEmptyComplexError",
        protocol: rpcv2Cbor,
        code: 400,
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
        },
        body: "v2ZfX3R5cGV4JGF3cy5wcm90b2NvbHRlc3RzLnJwY3YyI0NvbXBsZXhFcnJvcv8=",
        bodyMediaType: "application/cbor"
    },
])


