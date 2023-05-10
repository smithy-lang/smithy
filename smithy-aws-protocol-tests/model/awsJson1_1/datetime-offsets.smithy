$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use aws.protocoltests.shared#DateTime
use smithy.test#httpResponseTests

// These tests are for verifying the client can correctly parse
// the `DateTime` timestamp with an offset
@tags(["client-only"])
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
