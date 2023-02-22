$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#DateTime
use aws.protocoltests.shared#HttpDate
use smithy.test#httpResponseTests

// These tests are for verifying the client can correctly parse
// the `DateTime` and `HttpDate` timestamps with fractional seconds
@tags(["client-only"])
@http(uri: "/FractionalSeconds", method: "POST")
operation FractionalSeconds {
    output: FractionalSecondsOutput
}

apply FractionalSeconds @httpResponseTests([
    {
        id: "RestXmlDateTimeWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse datetime timestamps with fractional seconds""",
        protocol: restXml,
        code: 200,
        body: """
            <FractionalSecondsOutput>
                <datetime>2000-01-02T20:34:56.123Z</datetime>
            </FractionalSecondsOutput>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: { datetime: 946845296.123 }
    },
    {
        id: "RestXmlHttpDateWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse http-date timestamps with fractional seconds""",
        protocol: restXml,
        code: 200,
        body: """
            <FractionalSecondsOutput>
                <httpdate>Sun, 02 Jan 2000 20:34:56.456 GMT</httpdate>
            </FractionalSecondsOutput>
            """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: { httpdate: 946845296.456 }
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
    httpdate: HttpDate
}
