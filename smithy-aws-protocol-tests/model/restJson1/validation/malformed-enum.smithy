$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/MalformedEnum", method: "POST")
operation MalformedEnum {
    input: MalformedEnumInput,
    errors: [ValidationException]
}

apply MalformedEnum @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedEnumString",
        documentation: """
        When a string member does not contain a valid enum value,
        the response should be a 400 ValidationException. Internal-only
        enum values are excluded from the response message.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedEnum",
            body: """
            { "string" : $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/string' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]",
                      "fieldList" : [{"message": "Value at '/string' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]", "path": "/string"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "XYZ"]
        }
    },
    {
        id: "RestJsonMalformedEnumTraitString",
        documentation: """
        When a string member does not contain a valid enum value,
        the response should be a 400 ValidationException. Internal-only
        enum values are excluded from the response message.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedEnum",
            body: """
            { "stringWithEnumTrait" : $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/stringWithEnumTrait' failed to satisfy constraint: Member must satisfy enum value set: [abc, def]",
                      "fieldList" : [{"message": "Value at '/stringWithEnumTrait' failed to satisfy constraint: Member must satisfy enum value set: [abc, def]", "path": "/stringWithEnumTrait"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "XYZ"]
        }
    },
    {
        id: "RestJsonMalformedEnumList",
        documentation: """
        When a list member value does not contain a valid enum value,
        the response should be a 400 ValidationException. Internal-only
        enum values are excluded from the response message.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedEnum",
            body: """
            { "list" : [$value:S] }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/list/0' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]",
                      "fieldList" : [{"message": "Value at '/list/0' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]", "path": "/list/0"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "XYZ"]
        }
    },
    {
        id: "RestJsonMalformedEnumMapKey",
        documentation: """
        When a map member's key does not contain a valid enum value,
        the response should be a 400 ValidationException. Internal-only
        enum values are excluded from the response message.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedEnum",
            body: """
            { "map" : { $value:S : "abc" } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/map' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]",
                      "fieldList" : [{"message": "Value at '/map' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]", "path": "/map"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "XYZ"]
        }
    },
    {
        id: "RestJsonMalformedEnumMapValue",
        documentation: """
        When a map member's value does not contain a valid enum value,
        the response should be a 400 ValidationException. Internal-only
        enum values are excluded from the response message.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedEnum",
            body: """
            { "map" : { "abc": $value:S } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/map/abc' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]",
                      "fieldList" : [{"message": "Value at '/map/abc' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]", "path": "/map/abc"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "XYZ"]
        }
    },
    {
        id: "RestJsonMalformedEnumUnion",
        documentation: """
        When a union member's value does not contain a valid enum value,
        the response should be a 400 ValidationException. Internal-only
        enum values are excluded from the response message.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedEnum",
            body: """
            { "union" : { "first": $value:S } }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/union/first' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]",
                      "fieldList" : [{"message": "Value at '/union/first' failed to satisfy constraint: Member must satisfy enum value set: [abc, def, jkl]", "path": "/union/first"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "XYZ"]
        }
    },
])

structure MalformedEnumInput {
    string: EnumString,

    stringWithEnumTrait: EnumTraitString,

    list: EnumList,

    map: EnumMap,

    union: EnumUnion
}

enum EnumString {
    ABC = "abc"
    DEF = "def"

    @internal
    GHI = "ghi"

    @tags(["internal"])
    JKL = "jkl"
}

@suppress(["ModelDeprecation"])
@enum([
    {value: "abc", name: "ABC", tags: ["external"]},
    {value: "def", name: "DEF"},
    {value: "ghi", name: "GHI", tags: ["internal"]},
])
string EnumTraitString

list EnumList {
    member: EnumString
}

map EnumMap {
    key: EnumString,
    value: EnumString
}

union EnumUnion {
    first: EnumString,
    second: EnumString
}
