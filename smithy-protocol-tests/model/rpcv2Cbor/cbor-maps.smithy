$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocoltests.shared#FooEnumMap
use smithy.protocoltests.shared#GreetingStruct
use smithy.protocoltests.shared#SparseStringMap
use smithy.protocoltests.shared#StringSet
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.protocols#rpcv2Cbor
use smithy.framework#ValidationException

/// The example tests basic map serialization.
operation RpcV2CborDenseMaps {
    input: RpcV2CborDenseMapsInputOutput,
    output: RpcV2CborDenseMapsInputOutput,
    errors: [ValidationException]
}

apply RpcV2CborDenseMaps @httpRequestTests([
    {
        id: "RpcV2CborMaps",
        documentation: "Serializes maps",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborDenseMaps",
        body: "oW5kZW5zZVN0cnVjdE1hcKJjZm9voWJoaWV0aGVyZWNiYXqhYmhpY2J5ZQ=="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
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
        id: "RpcV2CborSerializesZeroValuesInMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborDenseMaps",
        body: "om5kZW5zZU51bWJlck1hcKFheABvZGVuc2VCb29sZWFuTWFwoWF49A==",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            "denseNumberMap": {
                "x": 0
            },
            "denseBooleanMap": {
                "x": false
            }
        }
    },
    {
        id: "RpcV2CborSerializesDenseSetMap",
        documentation: "A request that contains a dense map of sets.",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborDenseMaps",
        body: "oWtkZW5zZVNldE1hcKJheIBheYJhYWFi",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            "denseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
])

apply RpcV2CborDenseMaps @httpResponseTests([
    {
        id: "RpcV2CborMaps",
        documentation: "Deserializes maps",
        protocol: rpcv2Cbor,
        code: 200,
        body: "oW5kZW5zZVN0cnVjdE1hcKJjZm9voWJoaWV0aGVyZWNiYXqhYmhpY2J5ZQ==",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
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
        id: "RpcV2CborDeserializesZeroValuesInMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: rpcv2Cbor,
        code: 200,
        body: "om5kZW5zZU51bWJlck1hcKFheABvZGVuc2VCb29sZWFuTWFwoWF49A==",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "denseNumberMap": {
                "x": 0
            },
            "denseBooleanMap": {
                "x": false
            }
        }
    },
    {
        id: "RpcV2CborDeserializesDenseSetMap",
        documentation: "A response that contains a dense map of sets",
        protocol: rpcv2Cbor,
        code: 200,
        body: "oWtkZW5zZVNldE1hcKJheIBheYJhYWFi",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "denseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
    {
        id: "RpcV2CborDeserializesDenseSetMapAndSkipsNull",
        documentation: """
            Clients SHOULD tolerate seeing a null value in a dense map, and they SHOULD
            drop the null key-value pair.""",
        protocol: rpcv2Cbor,
        appliesTo: "client",
        code: 200,
        body: "oWtkZW5zZVNldE1hcKNheIBheYJhYWFiYXr2",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "denseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    }
])

structure RpcV2CborDenseMapsInputOutput {
    denseStructMap: DenseStructMap,
    denseNumberMap: DenseNumberMap,
    denseBooleanMap: DenseBooleanMap,
    denseStringMap: DenseStringMap,
    denseSetMap: DenseSetMap,
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


apply RpcV2CborSparseMaps @httpRequestTests([
    {
        id: "RpcV2CborSparseMaps",
        documentation: "Serializes sparse maps",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborSparseMaps",
        body: "v29zcGFyc2VTdHJ1Y3RNYXC/Y2Zvb79iaGlldGhlcmX/Y2Jher9iaGljYnll////",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
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
        id: "RpcV2CborSerializesNullMapValues",
        documentation: "Serializes null map values in sparse maps",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborSparseMaps",
        body: "v3BzcGFyc2VCb29sZWFuTWFwv2F49v9vc3BhcnNlTnVtYmVyTWFwv2F49v9vc3BhcnNlU3RyaW5nTWFwv2F49v9vc3BhcnNlU3RydWN0TWFwv2F49v//"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
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
        id: "RpcV2CborSerializesSparseSetMap",
        documentation: "A request that contains a sparse map of sets",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborSparseMaps",
        body: "v2xzcGFyc2VTZXRNYXC/YXif/2F5n2FhYWL///8="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
    {
        id: "RpcV2CborSerializesSparseSetMapAndRetainsNull",
        documentation: "A request that contains a sparse map of sets.",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborSparseMaps",
        body: "v2xzcGFyc2VTZXRNYXC/YXif/2F5n2FhYWL/YXr2//8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"],
                "z": null
            }
        }
    },
    {
        id: "RpcV2CborSerializesZeroValuesInSparseMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborSparseMaps",
        body: "v29zcGFyc2VOdW1iZXJNYXC/YXgA/3BzcGFyc2VCb29sZWFuTWFwv2F49P//"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            "sparseNumberMap": {
                "x": 0
            },
            "sparseBooleanMap": {
                "x": false
            }
        }
    }
])

apply RpcV2CborSparseMaps @httpResponseTests([
    {
        id: "RpcV2CborSparseJsonMaps",
        documentation: "Deserializes sparse maps",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v29zcGFyc2VTdHJ1Y3RNYXC/Y2Zvb79iaGlldGhlcmX/Y2Jher9iaGljYnll////",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
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
        id: "RpcV2CborDeserializesNullMapValues",
        documentation: "Deserializes null map values",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v3BzcGFyc2VCb29sZWFuTWFwv2F49v9vc3BhcnNlTnVtYmVyTWFwv2F49v9vc3BhcnNlU3RyaW5nTWFwv2F49v9vc3BhcnNlU3RydWN0TWFwv2F49v//"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
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
        id: "RpcV2CborDeserializesSparseSetMap",
        documentation: "A response that contains a sparse map of sets",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2xzcGFyc2VTZXRNYXC/YXmfYWFhYv9heJ////8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"]
            }
        }
    },
    {
        id: "RpcV2CborDeserializesSparseSetMapAndRetainsNull",
        documentation: "A response that contains a sparse map of sets with a null",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2xzcGFyc2VTZXRNYXC/YXif/2F5n2FhYWL/YXr2//8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "sparseSetMap": {
                "x": [],
                "y": ["a", "b"],
                "z": null
            }
        }
    },
    {
        id: "RpcV2CborDeserializesZeroValuesInSparseMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v29zcGFyc2VOdW1iZXJNYXC/YXgA/3BzcGFyc2VCb29sZWFuTWFwv2F49P//"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "sparseNumberMap": {
                "x": 0
            },
            "sparseBooleanMap": {
                "x": false
            }
        }
    }
])

operation RpcV2CborSparseMaps {
    input: RpcV2CborSparseMapsInputOutput
    output: RpcV2CborSparseMapsInputOutput
    errors: [ValidationException]
}

structure RpcV2CborSparseMapsInputOutput {
    sparseStructMap: SparseStructMap,
    sparseNumberMap: SparseNumberMap,
    sparseBooleanMap: SparseBooleanMap,
    sparseStringMap: SparseStringMap,
    sparseSetMap: SparseSetMap,
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
