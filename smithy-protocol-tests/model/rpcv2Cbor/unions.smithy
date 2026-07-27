$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This operation uses unions for inputs and outputs.
@idempotent
operation RpcV2CborUnions {
    input: RpcV2CborUnionInputOutput
    output: RpcV2CborUnionInputOutput
}

structure RpcV2CborUnionInputOutput {
    contents: RpcV2CborUnion
    otherValue: String
}

union RpcV2CborUnion {
    stringValue: String
    unionValue: RpcV2CborNestedUnion
}

union RpcV2CborNestedUnion {
    stringValue: String
}

apply RpcV2CborUnions @httpRequestTests([
    {
        id: "RpcV2CborSerializesUnionValue"
        documentation: "Serializes a union followed by another structure member"
        protocol: rpcv2Cbor
        method: "POST"
        uri: "/service/RpcV2Protocol/operation/RpcV2CborUnions"
        body: "omhjb250ZW50c6Frc3RyaW5nVmFsdWVjZm9vam90aGVyVmFsdWVjYmFy"
        bodyMediaType: "application/cbor"
        headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor", Accept: "application/cbor" }
        requireHeaders: ["Content-Length"]
        params: {
            contents: { stringValue: "foo" }
            otherValue: "bar"
        }
    }
    {
        id: "RpcV2CborSerializesNestedUnionValue"
        documentation: "Serializes a nested union followed by another structure member"
        protocol: rpcv2Cbor
        method: "POST"
        uri: "/service/RpcV2Protocol/operation/RpcV2CborUnions"
        body: "omhjb250ZW50c6FqdW5pb25WYWx1ZaFrc3RyaW5nVmFsdWVjZm9vam90aGVyVmFsdWVjYmFy"
        bodyMediaType: "application/cbor"
        headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor", Accept: "application/cbor" }
        requireHeaders: ["Content-Length"]
        params: {
            contents: {
                unionValue: { stringValue: "foo" }
            }
            otherValue: "bar"
        }
    }
])

apply RpcV2CborUnions @httpResponseTests([
    {
        id: "RpcV2CborDeserializesUnionValue"
        documentation: "Deserializes a tagged union followed by another structure member"
        protocol: rpcv2Cbor
        code: 200
        body: "omhjb250ZW50c6Frc3RyaW5nVmFsdWVjZm9vam90aGVyVmFsdWVjYmFy"
        bodyMediaType: "application/cbor"
        headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor" }
        params: {
            contents: { stringValue: "foo" }
            otherValue: "bar"
        }
    }
    {
        id: "RpcV2CborDeserializesNestedUnionValue"
        documentation: "Deserializes a nested union followed by another structure member"
        protocol: rpcv2Cbor
        code: 200
        body: "omhjb250ZW50c6FqdW5pb25WYWx1ZaFrc3RyaW5nVmFsdWVjZm9vam90aGVyVmFsdWVjYmFy"
        bodyMediaType: "application/cbor"
        headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor" }
        params: {
            contents: {
                unionValue: { stringValue: "foo" }
            }
            otherValue: "bar"
        }
    }
])
