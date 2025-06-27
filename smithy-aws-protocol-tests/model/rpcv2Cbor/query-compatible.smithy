$version: "2"

namespace aws.protocoltests.rpcv2cbor

use aws.api#service
use aws.auth#sigv4
use aws.protocols#awsQueryCompatible
use aws.protocols#awsQueryError
use aws.protocoltests.config#ErrorCodeParams
use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests


@service(sdkId: "Query Compatible RpcV2 Protocol")
@sigv4(name: "querycompatiblerpcv2protocol")
@rpcv2Cbor
@title("Query Compatible RpcV2 Protocol Service")
@awsQueryCompatible
service QueryCompatibleRpcV2Protocol {
    version: "2025-06-20"
    operations: [
        QueryCompatibleOperation
    ]
}


@idempotent
operation QueryCompatibleOperation {
    errors: [
        NoCustomCodeError
        CustomCodeError
    ]
}

apply QueryCompatibleOperation @httpRequestTests([
    {
        id: "QueryCompatibleRpcV2CborSendsQueryModeHeader"
        documentation: "Clients for query-compatible services MUST send the x-amzn-query-mode header."
        protocol: rpcv2Cbor
        method: "POST"
        headers: { "smithy-protocol": "rpc-v2-cbor", Accept: "application/cbor", "x-amzn-query-mode": "true" }
        forbidHeaders: ["Content-Type", "X-Amz-Target"]
        uri: "/service/QueryCompatibleRpcV2Protocol/operation/QueryCompatibleOperation"
        body: ""
    }
])

@error("client")
structure NoCustomCodeError {
    message: String
}

apply NoCustomCodeError @httpResponseTests([
    {
        id: "QueryCompatibleRpcV2CborNoCustomCodeError"
        documentation: "Parses simple RpcV2 CBOR errors with no query error code"
        protocol: rpcv2Cbor
        params: { message: "Hi" }
        code: 400
        headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor" }
        body: "uQACZl9fdHlwZXgtYXdzLnByb3RvY29sdGVzdHMucnBjdjJjYm9yI05vQ3VzdG9tQ29kZUVycm9yZ21lc3NhZ2ViSGk="
        bodyMediaType: "application/cbor"
        vendorParamsShape: ErrorCodeParams
        vendorParams: { code: "NoCustomCodeError" }
    }
])

@awsQueryError(code: "Customized", httpResponseCode: 402)
@error("client")
structure CustomCodeError {
    message: String
}

apply CustomCodeError @httpResponseTests([
    {
        id: "QueryCompatibleRpcV2CborCustomCodeError"
        documentation: "Parses simple RpcV2 CBOR errors with query error code"
        protocol: rpcv2Cbor
        params: { message: "Hi" }
        code: 400
        headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor", "x-amzn-query-error": "Customized;Sender" }
        body: "uQACZl9fdHlwZXgrYXdzLnByb3RvY29sdGVzdHMucnBjdjJjYm9yI0N1c3RvbUNvZGVFcnJvcmdtZXNzYWdlYkhp"
        bodyMediaType: "application/cbor"
        vendorParamsShape: ErrorCodeParams
        vendorParams: { code: "Customized", type: "Sender" }
    }
])
