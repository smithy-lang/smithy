$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#DateTime
use smithy.test#httpResponseTests

// These tests verify that clients can parse `DateTime` timestamps with fractional seconds.
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
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
}
