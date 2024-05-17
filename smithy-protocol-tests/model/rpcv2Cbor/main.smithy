$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@rpcv2Cbor
@title("RpcV2 Protocol Service")
service RpcV2Protocol {
    version: "2020-07-14",
    operations: [
        NoInputOutput,
        EmptyInputOutput,
        OptionalInputOutput,
        SimpleScalarProperties,
        RpcV2CborLists,
        RpcV2CborDenseMaps,
        RpcV2CborSparseMaps,
        RecursiveShapes,
        GreetingWithErrors,
        FractionalSeconds,
        OperationWithDefaults,
        SparseNullsOperation,
        Float16
    ]
}

structure EmptyStructure {

}

structure SimpleStructure {
    value: String,
}
