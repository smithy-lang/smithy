$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests

@awsJson1_0
@aws.api#service(sdkId: "Json10NonQueryCompatCorpus", arnNamespace: "awsjson10nonquerycompatcorpus")
@aws.auth#sigv4(name: "awsjson10nonquerycompatcorpus")
service AwsJson10NonQueryCompatCorpusTests {
    operations: [
        NonQueryCompatOp
    ]
}

apply NonQueryCompatOp @httpRequestTests([
    {
        id: "AwsJson10NonQueryCompatForbidsQueryModeHeader"
        documentation: "Clients for services that are not query-compatible MUST NOT send the x-amzn-query-mode header"
        protocol: awsJson1_0
        method: "POST"
        uri: "/"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "Content-Type": "application/x-amz-json-1.0", "X-Amz-Target": "AwsJson10NonQueryCompatCorpusTests.NonQueryCompatOp" }
        forbidHeaders: ["x-amzn-query-mode"]
        params: {}
    }
])
