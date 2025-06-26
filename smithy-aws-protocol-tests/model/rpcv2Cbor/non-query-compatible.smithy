$version: "2"

namespace aws.protocoltests.rpcv2cbor

use aws.api#service
use aws.auth#sigv4
use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests

@service(sdkId: "Non Query Compatible RpcV2 Protocol")
@sigv4(name: "nonquerycompatiblerpcv2protocol")
@rpcv2Cbor
@title("Non Query Compatible RpcV2 Protocol Service")
service NonQueryCompatibleRpcV2Protocol {
    version: "2025-06-20"
    operations: [
        NonQueryCompatibleOperation
    ]
}

@httpRequestTests([
    {
        id: "NonQueryCompatibleRpcV2CborForbidsQueryModeHeader"
        documentation: "The query mode header MUST NOT be set on non-query-compatible services."
        protocol: rpcv2Cbor
        method: "POST"
        headers: { "smithy-protocol": "rpc-v2-cbor", Accept: "application/cbor" }
        forbidHeaders: ["x-amzn-query-mode"]
        uri: "/service/NonQueryCompatibleRpcV2Protocol/operation/QueryIncompatibleOperation"
        body: ""
    }
])
@idempotent
operation NonQueryCompatibleOperation {}
