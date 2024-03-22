// This file defines test cases that serialize lists in JSON documents.

$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

use aws.protocoltests.shared#BooleanList
use aws.protocoltests.shared#BlobList
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#IntegerEnumList
use aws.protocoltests.shared#IntegerList
use aws.protocoltests.shared#NestedStringList
use aws.protocoltests.shared#SparseStringList
use aws.protocoltests.shared#SparseStringMap
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringSet
use aws.protocoltests.shared#TimestampList
use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy.framework#ValidationException

/// This test case serializes JSON lists for the following cases for both
/// input and output:
///
/// 1. Normal lists.
/// 2. Normal sets.
/// 3. Lists of lists.
/// 4. Lists of structures.
@idempotent
operation RpcV2CborLists {
    input: RpcV2CborListInputOutput,
    output: RpcV2CborListInputOutput,
    errors: [ValidationException]
}

apply RpcV2CborLists @httpRequestTests([
    {
        id: "RpcV2CborLists",
        documentation: "Serializes RpcV2 Cbor lists",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v2pzdHJpbmdMaXN0gmNmb29jYmFyaXN0cmluZ1NldIJjZm9vY2JhcmtpbnRlZ2VyTGlzdIIBAmtib29sZWFuTGlzdIL19G10aW1lc3RhbXBMaXN0gsH7QdTX%2B%2FOAAADB%2B0HU1%2FvzgAAAaGVudW1MaXN0gmNGb29hMGtpbnRFbnVtTGlzdIIBAnBuZXN0ZWRTdHJpbmdMaXN0goJjZm9vY2JhcoJjYmF6Y3F1eG1zdHJ1Y3R1cmVMaXN0gqJhYWExYWJhMqJhYWEzYWJhNGhibG9iTGlzdIJDZm9vQ2Jhcv8%3D
        body: "v2pzdHJpbmdMaXN0gmNmb29jYmFyaXN0cmluZ1NldIJjZm9vY2JhcmtpbnRlZ2VyTGlzdIIBAmtib29sZWFuTGlzdIL19G10aW1lc3RhbXBMaXN0gsH7QdTX+/OAAADB+0HU1/vzgAAAaGVudW1MaXN0gmNGb29hMGtpbnRFbnVtTGlzdIIBAnBuZXN0ZWRTdHJpbmdMaXN0goJjZm9vY2JhcoJjYmF6Y3F1eG1zdHJ1Y3R1cmVMaXN0gqJhYWExYWJhMqJhYWEzYWJhNGhibG9iTGlzdIJDZm9vQ2Jhcv8="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "stringList": [
                "foo",
                "bar"
            ],
            "stringSet": [
                "foo",
                "bar"
            ],
            "integerList": [
                1,
                2
            ],
            "booleanList": [
                true,
                false
            ],
            "timestampList": [
                1398796238,
                1398796238
            ],
            "enumList": [
                "Foo",
                "0"
            ],
            "intEnumList": [
                1,
                2
            ],
            "nestedStringList": [
                [
                    "foo",
                    "bar"
                ],
                [
                    "baz",
                    "qux"
                ]
            ],
            "structureList": [
                {
                    "a": "1",
                    "b": "2"
                },
                {
                    "a": "3",
                    "b": "4"
                }
            ],
            "blobList" : [
                "foo",
                "bar"
            ]
        }
    },
    {
        id: "RpcV2CborListsEmpty",
        documentation: "Serializes empty JSON lists",
        tags: ["client-indefinite"]
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        body: "v2pzdHJpbmdMaXN0n///",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: []
        }
    },
    {
        id: "RpcV2CborListsEmptyUsingDefiniteLength",
        documentation: "Serializes empty JSON definite length lists",
        tags: ["client-definite"]
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        body: "oWpzdHJpbmdMaXN0gA=="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: []
        }
    },
    {
        id: "RpcV2CborIndefiniteStringInsideIndefiniteList",
        documentation: "Can deserialize indefinite length text strings inside an indefinite length list",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v2pzdHJpbmdMaXN0n394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h%2F394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r%2F3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n%2F%2F8%3D
        body: "v2pzdHJpbmdMaXN0n394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h/394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r/3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n//8="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: ["An example indefinite string, which will be chunked, on each comma", "Another example indefinite string with only one chunk", "This is a plain string"]
        }
        appliesTo: "server"
    },
    {
        id: "RpcV2CborIndefiniteStringInsideDefiniteList",
        documentation: "Can deserialize indefinite length text strings inside a definite length list",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=oWpzdHJpbmdMaXN0g394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h%2F394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r%2F3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n
        body: "oWpzdHJpbmdMaXN0g394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h/394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r/3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: ["An example indefinite string, which will be chunked, on each comma", "Another example indefinite string with only one chunk", "This is a plain string"]
        },
        appliesTo: "server"
    }
])

