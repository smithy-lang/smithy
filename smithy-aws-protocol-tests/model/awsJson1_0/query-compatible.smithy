$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use aws.protocols#awsQueryError
use aws.protocoltests.config#ErrorCodeParams
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "NonQueryCompatibleAwsJson10ForbidsQueryModeHeader"
        documentation: "The query mode header MUST NOT be set on non-query-compatible services."
        protocol: awsJson1_0
        method: "POST"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "JsonRpc10.QueryIncompatibleOperation" }
        uri: "/"
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
        id: "QueryCompatibleAwsJson10CborSendsQueryModeHeader"
        documentation: "Clients for query-compatible services MUST send the x-amzn-query-mode header."
        protocol: awsJson1_0
        method: "POST"
        headers: { "Content-Type": "application/x-amz-json-1.0", "x-amzn-query-mode": "true", "X-Amz-Target": "QueryCompatibleJsonRpc10.QueryCompatibleOperation" }
        uri: "/"
        body: "{}"
        bodyMediaType: "application/json"
    }
])

@error("client")
structure NoCustomCodeError {
    message: String
}

apply NoCustomCodeError @httpResponseTests([
    {
        id: "QueryCompatibleAwsJson10NoCustomCodeError"
        documentation: "Parses simple errors with no query error code"
        protocol: awsJson1_0
        params: { message: "Hi" }
        code: 400
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        body: """
            {
                "__type": "aws.protocoltests.json10#NoCustomCodeError",
                "message": "Hi"
            }"""
        bodyMediaType: "application/json"
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
        id: "QueryCompatibleAwsJson10CustomCodeError"
        documentation: "Parses simple errors with query error code"
        protocol: awsJson1_0
        params: { message: "Hi" }
        code: 400
        headers: { "Content-Type": "application/x-amz-json-1.0", "x-amzn-query-error": "Customized;Sender" }
        body: """
            {
                "__type": "aws.protocoltests.json10#CustomCodeError",
                "message": "Hi"
            }"""
        bodyMediaType: "application/json"
        vendorParamsShape: ErrorCodeParams
        vendorParams: { code: "Customized", type: "Sender" }
    }
])
