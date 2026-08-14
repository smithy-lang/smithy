$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply DefaultScalars @httpRequestTests([
    {
        id: "RpcV2JsonDefaultScalarsOmitsDefaults"
        tags: ["top-level-default-omission"]
        documentation: """
            Client does not synthesize top-level input defaults. The members are
            absent from params, so nothing is serialized: an explicitly provided
            value is always sent, even when it equals the default, so this case
            asserts the client does not fill them in on the caller's behalf."""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DefaultScalars"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        appliesTo: "client"
        params: {}
    }
    {
        id: "RpcV2JsonDefaultScalarsSerializeNonDefaults"
        tags: ["explicit-over-default"]
        documentation: "Serializes members when explicitly set to non-default values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DefaultScalars"
        body: """
            {
                "defaultBoolean": true,
                "defaultByte": 5,
                "defaultShort": 10,
                "defaultInteger": 42,
                "defaultLong": 100,
                "defaultFloat": 1.5,
                "defaultDouble": 2.5,
                "defaultString": "custom",
                "defaultBlob": "aGVsbG8=",
                "defaultEnum": "Bar",
                "defaultIntEnum": 2,
                "zeroBoolean": true,
                "zeroByte": 1,
                "zeroShort": 1,
                "zeroInteger": 1,
                "zeroLong": 1,
                "zeroFloat": 1.0,
                "zeroDouble": 1.0,
                "emptyString": "not empty",
                "emptyBlob": "YWJj"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            defaultBoolean: true
            defaultByte: 5
            defaultShort: 10
            defaultInteger: 42
            defaultLong: 100
            defaultFloat: 1.5
            defaultDouble: 2.5
            defaultString: "custom"
            defaultBlob: "hello"
            defaultEnum: "Bar"
            defaultIntEnum: 2
            zeroBoolean: true
            zeroByte: 1
            zeroShort: 1
            zeroInteger: 1
            zeroLong: 1
            zeroFloat: 1.0
            zeroDouble: 1.0
            emptyString: "not empty"
            emptyBlob: "abc"
        }
    }
])

apply DefaultScalars @httpResponseTests([
    {
        id: "RpcV2JsonDefaultScalarsPopulatesDefaultsOnDeserialize"
        documentation: "Client fills default values when members are absent in response"
        protocol: rpcv2Json
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: {
            defaultBoolean: false
            defaultByte: 0
            defaultShort: 0
            defaultInteger: 0
            defaultLong: 0
            defaultFloat: 0
            defaultDouble: 0
            defaultString: ""
            defaultBlob: ""
            defaultEnum: "Foo"
            defaultIntEnum: 1
            zeroBoolean: false
            zeroByte: 0
            zeroShort: 0
            zeroInteger: 0
            zeroLong: 0
            zeroFloat: 0
            zeroDouble: 0
            emptyString: ""
            emptyBlob: ""
        }
    }
])

apply DefaultCollections @httpRequestTests([
    {
        id: "RpcV2JsonDefaultCollectionsOmitsEmptyDefaults"
        tags: ["top-level-default-omission"]
        documentation: """
            Client does not synthesize the top-level empty list/map defaults. The
            members are absent from params rather than explicitly set to [] and
            {}, which would have to be serialized."""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DefaultCollections"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        appliesTo: "client"
        params: {}
    }
    {
        id: "RpcV2JsonDefaultCollectionsSerializeNonEmpty"
        tags: ["explicit-over-default"]
        documentation: "Serializes non-empty list and map"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/DefaultCollections"
        body: """
            {
                "defaultList": ["a", "b"],
                "defaultMap": {"key1": "value1", "key2": "value2"}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            defaultList: ["a", "b"]
            defaultMap: { key1: "value1", key2: "value2" }
        }
    }
])

