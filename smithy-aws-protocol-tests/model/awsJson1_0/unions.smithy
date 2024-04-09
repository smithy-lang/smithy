// This file defines test cases that serialize unions.

$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringMap
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#IntegerEnum


/// This operation uses unions for inputs and outputs.
@idempotent
operation JsonUnions {
    input: JsonUnionsInput,
    output: JsonUnionsOutput,
}

@input
structure JsonUnionsInput {
    contents: MyUnion
}

@output
structure JsonUnionsOutput {
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
    intEnumValue: IntegerEnum,
    listValue: StringList,
    mapValue: StringMap,
    structureValue: GreetingStruct,
}

apply JsonUnions @httpRequestTests([
    {
        id: "AwsJson10SerializeStringUnionValue",
        documentation: "Serializes a string union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                stringValue: "foo"
            }
        }
    },
    {
        id: "AwsJson10SerializeBooleanUnionValue",
        documentation: "Serializes a boolean union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                booleanValue: true
            }
        }
    },
    {
        id: "AwsJson10SerializeNumberUnionValue",
        documentation: "Serializes a number union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                numberValue: 1
            }
        }
    },
    {
        id: "AwsJson10SerializeBlobUnionValue",
        documentation: "Serializes a blob union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                blobValue: "foo"
            }
        }
    },
    {
        id: "AwsJson10SerializeTimestampUnionValue",
        documentation: "Serializes a timestamp union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                timestampValue: 1398796238
            }
        }
    },
    {
        id: "AwsJson10SerializeEnumUnionValue",
        documentation: "Serializes an enum union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                enumValue: "Foo"
            }
        }
    },
    {
        id: "AwsJson10SerializeIntEnumUnionValue",
        documentation: "Serializes an intEnum union value",
        protocol: awsJson1_0,
        method: "POST",
        "uri": "/",
        body: """
            {
                "contents": {
                    "intEnumValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                intEnumValue: 1
            }
        }
    },
    {
        id: "AwsJson10SerializeListUnionValue",
        documentation: "Serializes a list union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
        },
        params: {
            contents: {
                listValue: ["foo", "bar"]
            }
        }
    },
    {
        id: "AwsJson10SerializeMapUnionValue",
        documentation: "Serializes a map union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
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
        id: "AwsJson10SerializeStructureUnionValue",
        documentation: "Serializes a structure union value",
        protocol: awsJson1_0,
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
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.JsonUnions",
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
        id: "AwsJson10DeserializeStringUnionValue",
        documentation: "Deserializes a string union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "stringValue": "foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                stringValue: "foo"
            }
        }
    },
    {
        id: "AwsJson10DeserializeBooleanUnionValue",
        documentation: "Deserializes a boolean union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "booleanValue": true
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                booleanValue: true
            }
        }
    },
    {
        id: "AwsJson10DeserializeNumberUnionValue",
        documentation: "Deserializes a number union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "numberValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                numberValue: 1
            }
        }
    },
    {
        id: "AwsJson10DeserializeBlobUnionValue",
        documentation: "Deserializes a blob union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "blobValue": "Zm9v"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                blobValue: "foo"
            }
        }
    },
    {
        id: "AwsJson10DeserializeTimestampUnionValue",
        documentation: "Deserializes a timestamp union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "timestampValue": 1398796238
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                timestampValue: 1398796238
            }
        }
    },
    {
        id: "AwsJson10DeserializeEnumUnionValue",
        documentation: "Deserializes an enum union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "enumValue": "Foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                enumValue: "Foo"
            }
        }
    },
    {
        id: "AwsJson10DeserializeIntEnumUnionValue",
        documentation: "Deserializes an intEnum union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "intEnumValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                intEnumValue: 1
            }
        }
    },
    {
        id: "AwsJson10DeserializeListUnionValue",
        documentation: "Deserializes a list union value",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "contents": {
                    "listValue": ["foo", "bar"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                listValue: ["foo", "bar"]
            }
        }
    },
    {
        id: "AwsJson10DeserializeMapUnionValue",
        documentation: "Deserializes a map union value",
        protocol: awsJson1_0,
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
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
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
        id: "AwsJson10DeserializeStructureUnionValue",
        documentation: "Deserializes a structure union value",
        protocol: awsJson1_0,
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
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            contents: {
                structureValue: {
                    hi: "hello",
                }
            }
        }
    },
    {
        id: "AwsJson10DeserializeIgnoreType"
        appliesTo: "client"
        documentation: "Ignores an unrecognized __type property"
        protocol: awsJson1_0
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
            "Content-Type": "application/x-amz-json-1.0"
        },
        params: {
            contents: {
                structureValue: {
                    hi: "hello"
                }
            }
        }
    },
    {
        id: "AwsJson10DeserializeAllowNulls"
        appliesTo: "client"
        documentation: "Allows for `: null` to be set for all unset fields"
        protocol: awsJson1_0
        code: 200
        body: """
            {
                "contents": {
                  "stringValue": null,
                  "booleanValue": null,
                  "numberValue": null,
                  "blobValue": null,
                  "timestampValue": null,
                  "enumValue": null,
                  "intEnumValue": null,
                  "listValue": null,
                  "mapValue": null,
                  "structureValue": {
                      "hi": "hello"
                  }
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
        },
        params: {
            contents: {
                structureValue: {
                    hi: "hello"
                }
            }
        }
    }
])
