$version: "2.0"

namespace smithy.example

use smithy.protocols#rpcv2Cbor

@rpcv2Cbor
service DocumentService {
    version: "2023-02-10"
    operations: [
        DocumentOperation
    ]
}

operation DocumentOperation {
    input := {
        document: Document
    }
}
