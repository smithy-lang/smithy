// This file defines tests to ensure that implementations support endpoints with paths

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "QueryHostWithPath",
        documentation: """
                Custom endpoints supplied by users can have paths""",
        protocol: awsQuery,
        method: "POST",
        uri: "/custom/",
        body: "Action=HostWithPathOperation&Version=2020-01-08",
        host: "example.com/custom",
        appliesTo: "client"
    }
])

operation HostWithPathOperation {}
