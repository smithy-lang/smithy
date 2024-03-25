$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

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
        body: "v2ZfX3R5cGV4LnNtaXRoeS5wcm90b2NvbHRlc3RzLnJwY3YyQ2JvciNJbnZhbGlkR3JlZXRpbmdnTWVzc2FnZWJIaf8=",
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
        body: "v2ZfX3R5cGV4K3NtaXRoeS5wcm90b2NvbHRlc3RzLnJwY3YyQ2JvciNDb21wbGV4RXJyb3JoVG9wTGV2ZWxpVG9wIGxldmVsZk5lc3RlZL9jRm9vY2Jhcv//",
        bodyMediaType: "application/cbor"
    },
    {
        id: "RpcV2CborEmptyComplexError",
        protocol: rpcv2Cbor,
        code: 400,
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        body: "v2ZfX3R5cGV4K3NtaXRoeS5wcm90b2NvbHRlc3RzLnJwY3YyQ2JvciNDb21wbGV4RXJyb3L/",
        bodyMediaType: "application/cbor"
    },
])
