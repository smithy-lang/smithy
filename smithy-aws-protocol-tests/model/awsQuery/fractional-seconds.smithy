$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#DateTime
use smithy.test#httpResponseTests

// These tests verify that clients can parse `DateTime` timestamps with fractional seconds.
@tags(["client-only"])
operation FractionalSeconds {
    output: FractionalSecondsOutput
}

apply FractionalSeconds @httpResponseTests([
    {
        id: "AwsQueryDateTimeWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse datetime timestamps with fractional seconds""",
        protocol: awsQuery,
        code: 200,
        body: """
              <FractionalSecondsResponse xmlns="https://example.com/">
                  <FractionalSecondsResult>
                      <datetime>2000-01-02T20:34:56.123Z</datetime>
                  </FractionalSecondsResult>
              </FractionalSecondsResponse>
              """,
        params: { datetime: 946845296.123 }
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        appliesTo: "client"
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
}
