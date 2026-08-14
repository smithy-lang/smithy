$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply DocumentMembers @httpRequestTests([
    {
        id: "RpcV2JsonDocumentMembersJsonObject"
        documentation: "Serializes document as a JSON object"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "documentValue": {"key": "value"}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            documentValue: { key: "value" }
        }
    }
    {
        id: "RpcV2JsonDocumentMembersString"
        documentation: "Serializes document as a string"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "documentValue": "hello"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { documentValue: "hello" }
    }
    {
        id: "RpcV2JsonDocumentMembersNumber"
        documentation: "Serializes document as a number"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "documentValue": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { documentValue: 42 }
    }
    {
        id: "RpcV2JsonDocumentMembersBoolean"
        documentation: "Serializes document as a boolean"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "documentValue": true
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { documentValue: true }
    }
    {
        id: "RpcV2JsonDocumentMembersArray"
        documentation: "Serializes document as an array"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "documentValue": [1, 2, 3]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            documentValue: [1, 2, 3]
        }
    }
    {
        id: "RpcV2JsonDocumentMembersNull"
        documentation: "Serializes document as null"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "documentValue": null
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { documentValue: null }
    }
    {
        id: "RpcV2JsonDocumentMembersNestedStruct"
        documentation: "Serializes nested struct containing a document"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentMembers"
        body: """
            {
                "nestedStruct": {
                    "documentMember": {"nested": true},
                    "stringMember": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            nestedStruct: {
                documentMember: { nested: true }
                stringMember: "hello"
            }
        }
    }
])

apply DocumentMembers @httpResponseTests([
    {
        id: "RpcV2JsonDocumentMembersDeserializeJsonObject"
        documentation: "Deserializes document as a JSON object"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "documentValue": {"key": "value"}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            documentValue: { key: "value" }
        }
    }
    {
        id: "RpcV2JsonDocumentMembersDeserializeString"
        documentation: "Deserializes document as a string"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "documentValue": "hello"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { documentValue: "hello" }
    }
    {
        id: "RpcV2JsonDocumentMembersDeserializeNumber"
        documentation: "Deserializes document as a number"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "documentValue": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { documentValue: 42 }
    }
    {
        id: "RpcV2JsonDocumentMembersDeserializeBoolean"
        documentation: "Deserializes document as a boolean"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "documentValue": true
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { documentValue: true }
    }
    {
        id: "RpcV2JsonDocumentMembersDeserializeArray"
        documentation: "Deserializes document as an array"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "documentValue": [1, 2, 3]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            documentValue: [1, 2, 3]
        }
    }
    {
        id: "RpcV2JsonDocumentMembersDeserializeNestedStruct"
        documentation: "Deserializes nested struct containing a document"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "nestedStruct": {
                    "documentMember": {"nested": true},
                    "stringMember": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            nestedStruct: {
                documentMember: { nested: true }
                stringMember: "hello"
            }
        }
    }
    {
        id: "RpcV2JsonDocumentMembersDeserializeNull"
        documentation: "Deserializes document as null"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "documentValue": null
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { documentValue: null }
    }
])

apply ListOfDocuments @httpRequestTests([
    {
        id: "RpcV2JsonListOfDocumentsMixedTypes"
        documentation: "Serializes a list of mixed document types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ListOfDocuments"
        body: """
            {
                "values": [42, "hello", true, [1, 2], {"key": "value"}]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: [
                42
                "hello"
                true
                [1, 2]
                {
                    key: "value"
                }
            ]
        }
    }
])

apply ListOfDocuments @httpResponseTests([
    {
        id: "RpcV2JsonListOfDocumentsDeserialize"
        documentation: "Deserializes a list of mixed document types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": [42, "hello", true, [1, 2], {"key": "value"}]
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: [
                42
                "hello"
                true
                [1, 2]
                {
                    key: "value"
                }
            ]
        }
    }
])

apply MapOfDocuments @httpRequestTests([
    {
        id: "RpcV2JsonMapOfDocumentsMixedTypes"
        documentation: "Serializes a map of mixed document types"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MapOfDocuments"
        body: """
            {
                "values": {
                    "num": 42,
                    "str": "hello",
                    "bool": true,
                    "list": [1, 2],
                    "obj": {"key": "value"}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            values: {
                num: 42
                str: "hello"
                bool: true
                list: [1, 2]
                obj: { key: "value" }
            }
        }
    }
])

apply MapOfDocuments @httpResponseTests([
    {
        id: "RpcV2JsonMapOfDocumentsDeserialize"
        documentation: "Deserializes a map of mixed document types"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "values": {
                    "num": 42,
                    "str": "hello",
                    "bool": true,
                    "list": [1, 2],
                    "obj": {"key": "value"}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            values: {
                num: 42
                str: "hello"
                bool: true
                list: [1, 2]
                obj: { key: "value" }
            }
        }
    }
])

apply DocumentUnion @httpRequestTests([
    {
        id: "RpcV2JsonDocumentUnionDocumentValue"
        documentation: "Serializes union with document value variant"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DocumentUnion"
        body: """
            {
                "value": {
                    "documentValue": {"nested": "object", "count": 5}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            value: {
                documentValue: { nested: "object", count: 5 }
            }
        }
    }
])

apply DocumentUnion @httpResponseTests([
    {
        id: "RpcV2JsonDocumentUnionDeserializeDocumentValue"
        documentation: "Deserializes union with document value variant"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "value": {
                    "documentValue": {"nested": "object", "count": 5}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            value: {
                documentValue: { nested: "object", count: 5 }
            }
        }
    }
])
