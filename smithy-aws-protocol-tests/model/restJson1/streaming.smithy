// This file defines test cases that test HTTP streaming bindings.
// See: https://smithy.io/2.0/spec/streaming.html

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This examples serializes a streaming blob shape in the request body.
///
/// In this example, no JSON document is synthesized because the payload is
/// not a structure or a union type.
@http(uri: "/StreamingTraits", method: "POST")
operation StreamingTraits {
    input: StreamingTraitsInputOutput,
    output: StreamingTraitsInputOutput
}

apply StreamingTraits @httpRequestTests([
    {
        id: "RestJsonStreamingTraitsWithBlob",
        documentation: "Serializes a blob in the HTTP payload",
        protocol: restJson1,
        method: "POST",
        uri: "/StreamingTraits",
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "application/octet-stream"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    },
    {
        id: "RestJsonStreamingTraitsWithNoBlobBody",
        documentation: "Serializes an empty blob in the HTTP payload",
        protocol: restJson1,
        method: "POST",
        uri: "/StreamingTraits",
        body: "",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo"
        }
    },
])

apply StreamingTraits @httpResponseTests([
    {
        id: "RestJsonStreamingTraitsWithBlob",
        documentation: "Serializes a blob in the HTTP payload",
        protocol: restJson1,
        code: 200,
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "application/octet-stream"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    },
    {
        id: "RestJsonStreamingTraitsWithNoBlobBody",
        documentation: "Serializes an empty blob in the HTTP payload",
        protocol: restJson1,
        code: 200,
        body: "",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo"
        }
    }
])

structure StreamingTraitsInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: StreamingBlob = "",
}

@streaming
blob StreamingBlob

/// This examples serializes a streaming blob shape with a required content
/// length in the request body.
///
/// In this example, no JSON document is synthesized because the payload is
/// not a structure or a union type.
@http(uri: "/StreamingTraitsRequireLength", method: "POST")
operation StreamingTraitsRequireLength {
    input: StreamingTraitsRequireLengthInput
}

apply StreamingTraitsRequireLength @httpRequestTests([
    {
        id: "RestJsonStreamingTraitsRequireLengthWithBlob",
        documentation: "Serializes a blob in the HTTP payload with a required length",
        protocol: restJson1,
        method: "POST",
        uri: "/StreamingTraitsRequireLength",
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "application/octet-stream"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    },
    {
        id: "RestJsonStreamingTraitsRequireLengthWithNoBlobBody",
        documentation: "Serializes an empty blob in the HTTP payload",
        protocol: restJson1,
        method: "POST",
        uri: "/StreamingTraitsRequireLength",
        body: "",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo"
        }
    },
])

@input
structure StreamingTraitsRequireLengthInput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: FiniteStreamingBlob = "",
}

@streaming
@requiresLength
blob FiniteStreamingBlob

/// This examples serializes a streaming media-typed blob shape in the request body.
///
/// This examples uses a `@mediaType` trait on the payload to force a custom
/// content-type to be serialized.
@http(uri: "/StreamingTraitsWithMediaType", method: "POST")
operation StreamingTraitsWithMediaType {
    input: StreamingTraitsWithMediaTypeInputOutput,
    output: StreamingTraitsWithMediaTypeInputOutput
}

apply StreamingTraitsWithMediaType @httpRequestTests([
    {
        id: "RestJsonStreamingTraitsWithMediaTypeWithBlob",
        documentation: "Serializes a blob in the HTTP payload with a content-type",
        protocol: restJson1,
        method: "POST",
        uri: "/StreamingTraitsWithMediaType",
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "text/plain"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    }
])

apply StreamingTraitsWithMediaType @httpResponseTests([
    {
        id: "RestJsonStreamingTraitsWithMediaTypeWithBlob",
        documentation: "Serializes a blob in the HTTP payload with a content-type",
        protocol: restJson1,
        code: 200,
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "text/plain"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    }
])

structure StreamingTraitsWithMediaTypeInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: StreamingTextPlainBlob = ""
}

@streaming
@mediaType("text/plain")
blob StreamingTextPlainBlob
