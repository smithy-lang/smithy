$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedString", method: "POST")
operation MalformedString {
    input: MalformedStringInput
}

apply MalformedString @httpMalformedRequestTests([
    {
        id: "RestJsonHeaderMalformedStringInvalidBase64MediaType",
        documentation: """
        When string with the mediaType trait is bound to a header, its value
        must be base64 encoded. The server should reject values that aren't
        valid base64 out of hand.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedString",
            headers: {
                "content-type": "application/json",
                "amz-media-typed-header": "$value:L",
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            value: [
                // Insufficient padding
                "xyz",
                // Extant, but also insufficient padding
                "YmxvYg=",
                // Invalid characters
                "[][]",
                // Invalid characters which are commonly used as filename-safe
                // alternatives to + and /
                "-_=="
            ]
        }
    },
])

structure MalformedStringInput {
    @httpHeader("amz-media-typed-header")
    blob: JsonHeaderString,
}

@mediaType("application/json")
string JsonHeaderString
