// This file defines tests to ensure that implementations support endpoints with paths

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests

@httpRequestTests([
    {
        id: "RestJsonHostWithPath",
        documentation: """
                Custom endpoints supplied by users can have paths""",
        protocol: restJson1,
        method: "GET",
        uri: "/custom/HostWithPathOperation",
        body: "",
        host: "example.com/custom",
        appliesTo: "client"
    }
])
@readonly
@http(uri: "/HostWithPathOperation", method: "GET")
operation HostWithPathOperation {}
