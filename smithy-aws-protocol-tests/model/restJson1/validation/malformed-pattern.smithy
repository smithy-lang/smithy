$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/MalformedPattern", method: "POST")
operation MalformedPattern {
    input: MalformedPatternInput,
    errors: [ValidationException]
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedPatternOverride", method: "POST")
operation MalformedPatternOverride {
    input: MalformedPatternOverrideInput,
    errors: [ValidationException]
}

apply MalformedPattern @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedPatternString",
        documentation: """
        When a string member does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPattern",
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
                    { "message" : "1 validation error detected. Value at '/string' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$",
                      "fieldList" : [{"message": "Value at '/string' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$", "path": "/string"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternReDOSString",
        documentation: """
        When the specified pattern is susceptible to ReDOS, the service will not
        hang indefinitely while evaluating the pattern""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPattern",
            body: """
            { "evilString" : "000000000000000000000000000000000000000000000000000000000000000000000000000000000000!" }""",
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
                    { "message" : "1 validation error detected. Value 000000000000000000000000000000000000000000000000000000000000000000000000000000000000! at '/evilString' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9]+)+$$",
                      "fieldList" : [{"message": "Value 000000000000000000000000000000000000000000000000000000000000000000000000000000000000! at '/evilString' failed to satisfy constraint: Member must satisfy regular expression pattern: ^([0-9]+)+$$", "path": "/evilString"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedPatternList",
        documentation: """
        When a list member value does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPattern",
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
                    { "message" : "1 validation error detected. Value at '/list/0' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$",
                      "fieldList" : [{"message": "Value at '/list/0' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$", "path": "/list/0"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternMapKey",
        documentation: """
        When a map member's key does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPattern",
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
                    { "message" : "1 validation error detected. Value at '/map' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$",
                      "fieldList" : [{"message": "Value at '/map' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$", "path": "/map"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternMapValue",
        documentation: """
        When a map member's value does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPattern",
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
                    { "message" : "1 validation error detected. Value at '/map/abc' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$",
                      "fieldList" : [{"message": "Value at '/map/abc' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$", "path": "/map/abc"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternUnion",
        documentation: """
        When a union member's value does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPattern",
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
                    { "message" : "1 validation error detected. Value at '/union/first' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$",
                      "fieldList" : [{"message": "Value at '/union/first' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$$", "path": "/union/first"}]}"""
                }
            }
        },
        testParameters: {
            value: ["ABC", "xyz"]
        }
    },
])



apply MalformedPatternOverride @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedPatternStringOverride",
        documentation: """
        When a string member does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPatternOverride",
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
                    { "message" : "1 validation error detected. Value at '/string' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$",
                      "fieldList" : [{"message": "Value at '/string' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$", "path": "/string"}]}"""
                }
            }
        },
        testParameters: {
            value: ["abc", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternListOverride",
        documentation: """
        When a list member value does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPatternOverride",
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
                    { "message" : "1 validation error detected. Value at '/list/0' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$",
                      "fieldList" : [{"message": "Value at '/list/0' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$", "path": "/list/0"}]}"""
                }
            }
        },
        testParameters: {
            value: ["abc", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternMapKeyOverride",
        documentation: """
        When a map member's key does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPatternOverride",
            body: """
            { "map" : { $value:S : "ghi" } }""",
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
                    { "message" : "1 validation error detected. Value at '/map' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$",
                      "fieldList" : [{"message": "Value at '/map' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$", "path": "/map"}]}"""
                }
            }
        },
        testParameters: {
            value: ["abc", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternMapValueOverride",
        documentation: """
        When a map member's value does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPatternOverride",
            body: """
            { "map" : { "ghi": $value:S } }""",
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
                    { "message" : "1 validation error detected. Value at '/map/ghi' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$",
                      "fieldList" : [{"message": "Value at '/map/ghi' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$", "path": "/map/ghi"}]}"""
                }
            }
        },
        testParameters: {
            value: ["abc", "xyz"]
        }
    },
    {
        id: "RestJsonMalformedPatternUnionOverride",
        documentation: """
        When a union member's value does not match the specified pattern,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedPatternOverride",
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
                    { "message" : "1 validation error detected. Value at '/union/first' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$",
                      "fieldList" : [{"message": "Value at '/union/first' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[g-m]+$$", "path": "/union/first"}]}"""
                }
            }
        },
        testParameters: {
            value: ["abc", "xyz"]
        }
    },
])

structure MalformedPatternInput {
    string: PatternString,

    evilString: EvilString,

    list: PatternList,

    map: PatternMap,

    union: PatternUnion
}

structure MalformedPatternOverrideInput {
    @pattern("^[g-m]+$")
    string: PatternString,

    list: PatternListOverride,

    map: PatternMapOverride,

    union: PatternUnionOverride
}

@pattern("^[a-m]+$")
string PatternString

@pattern("^([0-9]+)+$")
string EvilString

list PatternList {
    member: PatternString
}

list PatternListOverride {
    @pattern("^[g-m]+$")
    member: PatternString
}

map PatternMap {
    key: PatternString,
    value: PatternString
}

map PatternMapOverride {
    @pattern("^[g-m]+$")
    key: PatternString,

    @pattern("^[g-m]+$")
    value: PatternString
}

union PatternUnion {
    first: PatternString,
    second: PatternString
}

union PatternUnionOverride {
    @pattern("^[g-m]+$")
    first: PatternString,

    @pattern("^[g-m]+$")
    second: PatternString
}
