$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/MalformedLength", method: "POST")
operation MalformedLength {
    input: MalformedLengthInput,
    errors: [ValidationException]
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedLengthOverride", method: "POST")
operation MalformedLengthOverride {
    input: MalformedLengthOverrideInput,
    errors: [ValidationException]
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedLengthQueryString", method: "POST")
operation MalformedLengthQueryString {
    input: MalformedLengthQueryStringInput,
    errors: [ValidationException]
}

apply MalformedLength @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedLengthBlob",
        documentation: """
        When a blob member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "blob" : $value:S }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/blob' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/blob' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/blob"}]}"""
                }
            }
        },
        testParameters: {
            value: ["YQ==", "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="],
            inputLength: ["1", "26"]
        }
    },
    {
        id: "RestJsonMalformedLengthString",
        documentation: """
        When a string member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/string' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/string' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/string"}]}"""
                }
            }
        },
        testParameters: {
            value: ["a", "abcdefghijklmnopqrstuvwxyz", "👍"],
            inputLength: ["1", "26", "1"]
        }
    },
    {
        id: "RestJsonMalformedLengthMinString",
        documentation: """
        When a string member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "minString" : "a" }""",
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
                    { "message" : "1 validation error detected. Value with length 1 at '/minString' failed to satisfy constraint: Member must have length greater than or equal to 2",
                      "fieldList" : [{"message": "Value with length 1 at '/minString' failed to satisfy constraint: Member must have length greater than or equal to 2", "path": "/minString"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedLengthMaxString",
        documentation: """
        When a string member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "maxString" : "abcdefghijklmnopqrstuvwxyz" }""",
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
                    { "message" : "1 validation error detected. Value with length 26 at '/maxString' failed to satisfy constraint: Member must have length less than or equal to 8",
                      "fieldList" : [{"message": "Value with length 26 at '/maxString' failed to satisfy constraint: Member must have length less than or equal to 8", "path": "/maxString"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedLengthList",
        documentation: """
        When a list member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "list" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/list' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/list' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/list"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "[\"abc\"]",
                     "[\"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\"]"],
            inputLength: ["1", "10"]
        }
    },
    {
        id: "RestJsonMalformedLengthListValue",
        documentation: """
        When a list member's value does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "list" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/list/0' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/list/0' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/list/0"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "[\"a\", \"abc\"]",
                     "[\"abcdefghijklmnopqrstuvwxyz\", \"abc\"]" ],
            inputLength: ["1", "26"]
        }
    },

    // A valid map has 2-8 keys of length 2-8 that point to lists of length 2-8 with string values of length 2-8
    {
        id: "RestJsonMalformedLengthMap",
        documentation: """
        When a map member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "map" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/map' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/map' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/map"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "{\"abc\": [\"def\", \"efg\", \"fgh\"]}",
                     """
                     {"abc": ["abc", "def", "efg"], "bcd": ["abc", "def", "efg"],
                      "cde": ["abc", "def", "efg"], "def": ["abc", "def", "efg"],
                      "efg": ["abc", "def", "efg"], "fgh": ["abc", "def", "efg"],
                      "ghi": ["abc", "def", "efg"], "jkl": ["abc", "def", "efg"],
                      "klm": ["abc", "def", "efg"], "lmn": ["abc", "def", "efg"] }"""],
            inputLength: ["1", "10"]
        }
    },
    {
        id: "RestJsonMalformedLengthMapKey",
        documentation: """
        When a map member's key does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "map" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/map' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/map' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/map"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "{\"a\": [\"abc\", \"def\", \"efg\"], \"bcd\": [\"abc\", \"def\", \"efg\"], \"cde\": [\"abc\", \"def\", \"efg\"]}",
                     "{\"abcdefghijklmnopqrstuvwxyz\": [\"abc\", \"def\", \"efg\"], \"bcd\": [\"abc\", \"def\", \"efg\"], \"cde\": [\"abc\", \"def\", \"efg\"]}" ],
            inputLength: ["1", "26"]
        }
    },
    {
        id: "RestJsonMalformedLengthMapValue",
        documentation: """
        When a map member's value does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLength",
            body: """
            { "map" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/map/abc' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/map/abc' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/map/abc"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "{\"abc\": [\"def\"], \"bcd\": [\"abc\", \"def\", \"efg\"], \"cde\": [\"abc\", \"def\", \"efg\"]}",
                     """
                     {"abc": ["def", "efg", "fgh", "def", "efg", "fgh", "def", "efg", "fgh", "def"],
                      "bcd": ["abc", "def", "efg"],
                      "cde": ["abc", "def", "efg"]}""" ],
            inputLength: ["1", "10"]
        }
    },
])

