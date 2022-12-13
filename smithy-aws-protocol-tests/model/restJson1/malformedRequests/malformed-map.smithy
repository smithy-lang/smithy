$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedMap", method: "POST")
operation MalformedMap {
    input: MalformedMapInput
}

apply MalformedMap @httpMalformedRequestTests([
    {
        id: "RestJsonBodyMalformedMapNullKey",
        documentation: """
        When a map contains a null key, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedMap",
            body: """
            { "bodyMap" : { null: "abc" }  }""",
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
        id: "RestJsonBodyMalformedMapNullValue",
        documentation: """
        When a dense map contains a null value, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedMap",
            body: """
            { "bodyMap" : { "abc": null }  }""",
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

structure MalformedMapInput {
    bodyMap: SimpleMap
}


map SimpleMap {
    key: String,
    value: String
}

