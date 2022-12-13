$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedBoolean/{booleanInPath}", method: "POST")
operation MalformedBoolean {
    input: MalformedBooleanInput
}

apply MalformedBoolean @httpMalformedRequestTests([
    {
        id: "RestJsonBodyBooleanStringCoercion",
        documentation: """
        Attempted string coercion should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedBoolean/true",
            body: """
            { "booleanInBody" : $value:S }""",
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
            "value" : ["true", "True", "TRUE", "y", "Y", "yes", "Yes", "YES", "1", "on", "On", "ON",
                       "false", "False", "FALSE", "n", "N", "no", "No", "NO", "0", "off", "Off", "OFF"]
        }
    },
    {
        id: "RestJsonBodyBooleanBadLiteral",
        documentation: """
        YAML-style alternate boolean literals should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedBoolean/true",
            body: """
            { "booleanInBody" : $value:L }""",
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
            "value" : ["True", "TRUE", "y", "Y", "yes", "Yes", "YES", "1", "on", "On", "ON",
                       "False", "FALSE", "n", "N", "no", "No", "NO", "0", "off", "Off", "OFF"]
        }
    },
    {
        id: "RestJsonPathBooleanStringCoercion",
        documentation: """
        Attempted string coercion should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedBoolean/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["True", "TRUE", "y", "Y", "yes", "Yes", "YES", "1", "on", "On", "ON",
                       "False", "FALSE", "n", "N", "no", "No", "NO", "0", "off", "Off", "OFF"]
        }
    },
    {
        id: "RestJsonQueryBooleanStringCoercion",
        documentation: """
        Attempted string coercion should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedBoolean/true",
            queryParams: [
                "booleanInQuery=$value:L"
            ]
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["True", "TRUE", "y", "Y", "yes", "Yes", "YES", "1", "on", "On", "ON",
                       "False", "FALSE", "n", "N", "no", "No", "NO", "0", "off", "Off", "OFF"]
        }
    },
    {
        id: "RestJsonHeaderBooleanStringCoercion",
        documentation: """
        Attempted string coercion should result in SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedBoolean/true",
            headers: {
                "booleanInHeader" : "$value:L"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["True", "TRUE", "y", "Y", "yes", "Yes", "YES", "1", "on", "On", "ON",
                       "False", "FALSE", "n", "N", "no", "No", "NO", "0", "off", "Off", "OFF"]
        }
    }
])

structure MalformedBooleanInput {
    booleanInBody: Boolean,

    @httpLabel
    @required
    booleanInPath: Boolean,

    @httpQuery("booleanInQuery")
    booleanInQuery: Boolean,

    @httpHeader("booleanInHeader")
    booleanInHeader: Boolean
}

