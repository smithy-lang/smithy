$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.protocols#rpcv2Json

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
    output: GreetingWithErrorsOutput
    errors: [InvalidGreeting, ComplexError]
}

structure GreetingWithErrorsOutput {
    greeting: String
}

/// This error is thrown when an invalid greeting value is provided.
@error("client")
structure InvalidGreeting {
    Message: String
}

apply InvalidGreeting @httpResponseTests([
    {
        id: "RpcV2JsonResponseInvalidGreetingError"
        documentation: "Parses simple RpcV2 JSON errors"
        protocol: rpcv2Json
        params: {
            Message: "Hi"
        }
        code: 400
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        body: """
            {
                "__type": "smithy.protocoltests.rpcv2Json#InvalidGreeting",
                "Message": "Hi"
            }"""
        bodyMediaType: "application/json"
    }
])

/// This error is thrown when a request is invalid.
@error("client")
structure ComplexError {
    TopLevel: String
    Nested: ComplexNestedErrorData
}

structure ComplexNestedErrorData {
    Foo: String
}

apply ComplexError @httpResponseTests([
    {
        id: "RpcV2JsonResponseComplexError"
        documentation: "Parses a complex error with no message member"
        protocol: rpcv2Json
        params: {
            TopLevel: "Top level"
            Nested: {
                Foo: "bar"
            }
        }
        code: 400
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        body: """
            {
                "__type": "smithy.protocoltests.rpcv2Json#ComplexError",
                "TopLevel": "Top level",
                "Nested": {
                    "Foo": "bar"
                }
            }"""
        bodyMediaType: "application/json"
    }
    {
        id: "RpcV2JsonResponseEmptyComplexError"
        protocol: rpcv2Json
        code: 400
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        body: """
            {
                "__type": "smithy.protocoltests.rpcv2Json#ComplexError"
            }"""
        bodyMediaType: "application/json"
    }
])
