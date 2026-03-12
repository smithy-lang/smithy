$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "RpcV2JsonRequestSimpleScalarProperties"
        protocol: rpcv2Json
        documentation: "Serializes simple scalar properties"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "byteValue": 5,
                "doubleValue": 1.889,
                "falseBooleanValue": false,
                "floatValue": 7.625,
                "integerValue": 256,
                "longValue": 9873,
                "shortValue": 9898,
                "stringValue": "simple",
                "trueBooleanValue": true,
                "blobValue": "Zm9v"
            }"""
        params: {
            byteValue: 5
            doubleValue: 1.889
            falseBooleanValue: false
            floatValue: 7.625
            integerValue: 256
            longValue: 9873
            shortValue: 9898
            stringValue: "simple"
            trueBooleanValue: true
            blobValue: "foo"
        }
    }
    {
        id: "RpcV2JsonRequestClientDoesntSerializeNullStructureValues"
        documentation: "RpcV2 JSON should not serialize null structure values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: "{}"
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
            stringValue: null
        }
        appliesTo: "client"
    }
    {
        id: "RpcV2JsonRequestServerDoesntDeSerializeNullStructureValues"
        documentation: "RpcV2 JSON should not deserialize null structure values"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "stringValue": null
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
        params: {}
        appliesTo: "server"
    }
    {
        id: "RpcV2JsonRequestSupportsNaNFloatInputs"
        protocol: rpcv2Json
        documentation: "Supports handling NaN float values."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "doubleValue": "NaN",
                "floatValue": "NaN"
            }"""
        params: {
            doubleValue: "NaN"
            floatValue: "NaN"
        }
    }
    {
        id: "RpcV2JsonRequestSupportsInfinityFloatInputs"
        protocol: rpcv2Json
        documentation: "Supports handling Infinity float values."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "doubleValue": "Infinity",
                "floatValue": "Infinity"
            }"""
        params: {
            doubleValue: "Infinity"
            floatValue: "Infinity"
        }
    }
    {
        id: "RpcV2JsonRequestSupportsNegativeInfinityFloatInputs"
        protocol: rpcv2Json
        documentation: "Supports handling -Infinity float values."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "doubleValue": "-Infinity",
                "floatValue": "-Infinity"
            }"""
        params: {
            doubleValue: "-Infinity"
            floatValue: "-Infinity"
        }
    }
    {
        id: "RpcV2JsonRequestExtraFieldsInTheBodyShouldBeSkippedByServers"
        protocol: rpcv2Json
        documentation: """
            The server should skip over additional fields that are not part of the structure. This allows a
            client generated against a newer Smithy model to be able to communicate with a server that is
            generated against an older Smithy model."""
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "byteValue": 5,
                "doubleValue": 1.889,
                "falseBooleanValue": false,
                "floatValue": 7.625,
                "extraObject": {
                    "normalString": "foo",
                    "withAnArray": [1, 2, 3]
                },
                "integerValue": 256,
                "longValue": 9873,
                "shortValue": 9898,
                "stringValue": "simple",
                "someOtherField": "this should be skipped",
                "trueBooleanValue": true,
                "blobValue": "Zm9v"
            }"""
        params: {
            byteValue: 5
            doubleValue: 1.889
            falseBooleanValue: false
            floatValue: 7.625
            integerValue: 256
            longValue: 9873
            shortValue: 9898
            stringValue: "simple"
            trueBooleanValue: true
            blobValue: "foo"
        }
        appliesTo: "server"
    }
    {
        id: "RpcV2JsonRequestServersShouldHandleNoAcceptHeader"
        protocol: rpcv2Json
        documentation: "Servers should tolerate requests without an Accept header set."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/SimpleScalarProperties"
        body: """
            {
                "byteValue": 5,
                "doubleValue": 1.889,
                "falseBooleanValue": false,
                "floatValue": 7.625,
                "integerValue": 256,
                "longValue": 9873,
                "shortValue": 9898,
                "stringValue": "simple",
                "trueBooleanValue": true,
                "blobValue": "Zm9v"
            }"""
        params: {
            byteValue: 5
            doubleValue: 1.889
            falseBooleanValue: false
            floatValue: 7.625
            integerValue: 256
            longValue: 9873
            shortValue: 9898
            stringValue: "simple"
            trueBooleanValue: true
            blobValue: "foo"
        }
        appliesTo: "server"
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseSimpleScalarProperties"
        protocol: rpcv2Json
        documentation: "Serializes simple scalar properties"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "trueBooleanValue": true,
                "falseBooleanValue": false,
                "byteValue": 5,
                "doubleValue": 1.889,
                "floatValue": 7.625,
                "integerValue": 256,
                "shortValue": 9898,
                "stringValue": "simple",
                "blobValue": "Zm9v"
            }"""
        code: 200
        params: {
            trueBooleanValue: true
            falseBooleanValue: false
            byteValue: 5
            doubleValue: 1.889
            floatValue: 7.625
            integerValue: 256
            shortValue: 9898
            stringValue: "simple"
            blobValue: "foo"
        }
    }
    {
        id: "RpcV2JsonResponseClientDoesntDeserializeNullStructureValues"
        documentation: "RpcV2 JSON should not deserialize null structure values"
        protocol: rpcv2Json
        body: """
            {
                "stringValue": null
            }"""
        code: 200
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {}
        appliesTo: "client"
    }
    {
        id: "RpcV2JsonResponseServerDoesntSerializeNullStructureValues"
        documentation: "RpcV2 JSON should not serialize null structure values"
        protocol: rpcv2Json
        body: "{}"
        code: 200
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            stringValue: null
        }
        appliesTo: "server"
    }
    {
        id: "RpcV2JsonResponseSupportsNaNFloatOutputs"
        protocol: rpcv2Json
        documentation: "Supports handling NaN float values."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        bodyMediaType: "application/json"
        body: """
            {
                "doubleValue": "NaN",
                "floatValue": "NaN"
            }"""
        params: {
            doubleValue: "NaN"
            floatValue: "NaN"
        }
    }
    {
        id: "RpcV2JsonResponseSupportsInfinityFloatOutputs"
        protocol: rpcv2Json
        documentation: "Supports handling Infinity float values."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        bodyMediaType: "application/json"
        body: """
            {
                "doubleValue": "Infinity",
                "floatValue": "Infinity"
            }"""
        params: {
            doubleValue: "Infinity"
            floatValue: "Infinity"
        }
    }
    {
        id: "RpcV2JsonResponseSupportsNegativeInfinityFloatOutputs"
        protocol: rpcv2Json
        documentation: "Supports handling Negative Infinity float values."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        bodyMediaType: "application/json"
        body: """
            {
                "doubleValue": "-Infinity",
                "floatValue": "-Infinity"
            }"""
        params: {
            doubleValue: "-Infinity"
            floatValue: "-Infinity"
        }
    }
    {
        id: "RpcV2JsonResponseExtraFieldsInTheBodyShouldBeSkippedByClients"
        protocol: rpcv2Json
        documentation: """
            The client should skip over additional fields that are not part of the structure. This allows a
            client generated against an older Smithy model to be able to communicate with a server that is
            generated against a newer Smithy model."""
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        bodyMediaType: "application/json"
        body: """
            {
                "byteValue": 5,
                "doubleValue": 1.889,
                "falseBooleanValue": false,
                "floatValue": 7.625,
                "extraObject": {
                    "normalString": "foo",
                    "withAnArray": [1, 2, 3]
                },
                "integerValue": 256,
                "longValue": 9873,
                "shortValue": 9898,
                "stringValue": "simple",
                "someOtherField": "this should be skipped",
                "trueBooleanValue": true,
                "blobValue": "Zm9v"
            }"""
        params: {
            byteValue: 5
            doubleValue: 1.889
            falseBooleanValue: false
            floatValue: 7.625
            integerValue: 256
            longValue: 9873
            shortValue: 9898
            stringValue: "simple"
            trueBooleanValue: true
            blobValue: "foo"
        }
        appliesTo: "client"
    }
])
operation SimpleScalarProperties {
    input: SimpleScalarStructure
    output: SimpleScalarStructure
}

