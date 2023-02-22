$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#DateTime
use aws.protocoltests.shared#HttpDate
use smithy.test#httpResponseTests

// These tests are for verifying the client can correctly parse
// the `DateTime` and `HttpDate` timestamps with fractional seconds
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
    {
        id: "AwsQueryHttpDateWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse http-date timestamps with fractional seconds""",
        protocol: awsQuery,
        code: 200,
        body: """
              <FractionalSecondsResponse xmlns="https://example.com/">
                  <FractionalSecondsResult>
                      <httpdate>Sun, 02 Jan 2000 20:34:56.456 GMT</httpdate>
                  </FractionalSecondsResult>
              </FractionalSecondsResponse>
              """,
        params: { httpdate: 946845296.456 }
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        appliesTo: "client"
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
    httpdate: HttpDate
}
