$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedUnion", method: "POST")
operation MalformedUnion {
    input: MalformedUnionInput
}

apply MalformedUnion @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedUnionMultipleFieldsSet",
        documentation: """
            When the union has multiple fields set, the response should be a 400
            SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUnion",
            body: """
                { "union" : { "int": 2, "string": "three" } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        }
    },
    {
        id: "RestJsonMalformedUnionKnownAndUnknownFieldsSet",
        documentation: """
            When the union has multiple fields set, even when only one is modeled,
            the response should be a 400 SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUnion",
            body: """
            { "union" : { "int": 2, "unknownField": "three" } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        }
    },
    {
        id: "RestJsonMalformedUnionNoFieldsSet",
        documentation: """
            When the union has no fields set, the response should be a 400
            SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUnion",
            body: """
                { "union" : { "int": null } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        }
    },
    {
        id: "RestJsonMalformedUnionEmptyObjectNoFieldsSet",
        documentation: """
            When the union is an empty object, it has no fields set, so the
            response should be a 400 SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUnion",
            body: """
                { "union" : {  } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        }
    },
    {
        id: "RestJsonMalformedUnionValueIsArray",
        documentation: """
            When the union value is actually an array, the response should be a 400
            SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUnion",
            body: """
                { "union" : ["int"] }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        }
    }
    {
        id: "RestJsonMalformedUnionUnknownMember",
        documentation: """
            When an unknown union member is received, the response should be a 400
            SerializationException."""
        protocol: restJson1
        request: {
            method: "POST"
            uri: "/MalformedUnion"
            body: """
                {
                    "union": {
                        "unknown": "hello"
                    }
                }""",
            headers: {
                "content-type": "application/json"
            }
        }
        response: {
            code: 400
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        }
    }
])

structure MalformedUnionInput {
    union: SimpleUnion
}

union SimpleUnion {
    int: Integer,

    string: String
}
