// This file defines test cases that serialize synthesized JSON documents
// in the payload of HTTP requests and responses.

$version: "0.5.0"

namespace aws.protocols.tests.restjson

use aws.protocols.tests.shared#FooEnum
use aws.protocols.tests.shared#FooEnumList
use aws.protocols.tests.shared#FooEnumSet
use aws.protocols.tests.shared#FooEnumMap
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level JSON document.
// Note that headers are not serialized in the payload.
@idempotent
@http(uri: "/SimpleScalarProperties", method: "PUT")
operation SimpleScalarProperties(SimpleScalarPropertiesInputOutput) -> SimpleScalarPropertiesInputOutput

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "RestJsonSimpleScalarProperties",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-json-1.1",
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              {
                  "foo": "Foo",
                  "stringValue": "string",
                  "trueBooleanValue": true,
                  "falseBooleanValue": false,
                  "byteValue": 1,
                  "shortValue": 2,
                  "integerValue": 3,
                  "longValue": 4,
                  "floatValue": 5.5,
                  "DoubleDribble": 6.5
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "string",
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 1,
            shortValue: 2,
            integerValue: 3,
            longValue: 4,
            floatValue: 5.5,
            doubleValue: 6.5,
        }
    }
])

apply SimpleScalarProperties @httpResponseTests([
    {
        id: "RestJsonSimpleScalarProperties",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "foo": "Foo",
                  "stringValue": "string",
                  "trueBooleanValue": true,
                  "falseBooleanValue": false,
                  "byteValue": 1,
                  "shortValue": 2,
                  "integerValue": 3,
                  "longValue": 4,
                  "floatValue": 5.5,
                  "DoubleDribble": 6.5
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "string",
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 1,
            shortValue: 2,
            integerValue: 3,
            longValue: 4,
            floatValue: 5.5,
            doubleValue: 6.5,
        }
    }
])

structure SimpleScalarPropertiesInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    stringValue: String,
    trueBooleanValue: Boolean,
    falseBooleanValue: Boolean,
    byteValue: Byte,
    shortValue: Short,
    integerValue: Integer,
    longValue: Long,
    floatValue: Float,

    @jsonName("DoubleDribble")
    doubleValue: Double,
}

/// Blobs are base64 encoded
@http(uri: "/JsonBlobs", method: "POST")
operation JsonBlobs(JsonBlobsInputOutput) -> JsonBlobsInputOutput

