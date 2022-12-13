$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/MalformedRange", method: "POST")
operation MalformedRange {
    input: MalformedRangeInput,
    errors: [ValidationException]
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedRangeOverride", method: "POST")
operation MalformedRangeOverride {
    input: MalformedRangeOverrideInput,
    errors: [ValidationException]
}

apply MalformedRange @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedRangeByte",
        documentation: """
        When a byte member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRange",
            body: """
            { "byte" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value $value:L at '/byte' failed to satisfy constraint: Member must be between 2 and 8, inclusive",
                      "fieldList" : [{"message": "Value $value:L at '/byte' failed to satisfy constraint: Member must be between 2 and 8, inclusive", "path": "/byte"}]}"""
                }
            }
        },
        testParameters: {
            value: ["1", "9"]
        }
    },
    {
        id: "RestJsonMalformedRangeMinByte",
        documentation: """
        When a byte member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRange",
            body: """
            { "minByte" : 1 }""",
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
                    { "message" : "1 validation error detected. Value 1 at '/minByte' failed to satisfy constraint: Member must be greater than or equal to 2",
                      "fieldList" : [{"message": "Value 1 at '/minByte' failed to satisfy constraint: Member must be greater than or equal to 2", "path": "/minByte"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRangeMaxByte",
        documentation: """
        When a byte member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRange",
            body: """
            { "maxByte" : 9 }""",
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
                    { "message" : "1 validation error detected. Value 9 at '/maxByte' failed to satisfy constraint: Member must be less than or equal to 8",
                      "fieldList" : [{"message": "Value 9 at '/maxByte' failed to satisfy constraint: Member must be less than or equal to 8", "path": "/maxByte"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRangeFloat",
        documentation: """
        When a float member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRange",
            body: """
            { "float" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value $value:L at '/float' failed to satisfy constraint: Member must be between 2.2 and 8.8, inclusive",
                      "fieldList" : [{"message": "Value $value:L at '/float' failed to satisfy constraint: Member must be between 2.2 and 8.8, inclusive", "path": "/float"}]}"""
                }
            }
        },
        testParameters: {
            value: ["2.1", "8.9"]
        }
    },
    {
        id: "RestJsonMalformedRangeMinFloat",
        documentation: """
        When a float member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRange",
            body: """
            { "minFloat" : 2.1 }""",
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
                    { "message" : "1 validation error detected. Value 2.1 at '/minFloat' failed to satisfy constraint: Member must be greater than or equal to 2.2",
                      "fieldList" : [{"message": "Value 2.1 at '/minFloat' failed to satisfy constraint: Member must be greater than or equal to 2.2", "path": "/minFloat"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRangeMaxFloat",
        documentation: """
        When a float member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRange",
            body: """
            { "maxFloat" : 8.9 }""",
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
                    { "message" : "1 validation error detected. Value 8.9 at '/maxFloat' failed to satisfy constraint: Member must be less than or equal to 8.8",
                      "fieldList" : [{"message": "Value 8.9 at '/maxFloat' failed to satisfy constraint: Member must be less than or equal to 8.8", "path": "/maxFloat"}]}"""
                }
            }
        }
    },
])

// now repeat the above tests, but for the more specific constraints applied to the input member
apply MalformedRangeOverride @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedRangeByteOverride",
        documentation: """
        When a byte member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRangeOverride",
            body: """
            { "byte" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value $value:L at '/byte' failed to satisfy constraint: Member must be between 4 and 6, inclusive",
                      "fieldList" : [{"message": "Value $value:L at '/byte' failed to satisfy constraint: Member must be between 4 and 6, inclusive", "path": "/byte"}]}"""
                }
            }
        },
        testParameters: {
            value: ["3", "7"]
        }
    },
    {
        id: "RestJsonMalformedRangeMinByteOverride",
        documentation: """
        When a byte member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRangeOverride",
            body: """
            { "minByte" : 3 }""",
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
                    { "message" : "1 validation error detected. Value 3 at '/minByte' failed to satisfy constraint: Member must be greater than or equal to 4",
                      "fieldList" : [{"message": "Value 3 at '/minByte' failed to satisfy constraint: Member must be greater than or equal to 4", "path": "/minByte"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRangeMaxByteOverride",
        documentation: """
        When a byte member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRangeOverride",
            body: """
            { "maxByte" : 7 }""",
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
                    { "message" : "1 validation error detected. Value 7 at '/maxByte' failed to satisfy constraint: Member must be less than or equal to 6",
                      "fieldList" : [{"message": "Value 7 at '/maxByte' failed to satisfy constraint: Member must be less than or equal to 6", "path": "/maxByte"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRangeFloatOverride",
        documentation: """
        When a float member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRangeOverride",
            body: """
            { "float" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value $value:L at '/float' failed to satisfy constraint: Member must be between 4.4 and 6.6, inclusive",
                      "fieldList" : [{"message": "Value $value:L at '/float' failed to satisfy constraint: Member must be between 4.4 and 6.6, inclusive", "path": "/float"}]}"""
                }
            }
        },
        testParameters: {
            value: ["4.3", "6.7"]
        }
    },
    {
        id: "RestJsonMalformedRangeMinFloatOverride",
        documentation: """
        When a float member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRangeOverride",
            body: """
            { "minFloat" : 4.3 }""",
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
                    { "message" : "1 validation error detected. Value 4.3 at '/minFloat' failed to satisfy constraint: Member must be greater than or equal to 4.4",
                      "fieldList" : [{"message": "Value 4.3 at '/minFloat' failed to satisfy constraint: Member must be greater than or equal to 4.4", "path": "/minFloat"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRangeMaxFloatOverride",
        documentation: """
        When a float member does not fit within range bounds,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRangeOverride",
            body: """
            { "maxFloat" : 6.7 }""",
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
                    { "message" : "1 validation error detected. Value 6.7 at '/maxFloat' failed to satisfy constraint: Member must be less than or equal to 6.6",
                      "fieldList" : [{"message": "Value 6.7 at '/maxFloat' failed to satisfy constraint: Member must be less than or equal to 6.6", "path": "/maxFloat"}]}"""
                }
            }
        }
    },
])

structure MalformedRangeInput {
    byte: RangeByte,
    minByte: MinByte,
    maxByte: MaxByte,

    float: RangeFloat,
    minFloat: MinFloat,
    maxFloat: MaxFloat,
}

structure MalformedRangeOverrideInput {
    @range(min:4, max:6)
    byte: RangeByte,
    @range(min:4)
    minByte: MinByte,
    @range(max:6)
    maxByte: MaxByte,

    @range(min:4.4, max:6.6)
    float: RangeFloat,
    @range(min:4.4)
    minFloat: MinFloat,
    @range(max:6.6)
    maxFloat: MaxFloat,
}

@range(min:2, max:8)
byte RangeByte
@range(min:2)
byte MinByte
@range(max:8)
byte MaxByte

@range(min:2.2, max:8.8)
float RangeFloat
@range(min:2.2)
float MinFloat
@range(max:8.8)
float MaxFloat

