// This file defines test cases that serialize maps in JSON payloads.

$version: "1.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#SparseStringMap
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
                  "denseStructMap": {
                      "foo": {
                          "hi": "there"
                      },
                      "baz": {
                          "hi": "bye"
                      }
                  },
                  "sparseStructMap": {
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
            "denseStructMap": {
                "foo": {
                    "hi": "there"
                },
                "baz": {
                    "hi": "bye"
                }
            },
            "sparseStructMap": {
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
        documentation: "Serializes JSON map values in sparse maps",
        protocol: restJson1,
        method: "POST",
        uri: "/JsonMaps",
        body: """
            {
                "sparseBooleanMap": {
                    "x": null
                },
                "sparseNumberMap": {
                    "x": null
                },
                "sparseStringMap": {
                    "x": null
                },
                "sparseStructMap": {
                    "x": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "sparseBooleanMap": {
                "x": null
            },
            "sparseNumberMap": {
                "x": null
            },
            "sparseStringMap": {
                "x": null
            },
            "sparseStructMap": {
                "x": null
            }
        }
    },
    {
        id: "RestJsonSerializesZeroValuesInMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: restJson1,
        method: "POST",
        uri: "/JsonMaps",
        body: """
            {
                "denseNumberMap": {
                    "x": 0
                },
                "sparseNumberMap": {
                    "x": 0
                },
                "denseBooleanMap": {
                    "x": false
                },
                "sparseBooleanMap": {
                    "x": false
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "denseNumberMap": {
                "x": 0
            },
            "sparseNumberMap": {
                "x": 0
            },
            "denseBooleanMap": {
                "x": false
            },
            "sparseBooleanMap": {
                "x": false
            }
        }
    }
])

apply JsonMaps @httpResponseTests([
    {
        id: "RestJsonJsonMaps",
        documentation: "Deserializes JSON maps",
        protocol: restJson1,
        code: 200,
        body: """
              {
                  "denseStructMap": {
                      "foo": {
                          "hi": "there"
                      },
                      "baz": {
                          "hi": "bye"
                      }
                  },
                  "sparseStructMap": {
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
            "denseStructMap": {
                "foo": {
                    "hi": "there"
                },
                "baz": {
                    "hi": "bye"
                }
            },
            "sparseStructMap": {
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
        id: "RestJsonDeserializesNullMapValues",
        documentation: "Deserializes null JSON map values",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "sparseBooleanMap": {
                    "x": null
                },
                "sparseNumberMap": {
                    "x": null
                },
                "sparseStringMap": {
                    "x": null
                },
                "sparseStructMap": {
                    "x": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "sparseBooleanMap": {
                "x": null
            },
            "sparseNumberMap": {
                "x": null
            },
            "sparseStringMap": {
                "x": null
            },
            "sparseStructMap": {
                "x": null
            }
        }
    },
    {
        id: "RestJsonDeserializesZeroValuesInMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "denseNumberMap": {
                    "x": 0
                },
                "sparseNumberMap": {
                    "x": 0
                },
                "denseBooleanMap": {
                    "x": false
                },
                "sparseBooleanMap": {
                    "x": false
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "denseNumberMap": {
                "x": 0
            },
            "sparseNumberMap": {
                "x": 0
            },
            "denseBooleanMap": {
                "x": false
            },
            "sparseBooleanMap": {
                "x": false
            }
        }
    }
])

structure JsonMapsInputOutput {
    denseStructMap: DenseStructMap,
    sparseStructMap: SparseStructMap,
    denseNumberMap: DenseNumberMap,
    denseBooleanMap: DenseBooleanMap,
    denseStringMap: DenseStringMap,
    sparseNumberMap: SparseNumberMap,
    sparseBooleanMap: SparseBooleanMap,
    sparseStringMap: SparseStringMap,
    denseSetMap: DenseSetMap,
    sparseSetMap: SparseSetMap,
}

map DenseStructMap {
    key: String,
    value: GreetingStruct
}

@sparse
map SparseStructMap {
    key: String,
    value: GreetingStruct
}

map DenseBooleanMap {
    key: String,
    value: Boolean
}

map DenseNumberMap {
    key: String,
    value: Integer
}

map DenseStringMap {
    key: String,
    value: String
}

@sparse
map SparseBooleanMap {
    key: String,
    value: Boolean
}

@sparse
map SparseNumberMap {
    key: String,
    value: Integer
}

map DenseSetMap {
    key: String,
    value: aws.protocoltests.shared#StringSet
}

@sparse
map SparseSetMap {
    key: String,
    value: aws.protocoltests.shared#StringSet
}