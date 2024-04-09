$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use aws.protocoltests.shared#DateTime
use smithy.test#httpResponseTests

// These tests verify that clients can parse `DateTime` timestamps with fractional seconds.
@tags(["client-only"])
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
    }
])

structure FractionalSecondsOutput {
    datetime: DateTime
}
