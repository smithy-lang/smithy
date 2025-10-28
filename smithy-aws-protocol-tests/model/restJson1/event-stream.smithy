$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#DateTime
use smithy.test#InitialHttpRequest
use smithy.test#InitialHttpResponse
use smithy.test#eventStreamTests

@streaming
union EventStream {
    headers: HeadersEvent
    blobPayload: BlobPayloadEvent
    stringPayload: StringPayloadEvent
    structurePayload: StructurePayloadEvent
    unionPayload: UnionPayloadEvent
    headersAndExplicitPayload: HeadersAndExplicitPayloadEvent
    headersAndImplicitPayload: HeadersAndImplicitPayloadEvent
    error: ErrorEvent
}

structure HeadersEvent {
    @eventHeader
    booleanHeader: Boolean

    @eventHeader
    byteHeader: Byte

    @eventHeader
    shortHeader: Short

    @eventHeader
    intHeader: Integer

    @eventHeader
    longHeader: Long

    @eventHeader
    blobHeader: Blob

    @eventHeader
    stringHeader: String

    @eventHeader
    timestampHeader: DateTime
}

structure BlobPayloadEvent {
    @eventPayload
    payload: Blob
}

structure StringPayloadEvent {
    @eventPayload
    payload: String
}

structure StructurePayloadEvent {
    @eventPayload
    payload: PayloadStructure
}

structure PayloadStructure {
    structureMember: String
}

structure UnionPayloadEvent {
    @eventPayload
    payload: PayloadUnion
}

union PayloadUnion {
    unionMember: String
}

structure HeadersAndExplicitPayloadEvent {
    @eventHeader
    header: String

    @eventPayload
    payload: PayloadStructure
}

structure HeadersAndImplicitPayloadEvent {
    @eventHeader
    header: String

    payload: String
}

@error("client")
structure ErrorEvent {
    message: String
}

@eventStreamTests([
    {
        id: "BooleanHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
    }
    {
        id: "ByteHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { byteHeader: 1 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    byteHeader: { byte: 1 }
                }
            }
        ]
    }
    {
        id: "ShortHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { shortHeader: 2 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    shortHeader: { short: 2 }
                }
            }
        ]
    }
    {
        id: "IntegerHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { intHeader: 3 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    intHeader: { integer: 3 }
                }
            }
        ]
    }
    {
        id: "LongHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { longHeader: 4294967294 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    longHeader: { long: 4294967294 }
                }
            }
        ]
    }
    {
        id: "BlobHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { blobHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "foo" }
                }
            }
        ]
    }
    {
        id: "StringHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { stringHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    stringHeader: { string: "foo" }
                }
            }
        ]
    }
    {
        id: "TimestampHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { timestampHeader: "2024-10-31T14:15:14Z" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    timestampHeader: { timestamp: "2024-10-31T14:15:14Z" }
                }
            }
        ]
    }
    {
        id: "MultipleHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "bar" }
                }
            }
        ]
    }
    {
        id: "StringPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    stringPayload: { payload: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "stringPayload" }
                    ":content-type": { string: "text/plain" }
                }
                body: "foo"
                bodyMediaType: "text/plain"
            }
        ]
    }
    {
        id: "BlobPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    blobPayload: { payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "blobPayload" }
                    ":content-type": { string: "application/octet-stream" }
                }
                body: "bar"
                bodyMediaType: "application/octet-stream"
            }
        ]
    }
    {
        id: "StructurePayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    structurePayload: {
                        payload: { structureMember: "foo" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "structurePayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"structureMember\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "UnionPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    unionPayload: {
                        payload: { unionMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "unionPayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"unionMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "HeadersAndExplicitPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "HeadersAndImplicitPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndImplicitPayload: { header: "foo", payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndImplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"payload\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "ServerErrorInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: { errorId: ErrorEvent }
        }
        appliesTo: "server"
    }
    {
        id: "ClientErrorInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        appliesTo: "client"
    }
    {
        id: "ServerUnexpectedErrorInput"
        documentation: "Servers must be able to handle structured, but unmodeled errors."
        protocol: restJson1
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "error" }
                    ":error-code": { string: "internal-error" }
                    ":error-message": { string: "An unknown error occurred." }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "MissingMessageTypeInput"
        documentation: "Servers must reject events that don't contain a :message-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "MalformedMessageTypeInput"
        documentation: "Servers must reject events that contain a malformed :message-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { blob: "event" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "MissingEventTypeInput"
        documentation: "Servers must reject message events that don't contain an :event-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "MalformedEventTypeInput"
        documentation: "Servers must reject message events that contain a malformed :event-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { blob: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
])
@http(method: "POST", uri: "/InputStream")
operation InputStream {
    input := {
        @httpPayload
        stream: EventStream
    }
}

