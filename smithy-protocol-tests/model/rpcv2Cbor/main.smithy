$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor

@rpcv2Cbor
@title("RpcV2 Protocol Service")
service RpcV2Protocol {
    version: "2020-07-14"
    operations: [
        NoInputOutput
        EmptyInputOutput
        OptionalInputOutput
        SimpleScalarProperties
        RpcV2CborLists
        RpcV2CborDenseMaps
        RpcV2CborSparseMaps
        RecursiveShapes
        GreetingWithErrors
        FractionalSeconds
        OperationWithDefaults
        SparseNullsOperation
    ]
}

structure EmptyStructure {}

structure SimpleStructure {
    value: String
}
