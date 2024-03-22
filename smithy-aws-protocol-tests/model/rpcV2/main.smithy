$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor
use aws.api#service
use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@service(sdkId: "Sample RpcV2 Protocol")
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
        RpcV2CborMaps,
        RecursiveShapes,
        GreetingWithErrors,
        FractionalSeconds,
        OperationWithDefaults,
        SparseNullsOperation
    ]
}

structure EmptyStructure {

}

structure SimpleStructure {
    value: String,
}
