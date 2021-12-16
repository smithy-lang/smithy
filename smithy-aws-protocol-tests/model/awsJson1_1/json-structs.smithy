// This file defines test cases that serialize structures. Over time this
// will take over much of what is in kitchen-sink as it gets refactored
// to not put everything into such a small number of tests.

$version: "2.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "AwsJson11SupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: """
            {
                "floatValue": "NaN",
                "doubleValue": "NaN"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.SimpleScalarProperties",
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "AwsJson11SupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: """
            {
                "floatValue": "Infinity",
                "doubleValue": "Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.SimpleScalarProperties",
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "AwsJson11SupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: awsJson1_1,
        method: "POST",
        uri: "/",
        body: """
            {
                "floatValue": "-Infinity",
                "doubleValue": "-Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
            "X-Amz-Target": "JsonProtocol.SimpleScalarProperties",
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

apply SimpleScalarProperties @httpResponseTests([
    {
        id: "AwsJson11SupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "floatValue": "NaN",
                "doubleValue": "NaN"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "AwsJson11SupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "floatValue": "Infinity",
                "doubleValue": "Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "AwsJson11SupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: awsJson1_1,
        code: 200,
        body: """
            {
                "floatValue": "-Infinity",
                "doubleValue": "-Infinity"
            }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/x-amz-json-1.1",
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

// This example serializes simple scalar types in the top level JSON document.
operation SimpleScalarProperties {
    input: SimpleScalarPropertiesInputOutput,
    output: SimpleScalarPropertiesInputOutput
}

structure SimpleScalarPropertiesInputOutput {
    floatValue: Float,
    doubleValue: Double,
}
