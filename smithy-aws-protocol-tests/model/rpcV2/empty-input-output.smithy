$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests


@httpRequestTests([
    {
        id: "no_input",
        protocol: rpcv2Cbor,
        documentation: "Body is empty and no Content-Type header if no input",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
        },
        forbidHeaders: [
            "Content-Type",
            "X-Amz-Target"
        ]
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/NoInputOutput",
        body: ""
    },
    {
        id: "no_input_server_allows_accept",
        protocol: rpcv2Cbor,
        documentation: "Servers should allow the Accept header to be set to the default content-type.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/NoInputOutput",
        body: "",
        appliesTo: "server"
    },
    {
        id: "no_input_server_allows_empty_cbor",
        protocol: rpcv2Cbor,
        documentation: "Servers should accept CBOR empty struct if no input.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/NoInputOutput",
        body: "v/8=",
        appliesTo: "server"
    },
    {
        id: "NoInputServerIngoresUnexpectedFields",
        protocol: rpcv2Cbor,
        documentation: "Servers should accept CBOR empty struct if no input.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/NoInputOutput",
        body: "v/8=",
        appliesTo: "server"
    }
])
@httpResponseTests([
    {
        id: "no_output",
        protocol: rpcv2Cbor,
        documentation: "Body is empty and no Content-Type header if no response",
        body: "",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
        },
        forbidHeaders: [
            "Content-Type"
        ]
        code: 200,
    },
    {
        id: "no_output_client_allows_accept",
        protocol: rpcv2Cbor,
        documentation: "Servers should allow the accept header to be set to the default content-type.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        body: "",
        code: 200,
        appliesTo: "client",
    },
    {
        id: "no_input_client_allows_empty_cbor",
        protocol: rpcv2Cbor,
        documentation: "Client should accept CBOR empty struct if no output",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        body: "v/8=",
        code: 200,
        appliesTo: "client",
    }
])
operation NoInputOutput {}


@httpRequestTests([
    {
        id: "empty_input",
        protocol: rpcv2Cbor,
        documentation: "When Input structure is empty we write CBOR equivalent of {}",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        forbidHeaders: [
            "X-Amz-Target"
        ]
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/EmptyInputOutput",
        bodyMediaType: "application/cbor",
        body: "v/8=",
    },
])
@httpResponseTests([
    {
        id: "empty_output",
        protocol: rpcv2Cbor,
        documentation: "When output structure is empty we write CBOR equivalent of {}",
        body: "v/8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        code: 200,
    },
])
operation EmptyInputOutput {
    input: EmptyStructure,
    output: EmptyStructure
}

@httpRequestTests([
    {
        id: "optional_input",
        protocol: rpcv2Cbor,
        documentation: "When input is empty we write CBOR equivalent of {}",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        },
        forbidHeaders: [
            "X-Amz-Target"
        ]
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/OptionalInputOutput",
        bodyMediaType: "application/cbor",
        body: "v/8=",
    },
])
@httpResponseTests([
    {
        id: "optional_output",
        protocol: rpcv2Cbor,
        documentation: "When output is empty we write CBOR equivalent of {}",
        body: "v/8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        code: 200,
    },
])
operation OptionalInputOutput {
    input: SimpleStructure,
    output: SimpleStructure
}
