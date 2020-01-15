// This file defines test cases that serialize maps in JSON payloads.

$version: "0.5.0"

namespace aws.protocols.tests.restjson

use aws.protocols.tests.shared#FooEnumMap
use aws.protocols.tests.shared#GreetingStruct
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests basic map serialization.
@http(uri: "/JsonMaps", method: "POST")
operation JsonMaps {
    input: JsonMapsInputOutput,
    output: JsonMapsInputOutput
}

apply JsonMaps @httpRequestTests([
    {
        id: "RestJsonJsonMaps",
        description: "Serializes JSON maps",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/JsonMaps",
        body: """
              {
                  "myMap": {
                      "foo": {
                          "hi": "there"
                      },
                      "baz": {
                          "hi": "bye"
                      }
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "myMap": {
                "foo": {
                    "hi": "there"
                },
                "baz": {
                    "hi": "bye"
                }
            }
        }
    }
])

apply JsonMaps @httpResponseTests([
    {
        id: "RestJsonJsonMaps",
        description: "Serializes JSON maps",
        protocol: "aws.rest-json-1.1",
        code: 200,
        body: """
              {
                  "myMap": {
                      "foo": {
                          "hi": "there"
                      },
                      "baz": {
                          "hi": "bye"
                      }
                  }
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            myMap: {
                foo: {
                    hi: "there"
                },
                baz: {
                    hi: "bye"
                }
            }
        }
    }
])

structure JsonMapsInputOutput {
    myMap: JsonMapsInputOutputMap,
}

map JsonMapsInputOutputMap {
    key: String,
    value: GreetingStruct
}
