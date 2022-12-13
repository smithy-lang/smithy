$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedInteger/{integerInPath}", method: "POST")
operation MalformedInteger {
    input: MalformedIntegerInput
}

apply MalformedInteger @httpMalformedRequestTests([
    {
        id: "RestJsonBodyIntegerUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/1",
            body: """
            { "integerInBody" : $value:L }""",
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
            "value" : [ "-9223372000000000000", "9223372000000000000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonPathIntegerUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : [ "-9223372000000000000", "9223372000000000000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonQueryIntegerUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/1",
            queryParams: [
                "integerInQuery=$value:L"
            ]
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : [ "-9223372000000000000", "9223372000000000000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonHeaderIntegerUnderflowOverflow",
        documentation: """
        Underflow or overflow should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/1",
            headers: {
               "integerInHeader" : "$value:L"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : [ "-9223372000000000000", "9223372000000000000", "123000000000000000000000" ]
        },
        tags: ["underflow/overflow"]
    },
    {
        id: "RestJsonBodyIntegerMalformedValueRejected",
        documentation: """
        Malformed values in the body should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/1",
            body: """
            { "integerInBody" : $value:L }""",
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
        id: "RestJsonPathIntegerMalformedValueRejected",
        documentation: """
        Malformed values in the path should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/$value:L"
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
        id: "RestJsonQueryIntegerMalformedValueRejected",
        documentation: """
        Malformed values in query parameters should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/1",
            queryParams: [
                "integerInQuery=$value:L"
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
        id: "RestJsonHeaderIntegerMalformedValueRejected",
        documentation: """
        Malformed values in headers should be rejected""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedInteger/1",
            headers: {
               "integerInHeader" : "$value:L"
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

structure MalformedIntegerInput {
    integerInBody: Integer,

    @httpLabel
    @required
    integerInPath: Integer,

    @httpQuery("integerInQuery")
    integerInQuery: Integer,

    @httpHeader("integerInHeader")
    integerInHeader: Integer
}

