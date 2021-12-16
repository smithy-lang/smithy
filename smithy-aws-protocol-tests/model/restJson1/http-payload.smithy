// This file defines test cases that test HTTP payload bindings.
// See: https://awslabs.github.io/smithy/1.0/spec/http.html#httppayload-trait

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#TextPlainBlob
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This examples serializes a blob shape in the payload.
///
/// In this example, no JSON document is synthesized because the payload is
/// not a structure or a union type.
@http(uri: "/HttpPayloadTraits", method: "POST")
operation HttpPayloadTraits {
    input: HttpPayloadTraitsInputOutput,
    output: HttpPayloadTraitsInputOutput
}

apply HttpPayloadTraits @httpRequestTests([
    {
        id: "RestJsonHttpPayloadTraitsWithBlob",
        documentation: "Serializes a blob in the HTTP payload",
        protocol: restJson1,
        method: "POST",
        uri: "/HttpPayloadTraits",
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "Content-Type": "application/octet-stream",
            "X-Foo": "Foo"
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
        id: "RestJsonHttpPayloadTraitsWithNoBlobBody",
        documentation: "Serializes an empty blob in the HTTP payload",
        protocol: restJson1,
        method: "POST",
        uri: "/HttpPayloadTraits",
        body: "",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo"
        }
    },
    {
        id: "RestJsonHttpPayloadTraitsWithBlobAcceptsAllContentTypes",
        documentation: """
            Servers must accept any content type for blob inputs
            without the media type trait.""",
        protocol: restJson1,
        method: "POST",
        uri: "/HttpPayloadTraits",
        body: "This is definitely a jpeg",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "image/jpeg"
        },
        params: {
            foo: "Foo",
            blob: "This is definitely a jpeg"
        },
        appliesTo: "server",
    },
    {
        id: "RestJsonHttpPayloadTraitsWithBlobAcceptsAllAccepts",
        documentation: """
            Servers must accept any accept header for blob inputs
            without the media type trait.""",
        protocol: restJson1,
        method: "POST",
        uri: "/HttpPayloadTraits",
        body: "This is definitely a jpeg",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Accept": "image/jpeg"
        },
        params: {
            foo: "Foo",
            blob: "This is definitely a jpeg"
        },
        appliesTo: "server",
    },
])

apply HttpPayloadTraits @httpResponseTests([
    {
        id: "RestJsonHttpPayloadTraitsWithBlob",
        documentation: "Serializes a blob in the HTTP payload",
        protocol: restJson1,
        code: 200,
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo"
        },
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    },
    {
        id: "RestJsonHttpPayloadTraitsWithNoBlobBody",
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
    },
])

structure HttpPayloadTraitsInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: Blob,
}

/// This examples uses a `@mediaType` trait on the payload to force a custom
/// content-type to be serialized.
@http(uri: "/HttpPayloadTraitsWithMediaType", method: "POST")
operation HttpPayloadTraitsWithMediaType {
    input: HttpPayloadTraitsWithMediaTypeInputOutput,
    output: HttpPayloadTraitsWithMediaTypeInputOutput
}

apply HttpPayloadTraitsWithMediaType @httpRequestTests([
    {
        id: "RestJsonHttpPayloadTraitsWithMediaTypeWithBlob",
        documentation: "Serializes a blob in the HTTP payload with a content-type",
        protocol: restJson1,
        method: "POST",
        uri: "/HttpPayloadTraitsWithMediaType",
        body: "blobby blob blob",
        bodyMediaType: "application/octet-stream",
        headers: {
            "X-Foo": "Foo",
            "Content-Type": "text/plain"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            foo: "Foo",
            blob: "blobby blob blob"
        }
    }
])

apply HttpPayloadTraitsWithMediaType @httpResponseTests([
    {
        id: "RestJsonHttpPayloadTraitsWithMediaTypeWithBlob",
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

structure HttpPayloadTraitsWithMediaTypeInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    @httpPayload
    blob: TextPlainBlob,
}

/// This examples serializes a structure in the payload.
///
/// Note that serializing a structure changes the wrapper element name
/// to match the targeted structure.
@idempotent
@http(uri: "/HttpPayloadWithStructure", method: "PUT")
operation HttpPayloadWithStructure {
    input: HttpPayloadWithStructureInputOutput,
    output: HttpPayloadWithStructureInputOutput
}

apply HttpPayloadWithStructure @httpRequestTests([
    {
        id: "RestJsonHttpPayloadWithStructure",
        documentation: "Serializes a structure in the payload",
        protocol: restJson1,
        method: "PUT",
        uri: "/HttpPayloadWithStructure",
        body: """
              {
                  "greeting": "hello",
                  "name": "Phreddy"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                greeting: "hello",
                name: "Phreddy"
            }
        }
    }
])

apply HttpPayloadWithStructure @httpResponseTests([
    {
        id: "RestJsonHttpPayloadWithStructure",
        documentation: "Serializes a structure in the payload",
        protocol: restJson1,
        code: 200,
        body: """
              {
                  "greeting": "hello",
                  "name": "Phreddy"
              }""",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json"
        },
        params: {
            nested: {
                greeting: "hello",
                name: "Phreddy"
            }
        }
    }
])

structure HttpPayloadWithStructureInputOutput {
    @httpPayload
    nested: NestedPayload,
}

structure NestedPayload {
    greeting: String,
    name: String,
}
