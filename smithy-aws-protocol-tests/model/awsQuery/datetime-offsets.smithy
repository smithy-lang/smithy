$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
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
        id: "AwsQueryDateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: awsQuery,
        code: 200,
        body: """
              <DatetimeOffsetsResponse xmlns="https://example.com/">
                  <DatetimeOffsetsResult>
                      <datetime>2019-12-16T22:48:18-01:00</datetime>
                  </DatetimeOffsetsResult>
              </DatetimeOffsetsResponse>
              """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        appliesTo: "client"
    },
    {
        id: "AwsQueryDateTimeWithPositiveOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: awsQuery,
        code: 200,
        body: """
              <DatetimeOffsetsResponse xmlns="https://example.com/">
                  <DatetimeOffsetsResult>
                      <datetime>2019-12-17T00:48:18+01:00</datetime>
                  </DatetimeOffsetsResult>
              </DatetimeOffsetsResponse>
              """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        appliesTo: "client"
    },
])

structure DatetimeOffsetsOutput {
    datetime: DateTime
}
