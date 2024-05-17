$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.test#httpResponseTests

// Verifies that clients can upcast float16s in responses.
@tags(["client-only"])
operation Float16 {
    output: Float16Output
}

apply Float16 @httpResponseTests([
    {
        id: "RpcV2CborFloat16Inf",
        documentation: "Ensures that clients can correctly parse float16 +Inf.",
        protocol: rpcv2Cbor,
        code: 200,
        // https://cbor.nemo157.com/?type=hex&value=a16576616c7565f97c00
        body: "oWV2YWx1Zfl8AA==",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: { value: "Infinity" }
        bodyMediaType: "application/cbor",
        appliesTo: "client"
    },
    {
        id: "RpcV2CborFloat16NegInf",
        documentation: "Ensures that clients can correctly parse float16 -Inf.",
        protocol: rpcv2Cbor,
        code: 200,
        // https://cbor.nemo157.com/?type=hex&value=a16576616c7565f9fc00
        body: "oWV2YWx1Zfn8AA==",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: { value: "-Infinity" }
        bodyMediaType: "application/cbor",
        appliesTo: "client"
    },
    {
        id: "RpcV2CborFloat16LSBNaN",
        documentation: "Ensures that clients can correctly parse float16 NaN with high LSB.",
        protocol: rpcv2Cbor,
        code: 200,
        // https://cbor.nemo157.com/?type=hex&value=a16576616c7565f97c01
        body: "oWV2YWx1Zfl8AQ==",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: { value: "NaN" }
        bodyMediaType: "application/cbor",
        appliesTo: "client"
    },
    {
        id: "RpcV2CborFloat16MSBNaN",
        documentation: "Ensures that clients can correctly parse float16 NaN with high MSB.",
        protocol: rpcv2Cbor,
        code: 200,
        // https://cbor.nemo157.com/?type=hex&value=a16576616c7565f97e00
        body: "oWV2YWx1Zfl+AA==",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: { value: "NaN" }
        bodyMediaType: "application/cbor",
        appliesTo: "client"
    },
    {
        id: "RpcV2CborFloat16Subnormal",
        documentation: "Ensures that clients can correctly parse a subnormal float16.",
        protocol: rpcv2Cbor,
        code: 200,
        // https://cbor.nemo157.com/?type=hex&value=a16576616c7565f90050
        body: "oWV2YWx1ZfkAUA==",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: { value: 0.00000476837158203125 } // 1.25 * 2^-18
        bodyMediaType: "application/cbor",
        appliesTo: "client"
    }
])

structure Float16Output {
    value: Double
}