apply JsonBlobs @httpRequestTests([
    {
        id: "RestJsonJsonBlobs",
        description: "Blobs are base64 encoded",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/JsonBlobs",
        body: """
              {
                  "data": "dmFsdWU="
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            data: "value"
        }
    }
])

apply JsonBlobs @httpResponseTests([
    {
        id: "RestJsonJsonBlobs",
        description: "Blobs are base64 encoded",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "data": "dmFsdWU="
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            data: "value"
        }
    }
])

structure JsonBlobsInputOutput {
    data: Blob
}

/// This tests how timestamps are serialized, including using the
/// default format of date-time and various @timestampFormat trait
/// values.
@http(uri: "/JsonTimestamps", method: "POST")
operation JsonTimestamps(JsonTimestampsInputOutput) -> JsonTimestampsInputOutput

apply JsonTimestamps @httpRequestTests([
    {
        id: "RestJsonJsonTimestamps",
        description: "Tests how normal timestamps are serialized",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/JsonTimestamps",
        body: """
              {
                  "normal": 1398796238
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            normal: 1398796238
        }
    },
    {
        id: "RestJsonJsonTimestampsWithDateTimeFormat",
        description: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/JsonTimestamps",
        body: """
              {
                  "dateTime": "2014-04-29T18:30:38Z"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            dateTime: 1398796238
        }
    },
    {
        id: "RestJsonJsonTimestampsWithEpochSecondsFormat",
        description: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/JsonTimestamps",
        body: """
              {
                  "epochSeconds": 1398796238
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            epochSeconds: 1398796238
        }
    },
    {
        id: "RestJsonJsonTimestampsWithHttpDateFormat",
        description: "Ensures that the timestampFormat of http-date works",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/JsonTimestamps",
        body: """
              {
                  "httpDate": "Tue, 29 Apr 2014 18:30:38 GMT"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            httpDate: 1398796238
        }
    },
])

apply JsonTimestamps @httpResponseTests([
    {
        id: "RestJsonJsonTimestamps",
        description: "Tests how normal timestamps are serialized",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "normal": 1398796238
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            normal: 1398796238
        }
    },
    {
        id: "RestJsonJsonTimestampsWithDateTimeFormat",
        description: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "dateTime": "2014-04-29T18:30:38Z"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            dateTime: 1398796238
        }
    },
    {
        id: "RestJsonJsonTimestampsWithEpochSecondsFormat",
        description: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "epochSeconds": 1398796238
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            epochSeconds: 1398796238
        }
    },
    {
        id: "RestJsonJsonTimestampsWithHttpDateFormat",
        description: "Ensures that the timestampFormat of http-date works",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "httpDate": "Tue, 29 Apr 2014 18:30:38 GMT"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            httpDate: 1398796238
        }
    },
])

structure JsonTimestampsInputOutput {
    normal: Timestamp,

    @timestampFormat("date-time")
    dateTime: Timestamp,

    @timestampFormat("epoch-seconds")
    epochSeconds: Timestamp,

    @timestampFormat("http-date")
    httpDate: Timestamp,
}

/// This example serializes enums as top level properties, in lists, sets, and maps.
@idempotent
@http(uri: "/JsonEnums", method: "PUT")
operation JsonEnums(JsonEnumsInputOutput) -> JsonEnumsInputOutput

apply JsonEnums @httpRequestTests([
    {
        id: "RestJsonJsonEnums",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-json-1.1",
        method: "PUT",
        uri: "/JsonEnums",
        body: """
              {
                  "fooEnum1": "Foo",
                  "fooEnum2": "0",
                  "fooEnum3": "1",
                  "fooEnumList": [
                      "Foo",
                      "0"
                  ],
                  "fooEnumSet": [
                      "Foo",
                      "0"
                  ],
                  "fooEnumMap": {
                      "hi": "Foo",
                      "zero": "0"
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            fooEnum1: "Foo",
            fooEnum2: "0",
            fooEnum3: "1",
            fooEnumList: ["Foo", "0"],
            fooEnumSet: ["Foo", "0"],
            fooEnumMap: {
                "hi": "Foo",
                "zero": "0"
            }
        }
    }
])

apply JsonEnums @httpResponseTests([
    {
        id: "RestJsonJsonEnums",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "fooEnum1": "Foo",
                  "fooEnum2": "0",
                  "fooEnum3": "1",
                  "fooEnumList": [
                      "Foo",
                      "0"
                  ],
                  "fooEnumSet": [
                      "Foo",
                      "0"
                  ],
                  "fooEnumMap": {
                      "hi": "Foo",
                      "zero": "0"
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            fooEnum1: "Foo",
            fooEnum2: "0",
            fooEnum3: "1",
            fooEnumList: ["Foo", "0"],
            fooEnumSet: ["Foo", "0"],
            fooEnumMap: {
                "hi": "Foo",
                "zero": "0"
            }
        }
    }
])

structure JsonEnumsInputOutput {
    fooEnum1: FooEnum,
    fooEnum2: FooEnum,
    fooEnum3: FooEnum,
    fooEnumList: FooEnumList,
    fooEnumSet: FooEnumSet,
    fooEnumMap: FooEnumMap,
}

/// Recursive shapes
@idempotent
@http(uri: "/RecursiveShapes", method: "PUT")
operation RecursiveShapes(RecursiveShapesInputOutput) -> RecursiveShapesInputOutput

apply RecursiveShapes @httpRequestTests([
    {
        id: "RestJsonRecursiveShapes",
        description: "Serializes recursive structures",
        protocol: "aws.rest-json-1.1",
        method: "PUT",
        uri: "/JsonEnums",
        body: """
              {
                  "nested": {
                      "foo": "Foo1",
                      "nested": {
                          "bar": "Bar1",
                          "recursiveMember": {
                              "foo": "Foo2",
                              "nested": {
                                  "bar": "Bar2"
                              }
                          }
                      }
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

apply RecursiveShapes @httpResponseTests([
    {
        id: "RestJsonRecursiveShapes",
        description: "Serializes recursive structures",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "nested": {
                      "foo": "Foo1",
                      "nested": {
                          "bar": "Bar1",
                          "recursiveMember": {
                              "foo": "Foo2",
                              "nested": {
                                  "bar": "Bar2"
                              }
                          }
                      }
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

structure RecursiveShapesInputOutput {
    nested: RecursiveShapesInputOutputNested1
}

structure RecursiveShapesInputOutputNested1 {
    foo: String,
    nested: RecursiveShapesInputOutputNested2
}

structure RecursiveShapesInputOutputNested2 {
    bar: String,
    recursiveMember: RecursiveShapesInputOutputNested1,
}
