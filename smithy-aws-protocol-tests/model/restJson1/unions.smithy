// This file defines test cases that serialize unions.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringMap
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#FooEnum


/// This operation uses unions for inputs and outputs.
@idempotent
@http(uri: "/JsonUnions", method: "PUT")
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

    // Note that this uses a conflicting structure name with
    // GreetingStruct, so it must be renamed in the service.
    renamedStructureValue: aws.protocoltests.restjson.nested#GreetingStruct,
}

apply JsonUnions @httpRequestTests([
    {
        id: "RestJsonSerializeStringUnionValue",
        documentation: "Serializes a string union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "stringValue": "foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                stringValue: "foo"
            }
        }
    },
    {
        id: "RestJsonSerializeBooleanUnionValue",
        documentation: "Serializes a boolean union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "booleanValue": true
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                booleanValue: true
            }
        }
    },
    {
        id: "RestJsonSerializeNumberUnionValue",
        documentation: "Serializes a number union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "numberValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                numberValue: 1
            }
        }
    },
    {
        id: "RestJsonSerializeBlobUnionValue",
        documentation: "Serializes a blob union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "blobValue": "Zm9v"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                blobValue: "foo"
            }
        }
    },
    {
        id: "RestJsonSerializeTimestampUnionValue",
        documentation: "Serializes a timestamp union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "timestampValue": 1398796238
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                timestampValue: 1398796238
            }
        }
    },
    {
        id: "RestJsonSerializeEnumUnionValue",
        documentation: "Serializes an enum union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "enumValue": "Foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                enumValue: "Foo"
            }
        }
    },
    {
        id: "RestJsonSerializeListUnionValue",
        documentation: "Serializes a list union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "listValue": ["foo", "bar"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                listValue: ["foo", "bar"]
            }
        }
    },
    {
        id: "RestJsonSerializeMapUnionValue",
        documentation: "Serializes a map union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
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
        headers: {"Content-Type": "application/json"},
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
        id: "RestJsonSerializeStructureUnionValue",
        documentation: "Serializes a structure union value",
        protocol: restJson1,
        method: "PUT",
        "uri": "/JsonUnions",
        body: """
            {
                "contents": {
                    "structureValue": {
                        "hi": "hello"
                    }
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                structureValue: {
                    hi: "hello",
                }
            }
        }
    },
    {
        id: "RestJsonSerializeRenamedStructureUnionValue",
        documentation: "Serializes a renamed structure union value",
        protocol: restJson1,
        method: "PUT",
        uri: "/JsonUnions",
        body: """
            {
                "contents": {
                    "renamedStructureValue": {
                        "salutation": "hello!"
                    }
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                renamedStructureValue: {
                    salutation: "hello!",
                }
            }
        }
    },
])

