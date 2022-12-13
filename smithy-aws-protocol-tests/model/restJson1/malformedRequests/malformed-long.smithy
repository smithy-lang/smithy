$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedLong/{longInPath}", method: "POST")
operation MalformedLong {
    input: MalformedLongInput
}

apply MalformedLong @httpMalformedRequestTests([
    {
        id: "RestJsonBodyLongUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/1",
            body: """
            { "longInBody" : $value:L }""",
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
        testParameters: {
            "value" : ["-184467440737095500000", "184467440737095500000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonPathLongUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["-184467440737095500000", "184467440737095500000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonQueryLongUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/1",
            queryParams: [
                "longInQuery=$value:L"
            ]
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["-184467440737095500000", "184467440737095500000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonHeaderLongUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/1",
            headers: {
               "longInHeader" : "$value:L"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["-184467440737095500000", "184467440737095500000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonBodyLongMalformedValueRejected",
        documentation: """
        Malformed values in the body should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/1",
            body: """
            { "longInBody" : $value:L }""",
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
            "value" : ["\"123\"", "true", "1.001", "2ABC", "0x42",
                       "Infinity", "\"Infinity\"", "-Infinity", "\"-Infinity\"", "NaN", "\"NaN\""],
            "tag" : ["string_coercion", "boolean_coercion", "float_truncation", "trailing_chars", "hex",
                       "inf", "string_inf", "negative_inf", "string_negative_inf", "nan", "string_nan"]
        },
        tags: [ "$tag:L" ]
    },
    {
        id: "RestJsonPathLongMalformedValueRejected",
        documentation: """
        Malformed values in the path should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["true", "1.001", "2ABC", "0x42", "Infinity", "-Infinity", "NaN"],
            "tag" : ["boolean_coercion", "float_truncation", "trailing_chars", "hex", "inf", "negative_inf", "nan"]
        },
        tags: [ "$tag:L" ]
    },
    {
        id: "RestJsonQueryLongMalformedValueRejected",
        documentation: """
        Malformed values in query parameters should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/1",
            queryParams: [
                "longInQuery=$value:L"
            ]
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["true", "1.001", "2ABC", "0x42", "Infinity", "-Infinity", "NaN"],
            "tag" : ["boolean_coercion", "float_truncation", "trailing_chars", "hex", "inf", "negative_inf", "nan"]
        },
        tags: [ "$tag:L" ]
    },
    {
        id: "RestJsonHeaderLongMalformedValueRejected",
        documentation: """
        Malformed values in headers should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLong/1",
            headers: {
               "longInHeader" : "$value:L"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters : {
            "value" : ["true", "1.001", "2ABC", "0x42", "Infinity", "-Infinity", "NaN"],
            "tag" : ["boolean_coercion", "float_truncation", "trailing_chars", "hex", "inf", "negative_inf", "nan"]
        },
        tags: [ "$tag:L" ]
    },
])

structure MalformedLongInput {
    longInBody: Long,

    @httpLabel
    @required
    longInPath: Long,

    @httpQuery("longInQuery")
    longInQuery: Long,

    @httpHeader("longInHeader")
    longInHeader: Long
}

