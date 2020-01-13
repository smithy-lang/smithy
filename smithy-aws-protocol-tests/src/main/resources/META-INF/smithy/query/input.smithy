// This file defines test cases that test the basics of input serialization.

$version: "0.5.0"

namespace aws.protocols.tests.query

use aws.protocols.tests.shared#EpochSeconds
use aws.protocols.tests.shared#FooEnum
use smithy.test#httpRequestTests

/// This test serializes strings, numbers, and boolean values.
operation SimpleInputParams(SimpleInputParamsInput)

apply SimpleInputParams @httpRequestTests([
    {
        id: "QuerySimpleInputParamsStrings",
        description: "Serializes strings",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &Foo=val1
              &Bar=val2""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Foo: "val1",
            Bar: "val2"
        }
    },
    {
        id: "QuerySimpleInputParamsStringAndBooleanTrue",
        description: "Serializes booleans that are true",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &Foo=val
              &Baz=true""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Foo: "val1",
            Baz: true,
        }
    },
    {
        id: "QuerySimpleInputParamsStringsAndBooleanFalse",
        description: "Serializes booleans that are false",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &Foo=val1
              &Baz=false""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Baz: false,
        }
    },
    {
        id: "QuerySimpleInputParamsInteger",
        description: "Serializes integers",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &Bam=10""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Bam: 10,
        }
    },
    {
        id: "QuerySimpleInputParamsFloat",
        description: "Serializes floats",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &Boo=10.8""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Boo: 10.8,
        }
    },
    {
        id: "QuerySimpleInputParamsBlob",
        description: "Blobs are base64 encoded in the query string",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &Qux=dmFsdWU%3D""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            Qux: "value",
        }
    },
    {
        id: "QueryEnums",
        description: "Serializes enums in the query string",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=SimpleInputParams
              &Version=2020-01-08
              &FooEnum=Foo""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FooEnum: "Foo",
        }
    }
])

structure SimpleInputParamsInput {
    Foo: String,
    Bar: String,
    Baz: Boolean,
    Bam: Integer,
    Boo: Double,
    Qux: Blob,
    FooEnum: FooEnum,
}

/// This test serializes timestamps.
///
/// 1. Timestamps are serialized as RFC 3339 date-time values by default.
/// 2. A timestampFormat trait on a member changes the format.
/// 3. A timestampFormat trait on the shape targeted by the member changes the format.
operation QueryTimestamps(QueryTimestampsInput)

apply QueryTimestamps @httpRequestTests([
    {
        id: "QueryTimestampsInput",
        description: "Serializes timestamps",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryTimestamps
              &Version=2020-01-08
              &normalFormat=2015-01-25T08%3A00%3A00Z
              &epochMember=1422172800
              &epochTarget=1422172800""",
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
operation NestedStructures(NestedStructuresInput)

apply NestedStructures @httpRequestTests([
    {
        id: "NestedStructures",
        description: "Serializes nested structures using dots",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=NestedStructures
              &Version=2020-01-08
              &Nested.StringArg=foo
              &Nested.OtherArg=true
              &Nested.RecursiveArg.StringArg=baz""",
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
operation QueryIdempotencyTokenAutoFill(QueryIdempotencyTokenAutoFillInput)

apply QueryIdempotencyTokenAutoFill @httpRequestTests([
    {
        id: "QueryProtocolIdempotencyTokenAutoFill",
        description: "Automatically adds idempotency token when not set",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=NestedStructures
              &Version=2020-01-08
              &token=00000000-0000-4000-8000-000000000000""",
        bodyMediaType: "application/x-www-form-urlencoded",
    },
    {
        id: "QueryProtocolIdempotencyTokenAutoFillIsSet",
        description: "Uses the given idempotency token as-is",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=NestedStructures
              &Version=2020-01-08
              &token=00000000-0000-4000-8000-000000000123""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            token: "00000000-0000-4000-8000-000000000123"
        }
    }
])

structure QueryIdempotencyTokenAutoFillInput {
    @idempotencyToken
    token: String,
}
