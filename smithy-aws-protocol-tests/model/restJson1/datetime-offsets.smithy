$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#DateTime
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
        id: "RestJsonDateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: restJson1,
        code: 200,
        body:
        """
              {
                  "datetime": "2019-12-16T22:48:18-01:00"
              }
        """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/json",
        appliesTo: "client"
    },
    {
        id: "RestJsonDateTimeWithPositiveOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: restJson1,
        code: 200,
        body:
        """
              {
                  "datetime": "2019-12-17T00:48:18+01:00"
              }
        """,
        params: { datetime: 1576540098 }
        bodyMediaType: "application/json",
        appliesTo: "client"
    },
])

structure DatetimeOffsetsOutput {
    datetime: DateTime
}
