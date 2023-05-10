$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use aws.protocoltests.shared#BlobSet
use aws.protocoltests.shared#BooleanSet
use aws.protocoltests.shared#ByteSet
use aws.protocoltests.shared#DateTimeSet
use aws.protocoltests.shared#FooEnumSet
use aws.protocoltests.shared#HttpDateSet
use aws.protocoltests.shared#IntegerSet
use aws.protocoltests.shared#IntegerEnumSet
use aws.protocoltests.shared#ListSet
use aws.protocoltests.shared#LongSet
use aws.protocoltests.shared#ShortSet
use aws.protocoltests.shared#StringSet
use aws.protocoltests.shared#StructureSet
use aws.protocoltests.shared#TimestampSet
use aws.protocoltests.shared#UnionSet
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/MalformedUniqueItems", method: "POST")
operation MalformedUniqueItems {
    input: MalformedUniqueItemsInput,
    errors: [ValidationException]
}

apply MalformedUniqueItems @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedUniqueItemsBlobList",
        documentation: """
        When a blob list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "blobList" : ["YQ==", "YQ=="] }""",
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
                    { "message" : "1 validation error detected. Value at '/blobList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/blobList' failed to satisfy constraint: Member must have unique values", "path": "/blobList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsBooleanList",
        documentation: """
        When a boolean list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "booleanList" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value at '/booleanList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/booleanList' failed to satisfy constraint: Member must have unique values", "path": "/booleanList"}]}"""
                }
            }
        },
        testParameters: {
            value: ["[true, true]", "[false, false]"]
        }

    },
    {
        id: "RestJsonMalformedUniqueItemsStringList",
        documentation: """
        When a string list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "stringList" : ["abc", "abc"] }""",
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
                    { "message" : "1 validation error detected. Value at '/stringList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/stringList' failed to satisfy constraint: Member must have unique values", "path": "/stringList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsByteList",
        documentation: """
        When a byte list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "byteList" : [1, 1] }""",
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
                    { "message" : "1 validation error detected. Value at '/byteList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/byteList' failed to satisfy constraint: Member must have unique values", "path": "/byteList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsShortList",
        documentation: """
        When a short list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "shortList" : [2, 2] }""",
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
                    { "message" : "1 validation error detected. Value at '/shortList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/shortList' failed to satisfy constraint: Member must have unique values", "path": "/shortList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsIntegerList",
        documentation: """
        When an integer list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "integerList" : [3, 3] }""",
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
                    { "message" : "1 validation error detected. Value at '/integerList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/integerList' failed to satisfy constraint: Member must have unique values", "path": "/integerList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsLongList",
        documentation: """
        When an integer list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "longList" : [4, 4] }""",
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
                    { "message" : "1 validation error detected. Value at '/longList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/longList' failed to satisfy constraint: Member must have unique values", "path": "/longList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsTimestampList",
        documentation: """
        When a timestamp list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "timestampList" : [1676660607, 1676660607] }""",
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
                    { "message" : "1 validation error detected. Value at '/timestampList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/timestampList' failed to satisfy constraint: Member must have unique values", "path": "/timestampList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsDateTimeList",
        documentation: """
        When a date-time timestamp list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "dateTimeList" : ["1985-04-12T23:20:50.52Z", "1985-04-12T23:20:50.52Z"] }""",
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
                    { "message" : "1 validation error detected. Value at '/dateTimeList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/dateTimeList' failed to satisfy constraint: Member must have unique values", "path": "/dateTimeList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsHttpDateList",
        documentation: """
        When a http-date timestamp list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "httpDateList" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value at '/httpDateList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/httpDateList' failed to satisfy constraint: Member must have unique values", "path": "/httpDateList"}]}"""
                }
            }
        },
        testParameters: {
            value: ["[\"Tue, 29 Apr 2014 18:30:38 GMT\", \"Tue, 29 Apr 2014 18:30:38 GMT\"]", "[\"Sun, 02 Jan 2000 20:34:56.000 GMT\", \"Sun, 02 Jan 2000 20:34:56.000 GMT\"]"]
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsEnumList",
        documentation: """
        When an enum list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "enumList" : ["Foo", "Foo"] }""",
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
                    { "message" : "1 validation error detected. Value at '/enumList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/enumList' failed to satisfy constraint: Member must have unique values", "path": "/enumList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsIntEnumList",
        documentation: """
        When an intEnum list contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "intEnumList" : [3, 3] }""",
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
                    { "message" : "1 validation error detected. Value at '/intEnumList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/intEnumList' failed to satisfy constraint: Member must have unique values", "path": "/intEnumList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsListList",
        documentation: """
        When an list of lists contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "listList" : [["foo","bar"], ["foo","bar"]] }""",
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
                    { "message" : "1 validation error detected. Value at '/listList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/listList' failed to satisfy constraint: Member must have unique values", "path": "/listList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsStructureList",
        documentation: """
        When an list of structures contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "structureList" : [{"hi": "hello"}, {"hi": "hello"}] }""",
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
                    { "message" : "1 validation error detected. Value at '/structureList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/structureList' failed to satisfy constraint: Member must have unique values", "path": "/structureList"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsStructureMissingKeyList",
        documentation: """
        When a list of structures does not contain required keys,
        the response should be a 400 ValidationException and not
        a 500 error.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "structureListWithNoKey" : [{"hi2": "bar"}] }""",
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
                    { "message" : "1 validation error detected. Value at '/structureListWithNoKey/0/hi' failed to satisfy constraint: Member must not be null",
                      "fieldList" : [{"message": "Value at '/structureListWithNoKey/0/hi' failed to satisfy constraint: Member must not be null", "path": "/structureListWithNoKey/0/hi"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedUniqueItemsUnionList",
        documentation: """
        When an list of unions contains non-unique values,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedUniqueItems",
            body: """
            { "unionList" : $value:L }""",
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
                    { "message" : "1 validation error detected. Value at '/unionList' failed to satisfy constraint: Member must have unique values",
                      "fieldList" : [{"message": "Value at '/unionList' failed to satisfy constraint: Member must have unique values", "path": "/unionList"}]}"""
                }
            }
        },
        testParameters: {
            value: ["[{\"string\": \"foo\"}, {\"string\": \"foo\"}]", "[{\"integer\": 1}, {\"integer\": 1}]"]
        }
    },
])


string MyStringKey

structure MissingKeyStructure {
  @required
  hi: MyStringKey
}

@uniqueItems
list StructureSetWithNoKey {
  member: MissingKeyStructure
}

structure MalformedUniqueItemsInput {
    blobList: BlobSet
    booleanList: BooleanSet
    stringList: StringSet
    byteList: ByteSet
    shortList: ShortSet
    integerList: IntegerSet
    longList: LongSet
    timestampList: TimestampSet
    dateTimeList: DateTimeSet
    httpDateList: HttpDateSet
    enumList: FooEnumSet
    intEnumList: IntegerEnumSet
    listList: ListSet
    structureList: StructureSet
    structureListWithNoKey: StructureSetWithNoKey
    unionList: UnionSet
}
