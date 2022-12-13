$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedRequestBody", method: "POST")
operation MalformedRequestBody {
    input: MalformedRequestBodyInput
}

apply MalformedRequestBody @httpMalformedRequestTests([
    {
        id: "RestJsonInvalidJsonBody",
        documentation: """
        When the request body is not valid JSON, the response should be a 400
        SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRequestBody",
            body: "$value:L",
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
            "value": ["{[",
                      "{ \"int\": 10 }abc",
                      "abc{ \"int\": 10 }",
                      """
                      {
                          "int": 10 // the integer should be 10
                      }""",
                      """
                      {
                          "int": 10 /* the integer should be 10 */
                      }""",
                      "{\"int\" :\u000c10}",
                      "{'int': 10}",
                      "{\"int\": 10,}",
           ]
        }
    },
    {
        id: "RestJsonTechnicallyValidJsonBody",
        documentation: """
        When the request body is technically valid, but cannot map to a Smithy structure,
        the response should be a 400 SerializationException.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedRequestBody",
            body: "$value:L",
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
            value: ["[{ \"int\": 10}]", "10", "null"]
        },
        tags: ["technically_valid_json_body"]
    },
])

structure MalformedRequestBodyInput {
    int: Integer,
    float: Float
}

