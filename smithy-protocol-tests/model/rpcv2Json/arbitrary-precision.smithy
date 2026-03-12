$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "RpcV2JsonRequestBigDecimalSimpleValue"
        protocol: rpcv2Json
        documentation: "Serializes a simple big decimal value as a JSON string to preserve precision."
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigDecimalOperation"
        body: """
            {
                "value": "1.5"
            }"""
        params: {
            value: 1.5
        }
    }
    {
        id: "RpcV2JsonRequestBigDecimalHighPrecision"
        protocol: rpcv2Json
        documentation: """
            Serializes a big decimal value that exceeds double precision in the decimal
            portion. Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigDecimalOperation"
        body: """
            {
                "value": "0.100000000000000000000001"
            }"""
        params: {
            value: 0.100000000000000000000001
        }
    }
    {
        id: "RpcV2JsonRequestBigDecimalNegativeHighPrecision"
        protocol: rpcv2Json
        documentation: """
            Serializes a negative big decimal value that exceeds double precision in
            the decimal portion. Implementations use JSON strings to preserve this
            precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigDecimalOperation"
        body: """
            {
                "value": "-0.100000000000000000000001"
            }"""
        params: {
            value: -0.100000000000000000000001
        }
    }
    {
        id: "RpcV2JsonRequestBigDecimalLargeWithFraction"
        protocol: rpcv2Json
        documentation: """
            Serializes a big decimal value that exceeds double precision in the integer
            portion. Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigDecimalOperation"
        body: """
            {
                "value": "100000000000000000000001.0"
            }"""
        params: {
            value: 100000000000000000000001.0
        }
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseBigDecimalSimpleValue"
        protocol: rpcv2Json
        documentation: "Serializes a simple big decimal value as a JSON string to preserve precision."
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "1.5"
            }"""
        code: 200
        params: {
            value: 1.5
        }
    }
    {
        id: "RpcV2JsonResponseBigDecimalHighPrecision"
        protocol: rpcv2Json
        documentation: """
            Serializes a big decimal value that exceeds double precision in the decimal
            portion. Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "0.100000000000000000000001"
            }"""
        code: 200
        params: {
            value: 0.100000000000000000000001
        }
    }
    {
        id: "RpcV2JsonResponseBigDecimalNegativeHighPrecision"
        protocol: rpcv2Json
        documentation: """
            Serializes a negative big decimal value that exceeds double precision in
            the decimal portion. Implementations use JSON strings to preserve this
            precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "-0.100000000000000000000001"
            }"""
        code: 200
        params: {
            value: -0.100000000000000000000001
        }
    }
    {
        id: "RpcV2JsonResponseBigDecimalLargeWithFraction"
        protocol: rpcv2Json
        documentation: """
            Serializes a big decimal value that exceeds double precision in the integer
            portion. Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "100000000000000000000001.0"
            }"""
        code: 200
        params: {
            value: 100000000000000000000001.0
        }
    }
])
operation BigDecimalOperation {
    input: BigDecimalStructure
    output: BigDecimalStructure
}

structure BigDecimalStructure {
    value: BigDecimal
}

@httpRequestTests([
    {
        id: "RpcV2JsonRequestBigIntegerSimpleValue"
        protocol: rpcv2Json
        documentation: "Serializes a simple big integer value as a JSON string to preserve precision."
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigIntegerOperation"
        body: """
            {
                "value": "42"
            }"""
        params: {
            value: 42
        }
    }
    {
        id: "RpcV2JsonRequestBigIntegerExceedingLongRange"
        protocol: rpcv2Json
        documentation: """
            Serializes a big integer value that exceeds the precision of a long.
            Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigIntegerOperation"
        body: """
            {
                "value": "9223372036854775808"
            }"""
        params: {
            value: 9223372036854775808
        }
    }
    {
        id: "RpcV2JsonRequestBigIntegerNegativeLargeValue"
        protocol: rpcv2Json
        documentation: """
            Serializes a negative big integer value that exceeds the precision of a long.
            Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/service/RpcV2JsonProtocol/operation/BigIntegerOperation"
        body: """
            {
                "value": "-9223372036854775809"
            }"""
        params: {
            value: -9223372036854775809
        }
    }
])
@httpResponseTests([
    {
        id: "RpcV2JsonResponseBigIntegerSimpleValue"
        protocol: rpcv2Json
        documentation: "Serializes a simple big integer value as a JSON string to preserve precision."
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "42"
            }"""
        code: 200
        params: {
            value: 42
        }
    }
    {
        id: "RpcV2JsonResponseBigIntegerExceedingLongRange"
        protocol: rpcv2Json
        documentation: """
            Serializes a big integer value that exceeds the precision of a long.
            Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "9223372036854775808"
            }"""
        code: 200
        params: {
            value: 9223372036854775808
        }
    }
    {
        id: "RpcV2JsonResponseBigIntegerNegativeLargeValue"
        protocol: rpcv2Json
        documentation: """
            Serializes a negative big integer value that exceeds the precision of a long.
            Implementations use JSON strings to preserve this precision."""
        tags: ["arbitrary-precision"]
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        bodyMediaType: "application/json"
        body: """
            {
                "value": "-9223372036854775809"
            }"""
        code: 200
        params: {
            value: -9223372036854775809
        }
    }
])
operation BigIntegerOperation {
    input: BigIntegerStructure
    output: BigIntegerStructure
}

structure BigIntegerStructure {
    value: BigInteger
}
