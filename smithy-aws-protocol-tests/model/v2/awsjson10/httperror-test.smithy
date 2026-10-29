$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use smithy.test#httpResponseTests

apply smithy.protocoltests.corpus#HttpErrorConflict @httpResponseTests([
    {
        id: "AwsJson10HttpErrorConflictDeserialize"
        documentation: """
            Deserializes a client error whose @httpError(409) overrides the
            default 400 status implied by @error("client")"""
        protocol: awsJson1_0
        code: 409
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorConflict",
                "message": "resource already exists"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource already exists" }
    }
])

apply smithy.protocoltests.corpus#HttpErrorGone @httpResponseTests([
    {
        id: "AwsJson10HttpErrorGoneDeserialize"
        documentation: """
            Deserializes a client error whose @httpError(410) overrides the
            default 400 status, with more than one modeled member"""
        protocol: awsJson1_0
        code: 410
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorGone",
                "message": "resource was deleted",
                "details": "deleted on 2021-01-01"
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "resource was deleted", details: "deleted on 2021-01-01" }
    }
])

apply smithy.protocoltests.corpus#HttpErrorServiceUnavailable @httpResponseTests([
    {
        id: "AwsJson10HttpErrorServiceUnavailableDeserialize"
        documentation: """
            Deserializes a server error whose @httpError(503) overrides the
            default 500 status implied by @error("server")"""
        protocol: awsJson1_0
        code: 503
        body: """
            {
                "__type": "smithy.protocoltests.corpus#HttpErrorServiceUnavailable",
                "message": "try again later",
                "retryAfterSeconds": 42
            }"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0" }
        params: { message: "try again later", retryAfterSeconds: 42 }
    }
])