@eventStreamTests([
    {
        id: "BooleanHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
    }
    {
        id: "ByteHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { byteHeader: 1 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    byteHeader: { byte: 1 }
                }
            }
        ]
    }
    {
        id: "ShortHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { shortHeader: 2 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    shortHeader: { short: 2 }
                }
            }
        ]
    }
    {
        id: "IntegerHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { intHeader: 3 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    intHeader: { integer: 3 }
                }
            }
        ]
    }
    {
        id: "LongHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { longHeader: 4294967294 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    longHeader: { long: 4294967294 }
                }
            }
        ]
    }
    {
        id: "BlobHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { blobHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "foo" }
                }
            }
        ]
    }
    {
        id: "StringHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { stringHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    stringHeader: { string: "foo" }
                }
            }
        ]
    }
    {
        id: "TimestampHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { timestampHeader: "2024-10-31T14:15:14Z" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    timestampHeader: { timestamp: "2024-10-31T14:15:14Z" }
                }
            }
        ]
    }
    {
        id: "MultipleHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "bar" }
                }
            }
        ]
    }
    {
        id: "StringPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    stringPayload: { payload: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "stringPayload" }
                    ":content-type": { string: "text/plain" }
                }
                body: "foo"
                bodyMediaType: "text/plain"
            }
        ]
    }
    {
        id: "BlobPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    blobPayload: { payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "blobPayload" }
                    ":content-type": { string: "application/octet-stream" }
                }
                body: "bar"
                bodyMediaType: "application/octet-stream"
            }
        ]
    }
    {
        id: "StructurePayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    structurePayload: {
                        payload: { structureMember: "foo" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "structurePayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"structureMember\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "UnionPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    unionPayload: {
                        payload: { unionMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "unionPayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"unionMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "HeadersAndExplicitPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "HeadersAndImplicitPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndImplicitPayload: { header: "foo", payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndImplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"payload\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "ServerErrorOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        appliesTo: "server"
    }
    {
        id: "ClientErrorOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: { errorId: ErrorEvent }
        }
        appliesTo: "client"
    }
    {
        id: "ClientUnexpectedErrorOutput"
        documentation: "Clients must be able to handle structured, but unmodeled errors."
        protocol: restJson1
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "error" }
                    ":error-code": { string: "internal-error" }
                    ":error-message": { string: "An unknown error occurred." }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "MissingMessageTypeOutput"
        documentation: "Clients must reject events that don't contain a :message-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "MalformedMessageTypeOutput"
        documentation: "Client must reject events that contain a malformed :message-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { blob: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "MissingEventTypeOutput"
        documentation: "Clients must reject message events that don't contain an :event-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "MalformedEventTypeOutput"
        documentation: "Clients must reject message events that contain a malformed :event-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { blob: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "ModeledProtocolError"
        protocol: restJson1
        initialResponse: {
            code: 500
            headers: { "Content-Type": "application/json", "X-Amzn-Errortype": "ServiceUnavailableError" }
            body: "{\"message\": \"foo\"}"
            bodyMediaType: "application/json"
        }
        initialResponseShape: InitialHttpResponse
        expectation: {
            failure: { errorId: ServiceUnavailableError }
        }
        appliesTo: "client"
    }
    {
        id: "UnmodeledProtocolError"
        protocol: restJson1
        initialResponse: {
            code: 500
            headers: { "Content-Type": "text/plain" }
            body: "service unavailable"
            bodyMediaType: "text/plain"
        }
        initialResponseShape: InitialHttpResponse
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
])
@http(method: "POST", uri: "/OutputStream")
operation OutputStream {
    output := {
        @httpPayload
        stream: EventStream
    }

    errors: [
        ServiceUnavailableError
    ]
}

@error("server")
@httpError(500)
structure ServiceUnavailableError {
    message: String
}

