$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
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
        id: "Ec2QueryDateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: ec2Query,
        code: 200,
        body: """
              <DatetimeOffsetsResponse xmlns="https://example.com/">
                  <datetime>2019-12-16T22:48:18-01:00</datetime>
                  <RequestId>requestid</RequestId>
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
                  <RequestId>requestid</RequestId>
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

// These tests are for verifying the server can correctly parse
// the `DateTime` timestamp with an offset
@tags(["server-only"])
operation OffsetDatetimes {
    input: OffsetDatetimesInput
}

apply OffsetDatetimes @httpRequestTests([
    {
        id: "Ec2QueryNegativeOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=OffsetDatetimes&Version=2020-01-08&Datetime=2019-12-16T22%3A48%3A18-01%3A00",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            datetime: 1576540098,
        }
    },
    {
        id: "Ec2QueryPositiveOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=OffsetDatetimes&Version=2020-01-08&Datetime=2019-12-17T00%3A48%3A18%2B01%3A00",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            datetime: 1576540098,
        }
    },
])

structure OffsetDatetimesInput {
    datetime: DateTime
}
