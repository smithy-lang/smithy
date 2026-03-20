$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests


@httpRequestTests([
    {
        id: "RpcV2JsonRequestNoInput"
        protocol: rpcv2Json
        documentation: "Body is empty and no Content-Type header if no input"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Accept": "application/json"
        }
        forbidHeaders: [
            "Content-Type"
            "X-Amz-Target"
        ]
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/NoInputOutput"
        body: ""
    }
    {
        id: "RpcV2JsonRequestNoInputServerAllowsEmptyJsonObject"
        protocol: rpcv2Json
        documentation: "Servers should accept a JSON empty object if no input."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Accept": "application/json"
            "Content-Type": "application/json"
        }
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/NoInputOutput"
        body: "{}"
        appliesTo: "server"
    }
    {
        id: "RpcV2JsonRequestNoInputServerAllowsEmptyBody"
        protocol: rpcv2Json
        documentation: """
            Servers should accept an empty body if there is no input. Additionally
            they should not raise an error if the `Accept` header is set."""
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Accept": "application/json"
            "Content-Type": "application/json"
        }
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/NoInputOutput"
        body: ""
        appliesTo: "server"
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseNoOutput"
        protocol: rpcv2Json
        documentation: "A `Content-Type` header should not be set if the response body is empty."
        body: ""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
        }
        forbidHeaders: [
            "Content-Type"
        ]
        code: 200
    }
    {
        id: "RpcV2JsonResponseNoOutputClientAllowsEmptyJsonObject"
        protocol: rpcv2Json
        documentation: "Clients should accept a JSON empty object if there is no output."
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        bodyMediaType: "application/json"
        body: "{}"
        appliesTo: "client"
    }
    {
        id: "RpcV2JsonResponseNoOutputClientAllowsEmptyBody"
        protocol: rpcv2Json
        documentation: """
            Clients should accept an empty body if there is no output and
            should not raise an error if the `Content-Type` header is set."""
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        bodyMediaType: "application/json"
        body: ""
        appliesTo: "client"
    }
])
operation NoInputOutput {}


@httpRequestTests([
    {
        id: "RpcV2JsonRequestEmptyInput"
        protocol: rpcv2Json
        documentation: "When Input structure is empty we write JSON equivalent of {}"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        forbidHeaders: [
            "X-Amz-Target"
        ]
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/EmptyInputOutput"
        bodyMediaType: "application/json"
        body: "{}"
    }
    {
        id: "RpcV2JsonRequestEmptyInputNoBody"
        protocol: rpcv2Json
        documentation: "When Input structure is empty the server should accept an empty body"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Accept": "application/json"
            "Content-Type": "application/json"
        }
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/EmptyInputOutput"
        bodyMediaType: "application/json"
        body: ""
        appliesTo: "server"
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseEmptyOutput"
        protocol: rpcv2Json
        documentation: "When output structure is empty we write JSON equivalent of {}"
        body: "{}"
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
    }
    {
        id: "RpcV2JsonResponseEmptyOutputNoBody"
        protocol: rpcv2Json
        documentation: "When output structure is empty the client should accept an empty body"
        body: ""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
        appliesTo: "client"
    }
])
operation EmptyInputOutput {
    input: EmptyStructure
    output: EmptyStructure
}

@httpRequestTests([
    {
        id: "RpcV2JsonRequestOptionalInput"
        protocol: rpcv2Json
        documentation: "When input is empty we write JSON equivalent of {}"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        forbidHeaders: [
            "X-Amz-Target"
        ]
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/OptionalInputOutput"
        bodyMediaType: "application/json"
        body: "{}"
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseOptionalOutput"
        protocol: rpcv2Json
        documentation: "When output is empty we write JSON equivalent of {}"
        body: "{}"
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        code: 200
    }
])
operation OptionalInputOutput {
    input: SimpleStructure
    output: SimpleStructure
}
