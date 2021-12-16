$version: "2.0"

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
        id: "RestJsonMalformedSetDuplicateDocuments",
        documentation: """
        When the set has duplicated documents, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "docSet" : [{"a": 1}, {"b": 2, "c": 3}, {"c": 3, "b": 2}] }""",
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
        id: "RestJsonMalformedSetDuplicateTimestamps",
        documentation: """
        When the set has duplicated timestamps, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "tsSet" : [1515531081, 1423235322, 1515531081] }""",
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
        id: "RestJsonMalformedSetDuplicateBlobs",
        documentation: """
        When the set has duplicated blobs, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "blobSet" : ["YmxvYg==", "b3RoZXJibG9i", "YmxvYg=="] }""",
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
        id: "RestJsonMalformedSetDuplicateStructures",
        documentation: """
        When the set has duplicated structures, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "structSet" : [{"foo": "baz", "bar": 1}, {"foo": "quux", "bar": 2}, {"bar": 1, "foo": "baz"}] }""",
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
        id: "RestJsonMalformedSetDuplicateStructuresWithNullValues",
        documentation: """
        When the set has duplicated structures, where one structure has an
        explicit null value and the other leaves the member undefined,
        the response should be a 400 SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedSet",
            body: """
            { "structSet" : [{"foo": null, "bar": 1}, {"foo": "quux", "bar": 2}, {"bar": 1}] }""",
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
    set: SimpleSet,
    docSet: DocumentSet,
    tsSet: TimestampSet,
    blobSet: BlobSet,
    structSet: StructSet
}

set SimpleSet {
    member: String
}

set DocumentSet {
    member: Document
}

set TimestampSet {
    member: Timestamp
}

set BlobSet {
    member: Blob
}

set StructSet {
    member: StructForSet
}

structure StructForSet {
    foo: String,
    bar: Integer
}

document Document

