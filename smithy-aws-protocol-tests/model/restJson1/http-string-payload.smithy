$version: "2.0"

namespace aws.protocoltests.restjson

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@http(uri: "/EnumPayload", method: "POST")
@httpRequestTests([
    {
        id: "RestJsonEnumPayloadRequest",
        uri: "/EnumPayload",
        body: "enumvalue",
        params: { payload: "enumvalue" },
        method: "POST",
        protocol: "aws.protocols#restJson1"
    }
])
@httpResponseTests([
    {
        id: "RestJsonEnumPayloadResponse",
        body: "enumvalue",
        params: { payload: "enumvalue" },
        protocol: "aws.protocols#restJson1",
        code: 200
    }
])
operation HttpEnumPayload {
    input: EnumPayloadInput,
    output: EnumPayloadInput
}

structure EnumPayloadInput {
    @httpPayload
    payload: StringEnum
}

enum StringEnum {
    V = "enumvalue"
}

@http(uri: "/StringPayload", method: "POST")
@httpRequestTests([
    {
        id: "RestJsonStringPayloadRequest",
        uri: "/StringPayload",
        body: "rawstring",
        params: { payload: "rawstring" },
        method: "POST",
        protocol: "aws.protocols#restJson1"
    }
])
@httpResponseTests([
    {
        id: "RestJsonStringPayloadResponse",
        body: "rawstring",
        params: { payload: "rawstring" },
        protocol: "aws.protocols#restJson1",
        code: 200
    }
])
operation HttpStringPayload {
    input: StringPayloadInput,
    output: StringPayloadInput
}

structure StringPayloadInput {
    @httpPayload
    payload: String
}

