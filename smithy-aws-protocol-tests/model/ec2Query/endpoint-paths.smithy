// This file defines tests to ensure that implementations support endpoints with paths

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "Ec2QueryHostWithPath",
        documentation: """
                Custom endpoints supplied by users can have paths""",
        protocol: ec2Query,
        method: "POST",
        uri: "/custom/",
        body: "Action=HostWithPathOperation&Version=2020-01-08",
        host: "example.com/custom",
        appliesTo: "client"
    }
])

operation HostWithPathOperation {}