apply NestedDefaults @httpRequestTests([
    {
        id: "RpcV2JsonNestedDefaultsSerialize"
        documentation: "Serializes nested structs with defaults populated"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NestedDefaults"
        body: """
            {
                "topLevel": {
                    "nested": {
                        "greeting": "hello",
                        "count": 0,
                        "inner": {
                            "farewell": "goodbye"
                        }
                    },
                    "nestedList": [
                        {
                            "greeting": "hi",
                            "count": 5,
                            "inner": {
                                "farewell": "bye"
                            }
                        },
                        {
                            "greeting": "yo",
                            "count": 6,
                            "inner": {
                                "farewell": "later"
                            }
                        }
                    ],
                    "nestedMap": {
                        "entry1": {
                            "greeting": "hey",
                            "count": 10,
                            "inner": {
                                "farewell": "ciao"
                            }
                        },
                        "entry2": {
                            "greeting": "sup",
                            "count": 11,
                            "inner": {
                                "farewell": "adios"
                            }
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            topLevel: {
                nested: {
                    greeting: "hello"
                    count: 0
                    inner: { farewell: "goodbye" }
                }
                nestedList: [
                    {
                        greeting: "hi"
                        count: 5
                        inner: { farewell: "bye" }
                    }
                    {
                        greeting: "yo"
                        count: 6
                        inner: { farewell: "later" }
                    }
                ]
                nestedMap: {
                    entry1: {
                        greeting: "hey"
                        count: 10
                        inner: { farewell: "ciao" }
                    }
                    entry2: {
                        greeting: "sup"
                        count: 11
                        inner: { farewell: "adios" }
                    }
                }
            }
        }
    }
])

apply NestedDefaults @httpResponseTests([
    {
        id: "RpcV2JsonNestedDefaultsDeserializePopulatesDefaults"
        documentation: "Client fills defaults for absent nested fields"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "topLevel": {
                    "nested": {},
                    "nestedList": [{}, {}],
                    "nestedMap": {
                        "entry1": {},
                        "entry2": {}
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: {
            topLevel: {
                nested: { greeting: "hello", count: 0 }
                nestedList: [
                    {
                        greeting: "hello"
                        count: 0
                    }
                    {
                        greeting: "hello"
                        count: 0
                    }
                ]
                nestedMap: {
                    entry1: { greeting: "hello", count: 0 }
                    entry2: { greeting: "hello", count: 0 }
                }
            }
        }
    }
])

apply RequiredMembers @httpRequestTests([
    {
        id: "RpcV2JsonRequiredMembersSerialize"
        documentation: "Serializes all required fields"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RequiredMembers"
        body: """
            {
                "requiredString": "hello",
                "requiredInteger": 42,
                "requiredBoolean": true,
                "requiredList": ["a", "b"],
                "requiredMap": {"key1": "value1", "key2": "value2"},
                "requiredStringWithDefault": "custom",
                "requiredIntegerWithDefault": 5,
                "requiredBooleanWithDefault": true,
                "requiredListWithDefault": ["c", "d"],
                "requiredMapWithDefault": {"k1": "v1", "k2": "v2"}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            requiredString: "hello"
            requiredInteger: 42
            requiredBoolean: true
            requiredList: ["a", "b"]
            requiredMap: { key1: "value1", key2: "value2" }
            requiredStringWithDefault: "custom"
            requiredIntegerWithDefault: 5
            requiredBooleanWithDefault: true
            requiredListWithDefault: ["c", "d"]
            requiredMapWithDefault: { k1: "v1", k2: "v2" }
        }
    }
])

