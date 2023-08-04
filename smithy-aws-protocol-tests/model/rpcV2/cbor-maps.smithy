$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#SparseStringMap
use aws.protocoltests.shared#StringSet
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.protocols#rpcv2Cbor

/// The example tests basic map serialization.
operation RpcV2CborMaps {
    input: RpcV2CborMapsInputOutput,
    output: RpcV2CborMapsInputOutput
}

apply RpcV2CborMaps @httpRequestTests([
    {
        id: "RpcV2CborMaps",
        documentation: "Serializes maps",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborMaps",
        body: "v25kZW5zZVN0cnVjdE1hcL9jYmF6v2JoaWNieWX/Y2Zvb79iaGlldGhlcmX//29zcGFyc2VTdHJ1Y3RNYXC/Y2Jher9iaGljYnll/2Nmb2+/YmhpZXRoZXJl////",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
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
        id: "RpcV2CborSerializesNullMapValues",
        documentation: "Serializes map values in sparse maps",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborMaps",
        body: "v3BzcGFyc2VCb29sZWFuTWFwv2F49v9vc3BhcnNlTnVtYmVyTWFwv2F49v9vc3BhcnNlU3RydWN0TWFwv2F49v//",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
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
        id: "RpcV2CborSerializesZeroValuesInMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborMaps",
        body: "v29kZW5zZUJvb2xlYW5NYXC/YXj0/25kZW5zZU51bWJlck1hcL9heAD/cHNwYXJzZUJvb2xlYW5NYXC/YXj0/29zcGFyc2VOdW1iZXJNYXC/YXgA//8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
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
    },
    {
        id: "RpcV2CborSerializesSparseSetMap",
        documentation: "A request that contains a sparse map of sets",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborMaps",
        body: "v2xzcGFyc2VTZXRNYXC/YXmfYWFhYv9heJ////8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
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
        id: "RpcV2CborSerializesDenseSetMap",
        documentation: "A request that contains a dense map of sets.",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborMaps",
        body: "v2xzcGFyc2VTZXRNYXC/YXmfYWFhYv9heJ////8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
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
        id: "RpcV2CborSerializesSparseSetMapAndRetainsNull",
        documentation: "A request that contains a sparse map of sets.",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborMaps",
        body: "v2xzcGFyc2VTZXRNYXC/YXif/2F5n2FhYWL/YXr2//8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
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

apply RpcV2CborMaps @httpResponseTests([
    {
        id: "RpcV2CborMaps",
        documentation: "Deserializes maps",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v25kZW5zZVN0cnVjdE1hcL9jYmF6v2JoaWNieWX/Y2Zvb79iaGlldGhlcmX//29zcGFyc2VTdHJ1Y3RNYXC/Y2Jher9iaGljYnll/2Nmb2+/YmhpZXRoZXJl////",
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
        id: "RpcV2CborDeserializesNullMapValues",
        documentation: "Deserializes null map values",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v3BzcGFyc2VCb29sZWFuTWFwv2F49v9vc3BhcnNlTnVtYmVyTWFwv2F49v9vc3BhcnNlU3RydWN0TWFwv2F49v//",
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
        id: "RpcV2CborDeserializesZeroValuesInMaps",
        documentation: "Ensure that 0 and false are sent over the wire in all maps and lists",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v29kZW5zZUJvb2xlYW5NYXC/YXj0/25kZW5zZU51bWJlck1hcL9heAD/cHNwYXJzZUJvb2xlYW5NYXC/YXj0/29zcGFyc2VOdW1iZXJNYXC/YXgA//8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
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
        id: "RpcV2CborDeserializesDenseSetMap",
        documentation: "A response that contains a dense map of sets.",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2xzcGFyc2VTZXRNYXC/YXmfYWFhYv9heJ////8=",
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
        id: "RpcV2CborDeserializesSparseSetMapAndRetainsNull",
        documentation: "A response that contains a sparse map of sets.",
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
        id: "RpcV2CborDeserializesDenseSetMapAndSkipsNull",
        documentation: """
            Clients SHOULD tolerate seeing a null value in a dense map, and they SHOULD
            drop the null key-value pair.""",
        protocol: rpcv2Cbor,
        appliesTo: "client",
        code: 200,
        body: "v2xzcGFyc2VTZXRNYXC/YXif/2F5n2FhYWL/YXr2//8=",
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

structure RpcV2CborMapsInputOutput {
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
    value: StringSet
}

@sparse
map SparseSetMap {
    key: String,
    value: StringSet
}

