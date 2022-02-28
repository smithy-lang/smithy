// This file defines test cases that test HTTP response code bindings.
// See: https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html#httpresponsecode-trait

$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpResponseTests

@idempotent
@http(uri: "/HttpResponseCode", method: "PUT")
operation HttpResponseCode {
    output: HttpResponseCodeOutput
}

structure HttpResponseCodeOutput {
    @httpResponseCode
    Status: Integer
}

apply HttpResponseCode @httpResponseTests([
    {
        id: "RestJsonHttpResponseCode",
        documentation: """
                Binds the http response code to an output structure. Note that
                even though all members are bound outside of the payload, an
                empty JSON object is serialized in the response. However,
                clients should be able to handle an empty JSON object or an
                empty payload without failing to deserialize a response.""",
        protocol: restJson1,
        code: 201,
        headers: {
            "Content-Type": "application/json",
        },
        body: "{}",
        bodyMediaType: "application/json",
        params: {
            Status: 201,
        }
    },
    {
        id: "RestJsonHttpResponseCodeDefaultsToModeledCode",
        documentation: """
                Binds the http response code to the http trait's code if the
                code isn't explicitly set. A client would be parsing the
                http response code, so this would always be present, but
                a server doesn't require it to be set to serialize a request.""",
        protocol: restJson1,
        code: 200,
        headers: {
            "Content-Type": "application/json",
        },
        body: "{}",
        bodyMediaType: "application/json",
        // A client would parse the http response code, and so for clients it
        // will always be present, but a server doesn't require it to be set.
        params: {},
        appliesTo: "server"
    },
    {
        id: "RestJsonHttpResponseCodeWithNoPayload",
        documentation: """
                This test ensures that clients gracefully handle cases where
                the service responds with no payload rather than an empty JSON
                object.""",
        protocol: restJson1,
        code: 201,
        body: "",
        params: {
            Status: 201,
        },
        appliesTo: "client"
    },
])
