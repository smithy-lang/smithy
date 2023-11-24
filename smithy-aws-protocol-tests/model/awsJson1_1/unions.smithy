// This file defines test cases that serialize unions.

$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringMap
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#FooEnum


/// This operation uses unions for inputs and outputs.
@idempotent
operation JsonUnions {
    input: UnionInputOutput,
    output: UnionInputOutput,
}

/// A shared structure that contains a single union member.
structure UnionInputOutput {
    contents: MyUnion
}

/// A union with a representative set of types for members.
union MyUnion {
    stringValue: String,
    booleanValue: Boolean,
    numberValue: Integer,
    blobValue: Blob,
    timestampValue: Timestamp,
    enumValue: FooEnum,
    listValue: StringList,
    mapValue: StringMap,
    structureValue: GreetingStruct,
}

apply JsonUnions @httpRequestTests([
    {
        id: "AwsJson11SerializeStringUnionValue",
        documentation: "Serializes a string union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "stringValue": "foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                stringValue: "foo"
            }
        }
    },
    {
        id: "AwsJson11SerializeBooleanUnionValue",
        documentation: "Serializes a boolean union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "booleanValue": true
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                booleanValue: true
            }
        }
    },
    {
        id: "AwsJson11SerializeNumberUnionValue",
        documentation: "Serializes a number union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "numberValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                numberValue: 1
            }
        }
    },
    {
        id: "AwsJson11SerializeBlobUnionValue",
        documentation: "Serializes a blob union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "blobValue": "Zm9v"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                blobValue: "foo"
            }
        }
    },
    {
        id: "AwsJson11SerializeTimestampUnionValue",
        documentation: "Serializes a timestamp union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "timestampValue": 1398796238
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                timestampValue: 1398796238
            }
        }
    },
    {
        id: "AwsJson11SerializeEnumUnionValue",
        documentation: "Serializes an enum union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "enumValue": "Foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                enumValue: "Foo"
            }
        }
    },
    {
        id: "AwsJson11SerializeListUnionValue",
        documentation: "Serializes a list union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "listValue": ["foo", "bar"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                listValue: ["foo", "bar"]
            }
        }
    },
    {
        id: "AwsJson11SerializeMapUnionValue",
        documentation: "Serializes a map union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "mapValue": {
                        "foo": "bar",
                        "spam": "eggs"
                    }
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                mapValue: {
                    foo: "bar",
                    spam: "eggs",
                }
            }
        }
    },
    {
        id: "AwsJson11SerializeStructureUnionValue",
        documentation: "Serializes a structure union value",
        protocol: awsJson1_1,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "structureValue": {
                        "hi": "hello"
                    }
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.JsonUnions",
        },
        params: {
            contents: {
                structureValue: {
                    hi: "hello",
                }
            }
        }
    },
])

apply JsonUnions @httpResponseTests([
    {
        id: "AwsJson11DeserializeStringUnionValue",
        documentation: "Deserializes a string union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "stringValue": "foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                stringValue: "foo"
            }
        }
    },
    {
        id: "AwsJson11DeserializeBooleanUnionValue",
        documentation: "Deserializes a boolean union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "booleanValue": true
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                booleanValue: true
            }
        }
    },
    {
        id: "AwsJson11DeserializeNumberUnionValue",
        documentation: "Deserializes a number union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "numberValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                numberValue: 1
            }
        }
    },
    {
        id: "AwsJson11DeserializeBlobUnionValue",
        documentation: "Deserializes a blob union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "blobValue": "Zm9v"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                blobValue: "foo"
            }
        }
    },
    {
        id: "AwsJson11DeserializeTimestampUnionValue",
        documentation: "Deserializes a timestamp union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "timestampValue": 1398796238
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                timestampValue: 1398796238
            }
        }
    },
    {
        id: "AwsJson11DeserializeEnumUnionValue",
        documentation: "Deserializes an enum union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "enumValue": "Foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                enumValue: "Foo"
            }
        }
    },
    {
        id: "AwsJson11DeserializeListUnionValue",
        documentation: "Deserializes a list union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "listValue": ["foo", "bar"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                listValue: ["foo", "bar"]
            }
        }
    },
    {
        id: "AwsJson11DeserializeMapUnionValue",
        documentation: "Deserializes a map union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "mapValue": {
                        "foo": "bar",
                        "spam": "eggs"
                    }
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                mapValue: {
                    foo: "bar",
                    spam: "eggs"
                }
            }
        }
    },
    {
        id: "AwsJson11DeserializeStructureUnionValue",
        documentation: "Deserializes a structure union value",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "contents": {
                    "structureValue": {
                        "hi": "hello"
                    }
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            contents: {
                structureValue: {
                    hi: "hello",
                }
            }
        }
    },
    {
        id: "AwsJson11DeserializeIgnoreType"
        appliesTo: "client"
        documentation: "Ignores an unrecognized __type property"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "contents": {
                    "__type": "aws.protocoltests.json10#MyUnion",
                    "structureValue": {
                        "hi": "hello"
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
        }
        params: {
            contents: {
                structureValue: {
                    hi: "hello"
                }
            }
        }
    }
])
