$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedBlob", method: "POST")
operation MalformedBlob {
    input: MalformedBlobInput
}

apply MalformedBlob @httpMalformedRequestTests([
    {
        id: "RestJsonBodyMalformedBlobInvalidBase64",
        documentation: """
        When a blob member is not properly base64 encoded, or not encoded at
        all, the response should be a 400 SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedBlob",
            body: """
            { "blob" : $value:L }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            value: ["blob", "\"xyz\"", "\"YmxvYg=\"", "[98, 108, 11, 98]",
                    "[\"b\", \"l\",\"o\",\"b\"]", "981081198", "true", "[][]", "-_=="]
        }
    },
])

structure MalformedBlobInput {
    blob: Blob,
}


