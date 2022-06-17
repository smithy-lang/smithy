$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedUniqueItems", method: "POST")
operation MalformedUniqueItems {
    input: MalformedUniqueItemsInput
}

apply MalformedUniqueItems @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedUniqueItemsDuplicateItems",
        documentation: """
        When the list has duplicated items, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
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
        id: "RestJsonMalformedUniqueItemsDuplicateBlobs",
        documentation: """
        When the list has duplicated blobs, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "complexSet" : [{"foo": true, "blob": "YmxvYg=="}, {"foo": true, "blob": "b3RoZXJibG9i"}, {"foo": true, "blob": "YmxvYg=="}] }""",
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
        id: "RestJsonMalformedUniqueItemsNullItem",
        documentation: """
        When the list contains null, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
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

structure MalformedUniqueItemsInput {
    set: SimpleSet,
    complexSet: ComplexSet
}

@uniqueItems
list SimpleSet {
    member: String
}

@uniqueItems
list ComplexSet {
    member: ComplexSetStruct
}

structure ComplexSetStruct {
    foo: Boolean,
    blob: Blob
}