// now repeat the above tests, but for the more specific constraints applied to the input member
apply MalformedLengthOverride @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedLengthBlobOverride",
        documentation: """
        When a blob member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthOverride",
            body: """
            { "blob" : $value:S }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/blob' failed to satisfy constraint: Member must have length between 4 and 6, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/blob' failed to satisfy constraint: Member must have length between 4 and 6, inclusive", "path": "/blob"}]}"""
                }
            }
        },
        testParameters: {
            value: ["YWJj", "YWJjZGVmZw=="],
            inputLength: ["3", "7"]
        }
    },
    {
        id: "RestJsonMalformedLengthStringOverride",
        documentation: """
        When a string member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthOverride",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/string' failed to satisfy constraint: Member must have length between 4 and 6, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/string' failed to satisfy constraint: Member must have length between 4 and 6, inclusive", "path": "/string"}]}"""
                }
            }
        },
        testParameters: {
            value: ["abc", "abcdefg", "👍👍👍"],
            inputLength: ["3", "7", "3"]
        }
    },
    {
        id: "RestJsonMalformedLengthMinStringOverride",
        documentation: """
        When a string member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthOverride",
            body: """
            { "minString" : "abc" }""",
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
                    { "message" : "1 validation error detected. Value with length 3 at '/minString' failed to satisfy constraint: Member must have length greater than or equal to 4",
                      "fieldList" : [{"message": "Value with length 3 at '/minString' failed to satisfy constraint: Member must have length greater than or equal to 4", "path": "/minString"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedLengthMaxStringOverride",
        documentation: """
        When a string member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthOverride",
            body: """
            { "maxString" : "abcdefg" }""",
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
                    { "message" : "1 validation error detected. Value with length 7 at '/maxString' failed to satisfy constraint: Member must have length less than or equal to 6",
                      "fieldList" : [{"message": "Value with length 7 at '/maxString' failed to satisfy constraint: Member must have length less than or equal to 6", "path": "/maxString"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedLengthListOverride",
        documentation: """
        When a list member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthOverride",
            body: """
            { "list" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/list' failed to satisfy constraint: Member must have length between 4 and 6, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/list' failed to satisfy constraint: Member must have length between 4 and 6, inclusive", "path": "/list"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "[\"abc\", \"def\", \"ghi\"]",
                     "[\"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\", \"abc\"]"],
            inputLength: ["3", "7"]
        }
    },

    // A valid map has 4-6 keys of length 2-8 that point to lists of length 2-8 with string values of length 2-8
    {
        id: "RestJsonMalformedLengthMapOverride",
        documentation: """
        When a map member does not fit within length bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthOverride",
            body: """
            { "map" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value with length $inputLength:L at '/map' failed to satisfy constraint: Member must have length between 4 and 6, inclusive",
                      "fieldList" : [{"message": "Value with length $inputLength:L at '/map' failed to satisfy constraint: Member must have length between 4 and 6, inclusive", "path": "/map"}]}"""
                }
            }
        },
        testParameters: {
            value: [ "{\"abc\": [\"def\", \"efg\", \"fgh\"], \"bcd\": [\"abc\", \"def\", \"efg\"], \"def\": [\"abc\", \"def\", \"efg\"]}",
                     """
                     {"abc\": ["abc", "def", "efg"], "bcd": ["abc", "def", "efg"],
                      "cde\": ["abc", "def", "efg"], "def": ["abc", "def", "efg"],
                      "efg\": ["abc", "def", "efg"], "fgh": ["abc", "def", "efg"],
                      "ghi\": ["abc", "def", "efg"] }"""],
            inputLength: ["3", "7"]
        }
    }
])

// query strings that have unspecified value are treated as being an empty string
// which means length validation is as important as required validation for ensuring a specified value
apply MalformedLengthQueryString @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedLengthQueryStringNoValue",
        documentation: """
        When a required member has no value in the query string,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedLengthQueryString",
            body: "{}",
            queryParams: [
                "string"
            ],
            headers: {
                "content-type": "application/json"
            },
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
                    { "message" : "1 validation error detected. Value with length 0 at '/string' failed to satisfy constraint: Member must have length between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value with length 0 at '/string' failed to satisfy constraint: Member must have length between 2 and 8, inclusive", "path": "/string"}]}"""
                }
            }
        }
    },
])

structure MalformedLengthInput {
    blob: LengthBlob,
    string: LengthString,
    minString: MinLengthString,
    maxString: MaxLengthString,
    list: LengthList,
    map: LengthMap
}

structure MalformedLengthOverrideInput {
    @length(min:4, max:6)
    blob: LengthBlob,

    @length(min:4, max:6)
    string: LengthString,

    @length(min:4)
    minString: MinLengthString,

    @length(max:6)
    maxString: MaxLengthString,

    @length(min:4, max:6)
    list: LengthList,

    @length(min:4, max:6)
    map: LengthMap
}

structure MalformedLengthQueryStringInput {
    @httpQuery("string")
    string: LengthString
}

@length(min:2, max:8)
blob LengthBlob

@length(min:2, max:8)
string LengthString

@length(min:2)
string MinLengthString

@length(max:8)
string MaxLengthString

@length(min:2, max:8)
list LengthList {
    member: LengthString
}

@length(min:2, max:8)
map LengthMap {
    key: LengthString,
    value: LengthList
}
