$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use aws.protocoltests.config#ErrorCodeParams
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply QueryCompatErrorOp @httpRequestTests([
    {
        id: "AwsJson10QueryCompatSendsQueryModeHeader"
        documentation: "Clients for query-compatible services MUST send the x-amzn-query-mode header"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: {
            "Content-Type": "application/x-amz-json-1.0"
            "X-Amz-Target": "AwsJson10QueryCompatCorpusTests.QueryCompatErrorOp"
            "x-amzn-query-mode": "true"
        }
        params: {}
    }
])

apply QueryCompatError @httpResponseTests([
    {
        id: "AwsJson10QueryCompatNoCustomCodeError"
        documentation: """
            Parses an error that declares no @awsQueryError, so no
            x-amzn-query-error header is sent and the code exposed to the caller
            is the error shape's own name"""
        protocol: awsJson1_0
        code: 400
        body: """
            {
                "__type": "aws.protocoltests.corpus#QueryCompatError",
                "message": "query compatible failure"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        forbidHeaders: ["x-amzn-query-error"]
        params: { message: "query compatible failure" }
        vendorParamsShape: ErrorCodeParams
        vendorParams: { code: "QueryCompatError" }
    }
])

apply QueryCompatCustomCodeError @httpResponseTests([
    {
        id: "AwsJson10QueryCompatCustomCodeError"
        documentation: """
            Parses an error whose @awsQueryError code is surfaced through the
            x-amzn-query-error header, and exposes that customized code rather
            than the shape name"""
        protocol: awsJson1_0
        code: 400
        body: """
            {
                "__type": "aws.protocoltests.corpus#QueryCompatCustomCodeError",
                "message": "customized query error code"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "x-amzn-query-error": "CustomCode;Sender" }
        params: { message: "customized query error code" }
        vendorParamsShape: ErrorCodeParams
        vendorParams: { code: "CustomCode", type: "Sender" }
    }
])
