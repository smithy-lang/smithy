// This file defines tests to ensure that implementations support endpoints with paths

$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "AwsJson11HostWithPath",
        documentation: """
                Custom endpoints supplied by users can have paths""",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/custom/",
        body: "{}",
        host: "example.com/custom",
        appliesTo: "client"
    }
])

operation HostWithPathOperation {}