@eventStreamTests([
    {
        id: "DuplexBooleanHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
    }
    {
        id: "DuplexByteHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { byteHeader: 1 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    byteHeader: { byte: 1 }
                }
            }
        ]
    }
    {
        id: "DuplexShortHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { shortHeader: 2 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    shortHeader: { short: 2 }
                }
            }
        ]
    }
    {
        id: "DuplexIntegerHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { intHeader: 3 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    intHeader: { integer: 3 }
                }
            }
        ]
    }
    {
        id: "DuplexLongHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { longHeader: 4294967294 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    longHeader: { long: 4294967294 }
                }
            }
        ]
    }
    {
        id: "DuplexBlobHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { blobHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "foo" }
                }
            }
        ]
    }
    {
        id: "DuplexStringHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { stringHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    stringHeader: { string: "foo" }
                }
            }
        ]
    }
    {
        id: "DuplexTimestampHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { timestampHeader: "2024-10-31T14:15:14Z" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    timestampHeader: { timestamp: "2024-10-31T14:15:14Z" }
                }
            }
        ]
    }
    {
        id: "DuplexMultipleHeaderInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "bar" }
                }
            }
        ]
    }
    {
        id: "DuplexStringPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    stringPayload: { payload: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "stringPayload" }
                    ":content-type": { string: "text/plain" }
                }
                body: "foo"
                bodyMediaType: "text/plain"
            }
        ]
    }
    {
        id: "DuplexBlobPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    blobPayload: { payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "blobPayload" }
                    ":content-type": { string: "application/octet-stream" }
                }
                body: "bar"
                bodyMediaType: "application/octet-stream"
            }
        ]
    }
    {
        id: "DuplexStructurePayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    structurePayload: {
                        payload: { structureMember: "foo" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "structurePayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"structureMember\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexUnionPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    unionPayload: {
                        payload: { unionMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "unionPayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"unionMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexHeadersAndExplicitPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexHeadersAndImplicitPayloadInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndImplicitPayload: { header: "foo", payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndImplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"payload\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexServerErrorInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: { errorId: ErrorEvent }
        }
        appliesTo: "server"
    }
    {
        id: "DuplexClientErrorInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        appliesTo: "client"
    }
    {
        id: "DuplexServerUnexpectedErrorInput"
        documentation: "Servers must be able to handle structured, but unmodeled errors."
        protocol: restJson1
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "error" }
                    ":error-code": { string: "internal-error" }
                    ":error-message": { string: "An unknown error occurred." }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "DuplexMissingMessageTypeInput"
        documentation: "Servers must reject events that don't contain a :message-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "DuplexMalformedMessageTypeInput"
        documentation: "Servers must reject events that contain a malformed :message-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { blob: "event" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "DuplexMissingEventTypeInput"
        documentation: "Servers must reject message events that don't contain an :event-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "DuplexMalformedEventTypeInput"
        documentation: "Servers must reject message events that contain a malformed :event-type header."
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { blob: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "DuplexBooleanHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
    }
    {
        id: "DuplexByteHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { byteHeader: 1 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    byteHeader: { byte: 1 }
                }
            }
        ]
    }
    {
        id: "DuplexShortHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { shortHeader: 2 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    shortHeader: { short: 2 }
                }
            }
        ]
    }
    {
        id: "DuplexIntegerHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { intHeader: 3 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    intHeader: { integer: 3 }
                }
            }
        ]
    }
    {
        id: "DuplexLongHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { longHeader: 4294967294 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    longHeader: { long: 4294967294 }
                }
            }
        ]
    }
    {
        id: "DuplexBlobHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { blobHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "foo" }
                }
            }
        ]
    }
    {
        id: "DuplexStringHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { stringHeader: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    stringHeader: { string: "foo" }
                }
            }
        ]
    }
    {
        id: "DuplexTimestampHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { timestampHeader: "2024-10-31T14:15:14Z" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    timestampHeader: { timestamp: "2024-10-31T14:15:14Z" }
                }
            }
        ]
    }
    {
        id: "DuplexMultipleHeaderOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "bar" }
                }
            }
        ]
    }
    {
        id: "DuplexStringPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    stringPayload: { payload: "foo" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "stringPayload" }
                    ":content-type": { string: "text/plain" }
                }
                body: "foo"
                bodyMediaType: "text/plain"
            }
        ]
    }
    {
        id: "DuplexBlobPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    blobPayload: { payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "blobPayload" }
                    ":content-type": { string: "application/octet-stream" }
                }
                body: "bar"
                bodyMediaType: "application/octet-stream"
            }
        ]
    }
    {
        id: "DuplexStructurePayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    structurePayload: {
                        payload: { structureMember: "foo" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "structurePayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"structureMember\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexUnionPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    unionPayload: {
                        payload: { unionMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "unionPayload" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"unionMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexHeadersAndExplicitPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexHeadersAndImplicitPayloadOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndImplicitPayload: { header: "foo", payload: "bar" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndImplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"payload\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "DuplexServerErrorOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        appliesTo: "server"
    }
    {
        id: "DuplexClientErrorOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    error: { message: "foo" }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "error" }
                    ":content-type": { string: "application/json" }
                }
                body: "{\"message\":\"foo\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: { errorId: ErrorEvent }
        }
        appliesTo: "client"
    }
    {
        id: "DuplexClientUnexpectedErrorOutput"
        documentation: "Clients must be able to handle structured, but unmodeled errors."
        protocol: restJson1
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "error" }
                    ":error-code": { string: "internal-error" }
                    ":error-message": { string: "An unknown error occurred." }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "DuplexMissingMessageTypeOutput"
        documentation: "Clients must reject events that don't contain a :message-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "DuplexMalformedMessageTypeOutput"
        documentation: "Client must reject events that contain a malformed :message-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { blob: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "DuplexMissingEventTypeOutput"
        documentation: "Clients must reject message events that don't contain an :event-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "DuplexMalformedEventTypeOutput"
        documentation: "Clients must reject message events that contain a malformed :event-type header."
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headersAndExplicitPayload: {
                        header: "foo"
                        payload: { structureMember: "bar" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { blob: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: "{\"structureMember\":\"bar\"}"
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
])
@http(method: "POST", uri: "/DuplexStream")
operation DuplexStream {
    input := {
        @httpPayload
        stream: EventStream
    }

    output := {
        @httpPayload
        stream: EventStream
    }
}

@eventStreamTests([
    {
        id: "InitialRequestInput"
        protocol: restJson1
        initialRequestParams: { initialRequestMember: "foo" }
        initialRequest: {
            method: "POST"
            uri: "/InputStreamWithInitialRequest"
            headers: { "initial-request-member": "foo" }
        }
        initialRequestShape: InitialHttpRequest
    }
    {
        id: "MissingRequiredInitialRequestInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
])
@http(method: "POST", uri: "/InputStreamWithInitialRequest")
operation InputStreamWithInitialRequest {
    input := {
        @httpHeader("initial-request-member")
        @required
        initialRequestMember: String

        @httpPayload
        stream: EventStream
    }
}

@eventStreamTests([
    {
        id: "InitialResponseOutput"
        protocol: restJson1
        initialResponseParams: { initialResponseMember: "foo" }
        initialResponse: {
            code: 200
            headers: { "initial-request-member": "foo" }
        }
        initialResponseShape: InitialHttpResponse
    }
    {
        id: "MissingRequiredInitialResponseOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
])
@http(method: "POST", uri: "/OutputStreamWithInitialResponse")
operation OutputStreamWithInitialResponse {
    output := {
        @httpHeader("initial-response-member")
        @required
        initialResponseMember: String

        @httpPayload
        stream: EventStream
    }
}

@eventStreamTests([
    {
        id: "DuplexInitialRequestInput"
        protocol: restJson1
        initialRequestParams: { initialRequestMember: "foo" }
        initialRequest: {
            method: "POST"
            uri: "/DuplexStreamWithInitialMessages"
            headers: { "initial-request-member": "foo" }
        }
        initialRequestShape: InitialHttpRequest
    }
    {
        id: "DuplexMissingRequiredInitialRequestInput"
        protocol: restJson1
        events: [
            {
                type: "request"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "server"
    }
    {
        id: "DuplexInitialResponseOutput"
        protocol: restJson1
        initialResponseParams: { initialResponseMember: "foo" }
        initialResponse: {
            code: 200
            headers: { "initial-request-member": "foo" }
        }
        initialResponseShape: InitialHttpResponse
    }
    {
        id: "DuplexMissingRequiredInitialResponseOutput"
        protocol: restJson1
        events: [
            {
                type: "response"
                params: {
                    headers: { booleanHeader: true }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "DuplexModeledProtocolError"
        protocol: restJson1
        initialResponse: {
            code: 500
            headers: { "Content-Type": "application/json", "X-Amzn-Errortype": "ServiceUnavailableError" }
            body: "{\"message\": \"foo\"}"
            bodyMediaType: "application/json"
        }
        initialResponseShape: InitialHttpResponse
        expectation: {
            failure: { errorId: ServiceUnavailableError }
        }
        appliesTo: "client"
    }
    {
        id: "DuplexUnmodeledProtocolError"
        protocol: restJson1
        initialResponse: {
            code: 500
            headers: { "Content-Type": "text/plain" }
            body: "service unavailable"
            bodyMediaType: "text/plain"
        }
        initialResponseShape: InitialHttpResponse
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
])
@http(method: "POST", uri: "/DuplexStreamWithInitialMessages")
operation DuplexStreamWithInitialMessages {
    input := {
        @httpHeader("initial-request-member")
        @required
        initialRequestMember: String

        @httpPayload
        stream: EventStream
    }

    output := {
        @httpHeader("initial-response-member")
        @required
        initialResponseMember: String

        @httpPayload
        stream: EventStream
    }

    errors: [
        ServiceUnavailableError
    ]
}

@http(method: "POST", uri: "/DuplexStreamWithDistinctStreams")
operation DuplexStreamWithDistinctStreams {
    input := {
        @httpPayload
        stream: EventStream
    }

    output := {
        @httpPayload
        stream: SingletonEventStream
    }
}

union SingletonEventStream {
    singleton: SingletonEvent
}

structure SingletonEvent {
    value: String
}
