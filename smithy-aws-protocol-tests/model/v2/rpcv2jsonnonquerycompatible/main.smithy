$version: "2.0"

namespace aws.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests

@rpcv2Json
@aws.api#service(sdkId: "RpcV2JsonNonQueryCompatCorpus", arnNamespace: "rpcv2jsonnonquerycompatcorpus")
@aws.auth#sigv4(name: "rpcv2jsonnonquerycompatcorpus")
service RpcV2JsonNonQueryCompatCorpusTests {
    operations: [
        NonQueryCompatOp
    ]
}

apply NonQueryCompatOp @httpRequestTests([
    {
        id: "RpcV2JsonNonQueryCompatForbidsQueryModeHeader"
        documentation: "Clients for services that are not query-compatible MUST NOT send the x-amzn-query-mode header"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonNonQueryCompatCorpusTests/operation/NonQueryCompatOp"
        body: """
            {}"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        forbidHeaders: ["X-Amz-Target", "x-amzn-query-mode"]
        params: {}
    }
])

operation NonQueryCompatOp {
    input := {}
    output := {}
}
