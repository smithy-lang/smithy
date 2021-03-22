namespace aws.protocoltests.restjson
use smithy.test#httpRequestTests

@http(uri: "/EnumPayload", method: "POST")
@httpRequestTests([
    {
        id: "EnumPayload",
        uri: "/EnumPayload",
        body: "enumvalue",
        params: { payload: "enumvalue" },
        method: "POST",
        protocol: "aws.protocols#restJson1"
    }
])
operation HttpEnumPayload {
    input: EnumPayloadInput
}

structure EnumPayloadInput {
    @httpPayload
    payload: StringEnum
}

@enum([{"value": "enumvalue", "name": "V"}])
string StringEnum

@http(uri: "/StringPayload", method: "POST")
@httpRequestTests([
    {
        id: "StringPayload",
        uri: "/StringPayload",
        body: "rawstring",
        params: { payload: "rawstring" },
        method: "POST",
        protocol: "aws.protocols#restJson1"
    }
])
operation HttpStringPayload {
    input: StringPayloadInput
}

structure StringPayloadInput {
    @httpPayload
    payload: String
}
