// This file defines test cases that test the basics of input serialization.

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#EpochSeconds
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#IntegerEnum
use smithy.test#httpRequestTests

/// This test serializes strings, numbers, and boolean values.
operation SimpleInputParams {
    input: SimpleInputParamsInput
}

apply SimpleInputParams @httpRequestTests([
    {
        id: "QuerySimpleInputParamsStrings",
        documentation: "Serializes strings",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&Foo=val1&Bar=val2",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Foo: "val1",
            Bar: "val2"
        }
    },
    {
        id: "QuerySimpleInputParamsStringAndBooleanTrue",
        documentation: "Serializes booleans that are true",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&Foo=val1&Baz=true",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Foo: "val1",
            Baz: true,
        }
    },
    {
        id: "QuerySimpleInputParamsStringsAndBooleanFalse",
        documentation: "Serializes booleans that are false",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&Baz=false",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Baz: false,
        }
    },
    {
        id: "QuerySimpleInputParamsInteger",
        documentation: "Serializes integers",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&Bam=10",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Bam: 10,
        }
    },
    {
        id: "QuerySimpleInputParamsFloat",
        documentation: "Serializes floats",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&Boo=10.8",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Boo: 10.8,
        }
    },
    {
        id: "QuerySimpleInputParamsBlob",
        documentation: "Blobs are base64 encoded in the query string",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&Qux=dmFsdWU%3D",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Qux: "value",
        }
    },
    {
        id: "QueryEnums",
        documentation: "Serializes enums in the query string",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&FooEnum=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FooEnum: "Foo",
        }
    },
    {
        id: "QueryIntEnums",
        documentation: "Serializes intEnums in the query string",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&IntegerEnum=1",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            IntegerEnum: 1,
        }
    },
    {
        id: "AwsQuerySupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        body: "Action=SimpleInputParams&Version=2020-01-08&FloatValue=NaN&Boo=NaN",
        bodyMediaType: "application/x-www-form-urlencoded",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            FloatValue: "NaN",
            Boo: "NaN",
        }
    },
    {
        id: "AwsQuerySupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        body: "Action=SimpleInputParams&Version=2020-01-08&FloatValue=Infinity&Boo=Infinity",
        bodyMediaType: "application/x-www-form-urlencoded",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            FloatValue: "Infinity",
            Boo: "Infinity",
        }
    },
    {
        id: "AwsQuerySupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        body: "Action=SimpleInputParams&Version=2020-01-08&FloatValue=-Infinity&Boo=-Infinity",
        bodyMediaType: "application/x-www-form-urlencoded",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            FloatValue: "-Infinity",
            Boo: "-Infinity",
        }
    },
])

structure SimpleInputParamsInput {
    Foo: String,
    Bar: String,
    Baz: Boolean,
    Bam: Integer,
    FloatValue: Float,
    Boo: Double,
    Qux: Blob,
    FooEnum: FooEnum,
    IntegerEnum: IntegerEnum
}

/// This test serializes timestamps.
///
/// 1. Timestamps are serialized as RFC 3339 date-time values by default.
/// 2. A timestampFormat trait on a member changes the format.
/// 3. A timestampFormat trait on the shape targeted by the member changes the format.
operation QueryTimestamps {
    input: QueryTimestampsInput
}

apply QueryTimestamps @httpRequestTests([
    {
        id: "QueryTimestampsInput",
        documentation: "Serializes timestamps",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=QueryTimestamps&Version=2020-01-08&normalFormat=2015-01-25T08%3A00%3A00Z&epochMember=1422172800&epochTarget=1422172800",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            normalFormat: 1422172800,
            epochMember: 1422172800,
            epochTarget: 1422172800,
        }
    }
])

structure QueryTimestampsInput {
    // Timestamps are serialized as RFC 3339 date-time values by default.
    normalFormat: Timestamp,

    // This member has the timestampFormat trait, so it changes the
    // timestamp serialization.
    @timestampFormat("epoch-seconds")
    epochMember: Timestamp,

    // The targeted shape has the timestampFormat trait, so it changes the
    // timestamp serialization.
    epochTarget: EpochSeconds,
}

/// This test serializes nested and recursive structure members.
operation NestedStructures {
    input: NestedStructuresInput
}

apply NestedStructures @httpRequestTests([
    {
        id: "NestedStructures",
        documentation: "Serializes nested structures using dots",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=NestedStructures&Version=2020-01-08&Nested.StringArg=foo&Nested.OtherArg=true&Nested.RecursiveArg.StringArg=baz",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Nested: {
                StringArg: "foo",
                OtherArg: true,
                RecursiveArg: {
                    StringArg: "baz"
                }
            }
        }
    }
])

structure NestedStructuresInput {
    Nested: StructArg,
}

structure StructArg {
    StringArg: String,
    OtherArg: Boolean,
    RecursiveArg: StructArg,
}

/// Automatically adds idempotency tokens.
@tags(["client-only"])
operation QueryIdempotencyTokenAutoFill {
    input: QueryIdempotencyTokenAutoFillInput
}

apply QueryIdempotencyTokenAutoFill @httpRequestTests([
    {
        id: "QueryProtocolIdempotencyTokenAutoFill",
        documentation: "Automatically adds idempotency token when not set",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=QueryIdempotencyTokenAutoFill&Version=2020-01-08&token=00000000-0000-4000-8000-000000000000",
        bodyMediaType: "application/x-www-form-urlencoded",
        appliesTo: "client",
    },
    {
        id: "QueryProtocolIdempotencyTokenAutoFillIsSet",
        documentation: "Uses the given idempotency token as-is",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=QueryIdempotencyTokenAutoFill&Version=2020-01-08&token=00000000-0000-4000-8000-000000000123",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            token: "00000000-0000-4000-8000-000000000123"
        },
        appliesTo: "client",
    }
])

structure QueryIdempotencyTokenAutoFillInput {
    @idempotencyToken
    token: String,
}
