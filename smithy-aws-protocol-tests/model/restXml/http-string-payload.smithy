$version: "2.0"

namespace aws.protocoltests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@http(uri: "/EnumPayload", method: "POST")
@httpRequestTests([
    {
        id: "RestXmlEnumPayloadRequest",
        uri: "/EnumPayload",
        body: "enumvalue",
        params: { payload: "enumvalue" },
        method: "POST",
        protocol: "aws.protocols#restXml"
    }
])
@httpResponseTests([
    {
        id: "RestXmlEnumPayloadResponse",
        body: "enumvalue",
        params: { payload: "enumvalue" },
        protocol: "aws.protocols#restXml",
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
        id: "RestXmlStringPayloadRequest",
        uri: "/StringPayload",
        body: "rawstring",
        params: { payload: "rawstring" },
        method: "POST",
        protocol: "aws.protocols#restXml"
    }
])
@httpResponseTests([
    {
        id: "RestXmlStringPayloadResponse",
        body: "rawstring",
        params: { payload: "rawstring" },
        protocol: "aws.protocols#restXml",
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
