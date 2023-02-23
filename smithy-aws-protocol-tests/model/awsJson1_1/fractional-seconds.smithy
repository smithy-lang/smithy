$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
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
        id: "AwsJson11DateTimeWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse datetime timestamps with fractional seconds""",
        protocol: awsJson1_1,
        code: 200,
        body:
        """
              {
                  "datetime": "2000-01-02T20:34:56.123Z"
              }
        """,
        params: { datetime: 946845296.123 }
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
        },
        appliesTo: "client"
    },
    {
        id: "AwsJson11HttpDateWithFractionalSeconds",
        documentation: """
        Ensures that clients can correctly parse http-date timestamps with fractional seconds""",
        protocol: awsJson1_1,
        code: 200,
        body:
        """
              {
                  "httpdate": "Sun, 02 Jan 2000 20:34:56.456 GMT"
              }
        """,
        params: { httpdate: 946845296.456 }
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1"
        },
        appliesTo: "client"
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
    httpdate: HttpDate
}
