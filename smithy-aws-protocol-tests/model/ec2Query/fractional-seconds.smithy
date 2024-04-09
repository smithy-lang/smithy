$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
use aws.protocoltests.shared#DateTime
use smithy.test#httpResponseTests

// These tests verify that clients can parse `DateTime` timestamps with fractional seconds.
@tags(["client-only"])
operation FractionalSeconds {
    output: FractionalSecondsOutput
}

apply FractionalSeconds @httpResponseTests([
    {
        id: "Ec2QueryDateTimeWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse datetime timestamps with fractional seconds""",
        protocol: ec2Query,
        code: 200,
        body: """
              <FractionalSecondsResponse xmlns="https://example.com/">
                  <datetime>2000-01-02T20:34:56.123Z</datetime>
                  <RequestId>requestid</RequestId>
              </FractionalSecondsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        params: { datetime: 946845296.123 }
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
}
