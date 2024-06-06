$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#JpegBlob

apply MalformedContentTypeWithoutBody @httpMalformedRequestTests([
    {
        id: "RestJsonWithoutBodyExpectsEmptyContentType",
        documentation: """
        When there is no modeled input, content type must not be set and the body must be empty.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedContentTypeWithoutBody",
            body: "{}",
            headers: {
                // this should be omitted
                "content-type": "application/json"
            }
        },
        response: {
            code: 415,
            headers: {
                "x-amzn-errortype": "UnsupportedMediaTypeException"
            }
        },
        tags: [ "content-type" ]
    }
])

apply MalformedContentTypeWithBody @httpMalformedRequestTests([
    {
        id: "RestJsonWithBodyExpectsApplicationJsonContentType",
        documentation: """
        When there is modeled input, the content type must be application/json""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedContentTypeWithBody",
            body: "{}",
            headers: {
                // this should be application/json
                "content-type": "application/hal+json"
            }
        },
        response: {
            code: 415,
            headers: {
                "x-amzn-errortype": "UnsupportedMediaTypeException"
            }
        },
        tags: [ "content-type" ]
    }
    {
        id: "RestJsonWithBodyExpectsApplicationJsonContentTypeNoHeaders",
        documentation: """
        When there is modeled input, the content type must be application/json""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedContentTypeWithBody",
            body: "{}",
        },
        response: {
            code: 415,
            headers: {
                "x-amzn-errortype": "UnsupportedMediaTypeException"
            }
        },
        tags: [ "content-type" ]
    }
])

apply MalformedContentTypeWithPayload @httpMalformedRequestTests([
    {
        id: "RestJsonWithPayloadExpectsModeledContentType",
        documentation: """
        When there is a payload with a mediaType trait, the content type must match.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedContentTypeWithPayload",
            body: "{}",
            headers: {
                // this should be image/jpeg
                "content-type": "application/json"
            }
        },
        response: {
            code: 415,
            headers: {
                "x-amzn-errortype": "UnsupportedMediaTypeException"
            }
        },
        tags: [ "content-type" ]
    }
])

apply MalformedContentTypeWithPayload @httpMalformedRequestTests([
    {
        id: "RestJsonWithPayloadExpectsImpliedContentType",
        documentation: """
        When there is a payload without a mediaType trait, the content type must match the
        implied content type of the shape.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedContentTypeWithPayload",
            body: "{}",
            headers: {
                // this should be text/plain
                "content-type": "application/json"
            }
        },
        response: {
            code: 415,
            headers: {
                "x-amzn-errortype": "UnsupportedMediaTypeException"
            }
        },
        tags: [ "content-type" ]
    }
])

apply MalformedContentTypeWithoutBodyEmptyInput @httpMalformedRequestTests([
    {
        id: "RestJsonWithoutBodyEmptyInputExpectsEmptyContentType",
        documentation: """
        When there is no modeled body input, content type must not be set and the body must be empty.""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedContentTypeWithoutBodyEmptyInput",
            body: "{}",
            headers: {
                // this should be omitted
                "content-type": "application/json"
            }
        },
        response: {
            code: 415,
            headers: {
                "x-amzn-errortype": "UnsupportedMediaTypeException"
            }
        },
        tags: [ "content-type" ]
    }
])

@suppress(["UnstableTrait"])
@http(method: "POST", uri: "/MalformedContentTypeWithoutBody")
operation MalformedContentTypeWithoutBody {}

@suppress(["UnstableTrait"])
@http(method: "POST", uri: "/MalformedContentTypeWithBody")
operation MalformedContentTypeWithBody {
    input: GreetingStruct
}

@suppress(["UnstableTrait"])
@http(method: "POST", uri: "/MalformedContentTypeWithPayload")
operation MalformedContentTypeWithPayload {
    input: MalformedContentTypeWithPayloadInput
}

structure MalformedContentTypeWithPayloadInput {
    @httpPayload
    payload: JpegBlob
}

@suppress(["UnstableTrait"])
@http(method: "POST", uri: "/MalformedContentTypeWithGenericString")
operation MalformedContentTypeWithGenericString {
    input: MalformedContentTypeWithGenericStringInput
}

structure MalformedContentTypeWithGenericStringInput {
    @httpPayload
    payload: String
}

@suppress(["UnstableTrait"])
@http(method: "POST", uri: "/MalformedContentTypeWithoutBodyEmptyInput")
operation MalformedContentTypeWithoutBodyEmptyInput {
    input: MalformedContentTypeWithoutBodyEmptyInputInput
}

structure MalformedContentTypeWithoutBodyEmptyInputInput {
    @httpHeader("header")
    header: String
}
