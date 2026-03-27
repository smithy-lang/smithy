$version: "2.0"

namespace smithy.protocoltests.rpcv2Json

use smithy.protocols#rpcv2Json
use smithy.protocoltests.shared#DateTime
use smithy.protocoltests.shared#EpochSeconds
use smithy.protocoltests.shared#HttpDate
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// These tests verify that clients can parse timestamps with fractional seconds.
@tags(["client-only"])
operation FractionalSeconds {
    output: FractionalSecondsOutput
}

apply FractionalSeconds @httpResponseTests([
    {
        id: "RpcV2JsonResponseDateTimeWithFractionalSeconds"
        documentation: "Ensures that clients can correctly parse timestamps with fractional seconds"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "datetime": 946845296.123
            }"""
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: { datetime: 946845296.123 }
        bodyMediaType: "application/json"
        appliesTo: "client"
    }
])

structure FractionalSecondsOutput {
    datetime: Timestamp
}

/// This operation tests that the rpcv2Json protocol always uses epoch-seconds
/// for timestamp serialization, regardless of the `timestampFormat` trait.
/// Members targeting `DateTime` (date-time), `HttpDate` (http-date), and
/// `EpochSeconds` (epoch-seconds) must all serialize as JSON numbers.
operation TimestampFormatIgnored {
    input: TimestampFormatIgnoredIO
    output: TimestampFormatIgnoredIO
}

apply TimestampFormatIgnored @httpRequestTests([
    {
        id: "RpcV2JsonRequestTimestampFormatIgnored"
        documentation: """
            The rpcv2Json protocol always serializes timestamps as epoch-seconds JSON numbers.
            The timestampFormat trait MUST NOT be respected."""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonProtocol/operation/TimestampFormatIgnored"
        body: """
            {
                "dateTime": 946845296,
                "httpDate": 946845296,
                "epochSeconds": 946845296,
                "normal": 946845296
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            "Accept": "application/json"
        }
        requireHeaders: [
            "Content-Length"
        ]
        params: {
            dateTime: 946845296
            httpDate: 946845296
            epochSeconds: 946845296
            normal: 946845296
        }
    }
])

apply TimestampFormatIgnored @httpResponseTests([
    {
        id: "RpcV2JsonResponseTimestampFormatIgnored"
        documentation: """
            The rpcv2Json protocol always serializes timestamps as epoch-seconds JSON numbers.
            The timestampFormat trait MUST NOT be respected."""
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "dateTime": 946845296,
                "httpDate": 946845296,
                "epochSeconds": 946845296,
                "normal": 946845296
            }"""
        bodyMediaType: "application/json"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
        }
        params: {
            dateTime: 946845296
            httpDate: 946845296
            epochSeconds: 946845296
            normal: 946845296
        }
    }
])

structure TimestampFormatIgnoredIO {
    dateTime: DateTime
    httpDate: HttpDate
    epochSeconds: EpochSeconds
    normal: Timestamp
}