apply JsonUnions @httpResponseTests([
    {
        id: "RestJsonDeserializeStringUnionValue",
        documentation: "Deserializes a string union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "stringValue": "foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                stringValue: "foo"
            }
        }
    },
    {
        id: "RestJsonDeserializeBooleanUnionValue",
        documentation: "Deserializes a boolean union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "booleanValue": true
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                booleanValue: true
            }
        }
    },
    {
        id: "RestJsonDeserializeNumberUnionValue",
        documentation: "Deserializes a number union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "numberValue": 1
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                numberValue: 1
            }
        }
    },
    {
        id: "RestJsonDeserializeBlobUnionValue",
        documentation: "Deserializes a blob union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "blobValue": "Zm9v"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                blobValue: "foo"
            }
        }
    },
    {
        id: "RestJsonDeserializeTimestampUnionValue",
        documentation: "Deserializes a timestamp union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "timestampValue": 1398796238
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                timestampValue: 1398796238
            }
        }
    },
    {
        id: "RestJsonDeserializeEnumUnionValue",
        documentation: "Deserializes an enum union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "enumValue": "Foo"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                enumValue: "Foo"
            }
        }
    },
    {
        id: "RestJsonDeserializeListUnionValue",
        documentation: "Deserializes a list union value",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "contents": {
                    "listValue": ["foo", "bar"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                listValue: ["foo", "bar"]
            }
        }
    },
    {
        id: "RestJsonDeserializeMapUnionValue",
        documentation: "Deserializes a map union value",
        protocol: restJson1,
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
        headers: {"Content-Type": "application/json"},
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
        id: "RestJsonDeserializeStructureUnionValue",
        documentation: "Deserializes a structure union value",
        protocol: restJson1,
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
        headers: {"Content-Type": "application/json"},
        params: {
            contents: {
                structureValue: {
                    hi: "hello",
                }
            }
        }
    },
    {
        id: "RestJsonDeserializeIgnoreType"
        appliesTo: "client"
        documentation: "Ignores an unrecognized __type property"
        protocol: restJson1
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
            "Content-Type": "application/json"
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


/// This operation defines a union with a Unit member.
@http(uri: "/PostPlayerAction", method: "POST")
operation PostPlayerAction {
    input: PostPlayerActionInput,
    output: PostPlayerActionOutput
}

@input
structure PostPlayerActionInput {
    action: PlayerAction
}

@output
structure PostPlayerActionOutput {
    @required
    action: PlayerAction
}

union PlayerAction {
    /// Quit the game.
    quit: Unit
}

apply PostPlayerAction @httpRequestTests([
    {
        id: "RestJsonInputUnionWithUnitMember",
        documentation: "Unit types in unions are serialized like normal structures in requests.",
        protocol: restJson1,
        method: "POST",
        uri: "/PostPlayerAction",
        body: """
            {
                "action": {
                    "quit": {}
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            action: {
                quit: {}
            }
        }
    }
])

apply PostPlayerAction @httpResponseTests([
    {
        id: "RestJsonOutputUnionWithUnitMember",
        documentation: "Unit types in unions are serialized like normal structures in responses.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "action": {
                    "quit": {}
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            action: {
                quit: {}
            }
        }
    }
])


/// This operation defines a union that uses jsonName on some members.
@http(uri: "/PostUnionWithJsonName", method: "POST")
operation PostUnionWithJsonName {
    input: PostUnionWithJsonNameInput,
    output: PostUnionWithJsonNameOutput
}

@input
structure PostUnionWithJsonNameInput {
    value: UnionWithJsonName
}

@output
structure PostUnionWithJsonNameOutput {
    @required
    value: UnionWithJsonName
}

union UnionWithJsonName {
    @jsonName("FOO")
    foo: String,

    bar: String,

    @jsonName("_baz")
    baz: String
}

apply PostUnionWithJsonName @httpRequestTests([
    {
        id: "PostUnionWithJsonNameRequest1",
        documentation: "Tests that jsonName works with union members.",
        protocol: restJson1,
        method: "POST",
        uri: "/PostUnionWithJsonName",
        body: """
            {
                "value": {
                    "FOO": "hi"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            value: {
                foo: "hi"
            }
        }
    },
    {
        id: "PostUnionWithJsonNameRequest2",
        documentation: "Tests that jsonName works with union members.",
        protocol: restJson1,
        method: "POST",
        uri: "/PostUnionWithJsonName",
        body: """
            {
                "value": {
                    "_baz": "hi"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            value: {
                baz: "hi"
            }
        }
    },
    {
        id: "PostUnionWithJsonNameRequest3",
        documentation: "Tests that jsonName works with union members.",
        protocol: restJson1,
        method: "POST",
        uri: "/PostUnionWithJsonName",
        body: """
            {
                "value": {
                    "bar": "hi"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            value: {
                bar: "hi"
            }
        }
    }
])

apply PostUnionWithJsonName @httpResponseTests([
    {
        id: "PostUnionWithJsonNameResponse1",
        documentation: "Tests that jsonName works with union members.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "value": {
                    "FOO": "hi"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            value: {
                foo: "hi"
            }
        }
    },
    {
        id: "PostUnionWithJsonNameResponse2",
        documentation: "Tests that jsonName works with union members.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "value": {
                    "_baz": "hi"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            value: {
                baz: "hi"
            }
        }
    },
    {
        id: "PostUnionWithJsonNameResponse3",
        documentation: "Tests that jsonName works with union members.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "value": {
                    "bar": "hi"
                }
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            value: {
                bar: "hi"
            }
        }
    }
])
