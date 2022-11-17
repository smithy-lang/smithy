$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#DateTime
use smithy.test#httpRequestTests
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

// These tests are for verifying the server can correctly parse
// the `DateTime` timestamp with an offset
@tags(["server-only"])
operation OffsetDatetimes {
    input: OffsetDatetimesInput
}

apply OffsetDatetimes @httpRequestTests([
    {
        id: "AwsQueryNegativeOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=OffsetDatetimes&Version=2020-01-08&datetime=2019-12-16T22%3A48%3A18-01%3A00",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            datetime: 1576540098,
        }
    },
    {
        id: "AwsQueryPositiveOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=OffsetDatetimes&Version=2020-01-08&datetime=2019-12-17T00%3A48%3A18%2B01%3A00",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            datetime: 1576540098,
        }
    },
])

structure OffsetDatetimesInput {
    datetime: DateTime
}
