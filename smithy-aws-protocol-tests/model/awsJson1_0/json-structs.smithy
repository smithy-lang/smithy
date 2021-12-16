// This file defines test cases that serialize structures.

$version: "2.0"

namespace aws.protocoltests.json10

use aws.protocols#awsJson1_0
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "AwsJson10SupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: """
            {
                "floatValue": "NaN",
                "doubleValue": "NaN"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.SimpleScalarProperties",
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "AwsJson10SupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: """
            {
                "floatValue": "Infinity",
                "doubleValue": "Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.SimpleScalarProperties",
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "AwsJson10SupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: awsJson1_0,
        method: "POST",
        uri: "/",
        body: """
            {
                "floatValue": "-Infinity",
                "doubleValue": "-Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
            "X-Amz-Target": "JsonRpc10.SimpleScalarProperties",
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

apply SimpleScalarProperties @httpResponseTests([
    {
        id: "AwsJson10SupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "floatValue": "NaN",
                "doubleValue": "NaN"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "AwsJson10SupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "floatValue": "Infinity",
                "doubleValue": "Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "AwsJson10SupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: awsJson1_0,
        code: 200,
        body: """
            {
                "floatValue": "-Infinity",
                "doubleValue": "-Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.0",
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

// This example serializes simple scalar types in the top level JSON document.
operation SimpleScalarProperties {
    input: SimpleScalarPropertiesInput,
    output: SimpleScalarPropertiesOutput
}

@input
structure SimpleScalarPropertiesInput {
    floatValue: Float,
    doubleValue: Double,
}

@output
structure SimpleScalarPropertiesOutput {
    floatValue: Float,
    doubleValue: Double,
}
