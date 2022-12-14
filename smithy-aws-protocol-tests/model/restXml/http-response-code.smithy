// This file defines test cases that test HTTP response code bindings.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpresponsecode-trait

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpResponseTests

@idempotent
@http(uri: "/HttpResponseCode", method: "PUT")
operation HttpResponseCode {
    output: HttpResponseCodeOutput
}

structure HttpResponseCodeOutput {
    @httpResponseCode
    Status: Integer
}

apply HttpResponseCode @httpResponseTests([
    {
        id: "RestXmlHttpResponseCode",
        documentation: "Binds the http response code to an output structure.",
        protocol: restXml,
        code: 201,
        headers: {
            "Content-Type": "application/xml"
        },
        "body": "",
        "bodyMediaType": "application/xml",
        params: {
            Status: 201,
        }
    }
])