apply RpcV2CborLists @httpResponseTests([
    {
        id: "RpcV2CborLists",
        documentation: "Serializes RpcV2 Cbor lists",
        protocol: rpcv2Cbor,
        code: 200,
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v2pzdHJpbmdMaXN0n2Nmb29jYmFy%2F2lzdHJpbmdTZXSfY2Zvb2NiYXL%2Fa2ludGVnZXJMaXN0nwEC%2F2tib29sZWFuTGlzdJ%2F19P9tdGltZXN0YW1wTGlzdJ%2FB%2B0HU1%2FvzgAAAwftB1Nf784AAAP9oZW51bUxpc3SfY0Zvb2Ew%2F2tpbnRFbnVtTGlzdJ8BAv9wbmVzdGVkU3RyaW5nTGlzdJ%2BfY2Zvb2NiYXL%2Fn2NiYXpjcXV4%2F%2F9tc3RydWN0dXJlTGlzdJ%2B%2FYWFhMWFiYTL%2Fv2FhYTNhYmE0%2F%2F9oYmxvYkxpc3SfQ2Zvb0NiYXL%2F%2Fw%3D%3D
        body: "v2pzdHJpbmdMaXN0n2Nmb29jYmFy/2lzdHJpbmdTZXSfY2Zvb2NiYXL/a2ludGVnZXJMaXN0nwEC/2tib29sZWFuTGlzdJ/19P9tdGltZXN0YW1wTGlzdJ/B+0HU1/vzgAAAwftB1Nf784AAAP9oZW51bUxpc3SfY0Zvb2Ew/2tpbnRFbnVtTGlzdJ8BAv9wbmVzdGVkU3RyaW5nTGlzdJ+fY2Zvb2NiYXL/n2NiYXpjcXV4//9tc3RydWN0dXJlTGlzdJ+/YWFhMWFiYTL/v2FhYTNhYmE0//9oYmxvYkxpc3SfQ2Zvb0NiYXL//w=="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "stringList": [
                "foo",
                "bar"
            ],
            "stringSet": [
                "foo",
                "bar"
            ],
            "integerList": [
                1,
                2
            ],
            "booleanList": [
                true,
                false
            ],
            "timestampList": [
                1398796238,
                1398796238
            ],
            "enumList": [
                "Foo",
                "0"
            ],
            "intEnumList": [
                1,
                2
            ],
            "nestedStringList": [
                [
                    "foo",
                    "bar"
                ],
                [
                    "baz",
                    "qux"
                ]
            ],
            "structureList": [
                {
                    "a": "1",
                    "b": "2"
                },
                {
                    "a": "3",
                    "b": "4"
                }
            ],
            "blobList": [
                "foo",
                "bar"
            ]
        }
    },
    {
        id: "RpcV2CborListsEmpty",
        documentation: "Serializes empty RpcV2 Cbor lists",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2pzdHJpbmdMaXN0n///",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: []
        }
    },
    {
        id: "RpcV2CborIndefiniteStringInsideIndefiniteListCanDeserialize",
        documentation: "Can deserialize indefinite length text strings inside an indefinite length list",
        protocol: rpcv2Cbor,
        code: 200,
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v2pzdHJpbmdMaXN0n394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h%2F394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r%2F3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n%2F%2F8%3D
        body: "v2pzdHJpbmdMaXN0n394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h/394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r/3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n//8="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: ["An example indefinite string, which will be chunked, on each comma", "Another example indefinite string with only one chunk", "This is a plain string"]
        }
        appliesTo: "client"
    },
    {
        id: "RpcV2CborIndefiniteStringInsideDefiniteListCanDeserialize",
        documentation: "Can deserialize indefinite length text strings inside a definite length list",
        protocol: rpcv2Cbor,
        code: 200,
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=oWpzdHJpbmdMaXN0g394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h%2F394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r%2F3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n
        body: "oWpzdHJpbmdMaXN0g394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h/394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r/3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: ["An example indefinite string, which will be chunked, on each comma", "Another example indefinite string with only one chunk", "This is a plain string"]
        },
        appliesTo: "client"
    }
])

