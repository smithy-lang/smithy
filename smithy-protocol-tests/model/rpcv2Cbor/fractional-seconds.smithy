$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.protocoltests.shared#DateTime
use smithy.test#httpResponseTests

// These tests verify that clients can parse `DateTime` timestamps with fractional seconds.
@tags(["client-only"])
operation FractionalSeconds {
    output: FractionalSecondsOutput
}

apply FractionalSeconds @httpResponseTests([
    {
        id: "RpcV2CborDateTimeWithFractionalSeconds",
        documentation: "Ensures that clients can correctly parse timestamps with fractional seconds",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2hkYXRldGltZcH7Qcw32zgPvnf/",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: { datetime: 946845296.123 }
        bodyMediaType: "application/cbor",
        appliesTo: "client"
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
}
