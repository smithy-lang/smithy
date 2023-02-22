$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
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
        id: "RestXmlDateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: restXml,
        code: 200,
        body: """
            <DatetimeOffsetsOutput>
                <datetime>2019-12-16T22:48:18-01:00</datetime>
            </DatetimeOffsetsOutput>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: { datetime: 1576540098 }
    },
    {
        id: "RestXmlDateTimeWithPositiveOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: restXml,
        code: 200,
        body: """
            <DatetimeOffsetsOutput>
                <datetime>2019-12-17T00:48:18+01:00</datetime>
            </DatetimeOffsetsOutput>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: { datetime: 1576540098 }
    },
])

structure DatetimeOffsetsOutput {
    datetime: DateTime
}
