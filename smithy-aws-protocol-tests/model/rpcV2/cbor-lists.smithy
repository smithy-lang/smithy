// This file defines test cases that serialize lists in JSON documents.

$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

use aws.protocoltests.shared#BooleanList
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#IntegerEnumList
use aws.protocoltests.shared#IntegerList
use aws.protocoltests.shared#NestedStringList
use aws.protocoltests.shared#SparseStringList
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
        body: "v2tib29sZWFuTGlzdJ/19P9oZW51bUxpc3SfY0Zvb2Ew/2tpbnRFbnVtTGlzdJ8BAv9raW50ZWdlckxpc3SfAQL/cG5lc3RlZFN0cmluZ0xpc3Sfn2Nmb29jYmFy/59jYmF6Y3F1eP//anN0cmluZ0xpc3SfY2Zvb2NiYXL/aXN0cmluZ1NldJ9jZm9vY2Jhcv9tc3RydWN0dXJlTGlzdJ+/YWFhMWFiYTL/v2FhYTNhYmE0//9tdGltZXN0YW1wTGlzdJ/B+0HU1/vzgAAAwftB1Nf784AAAP//",
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
            ]
        }
    },
    {
        id: "RpcV2CborListsEmpty",
        documentation: "Serializes empty JSON lists",
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
        id: "RpcV2CborListsSerializeNull",
        documentation: "Serializes null values in lists",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        body: "v3BzcGFyc2VTdHJpbmdMaXN0n/ZiaGn//w==",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            sparseStringList: [null, "hi"]
        }
    },
    {
        id: "RpcV2CborSparseListWithIndefiniteString",
        documentation: "Serializes indefinite length text strings inside an indefinite length list",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        body: "v3BzcGFyc2VTdHJpbmdMaXN0n394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h/394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r/3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n//8="
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            sparseStringList: ["An example indefinite string, which will be chunked, on each comma", "Another example indefinite string with only one chunk", "This is a plain string"]
        }
    },
    {
        id: "RpcV2CborListWithIndefiniteString",
        documentation: "Serializes indefinite length text strings inside a definite length list",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RpcV2CborLists",
        body: "oWpzdHJpbmdMaXN0g394HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcsdyB3aGljaCB3aWxsIGJlIGNodW5rZWQsbiBvbiBlYWNoIGNvbW1h/394NUFub3RoZXIgZXhhbXBsZSBpbmRlZmluaXRlIHN0cmluZyB3aXRoIG9ubHkgb25lIGNodW5r/3ZUaGlzIGlzIGEgcGxhaW4gc3RyaW5n"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            stringList: ["An example indefinite string, which will be chunked, on each comma", "Another example indefinite string with only one chunk", "This is a plain string"]
        }
    }
])

apply RpcV2CborLists @httpResponseTests([
    {
        id: "RpcV2CborLists",
        documentation: "Serializes RpcV2 Cbor lists",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2tib29sZWFuTGlzdJ/19P9oZW51bUxpc3SfY0Zvb2Ew/2tpbnRFbnVtTGlzdJ8BAv9raW50ZWdlckxpc3SfAQL/cG5lc3RlZFN0cmluZ0xpc3Sfn2Nmb29jYmFy/59jYmF6Y3F1eP//anN0cmluZ0xpc3SfY2Zvb2NiYXL/aXN0cmluZ1NldJ9jZm9vY2Jhcv9tc3RydWN0dXJlTGlzdJ+/YWFhMWFiYTL/v2FhYTNhYmE0//9tdGltZXN0YW1wTGlzdJ/B+0HU1/vzgAAAwftB1Nf784AAAP//",
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
        id: "RpcV2CborListsSerializeNull",
        documentation: "Serializes null values in sparse lists",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v3BzcGFyc2VTdHJpbmdMaXN0n/ZiaGn//w==",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            sparseStringList: [null, "hi"]
        }
    }
])

structure RpcV2CborListInputOutput {
    stringList: StringList,

    sparseStringList: SparseStringList,

    stringSet: StringSet,

    integerList: IntegerList,

    booleanList: BooleanList,

    timestampList: TimestampList,

    enumList: FooEnumList,

    intEnumList: IntegerEnumList,

    nestedStringList: NestedStringList,

    structureList: StructureList
}

list StructureList {
    member: StructureListMember,
}

structure StructureListMember {
    a: String,
    b: String,
}
