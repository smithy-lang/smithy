$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json

@rpcv2Json
@title("RpcV2 JSON Corpus Protocol Tests")
service RpcV2JsonCorpusTests with [
    CoreProtocolTestService
    OptionalityProtocolTestService
    DocumentProtocolTestService
    EventStreamProtocolTestService
    HttpErrorProtocolTestService
    NamingProtocolTestService
    MiscSerdeTraitProtocolTestService
] {}
