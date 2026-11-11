$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply smithy.protocoltests.corpus#DocumentMembers @httpRequestTests([
    {
        id: "AwsJson11DocumentMembersJsonObject"
        documentation: "Serializes document as a JSON object"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "documentValue": {"key": "value"}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            documentValue: { key: "value" }
        }
    }
    {
        id: "AwsJson11DocumentMembersString"
        documentation: "Serializes document as a string"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "documentValue": "hello"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentMembers" }
        requireHeaders: ["Content-Length"]
        params: { documentValue: "hello" }
    }
    {
        id: "AwsJson11DocumentMembersNumber"
        documentation: "Serializes document as a number"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "documentValue": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentMembers" }
        requireHeaders: ["Content-Length"]
        params: { documentValue: 42 }
    }
    {
        id: "AwsJson11DocumentMembersBoolean"
        documentation: "Serializes document as a boolean"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "documentValue": true
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentMembers" }
        requireHeaders: ["Content-Length"]
        params: { documentValue: true }
    }
    {
        id: "AwsJson11DocumentMembersArray"
        documentation: "Serializes document as an array"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "documentValue": [1, 2, 3]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            documentValue: [1, 2, 3]
        }
    }
    {
        id: "AwsJson11DocumentMembersNestedStruct"
        documentation: "Serializes nested struct containing a document"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "nestedStruct": {
                    "documentMember": {"nested": true},
                    "stringMember": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentMembers" }
        requireHeaders: ["Content-Length"]
        params: {
            nestedStruct: {
                documentMember: { nested: true }
                stringMember: "hello"
            }
        }
    }
])

apply smithy.protocoltests.corpus#DocumentMembers @httpResponseTests([
    {
        id: "AwsJson11DocumentMembersDeserializeJsonObject"
        documentation: "Deserializes document as a JSON object"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "documentValue": {"key": "value"}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            documentValue: { key: "value" }
        }
    }
    {
        id: "AwsJson11DocumentMembersDeserializeString"
        documentation: "Deserializes document as a string"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "documentValue": "hello"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: { documentValue: "hello" }
    }
    {
        id: "AwsJson11DocumentMembersDeserializeNumber"
        documentation: "Deserializes document as a number"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "documentValue": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: { documentValue: 42 }
    }
    {
        id: "AwsJson11DocumentMembersDeserializeBoolean"
        documentation: "Deserializes document as a boolean"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "documentValue": true
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: { documentValue: true }
    }
    {
        id: "AwsJson11DocumentMembersDeserializeArray"
        documentation: "Deserializes document as an array"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "documentValue": [1, 2, 3]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            documentValue: [1, 2, 3]
        }
    }
    {
        id: "AwsJson11DocumentMembersDeserializeNestedStruct"
        documentation: "Deserializes nested struct containing a document"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "nestedStruct": {
                    "documentMember": {"nested": true},
                    "stringMember": "hello"
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            nestedStruct: {
                documentMember: { nested: true }
                stringMember: "hello"
            }
        }
    }
    {
        id: "AwsJson11DocumentMembersDeserializeNull"
        documentation: "Deserializes document as null"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "documentValue": null
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: { documentValue: null }
    }
])

