$version: "2"

namespace aws.protocoltests.rpcv2cbor

use aws.protocols#awsQueryError
use aws.protocoltests.config#ErrorCodeParams
use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "NonQueryCompatibleRpcV2CborForbidsQueryModeHeader"
        documentation: "The query mode header MUST NOT be set on non-query-compatible services."
        protocol: rpcv2Cbor
        method: "POST"
        headers: { "smithy-protocol": "rpc-v2-cbor", Accept: "application/cbor" }
        forbidHeaders: ["x-amzn-query-mode"]
        uri: "/service/NonQueryCompatibleRpcV2Protocol/operation/QueryIncompatibleOperation"
        body: "{}"
        bodyMediaType: "application/json"
    }
])
@idempotent
operation QueryIncompatibleOperation {}

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
        body: "v2ZfX3R5cGV4MHNtaXRoeS5wcm90b2NvbHRlc3RzLnJwY3YyQ2JvciNOb0N1c3RvbUNvZGVFcnJvcmdNZXNzYWdlYkhp/w=="
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
        body: "v2ZfX3R5cGV4LnNtaXRoeS5wcm90b2NvbHRlc3RzLnJwY3YyQ2JvciNDdXN0b21Db2RlRXJyb3JnTWVzc2FnZWJIaf8="
        bodyMediaType: "application/cbor"
        vendorParamsShape: ErrorCodeParams
        vendorParams: { code: "Customized", type: "Sender" }
    }
])
