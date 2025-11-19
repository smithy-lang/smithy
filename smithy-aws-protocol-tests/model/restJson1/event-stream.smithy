$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#DateTime
use smithy.test#InitialHttpRequest
use smithy.test#InitialHttpResponse
use smithy.test#eventStreamTests
use smithy.framework#ValidationException

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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
                bytes: "AAAASQAAADlvxG1ZDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYnl0ZUhlYWRlcgIBKFTmjg=="
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMLc2hvcnRIZWFkZXIDAAL1ETsK"
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMJaW50SGVhZGVyBAAAAAPlyUrb"
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
                bytes: "AAAAUAAAAEAr7VEyDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKbG9uZ0hlYWRlcgUAAAAA/////udnd/I="
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
                    headers: { blobHeader: "Zm9v" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "Zm9v" }
                }
                bytes: "AAAATQAAAD2dKQ+ADTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYmxvYkhlYWRlcgYAA2Zvb5sbbGM="
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
                bytes: "AAAATwAAAD8J5z3MDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMMc3RyaW5nSGVhZGVyBwADZm9vxT+2MA=="
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
                bytes: "AAAAVQAAAEWTZyrNDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMPdGltZXN0YW1wSGVhZGVyCAAAAZLi7jFQ6uV3Eg=="
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
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "YmFy" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "YmFy" }
                }
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgAMc3RyaW5nSGVhZGVyBwADZm9vCmJsb2JIZWFkZXIGAANiYXIDXbo7"
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
                bytes: "AAAAYAAAAE30fZUJDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADXN0cmluZ1BheWxvYWQNOmNvbnRlbnQtdHlwZQcACnRleHQvcGxhaW5mb29G1ELr"
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
                bytes: "AAAAbAAAAFkrV6x1DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAC2Jsb2JQYXlsb2FkDTpjb250ZW50LXR5cGUHABhhcHBsaWNhdGlvbi9vY3RldC1zdHJlYW1iYXJv5nGJ"
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
                body: """
                    {"structureMember":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAEHN0cnVjdHVyZVBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257InN0cnVjdHVyZU1lbWJlciI6ImZvbyJ9rcIRVA=="
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
                body: """
                    {"unionMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdwAAAFKrtdNuDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADHVuaW9uUGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbnsidW5pb25NZW1iZXIiOiJiYXIifcZDMD4="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                body: """
                    {"payload":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAjQAAAGxoUIY5DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRJbXBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJwYXlsb2FkIjoiYmFyIn15lZtT"
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVlcnJvcgs6ZXJyb3ItY29kZQcADmludGVybmFsLWVycm9yDjplcnJvci1tZXNzYWdlBwAaQW4gdW5rbm93biBlcnJvciBvY2N1cnJlZC4kun0t"
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2CzpldmVudC10eXBlBwAZaGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbgZoZWFkZXIHAANmb297InN0cnVjdHVyZU1lbWJlciI6ImJhciJ98LexJg=="
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
                    ":message-type": { blob: "ZXZlbnQ=" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAbQAAAER1MekcDTptZXNzYWdlLXR5cGUGAAVldmVudA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbgZoZWFkZXIHAANmb297InN0cnVjdHVyZU1lbWJlciI6ImJhciJ95dXDSw=="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                    ":event-type": { blob: "aGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA==" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQYAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifcP6KLk="
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

    errors: [ValidationException]
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
                bytes: "AAAASQAAADlvxG1ZDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYnl0ZUhlYWRlcgIBKFTmjg=="
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMLc2hvcnRIZWFkZXIDAAL1ETsK"
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMJaW50SGVhZGVyBAAAAAPlyUrb"
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
                bytes: "AAAAUAAAAEAr7VEyDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKbG9uZ0hlYWRlcgUAAAAA/////udnd/I="
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
                    headers: { blobHeader: "Zm9v" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "Zm9v" }
                }
                bytes: "AAAATQAAAD2dKQ+ADTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYmxvYkhlYWRlcgYAA2Zvb5sbbGM="
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
                bytes: "AAAATwAAAD8J5z3MDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMMc3RyaW5nSGVhZGVyBwADZm9vxT+2MA=="
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
                bytes: "AAAAVQAAAEWTZyrNDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMPdGltZXN0YW1wSGVhZGVyCAAAAZLi7jFQ6uV3Eg=="
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
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "YmFy" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "YmFy" }
                }
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgAMc3RyaW5nSGVhZGVyBwADZm9vCmJsb2JIZWFkZXIGAANiYXIDXbo7"
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
                bytes: "AAAAYAAAAE30fZUJDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADXN0cmluZ1BheWxvYWQNOmNvbnRlbnQtdHlwZQcACnRleHQvcGxhaW5mb29G1ELr"
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
                bytes: "AAAAbAAAAFkrV6x1DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAC2Jsb2JQYXlsb2FkDTpjb250ZW50LXR5cGUHABhhcHBsaWNhdGlvbi9vY3RldC1zdHJlYW1iYXJv5nGJ"
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
                body: """
                    {"structureMember":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAEHN0cnVjdHVyZVBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257InN0cnVjdHVyZU1lbWJlciI6ImZvbyJ9rcIRVA=="
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
                body: """
                    {"unionMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdwAAAFKrtdNuDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADHVuaW9uUGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbnsidW5pb25NZW1iZXIiOiJiYXIifcZDMD4="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                body: """
                    {"payload":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAjQAAAGxoUIY5DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRJbXBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJwYXlsb2FkIjoiYmFyIn15lZtT"
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVlcnJvcgs6ZXJyb3ItY29kZQcADmludGVybmFsLWVycm9yDjplcnJvci1tZXNzYWdlBwAaQW4gdW5rbm93biBlcnJvciBvY2N1cnJlZC4kun0t"
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2CzpldmVudC10eXBlBwAZaGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbgZoZWFkZXIHAANmb297InN0cnVjdHVyZU1lbWJlciI6ImJhciJ98LexJg=="
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
                    ":message-type": { blob: "ZXZlbnQ=" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUGAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifVwdfzU="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                    ":event-type": { blob: "aGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA==" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQYAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifcP6KLk="
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
            body: """
                {"message": "foo"}"""
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
        ValidationException
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
                bytes: "AAAASQAAADlvxG1ZDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYnl0ZUhlYWRlcgIBKFTmjg=="
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMLc2hvcnRIZWFkZXIDAAL1ETsK"
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMJaW50SGVhZGVyBAAAAAPlyUrb"
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
                bytes: "AAAAUAAAAEAr7VEyDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKbG9uZ0hlYWRlcgUAAAAA/////udnd/I="
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
                    headers: { blobHeader: "Zm9v" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "Zm9v" }
                }
                bytes: "AAAATQAAAD2dKQ+ADTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYmxvYkhlYWRlcgYAA2Zvb5sbbGM="
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
                bytes: "AAAATwAAAD8J5z3MDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMMc3RyaW5nSGVhZGVyBwADZm9vxT+2MA=="
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
                bytes: "AAAAVQAAAEWTZyrNDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMPdGltZXN0YW1wSGVhZGVyCAAAAZLi7jFQ6uV3Eg=="
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
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "YmFy" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "YmFy" }
                }
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgAMc3RyaW5nSGVhZGVyBwADZm9vCmJsb2JIZWFkZXIGAANiYXIDXbo7"
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
                bytes: "AAAAYAAAAE30fZUJDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADXN0cmluZ1BheWxvYWQNOmNvbnRlbnQtdHlwZQcACnRleHQvcGxhaW5mb29G1ELr"
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
                bytes: "AAAAbAAAAFkrV6x1DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAC2Jsb2JQYXlsb2FkDTpjb250ZW50LXR5cGUHABhhcHBsaWNhdGlvbi9vY3RldC1zdHJlYW1iYXJv5nGJ"
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
                body: """
                    {"structureMember":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAEHN0cnVjdHVyZVBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257InN0cnVjdHVyZU1lbWJlciI6ImZvbyJ9rcIRVA=="
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
                body: """
                    {"unionMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdwAAAFKrtdNuDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADHVuaW9uUGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbnsidW5pb25NZW1iZXIiOiJiYXIifcZDMD4="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                body: """
                    {"payload":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAjQAAAGxoUIY5DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRJbXBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJwYXlsb2FkIjoiYmFyIn15lZtT"
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVlcnJvcgs6ZXJyb3ItY29kZQcADmludGVybmFsLWVycm9yDjplcnJvci1tZXNzYWdlBwAaQW4gdW5rbm93biBlcnJvciBvY2N1cnJlZC4kun0t"
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2CzpldmVudC10eXBlBwAZaGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbgZoZWFkZXIHAANmb297InN0cnVjdHVyZU1lbWJlciI6ImJhciJ98LexJg=="
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
                    ":message-type": { blob: "ZXZlbnQ=" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAbQAAAER1MekcDTptZXNzYWdlLXR5cGUGAAVldmVudA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbgZoZWFkZXIHAANmb297InN0cnVjdHVyZU1lbWJlciI6ImJhciJ95dXDSw=="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                    ":event-type": { blob: "aGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA==" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQYAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifcP6KLk="
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
                bytes: "AAAASQAAADlvxG1ZDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYnl0ZUhlYWRlcgIBKFTmjg=="
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMLc2hvcnRIZWFkZXIDAAL1ETsK"
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMJaW50SGVhZGVyBAAAAAPlyUrb"
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
                bytes: "AAAAUAAAAEAr7VEyDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKbG9uZ0hlYWRlcgUAAAAA/////udnd/I="
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
                    headers: { blobHeader: "Zm9v" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    blobHeader: { blob: "Zm9v" }
                }
                bytes: "AAAATQAAAD2dKQ+ADTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMKYmxvYkhlYWRlcgYAA2Zvb5sbbGM="
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
                bytes: "AAAATwAAAD8J5z3MDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMMc3RyaW5nSGVhZGVyBwADZm9vxT+2MA=="
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
                bytes: "AAAAVQAAAEWTZyrNDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMPdGltZXN0YW1wSGVhZGVyCAAAAZLi7jFQ6uV3Eg=="
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
                    headers: { booleanHeader: true, stringHeader: "foo", blobHeader: "YmFy" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headers" }
                    booleanHeader: { boolean: true }
                    stringHeader: { string: "foo" }
                    blobHeader: { blob: "YmFy" }
                }
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgAMc3RyaW5nSGVhZGVyBwADZm9vCmJsb2JIZWFkZXIGAANiYXIDXbo7"
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
                bytes: "AAAAYAAAAE30fZUJDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADXN0cmluZ1BheWxvYWQNOmNvbnRlbnQtdHlwZQcACnRleHQvcGxhaW5mb29G1ELr"
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
                bytes: "AAAAbAAAAFkrV6x1DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAC2Jsb2JQYXlsb2FkDTpjb250ZW50LXR5cGUHABhhcHBsaWNhdGlvbi9vY3RldC1zdHJlYW1iYXJv5nGJ"
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
                body: """
                    {"structureMember":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAEHN0cnVjdHVyZVBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257InN0cnVjdHVyZU1lbWJlciI6ImZvbyJ9rcIRVA=="
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
                body: """
                    {"unionMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdwAAAFKrtdNuDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcADHVuaW9uUGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbnsidW5pb25NZW1iZXIiOiJiYXIifcZDMD4="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                body: """
                    {"payload":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAjQAAAGxoUIY5DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRJbXBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJwYXlsb2FkIjoiYmFyIn15lZtT"
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                body: """
                    {"message":"foo"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAdAAAAFObEpkoDTptZXNzYWdlLXR5cGUHAAlleGNlcHRpb24POmV4Y2VwdGlvbi10eXBlBwAFZXJyb3INOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb257Im1lc3NhZ2UiOiJmb28ifTua1S8="
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
                bytes: "AAAAbwAAAF+FlHOQDTptZXNzYWdlLXR5cGUHAAVlcnJvcgs6ZXJyb3ItY29kZQcADmludGVybmFsLWVycm9yDjplcnJvci1tZXNzYWdlBwAaQW4gdW5rbm93biBlcnJvciBvY2N1cnJlZC4kun0t"
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAfwAAAFacqFy2CzpldmVudC10eXBlBwAZaGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA06Y29udGVudC10eXBlBwAQYXBwbGljYXRpb24vanNvbgZoZWFkZXIHAANmb297InN0cnVjdHVyZU1lbWJlciI6ImJhciJ98LexJg=="
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
                    ":message-type": { blob: "ZXZlbnQ=" }
                    ":event-type": { string: "headersAndExplicitPayload" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUGAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifVwdfzU="
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
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifTafKXs="
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
                    ":event-type": { blob: "aGVhZGVyc0FuZEV4cGxpY2l0UGF5bG9hZA==" }
                    ":content-type": { string: "application/json" }
                    header: { string: "foo" }
                }
                body: """
                    {"structureMember":"bar"}"""
                bodyMediaType: "application/json"
                bytes: "AAAAlQAAAGw4wFp6DTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQYAGWhlYWRlcnNBbmRFeHBsaWNpdFBheWxvYWQNOmNvbnRlbnQtdHlwZQcAEGFwcGxpY2F0aW9uL2pzb24GaGVhZGVyBwADZm9veyJzdHJ1Y3R1cmVNZW1iZXIiOiJiYXIifcP6KLk="
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

    errors: [ValidationException]
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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

    errors: [ValidationException]
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
                bytes: "AAAASwAAADv7Cl8VDTptZXNzYWdlLXR5cGUHAAVldmVudAs6ZXZlbnQtdHlwZQcAB2hlYWRlcnMNYm9vbGVhbkhlYWRlcgC4J9Ws"
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
            body: """
                {"message": "foo"}"""
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

    errors: [ValidationException]
}

union SingletonEventStream {
    singleton: SingletonEvent
}

structure SingletonEvent {
    value: String
}