apply smithy.protocoltests.corpus#ListOfDocuments @httpRequestTests([
    {
        id: "AwsJson11ListOfDocumentsMixedTypes"
        documentation: "Serializes a list of mixed document types"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "values": [42, "hello", true, [1, 2], {"key": "value"}]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.ListOfDocuments" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#ListOfDocuments @httpResponseTests([
    {
        id: "AwsJson11ListOfDocumentsDeserialize"
        documentation: "Deserializes a list of mixed document types"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "values": [42, "hello", true, [1, 2], {"key": "value"}]
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
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

apply smithy.protocoltests.corpus#MapOfDocuments @httpRequestTests([
    {
        id: "AwsJson11MapOfDocumentsMixedTypes"
        documentation: "Serializes a map of mixed document types"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
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
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.MapOfDocuments" }
        requireHeaders: ["Content-Length"]
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

apply smithy.protocoltests.corpus#MapOfDocuments @httpResponseTests([
    {
        id: "AwsJson11MapOfDocumentsDeserialize"
        documentation: "Deserializes a map of mixed document types"
        protocol: awsJson1_1
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
        headers: { "Content-Type": "application/x-amz-json-1.1" }
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

apply smithy.protocoltests.corpus#DocumentUnion @httpRequestTests([
    {
        id: "AwsJson11DocumentUnionDocumentValue"
        documentation: "Serializes union with document value variant"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "value": {
                    "documentValue": {"nested": "object", "count": 5}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentUnion" }
        requireHeaders: ["Content-Length"]
        params: {
            value: {
                documentValue: { nested: "object", count: 5 }
            }
        }
    }
])

apply smithy.protocoltests.corpus#DocumentUnion @httpResponseTests([
    {
        id: "AwsJson11DocumentUnionDeserializeDocumentValue"
        documentation: "Deserializes union with document value variant"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "value": {
                    "documentValue": {"nested": "object", "count": 5}
                }
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        params: {
            value: {
                documentValue: { nested: "object", count: 5 }
            }
        }
    }
])

apply smithy.protocoltests.corpus#DocumentDefaults @httpRequestTests([
    {
        id: "AwsJson11DocumentDefaultsOmitsDefaults"
        tags: ["top-level-default-omission"]
        documentation: """
            Client does not synthesize top-level input defaults, so nothing is serialized"""
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentDefaults" }
        requireHeaders: ["Content-Length"]
        appliesTo: "client"
        params: {}
    }
    {
        id: "AwsJson11DocumentDefaultsSerializeNonDefaults"
        tags: ["explicit-over-default"]
        documentation: "Serializes document members when explicitly set to non-default values"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultDocumentString": "custom",
                "defaultDocumentBoolean": false,
                "defaultDocumentNumber": 8,
                "defaultDocumentList": ["x", "y"],
                "defaultDocumentMap": {"key": "value"},
                "defaultDocumentNull": "set"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentDefaults" }
        requireHeaders: ["Content-Length"]
        params: {
            defaultDocumentString: "custom"
            defaultDocumentBoolean: false
            defaultDocumentNumber: 8
            defaultDocumentList: ["x", "y"]
            defaultDocumentMap: { key: "value" }
            defaultDocumentNull: "set"
        }
    }
    {
        id: "AwsJson11DocumentDefaultsSerializeExplicitDefaults"
        tags: ["explicit-over-default"]
        documentation: "Serializes document members explicitly set to their default values"
        protocol: awsJson1_1
        method: "POST"
        uri: "/"
        body: """
            {
                "defaultDocumentString": "default",
                "defaultDocumentBoolean": true,
                "defaultDocumentNumber": 7,
                "defaultDocumentList": [],
                "defaultDocumentMap": {}
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1", "X-Amz-Target": "AwsJson11CorpusTests.DocumentDefaults" }
        requireHeaders: ["Content-Length"]
        params: {
            defaultDocumentString: "default"
            defaultDocumentBoolean: true
            defaultDocumentNumber: 7
            defaultDocumentList: []
            defaultDocumentMap: {}
        }
    }
])

apply smithy.protocoltests.corpus#DocumentDefaults @httpResponseTests([
    {
        id: "AwsJson11DocumentDefaultsPopulatesDefaultsOnDeserialize"
        documentation: """
            Client fills default document values when members are absent in response"""
        protocol: awsJson1_1
        code: 200
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.1" }
        appliesTo: "client"
        params: {
            defaultDocumentString: "default"
            defaultDocumentBoolean: true
            defaultDocumentNumber: 7
            defaultDocumentList: []
            defaultDocumentMap: {}
        }
    }
])