apply RequiredMembers @httpResponseTests([
    {
        id: "RpcV2JsonRequiredMembersDeserializeZeroValues"
        tags: ["error-correction"]
        documentation: "Client fills zero-values for required fields without defaults when server omits them"
        protocol: rpcv2Json
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: {
            requiredString: ""
            requiredInteger: 0
            requiredBoolean: false
            requiredList: []
            requiredMap: {}
            requiredStringWithDefault: "default"
            requiredIntegerWithDefault: 0
            requiredBooleanWithDefault: false
            requiredListWithDefault: []
            requiredMapWithDefault: {}
        }
    }
    {
        id: "RpcV2JsonRequiredMembersDeserializeDefaults"
        tags: ["error-correction"]
        documentation: "Client fills defaults for required fields with @default when server omits them"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "requiredString": "provided",
                "requiredInteger": 1,
                "requiredBoolean": true,
                "requiredList": ["x", "y"],
                "requiredMap": {"a": "b", "c": "d"}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        appliesTo: "client"
        params: {
            requiredString: "provided"
            requiredInteger: 1
            requiredBoolean: true
            requiredList: ["x", "y"]
            requiredMap: { a: "b", c: "d" }
            requiredStringWithDefault: "default"
            requiredIntegerWithDefault: 0
            requiredBooleanWithDefault: false
            requiredListWithDefault: []
            requiredMapWithDefault: {}
        }
    }
])

apply NullSparseMembers @httpRequestTests([
    {
        id: "RpcV2JsonNullSparseMembersSerialize"
        documentation: "Null values in sparse collections are serialized as JSON null"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/NullSparseMembers"
        body: """
            {
                "sparseStringList": [null, "hello", null, "world", null],
                "sparseStringMap": {"key1": null, "key2": "value", "key3": "value2"},
                "sparseStructList": [null, {"stringMember": "a", "integerMember": 1, "booleanMember": true}, null, {"stringMember": "b", "integerMember": 2, "booleanMember": false}],
                "sparseStructMap": {"key1": null, "key2": {"stringMember": "a", "integerMember": 1, "booleanMember": true}, "key3": {"stringMember": "b", "integerMember": 2, "booleanMember": false}}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            sparseStringList: [null, "hello", null, "world", null]
            sparseStringMap: { key1: null, key2: "value", key3: "value2" }
            sparseStructList: [
                null
                {
                    stringMember: "a"
                    integerMember: 1
                    booleanMember: true
                }
                null
                {
                    stringMember: "b"
                    integerMember: 2
                    booleanMember: false
                }
            ]
            sparseStructMap: {
                key1: null
                key2: { stringMember: "a", integerMember: 1, booleanMember: true }
                key3: { stringMember: "b", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply NullSparseMembers @httpResponseTests([
    {
        id: "RpcV2JsonNullSparseMembersDeserialize"
        documentation: "Null values in sparse collections are preserved on deserialization"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "sparseStringList": [null, "hello", null, "world", null],
                "sparseStringMap": {"key1": null, "key2": "value", "key3": "value2"},
                "sparseStructList": [null, {"stringMember": "a", "integerMember": 1, "booleanMember": true}, null, {"stringMember": "b", "integerMember": 2, "booleanMember": false}],
                "sparseStructMap": {"key1": null, "key2": {"stringMember": "a", "integerMember": 1, "booleanMember": true}, "key3": {"stringMember": "b", "integerMember": 2, "booleanMember": false}}
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: {
            sparseStringList: [null, "hello", null, "world", null]
            sparseStringMap: { key1: null, key2: "value", key3: "value2" }
            sparseStructList: [
                null
                {
                    stringMember: "a"
                    integerMember: 1
                    booleanMember: true
                }
                null
                {
                    stringMember: "b"
                    integerMember: 2
                    booleanMember: false
                }
            ]
            sparseStructMap: {
                key1: null
                key2: { stringMember: "a", integerMember: 1, booleanMember: true }
                key3: { stringMember: "b", integerMember: 2, booleanMember: false }
            }
        }
    }
])

apply ClientOptionalDefaults @httpRequestTests([
    {
        id: "RpcV2JsonClientOptionalDefaultsNotPopulated"
        tags: ["client-optional-default"]
        documentation: "Client does not populate defaults for @clientOptional members"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/ClientOptionalDefaults"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        appliesTo: "client"
        params: {}
    }
])
