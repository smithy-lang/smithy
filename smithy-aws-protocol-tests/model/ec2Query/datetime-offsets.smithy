$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
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
        id: "Ec2QueryDateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: ec2Query,
        code: 200,
        body: """
              <DatetimeOffsetsResponse xmlns="https://example.com/">
                  <datetime>2019-12-16T22:48:18-01:00</datetime>
                  <requestId>requestid</requestId>
              </DatetimeOffsetsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        params: { datetime: 1576540098 }
    },
    {
        id: "Ec2QueryDateTimeWithPositiveOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: ec2Query,
        code: 200,
        body: """
              <DatetimeOffsetsResponse xmlns="https://example.com/">
                  <datetime>2019-12-17T00:48:18+01:00</datetime>
                  <requestId>requestid</requestId>
              </DatetimeOffsetsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        params: { datetime: 1576540098 }
    }
])

structure DatetimeOffsetsOutput {
    datetime: DateTime
}
