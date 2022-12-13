// This file defines test cases that test the basics of empty input and
// output shape serialization.
//
// TODO: does an operation with no input always send {}? What about no output?

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
/// While this should be rare, code generators must support this.
@http(uri: "/NoInputAndNoOutput", method: "POST")
operation NoInputAndNoOutput {}

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "RestJsonNoInputAndNoOutput",
        documentation: """
                No input serializes no payload. When clients do not need to
                serialize any data in the payload, they should omit a payload
                altogether.""",
        protocol: restJson1,
        method: "POST",
        uri: "/NoInputAndNoOutput",
        body: ""
    },
    {
        id: "RestJsonNoInputAllowsAccept",
        documentation: """
                Servers should allow the accept header to be set to the
                default content-type.""",
        protocol: restJson1,
        method: "POST",
        uri: "/NoInputAndNoOutput",
        body: "",
        headers: {
            "Accept": "application/json"
        },
        appliesTo: "server",
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
       id: "RestJsonNoInputAndNoOutput",
       documentation: """
            When an operation does not define output, the service will respond
            with an empty payload, and may optionally include the content-type
            header.""",
       protocol: restJson1,
       code: 200,
       body: ""
   }
])

/// This test is similar to NoInputAndNoOutput, but uses explicit Unit types.
@http(uri: "/UnitInputAndOutput", method: "POST")
operation UnitInputAndOutput {
    input: Unit,
    output: Unit
}

apply UnitInputAndOutput @httpRequestTests([
    {
        id: "RestJsonUnitInputAndOutput",
        documentation: """
                A unit type input serializes no payload. When clients do not
                need to serialize any data in the payload, they should omit
                a payload altogether.""",
        protocol: restJson1,
        method: "POST",
        uri: "/UnitInputAndOutput",
        body: ""
    },
    {
        id: "RestJsonUnitInputAllowsAccept",
        documentation: """
                Servers should allow the accept header to be set to the
                default content-type.""",
        protocol: restJson1,
        method: "POST",
        uri: "/UnitInputAndOutput",
        body: "",
        headers: {
            "Accept": "application/json"
        },
        appliesTo: "server",
    }
])

apply UnitInputAndOutput @httpResponseTests([
   {
       id: "RestJsonUnitInputAndOutputNoOutput",
       documentation: """
            When an operation defines Unit output, the service will respond
            with an empty payload, and may optionally include the content-type
            header.""",
       protocol: restJson1,
       code: 200,
       body: ""
   }
])

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input and the
/// output is empty. While this should be rare, code generators must support
/// this.
@http(uri: "/NoInputAndOutputOutput", method: "POST")
operation NoInputAndOutput {
    output: NoInputAndOutputOutput
}

apply NoInputAndOutput @httpRequestTests([
    {
        id: "RestJsonNoInputAndOutput",
        documentation: """
                No input serializes no payload. When clients do not need to
                serialize any data in the payload, they should omit a payload
                altogether.""",
        protocol: restJson1,
        method: "POST",
        uri: "/NoInputAndOutputOutput",
        body: "",
    },
    {
        id: "RestJsonNoInputAndOutputAllowsAccept",
        documentation: """
                Servers should allow the accept header to be set to the
                default content-type.""",
        protocol: restJson1,
        method: "POST",
        uri: "/NoInputAndOutputOutput",
        body: "",
        headers: {
            "Accept": "application/json"
        },
        appliesTo: "server"
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "RestJsonNoInputAndOutputWithJson",
        documentation: """
                Operations that define output and do not bind anything to
                the payload return a JSON object in the response.""",
        protocol: restJson1,
        code: 200,
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
    },
    {
       id: "RestJsonNoInputAndOutputNoPayload",
       documentation: """
            This test is similar to RestJsonNoInputAndOutputWithJson, but
            it ensures that clients can gracefully handle responses that
            omit a JSON payload.""",
       protocol: restJson1,
       code: 200,
       body: "",
       appliesTo: "client",
   }
])

@output
structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has an empty input
/// and empty output structure that reuses the same shape. While this should
/// be rare, code generators must support this.
@http(uri: "/EmptyInputAndEmptyOutput", method: "POST")
operation EmptyInputAndEmptyOutput {
    input: EmptyInputAndEmptyOutputInput,
    output: EmptyInputAndEmptyOutputOutput
}

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "RestJsonEmptyInputAndEmptyOutput",
        documentation: """
                Clients should not serialize a JSON payload when no parameters
                are given that are sent in the body. A service will tolerate
                clients that omit a payload or that send a JSON object.""",
        protocol: restJson1,
        method: "POST",
        uri: "/EmptyInputAndEmptyOutput",
        body: "",
    },
    {
        id: "RestJsonEmptyInputAndEmptyOutputWithJson",
        documentation: """
                Similar to RestJsonEmptyInputAndEmptyOutput, but ensures that
                services gracefully handles receiving a JSON object.""",
        protocol: restJson1,
        method: "POST",
        uri: "/EmptyInputAndEmptyOutput",
        headers: {
            "Content-Type": "application/json",
        },
        body: "{}",
        bodyMediaType: "application/json",
        appliesTo: "server",
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "RestJsonEmptyInputAndEmptyOutput",
        documentation: """
                As of January 2021, server implementations are expected to
                respond with a JSON object regardless of if the output
                parameters are empty.""",
        protocol: restJson1,
        code: 200,
        headers: {
            "Content-Type": "application/json",
        },
        body: "{}",
        bodyMediaType: "application/json",
    },
    {
        id: "RestJsonEmptyInputAndEmptyOutputJsonObjectOutput",
        documentation: """
                This test ensures that clients can gracefully handle
                situations where a service omits a JSON payload entirely.""",
        protocol: restJson1,
        code: 200,
        body: "",
        appliesTo: "client",
    },
])

@input
structure EmptyInputAndEmptyOutputInput {}

@output
structure EmptyInputAndEmptyOutputOutput {}
