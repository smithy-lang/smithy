$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/MalformedRequired", method: "POST")
operation MalformedRequired {
    input: MalformedRequiredInput,
    errors: [ValidationException]
}

apply MalformedRequired @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedRequiredBodyUnset",
        documentation: """
        When a required member is not set in the message body,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRequired",
            body: """
            {  }""",
            queryParams: [
                "stringInQuery=abc"
            ],
            headers: {
                "content-type": "application/json",
                "string-in-headers": "abc"

            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/string' failed to satisfy constraint: Member must not be null",
                      "fieldList" : [{"message": "Value at '/string' failed to satisfy constraint: Member must not be null", "path": "/string"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRequiredBodyExplicitNull",
        documentation: """
        When a required member is set to null in the message body,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRequired",
            body: """
            { "string": null }""",
            queryParams: [
                "stringInQuery=abc"
            ],
            headers: {
                "content-type": "application/json",
                "string-in-headers": "abc"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/string' failed to satisfy constraint: Member must not be null",
                      "fieldList" : [{"message": "Value at '/string' failed to satisfy constraint: Member must not be null", "path": "/string"}]}"""
                }
            }
        }
    },
    {
        id: "RestJsonMalformedRequiredHeaderUnset",
        documentation: """
        When a required member is not set in headers,
        the response should be a 400 ValidationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRequired",
            body: """
            { "string": "abc" }""",
            queryParams: [
                "stringInQuery=abc"
            ],
            headers: {
                "content-type": "application/json"
            },
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "ValidationException"
            },
            body: {
                mediaType: "application/json",
                assertion: {
                    contents: """
                    { "message" : "1 validation error detected. Value at '/stringInHeader' failed to satisfy constraint: Member must not be null",
                      "fieldList" : [{"message": "Value at '/stringInHeader' failed to satisfy constraint: Member must not be null", "path": "/stringInHeader"}]}"""
                }
            }
        }
    }
])

structure MalformedRequiredInput {
    @required
    string: String,

    @required
    @httpQuery("stringInQuery")
    stringInQuery: String,

    @required
    @httpHeader("string-in-headers")
    stringInHeader: String
}
