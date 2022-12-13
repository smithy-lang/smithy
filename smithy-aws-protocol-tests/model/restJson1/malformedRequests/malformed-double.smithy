$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedDouble/{doubleInPath}", method: "POST")
operation MalformedDouble {
    input: MalformedDoubleInput
}

apply MalformedDouble @httpMalformedRequestTests([
    {
        id: "RestJsonBodyDoubleMalformedValueRejected",
        documentation: """
        Malformed values in the body should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedDouble/1",
            body: """
            { "doubleInBody" : $value:L }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["\"123\"", "true", "2ABC", "0x42", "Infinity", "-Infinity", "NaN"],
            "tag" : ["string_coercion", "boolean_coercion", "trailing_chars", "hex", "inf", "negative_inf", "nan"]
        },
        tags: [ "$tag:L" ]
    },
    {
        id: "RestJsonPathDoubleMalformedValueRejected",
        documentation: """
        Malformed values in the path should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedDouble/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["true", "2ABC", "0x42"],
            "tag" : ["boolean_coercion", "trailing_chars", "hex"]
        },
        tags: [ "$tag:L" ]
    },
    {
        id: "RestJsonQueryDoubleMalformedValueRejected",
        documentation: """
        Malformed values in query parameters should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedDouble/1",
            queryParams: [
                "doubleInQuery=$value:L"
            ]
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["true", "2ABC", "0x42"],
            "tag" : ["boolean_coercion", "trailing_chars", "hex"]
        },
        tags: [ "$tag:L" ]
    },
    {
        id: "RestJsonHeaderDoubleMalformedValueRejected",
        documentation: """
        Malformed values in headers should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedDouble/1",
            headers: {
               "doubleInHeader" : "$value:L"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["true", "2ABC", "0x42"],
            "tag" : ["boolean_coercion", "trailing_chars", "hex"]
        },
        tags: [ "$tag:L" ]
    },
])

structure MalformedDoubleInput {
    doubleInBody: Double,

    @httpLabel
    @required
    doubleInPath: Double,

    @httpQuery("doubleInQuery")
    doubleInQuery: Double,

    @httpHeader("doubleInHeader")
    doubleInHeader: Double
}

