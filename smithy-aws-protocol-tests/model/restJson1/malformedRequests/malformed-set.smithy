$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedSet", method: "POST")
operation MalformedSet {
    input: MalformedSetInput
}

apply MalformedSet @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedSetDuplicateItems",
        documentation: """
        When the set has duplicated items, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "set" : ["a", "a", "b", "c"] }""",
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
        id: "RestJsonMalformedSetNullItem",
        documentation: """
        When the set contains null, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "set" : ["a", null, "b", "c"] }""",
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
])

structure MalformedSetInput {
    set: SimpleSet
}


set SimpleSet {
    member: String
}

