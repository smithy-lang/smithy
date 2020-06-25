// This file defines test cases that serialize maps in JSON payloads.

$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#GreetingStruct
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
        documentation: "Serializes JSON maps",
        protocol: restJson1,
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
    },
    {
        id: "RestJsonSerializesNullMapValues",
        documentation: "Serializes null JSON map values",
        protocol: restJson1,
        method: "POST",
        uri: "/JsonMaps",
        body: """
            {
                "myMap": {
                    "foo": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            myMap: {
                "foo": null
            }
        }
    },
])

apply JsonMaps @httpResponseTests([
    {
        id: "RestJsonJsonMaps",
        documentation: "Deserializes JSON maps",
        protocol: restJson1,
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
    },
    {
        id: "RestJsonDeserializesNullMapValues",
        documentation: "Deserializes null JSON map values",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "myMap": {
                    "foo": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            myMap: {
                "foo": null
            }
        }
    },
])

structure JsonMapsInputOutput {
    myMap: JsonMapsInputOutputMap,
}

map JsonMapsInputOutputMap {
    key: String,
    value: GreetingStruct
}
