$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedFloat/{floatInPath}", method: "POST")
operation MalformedFloat {
    input: MalformedFloatInput
}

apply MalformedFloat @httpMalformedRequestTests([
    {
        id: "RestJsonBodyFloatMalformedValueRejected",
        documentation: """
        Malformed values in the body should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedFloat/1",
            body: """
            { "floatInBody" : $value:L }""",
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
        id: "RestJsonPathFloatMalformedValueRejected",
        documentation: """
        Malformed values in the path should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedFloat/$value:L"
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
        id: "RestJsonQueryFloatMalformedValueRejected",
        documentation: """
        Malformed values in query parameters should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedFloat/1",
            queryParams: [
                "floatInQuery=$value:L"
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
        id: "RestJsonHeaderFloatMalformedValueRejected",
        documentation: """
        Malformed values in headers should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedFloat/1",
            headers: {
               "floatInHeader" : "$value:L"
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

structure MalformedFloatInput {
    floatInBody: Float,

    @httpLabel
    @required
    floatInPath: Float,

    @httpQuery("floatInQuery")
    floatInQuery: Float,

    @httpHeader("floatInHeader")
    floatInHeader: Float
}

