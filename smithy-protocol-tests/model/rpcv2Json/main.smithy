$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocols#rpcv2Json

@rpcv2Json
@title("RpcV2 JSON Protocol Service")
service RpcV2JsonProtocol {
    version: "2020-07-14"
    operations: [
        NoInputOutput
        EmptyInputOutput
        OptionalInputOutput
        SimpleScalarProperties
        RpcV2JsonLists
        RpcV2JsonDenseMaps
        RpcV2JsonSparseMaps
        RecursiveShapes
        GreetingWithErrors
        FractionalSeconds
        TimestampFormatIgnored
        OperationWithDefaults
        SparseNullsOperation
        BigDecimalOperation
        BigIntegerOperation
    ]
}

structure EmptyStructure {}

structure SimpleStructure {
    value: String
}
