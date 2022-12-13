$version: "2.0"

namespace aws.protocoltests.restjson.validation

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use smithy.framework#ValidationException

@suppress(["UnstableTrait"])
@http(uri: "/SensitiveValidation", method: "POST")
operation SensitiveValidation {
    input: SensitiveValidationInput,
    errors: [ValidationException]
}

apply SensitiveValidation @httpMalformedRequestTests([
    {
        id: "RestJsonMalformedPatternSensitiveString",
        documentation: """
        When a sensitive member fails validation, the resultant
        ValidationException will omit the value of the input.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/SensitiveValidation",
            body: """
            { "string" : "ABC" }""",
            headers: {
                "content-type": "application/json"
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
                    { "message" : "1 validation error detected. Value at '/string' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$",
                      "fieldList" : [{"message": "Value at '/string' failed to satisfy constraint: Member must satisfy regular expression pattern: ^[a-m]+$", "path": "/string"}]}"""
                }
            }
        }
    },
])

structure SensitiveValidationInput {
    string: SensitivePatternString
}

@sensitive
@pattern("^[a-m]+$")
string SensitivePatternString
