$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#FooEnumSet
use aws.protocoltests.shared#FooEnumMap
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example serializes enums as top level properties, in lists, sets, and maps.
@idempotent
operation JsonEnums {
    input: JsonEnumsInputOutput,
    output: JsonEnumsInputOutput
}

apply JsonEnums @httpRequestTests([
    {
        id: "AwsJson11Enums",
        documentation: "Serializes simple scalar properties",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
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
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        bodyMediaType: "application/json",
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
        id: "AwsJson11Enums",
        documentation: "Serializes simple scalar properties",
        protocol: awsJson1_1,
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
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        bodyMediaType: "application/json",
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
