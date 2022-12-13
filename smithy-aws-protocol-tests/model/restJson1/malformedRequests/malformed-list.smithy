$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedList", method: "POST")
operation MalformedList {
    input: MalformedListInput
}

apply MalformedList @httpMalformedRequestTests([
    {
        id: "RestJsonBodyMalformedListNullItem",
        documentation: """
        When a dense list contains null, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedList",
            body: """
            { "bodyList" : ["a", null, "b", "c"] }""",
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
        id: "RestJsonBodyMalformedListUnclosed",
        documentation: """
        When a list does not have a closing bracket, the response should be
        a 400 SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedList",
            body: """
            { "bodyList" : ["a", "b", "c" }""",
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

structure MalformedListInput {
    bodyList: SimpleList,
}


list SimpleList {
    member: String
}

