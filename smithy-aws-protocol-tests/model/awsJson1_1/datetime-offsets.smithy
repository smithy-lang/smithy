$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use aws.protocoltests.shared#DateTime
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// These tests are for verifying the client can correctly parse
// the `DateTime` timestamp with an offset
@tags(["client-only"])
@http(uri: "/DatetimeOffsets", method: "POST")
operation DatetimeOffsets {
    output: DatetimeOffsetsOutput
}

apply DatetimeOffsets @httpResponseTests([
    {
        id: "AwsJson11DateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: awsJson1_1,
        code: 200,
        body:
        """
              {
                  "datetime": "2019-12-16T22:48:18-01:00"
              }
        """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
        },
        appliesTo: "client"
    },
    {
        id: "AwsJson11DateTimeWithPositiveOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: awsJson1_1,
        code: 200,
        body:
        """
              {
                  "datetime": "2019-12-17T00:48:18+01:00"
              }
        """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
        },
        appliesTo: "client"
    },
])

structure DatetimeOffsetsOutput {
    datetime: DateTime
}

// These tests are for verifying the server can correctly parse
// the `DateTime` timestamp with an offset
@tags(["server-only"])
@http(uri: "/OffsetDatetimes", method: "POST")
operation OffsetDatetimes {
    input: OffsetDatetimesInput
}

apply OffsetDatetimes @httpRequestTests([
    {
        id: "AwsJson11NegativeOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body:
        """
              {
                  "datetime": "2019-12-16T22:48:18-01:00"
              }
        """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.OffsetDatetimes",
        },
        appliesTo: "server"
    },
    {
        id: "AwsJson11PositiveOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body:
        """
              {
                  "datetime": "2019-12-17T00:48:18+01:00"
              }
        """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.OffsetDatetimes",
        },
        appliesTo: "server"
    }
])

structure OffsetDatetimesInput {
    datetime: DateTime
}
