// This file defines test cases that serialize maps in JSON payloads.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#SparseStringMap
use aws.protocoltests.shared#StringSet
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
                "denseBooleanMap": {
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
            "denseBooleanMap": {
                "x": false
            },
        }
    },
    {
        id: "RestJsonSerializesDenseSetMap",
        documentation: "A request that contains a dense map of sets.",
        protocol: restJson1,
        method: "POST",
        uri: "/JsonMaps",
        body: """
            {
                "denseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "denseSetMap": {
                "x": [],
                "y": ["a", "b"]
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
                "denseBooleanMap": {
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
            "denseBooleanMap": {
                "x": false
            },
        }
    },
    {
        id: "RestJsonDeserializesDenseSetMap",
        documentation: "A response that contains a dense map of sets.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "denseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "denseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
    {
        id: "RestJsonDeserializesDenseSetMapAndSkipsNull",
        documentation: """
            Clients SHOULD tolerate seeing a null value in a dense map, and they SHOULD
            drop the null key-value pair.""",
        protocol: restJson1,
        appliesTo: "client",
        code: 200,
        body: """
            {
                "denseSetMap": {
                    "x": [],
                    "y": ["a", "b"],
                    "z": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "denseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    }
])

structure JsonMapsInputOutput {
    denseStructMap: DenseStructMap
    denseNumberMap: DenseNumberMap
    denseBooleanMap: DenseBooleanMap
    denseStringMap: DenseStringMap
    denseSetMap: DenseSetMap
}

map DenseStructMap {
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

map DenseSetMap {
    key: String,
    value: StringSet
}

/// This example tests sparse map serialization.
@http(uri: "/SparseJsonMaps", method: "POST")
operation SparseJsonMaps {
    input: SparseJsonMapsInputOutput
    output: SparseJsonMapsInputOutput
}

apply SparseJsonMaps @httpRequestTests([
    {
        id: "RestJsonSparseJsonMaps",
        documentation: "Serializes JSON maps",
        protocol: restJson1,
        method: "POST",
        uri: "/SparseJsonMaps",
        body: """
              {
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
        id: "RestJsonSerializesSparseNullMapValues",
        documentation: "Serializes JSON map values in sparse maps",
        protocol: restJson1,
        method: "POST",
        uri: "/SparseJsonMaps",
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
        id: "RestJsonSerializesZeroValuesInSparseMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: restJson1,
        method: "POST",
        uri: "/SparseJsonMaps",
        body: """
            {
                "sparseNumberMap": {
                    "x": 0
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
            "sparseNumberMap": {
                "x": 0
            },
            "sparseBooleanMap": {
                "x": false
            }
        }
    },
    {
        id: "RestJsonSerializesSparseSetMap",
        documentation: "A request that contains a sparse map of sets",
        protocol: restJson1,
        method: "POST",
        uri: "/SparseJsonMaps",
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
    {
        id: "RestJsonSerializesSparseSetMapAndRetainsNull",
        documentation: "A request that contains a sparse map of sets.",
        protocol: restJson1,
        method: "POST",
        uri: "/SparseJsonMaps",
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"],
                    "z": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"],
                "z": null
            }
        }
    }
])

apply SparseJsonMaps @httpResponseTests([
    {
        id: "RestJsonSparseJsonMaps",
        documentation: "Deserializes JSON maps",
        protocol: restJson1,
        code: 200,
        body: """
              {
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
        id: "RestJsonDeserializesSparseNullMapValues",
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
        id: "RestJsonDeserializesZeroValuesInSparseMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "sparseNumberMap": {
                    "x": 0
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
            "sparseNumberMap": {
                "x": 0
            },
            "sparseBooleanMap": {
                "x": false
            }
        }
    },
    {
        id: "RestJsonDeserializesSparseSetMap",
        documentation: "A response that contains a sparse map of sets",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
    {
        id: "RestJsonDeserializesSparseSetMapAndRetainsNull",
        documentation: "A response that contains a sparse map of sets.",
        protocol: restJson1,
        code: 200,
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"],
                    "z": null
                }
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"],
                "z": null
            }
        }
    }
])

structure SparseJsonMapsInputOutput {
    sparseStructMap: SparseStructMap
    sparseNumberMap: SparseNumberMap
    sparseBooleanMap: SparseBooleanMap
    sparseStringMap: SparseStringMap
    sparseSetMap: SparseSetMap
}

@sparse
map SparseStructMap {
    key: String,
    value: GreetingStruct
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

@sparse
map SparseSetMap {
    key: String,
    value: StringSet
}