apply RecursiveShapes @httpRequestTests([
    {
        id: "RpcV2JsonRequestRecursiveShapes"
        documentation: "Serializes recursive structures"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/RecursiveShapes"
        body: """
            {
                "nested": {
                    "foo": "Foo1",
                    "nested": {
                        "bar": "Bar1",
                        "recursiveMember": {
                            "foo": "Foo2",
                            "nested": {
                                "bar": "Bar2"
                            }
                        }
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
            nested: {
                foo: "Foo1"
                nested: {
                    bar: "Bar1"
                    recursiveMember: {
                        foo: "Foo2"
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

apply RecursiveShapes @httpResponseTests([
    {
        id: "RpcV2JsonResponseRecursiveShapes"
        documentation: "Serializes recursive structures"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "nested": {
                    "foo": "Foo1",
                    "nested": {
                        "bar": "Bar1",
                        "recursiveMember": {
                            "foo": "Foo2",
                            "nested": {
                                "bar": "Bar2"
                            }
                        }
                    }
                }
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            nested: {
                foo: "Foo1"
                nested: {
                    bar: "Bar1"
                    recursiveMember: {
                        foo: "Foo2"
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

operation RecursiveShapes {
    input: RecursiveShapesInputOutput
    output: RecursiveShapesInputOutput
}

structure RecursiveShapesInputOutput {
    nested: RecursiveShapesInputOutputNested1
}

structure RecursiveShapesInputOutputNested1 {
    foo: String
    nested: RecursiveShapesInputOutputNested2
}

structure RecursiveShapesInputOutputNested2 {
    bar: String
    recursiveMember: RecursiveShapesInputOutputNested1
}

structure SimpleScalarStructure {
    trueBooleanValue: Boolean
    falseBooleanValue: Boolean
    byteValue: Byte
    doubleValue: Double
    floatValue: Float
    integerValue: Integer
    longValue: Long
    shortValue: Short
    stringValue: String
    blobValue: Blob
}
