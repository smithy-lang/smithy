// This file defines test cases that test HTTP response code bindings.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpresponsecode-trait

$version: "2.0"

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

@readonly
@http(method: "GET", uri: "/responseCodeRequired", code: 200)
operation ResponseCodeRequired {
    output: ResponseCodeRequiredOutput,
}

@output
structure ResponseCodeRequiredOutput {
    @required
    @httpResponseCode
    responseCode: Integer,
}

@readonly
@http(method: "GET", uri: "/responseCodeHttpFallback", code: 201)
operation ResponseCodeHttpFallback {
    input: ResponseCodeHttpFallbackInputOutput,
    output: ResponseCodeHttpFallbackInputOutput,
}

structure ResponseCodeHttpFallbackInputOutput {}

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

apply ResponseCodeRequired @httpResponseTests([
    {
        id: "RestJsonHttpResponseCodeRequired",
        documentation: """
                This test ensures that servers handle @httpResponseCode being @required.""",
        protocol: restJson1,
        code: 201,
        headers: {
            "Content-Type": "application/json"
        },
        body: "{}",
        bodyMediaType: "application/json",
        params: {
            responseCode: 201,
        },
        appliesTo: "server"
    }
])

apply ResponseCodeHttpFallback @httpResponseTests([
    {
        id: "RestJsonHttpResponseCodeNotSetFallsBackToHttpCode",
        documentation: """
                This test ensures that servers fall back to the code set
                by @http if @httpResponseCode is not set.""",
        protocol: restJson1,
        code: 201,
        headers: {
            "Content-Type": "application/json"
        },
        body: "{}",
        bodyMediaType: "application/json",
        appliesTo: "server"
    }
])
