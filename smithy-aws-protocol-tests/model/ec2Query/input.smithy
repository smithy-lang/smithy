// This file defines test cases that test the basics of input serialization.

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2QueryName
use aws.protocols#ec2Query
use aws.protocoltests.shared#EpochSeconds
use aws.protocoltests.shared#FooEnum
use smithy.test#httpRequestTests

/// This test serializes strings, numbers, and boolean values.
operation SimpleInputParams {
    input: SimpleInputParamsInput
}

apply SimpleInputParams @httpRequestTests([
    {
        id: "Ec2SimpleInputParamsStrings",
        documentation: "Serializes strings",
        protocol: ec2Query,
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
        id: "Ec2SimpleInputParamsStringAndBooleanTrue",
        documentation: "Serializes booleans that are true",
        protocol: ec2Query,
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
        id: "Ec2SimpleInputParamsStringsAndBooleanFalse",
        documentation: "Serializes booleans that are false",
        protocol: ec2Query,
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
        id: "Ec2SimpleInputParamsInteger",
        documentation: "Serializes integers",
        protocol: ec2Query,
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
        id: "Ec2SimpleInputParamsFloat",
        documentation: "Serializes floats",
        protocol: ec2Query,
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
        id: "Ec2SimpleInputParamsBlob",
        documentation: "Blobs are base64 encoded in the query string",
        protocol: ec2Query,
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
        id: "Ec2Enums",
        documentation: "Serializes enums in the query string",
        protocol: ec2Query,
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
        id: "Ec2Query",
        documentation: "Serializes query using ec2QueryName trait.",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&HasQueryName=Hi",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            HasQueryName: "Hi",
        }
    },
    {
        id: "Ec2QueryIsPreferred",
        documentation: "ec2QueryName trait is preferred over xmlName.",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&HasQueryAndXmlName=Hi",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            HasQueryAndXmlName: "Hi",
        }
    },
    {
        id: "Ec2XmlNameIsUppercased",
        documentation: "xmlName is used with the ec2 protocol, but the first character is uppercased",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=SimpleInputParams&Version=2020-01-08&UsesXmlName=Hi",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            UsesXmlName: "Hi",
        }
    },
    {
        id: "Ec2QuerySupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: ec2Query,
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
        id: "Ec2QuerySupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: ec2Query,
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
        id: "Ec2QuerySupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: ec2Query,
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

    @ec2QueryName("HasQueryName")
    HasQueryName: String,

    @ec2QueryName("HasQueryAndXmlName")
    @xmlName("hasQueryAndXmlName")
    HasQueryAndXmlName: String,

    @xmlName("usesXmlName")
    UsesXmlName: String,
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
        id: "Ec2TimestampsInput",
        documentation: "Serializes timestamps",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=QueryTimestamps&Version=2020-01-08&NormalFormat=2015-01-25T08%3A00%3A00Z&EpochMember=1422172800&EpochTarget=1422172800",
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
        id: "Ec2NestedStructures",
        documentation: "Serializes nested structures using dots",
        protocol: ec2Query,
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
        id: "Ec2ProtocolIdempotencyTokenAutoFill",
        documentation: "Automatically adds idempotency token when not set",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=QueryIdempotencyTokenAutoFill&Version=2020-01-08&Token=00000000-0000-4000-8000-000000000000",
        bodyMediaType: "application/x-www-form-urlencoded",
        appliesTo: "client",
    },
    {
        id: "Ec2ProtocolIdempotencyTokenAutoFillIsSet",
        documentation: "Uses the given idempotency token as-is",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        requireHeaders: [
            "Content-Length"
        ],
        body: "Action=QueryIdempotencyTokenAutoFill&Version=2020-01-08&Token=00000000-0000-4000-8000-000000000123",
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
