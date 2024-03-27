$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use aws.protocoltests.shared#SparseStringList
use aws.protocoltests.shared#SparseStringMap
use aws.protocoltests.shared#StringMap
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "AwsJson11StructuresDontSerializeNullValues",
        documentation: "Null structure values are dropped",
        protocol: awsJson1_1,
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.NullOperation",
        },
        params: {
            string: null
        },
        method: "POST",
        uri: "/",
        appliesTo: "client",
    },
    {
        id: "AwsJson11ServersDontDeserializeNullStructureValues",
        documentation: "Null structure values are dropped",
        protocol: awsJson1_1,
        body: """
            {
                "string": null
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.NullOperation",
        },
        params: {},
        method: "POST",
        uri: "/",
        appliesTo: "server",
    }
])
@httpResponseTests([
    {
        id: "AwsJson11StructuresDontDeserializeNullValues",
        documentation: "Null structure values are dropped",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "string": null
            }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {},
        appliesTo: "client",
    },
    {
        id: "AwsJson11ServersDontSerializeNullStructureValues",
        documentation: "Null structure values are dropped",
        protocol: awsJson1_1,
        code: 200,
        body: "{}",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/x-amz-json-1.1"},
        params: {
            string: null
        },
        appliesTo: "server",
    }
])
operation NullOperation {
    input: NullOperationInputOutput
    output: NullOperationInputOutput
}

structure NullOperationInputOutput {
    string: String
}

@httpRequestTests([
    {
        id: "AwsJson11SparseMapsSerializeNullValues"
        documentation: "Serializes null values in maps"
        protocol: awsJson1_1
        body: """
            {
                "sparseStringMap": {
                    "foo": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
            "X-Amz-Target": "JsonProtocol.SparseNullsOperation"
        }
        params: {
            "sparseStringMap": {
                "foo": null
            }
        }
        method: "POST"
        uri: "/"
    },
    {
        id: "AwsJson11SparseListsSerializeNull"
        documentation: "Serializes null values in lists"
        protocol: awsJson1_1
        body: """
            {
                "sparseStringList": [
                    null
                ]
            }"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
            "X-Amz-Target": "JsonProtocol.SparseNullsOperation"
        }
        params: {
            "sparseStringList": [
                null
            ]
        }
        method: "POST"
        uri: "/"
    }
])
@httpResponseTests([
    {
        id: "AwsJson11SparseMapsDeserializeNullValues"
        documentation: "Deserializes null values in maps"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "sparseStringMap": {
                    "foo": null
                }
            }"""
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.1"}
        params: {
            "sparseStringMap": {
                "foo": null
            }
        }
    }
    {
        id: "AwsJson11SparseListsDeserializeNull"
        documentation: "Deserializes null values in lists"
        protocol: awsJson1_1
        code: 200
        body: """
            {
                "sparseStringList": [
                    null
                ]
            }"""
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/x-amz-json-1.1"}
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