structure RpcV2CborListInputOutput {
    stringList: StringList,

    stringSet: StringSet,

    integerList: IntegerList,

    booleanList: BooleanList,

    timestampList: TimestampList,

    enumList: FooEnumList,

    intEnumList: IntegerEnumList,

    nestedStringList: NestedStringList,

    structureList: StructureList

    blobList: BlobList
}

list StructureList {
    member: StructureListMember,
}

structure StructureListMember {
    a: String,
    b: String,
}


@httpRequestTests([
    {
        id: "RpcV2CborSparseMapsSerializeNullValues"
        documentation: "Serializes null values in maps"
        protocol: rpcv2Cbor
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v29zcGFyc2VTdHJpbmdNYXC%2FY2Zvb%2Fb%2F%2Fw%3D%3D
        body: "v29zcGFyc2VTdHJpbmdNYXC/Y2Zvb/b//w=="
        params: {
            "sparseStringMap": {
                "foo": null
            }
        }
        method: "POST"
        uri: "/service/RpcV2Protocol/operation/SparseNullsOperation",
    },
    {
        id: "RpcV2CborSparseListsSerializeNull"
        documentation: "Serializes null values in lists"
        protocol: rpcv2Cbor
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v3BzcGFyc2VTdHJpbmdMaXN0n%2Fb%2F%2Fw%3D%3D
        body: "v3BzcGFyc2VTdHJpbmdMaXN0n/b//w=="
        params: {
            "sparseStringList": [
                null
            ]
        }
        method: "POST"
        uri: "/service/RpcV2Protocol/operation/SparseNullsOperation",
    }
])
@httpResponseTests([
    {
        id: "RpcV2CborSparseMapsDeserializeNullValues"
        documentation: "Deserializes null values in maps"
        protocol: rpcv2Cbor,
        code: 200,
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v29zcGFyc2VTdHJpbmdNYXC%2FY2Zvb%2Fb%2F%2Fw%3D%3D
        body: "v29zcGFyc2VTdHJpbmdNYXC/Y2Zvb/b//w=="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "sparseStringMap": {
                "foo": null
            }
        }
    }
    {
        id: "RpcV2CborSparseListsDeserializeNull"
        documentation: "Deserializes null values in lists"
        protocol: rpcv2Cbor,
        code: 200,
        // http://ec2-54-84-9-83.compute-1.amazonaws.com/hex?value=v3BzcGFyc2VTdHJpbmdMaXN0n%2Fb%2F%2Fw%3D%3D
        body: "v3BzcGFyc2VTdHJpbmdMaXN0n/b//w=="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            "sparseStringList": [
                null
            ]
        }
    }
])
operation SparseNullsOperation {
    input: SparseNullsOperationInputOutput
    output: SparseNullsOperationInputOutput
}

structure SparseNullsOperationInputOutput {
    sparseStringList: SparseStringList
    sparseStringMap: SparseStringMap
}
