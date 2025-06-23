$version: "2"

namespace aws.protocoltests.rpcv2cbor

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsQueryCompatible
use smithy.protocols#rpcv2Cbor

@service(sdkId: "Query Compatible RpcV2 Protocol")
@sigv4(name: "query-compatible-rpcv2")
@rpcv2Cbor
@title("Query Compatible RpcV2 Protocol Service")
@awsQueryCompatible
service QueryCompatibleRpcV2Protocol {
    version: "2025-06-20"
    operations: [
        QueryCompatibleOperation
    ]
}

@service(sdkId: "Non Query Compatible RpcV2 Protocol")
@sigv4(name: "non-query-compatible-rpcv2")
@rpcv2Cbor
@title("Non Query Compatible RpcV2 Protocol Service")
service NonQueryCompatibleRpcV2Protocol {
    version: "2025-06-20"
    operations: [
        QueryIncompatibleOperation
    ]
}
