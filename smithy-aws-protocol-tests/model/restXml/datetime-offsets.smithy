$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
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
        id: "RestXmlDateTimeWithNegativeOffset",
        documentation: """
        Ensures that clients can correctly parse datetime (timestamps) with offsets""",
        protocol: restXml,
        code: 200,
        body: """
            <DateTime>
                <datetime>2019-12-16T22:48:18-01:00</datetime>
            </DateTime>
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
            <DateTime>
                <datetime>2019-12-17T00:48:18+01:00</datetime>
            </DateTime>
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

// These tests are for verifying the server can correctly parse
// the `DateTime` timestamp with an offset
@tags(["server-only"])
@http(uri: "/OffsetDatetimes", method: "POST")
operation OffsetDatetimes {
    input: OffsetDatetimesInput
}

apply OffsetDatetimes @httpRequestTests([
    {
        id: "RestXmlNegativeOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: restXml,
        method: "POST",
        uri: "/OffsetDatetimes",
        body: """
            <DateTime>
                <datetime>2019-12-17T00:48:18+01:00</datetime>
            </DateTime>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            datetime: 1576540098,
        }
    },
    {
        id: "RestXmlPositiveOffsetDatetimes",
        documentation: """
        Ensures that servers can correctly parse datetime (timestamps) with offsets""",
        protocol: restXml,
        method: "POST",
        uri: "/OffsetDatetimes",
        body: """
            <DateTime>
                <datetime>2019-12-16T22:48:18-01:00</datetime>
            </DateTime>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            datetime: 1576540098,
        }
    },
])

structure OffsetDatetimesInput {
    datetime: DateTime
}
