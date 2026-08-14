$version: "2.0"

namespace aws.protocoltests.corpus

use aws.protocols#awsQueryCompatible
use smithy.protocols#rpcv2Json

@rpcv2Json
@awsQueryCompatible
@aws.api#service(sdkId: "RpcV2JsonQueryCompatCorpus", arnNamespace: "rpcv2jsonquerycompatcorpus")
@aws.auth#sigv4(name: "rpcv2jsonquerycompatcorpus")
service RpcV2JsonQueryCompatCorpusTests {
    operations: [
        QueryCompatErrorOp
    ]
}
