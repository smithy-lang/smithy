// This file defines test cases that serialize lists in JSON documents.

$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocoltests.shared#BooleanList
use smithy.protocoltests.shared#BlobList
use smithy.protocoltests.shared#FooEnumList
use smithy.protocoltests.shared#IntegerEnumList
use smithy.protocoltests.shared#IntegerList
use smithy.protocoltests.shared#NestedStringList
use smithy.protocoltests.shared#SparseStringList
use smithy.protocoltests.shared#SparseStringMap
use smithy.protocoltests.shared#StringList
use smithy.protocoltests.shared#StringSet
use smithy.protocoltests.shared#TimestampList
use smithy.protocols#rpcv2Json
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
operation RpcV2JsonLists {
    input: RpcV2JsonListInputOutput
    output: RpcV2JsonListInputOutput
    errors: [ValidationException]
}

apply RpcV2JsonLists @httpRequestTests([
    {
        id: "RpcV2JsonRequestLists"
        documentation: "Serializes RpcV2 JSON lists"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonLists"
        body: """
            {
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
                    "Zm9v",
                    "YmFy"
                ]
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
            "stringList": [
                "foo"
                "bar"
            ]
            "stringSet": [
                "foo"
                "bar"
            ]
            "integerList": [
                1
                2
            ]
            "booleanList": [
                true
                false
            ]
            "timestampList": [
                1398796238
                1398796238
            ]
            "enumList": [
                "Foo"
                "0"
            ]
            "intEnumList": [
                1
                2
            ]
            "nestedStringList": [
                [
                    "foo"
                    "bar"
                ]
                [
                    "baz"
                    "qux"
                ]
            ]
            "structureList": [
                {
                    "a": "1"
                    "b": "2"
                }
                {
                    "a": "3"
                    "b": "4"
                }
            ]
            "blobList": [
                "foo"
                "bar"
            ]
        }
    }
    {
        id: "RpcV2JsonRequestListsEmpty"
        documentation: "Serializes empty JSON lists"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RpcV2JsonLists"
        body: """
            {
                "stringList": []
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
            stringList: []
        }
    }
])

apply RpcV2JsonLists @httpResponseTests([
    {
        id: "RpcV2JsonResponseLists"
        documentation: "Serializes RpcV2 JSON lists"
        protocol: rpcv2Json
        code: 200
        body: """
            {
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
                    "Zm9v",
                    "YmFy"
                ]
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "stringList": [
                "foo"
                "bar"
            ]
            "stringSet": [
                "foo"
                "bar"
            ]
            "integerList": [
                1
                2
            ]
            "booleanList": [
                true
                false
            ]
            "timestampList": [
                1398796238
                1398796238
            ]
            "enumList": [
                "Foo"
                "0"
            ]
            "intEnumList": [
                1
                2
            ]
            "nestedStringList": [
                [
                    "foo"
                    "bar"
                ]
                [
                    "baz"
                    "qux"
                ]
            ]
            "structureList": [
                {
                    "a": "1"
                    "b": "2"
                }
                {
                    "a": "3"
                    "b": "4"
                }
            ]
            "blobList": [
                "foo"
                "bar"
            ]
        }
    }
    {
        id: "RpcV2JsonResponseListsEmpty"
        documentation: "Serializes empty RpcV2 JSON lists"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "stringList": []
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            stringList: []
        }
    }
])

structure RpcV2JsonListInputOutput {
    stringList: StringList
    stringSet: StringSet
    integerList: IntegerList
    booleanList: BooleanList
    timestampList: TimestampList
    enumList: FooEnumList
    intEnumList: IntegerEnumList
    nestedStringList: NestedStringList
    structureList: StructureList
    blobList: BlobList
}

list StructureList {
    member: StructureListMember
}

structure StructureListMember {
    a: String
    b: String
}


@httpRequestTests([
    {
        id: "RpcV2JsonRequestSparseMapsSerializeNullValues"
        documentation: "Serializes null values in maps"
        protocol: rpcv2Json
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        body: """
            {
                "sparseStringMap": {
                    "foo": null
                }
            }"""
        params: {
            "sparseStringMap": {
                "foo": null
            }
        }
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/SparseNullsOperation"
    }
    {
        id: "RpcV2JsonRequestSparseListsSerializeNull"
        documentation: "Serializes null values in lists"
        protocol: rpcv2Json
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        body: """
            {
                "sparseStringList": [
                    null
                ]
            }"""
        params: {
            "sparseStringList": [
                null
            ]
        }
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/SparseNullsOperation"
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseSparseMapsDeserializeNullValues"
        documentation: "Deserializes null values in maps"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "sparseStringMap": {
                    "foo": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            "sparseStringMap": {
                "foo": null
            }
        }
    }
    {
        id: "RpcV2JsonResponseSparseListsDeserializeNull"
        documentation: "Deserializes null values in lists"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "sparseStringList": [
                    null
                ]
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
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
