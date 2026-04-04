$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocoltests.shared#FooEnumMap
use smithy.protocoltests.shared#GreetingStruct
use smithy.protocoltests.shared#SparseStringMap
use smithy.protocoltests.shared#StringSet
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.protocols#rpcv2Json
use smithy.framework#ValidationException

/// The example tests basic map serialization.
operation RpcV2JsonDenseMaps {
    input: RpcV2JsonDenseMapsInputOutput
    output: RpcV2JsonDenseMapsInputOutput
    errors: [ValidationException]
}

apply RpcV2JsonDenseMaps @httpRequestTests([
    {
        id: "RpcV2JsonRequestMaps"
        documentation: "Serializes maps"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonDenseMaps"
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
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "denseStructMap": {
                "foo": {
                    "hi": "there"
                }
                "baz": {
                    "hi": "bye"
                }
            }
        }
    }
    {
        id: "RpcV2JsonRequestSerializesZeroValuesInMaps"
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonDenseMaps"
        body: """
            {
                "denseNumberMap": {
                    "x": 0
                },
                "denseBooleanMap": {
                    "x": false
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "denseNumberMap": {
                "x": 0
            }
            "denseBooleanMap": {
                "x": false
            }
        }
    }
    {
        id: "RpcV2JsonRequestSerializesDenseSetMap"
        documentation: "A request that contains a dense map of sets."
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonDenseMaps"
        body: """
            {
                "denseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "denseSetMap": {
                "x": []
                "y": ["a", "b"]
            }
        }
    }
])

apply RpcV2JsonDenseMaps @httpResponseTests([
    {
        id: "RpcV2JsonResponseMaps"
        documentation: "Deserializes maps"
        protocol: rpcv2Json
        code: 200
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
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "denseStructMap": {
                "foo": {
                    "hi": "there"
                }
                "baz": {
                    "hi": "bye"
                }
            }
        }
    }
    {
        id: "RpcV2JsonResponseDeserializesZeroValuesInMaps"
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "denseNumberMap": {
                    "x": 0
                },
                "denseBooleanMap": {
                    "x": false
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "denseNumberMap": {
                "x": 0
            }
            "denseBooleanMap": {
                "x": false
            }
        }
    }
    {
        id: "RpcV2JsonResponseDeserializesDenseSetMap"
        documentation: "A response that contains a dense map of sets"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "denseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "denseSetMap": {
                "x": []
                "y": ["a", "b"]
            }
        }
    }
])

structure RpcV2JsonDenseMapsInputOutput {
    denseStructMap: DenseStructMap
    denseNumberMap: DenseNumberMap
    denseBooleanMap: DenseBooleanMap
    denseStringMap: DenseStringMap
    denseSetMap: DenseSetMap
}

map DenseStructMap {
    key: String
    value: GreetingStruct
}

map DenseBooleanMap {
    key: String
    value: Boolean
}

map DenseNumberMap {
    key: String
    value: Integer
}

map DenseStringMap {
    key: String
    value: String
}

map DenseSetMap {
    key: String
    value: StringSet
}


apply RpcV2JsonSparseMaps @httpRequestTests([
    {
        id: "RpcV2JsonRequestSparseMaps"
        documentation: "Serializes sparse maps"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonSparseMaps"
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
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "sparseStructMap": {
                "foo": {
                    "hi": "there"
                }
                "baz": {
                    "hi": "bye"
                }
            }
        }
    }
    {
        id: "RpcV2JsonRequestSerializesNullMapValues"
        documentation: "Serializes null map values in sparse maps"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonSparseMaps"
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
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "sparseBooleanMap": {
                "x": null
            }
            "sparseNumberMap": {
                "x": null
            }
            "sparseStringMap": {
                "x": null
            }
            "sparseStructMap": {
                "x": null
            }
        }
    }
    {
        id: "RpcV2JsonRequestSerializesSparseSetMap"
        documentation: "A request that contains a sparse map of sets"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonSparseMaps"
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "sparseSetMap": {
                "x": []
                "y": ["a", "b"]
            }
        }
    }
    {
        id: "RpcV2JsonRequestSerializesSparseSetMapAndRetainsNull"
        documentation: "A request that contains a sparse map of sets."
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonSparseMaps"
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"],
                    "z": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "sparseSetMap": {
                "x": []
                "y": ["a", "b"]
                "z": null
            }
        }
    }
    {
        id: "RpcV2JsonRequestSerializesZeroValuesInSparseMaps"
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonSparseMaps"
        body: """
            {
                "sparseNumberMap": {
                    "x": 0
                },
                "sparseBooleanMap": {
                    "x": false
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            "sparseNumberMap": {
                "x": 0
            }
            "sparseBooleanMap": {
                "x": false
            }
        }
    }
])

apply RpcV2JsonSparseMaps @httpResponseTests([
    {
        id: "RpcV2JsonResponseSparseJsonMaps"
        documentation: "Deserializes sparse maps"
        protocol: rpcv2Json
        code: 200
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
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
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
    }
    {
        id: "RpcV2JsonResponseDeserializesNullMapValues"
        documentation: "Deserializes null map values"
        protocol: rpcv2Json
        code: 200
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
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "sparseBooleanMap": {
                "x": null
            }
            "sparseNumberMap": {
                "x": null
            }
            "sparseStringMap": {
                "x": null
            }
            "sparseStructMap": {
                "x": null
            }
        }
    }
    {
        id: "RpcV2JsonResponseDeserializesSparseSetMap"
        documentation: "A response that contains a sparse map of sets"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"]
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "sparseSetMap": {
                "x": []
                "y": ["a", "b"]
            }
        }
    }
    {
        id: "RpcV2JsonResponseDeserializesSparseSetMapAndRetainsNull"
        documentation: "A response that contains a sparse map of sets with a null"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "sparseSetMap": {
                    "x": [],
                    "y": ["a", "b"],
                    "z": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "sparseSetMap": {
                "x": []
                "y": ["a", "b"]
                "z": null
            }
        }
    }
    {
        id: "RpcV2JsonResponseDeserializesZeroValuesInSparseMaps"
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "sparseNumberMap": {
                    "x": 0
                },
                "sparseBooleanMap": {
                    "x": false
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "sparseNumberMap": {
                "x": 0
            }
            "sparseBooleanMap": {
                "x": false
            }
        }
    }
])

operation RpcV2JsonSparseMaps {
    input: RpcV2JsonSparseMapsInputOutput
    output: RpcV2JsonSparseMapsInputOutput
    errors: [ValidationException]
}

structure RpcV2JsonSparseMapsInputOutput {
    sparseStructMap: SparseStructMap
    sparseNumberMap: SparseNumberMap
    sparseBooleanMap: SparseBooleanMap
    sparseStringMap: SparseStringMap
    sparseSetMap: SparseSetMap
}

@sparse
map SparseStructMap {
    key: String
    value: GreetingStruct
}

@sparse
map SparseBooleanMap {
    key: String
    value: Boolean
}

@sparse
map SparseNumberMap {
    key: String
    value: Integer
}

@sparse
map SparseSetMap {
    key: String
    value: StringSet
}
