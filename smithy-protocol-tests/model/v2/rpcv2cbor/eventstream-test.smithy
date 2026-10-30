$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Cbor
use smithy.test#InitialHttpRequest
use smithy.test#InitialHttpResponse
use smithy.test#eventStreamTests

// Every structure, union and implicit event payload in this file is the CBOR
// document for the case's params, written as BASE64 with a bodyMediaType of
// application/cbor, which is the convention of model/rpcv2Cbor/. A runner MUST
// base64-decode `body` before comparing it. A raw blob or string @eventPayload
// is not CBOR and is written as literal text instead.
//
// The bodies use definite-length maps, arrays and strings per RFC 8949 section
// 4.2.1, but the protocol constrains only the CBOR major types, so COMPARISON
// MUST BE SEMANTIC: decode the body and compare the data model. A conformant
// implementation may use indefinite-length encodings, may order map keys
// differently, and may write a float in any form that preserves its value, so
// a byte comparison of an rpcv2Cbor body is not a conformance test. A type
// difference still is one: a bare integer where tag 1, tag 2 or tag 4 is
// expected loses information and must fail.
//
// An output whose only member is the event stream has no non-stream members to
// serialize into an initial-response event. The cases here pin the reading
// under which the event is sent anyway, carrying the serialization of that
// empty set of members -- an empty CBOR map, written as the canonical
// definite-length 0xa0 -- plus an application/cbor :content-type. The
// competing reading is that such an output models no initial response at all,
// so none is sent. The event stream format's initial-message clause requires
// every member of the output except the event stream member to be serialized
// into the event payload but does not say what that payload is when there are
// none, and the streaming specification makes an initial-response optional
// without settling whether one exists here. That interaction is UNRESOLVED,
// and these cases therefore do NOT discriminate between the two readings: they
// assert one of them, so an implementation taking the other fails them for a
// reason no normative text establishes as nonconformance.
//
// A MODELED event whose structure has no members is a separate question and is
// answered the other way. HeartbeatEvent and EndStreamEvent are asserted to
// carry NO payload and therefore no :content-type, which is how the event
// stream format's own serialization example renders an event with nothing
// outside its headers.
//
// The :content-type of a serialized structure or union payload is asserted as
// application/cbor, inferred from this protocol's own media type, because the
// event stream format requires only that the value follow the rules of the
// service protocol and does not itself fix it.
apply EventStreamResponse @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseScalars"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Deserializes an event
            whose implicit payload carries every scalar type: the blob is a raw
            CBOR byte string rather than base64, the timestamp is tag 1 epoch
            seconds, the enum is its string value and the intEnum is an integer."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    scalarsEvent: {
                        booleanMember: true
                        byteMember: 9
                        shortMember: 512
                        integerMember: 1234
                        longMember: 9000000000
                        floatMember: 1.5
                        doubleMember: 2.25
                        stringMember: "scalars event"
                        blobMember: "foo"
                        timestampMember: 1609502096
                        stringEnum: "Foo"
                        intEnum: 1
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "rG1ib29sZWFuTWVtYmVy9WpieXRlTWVtYmVyCWtzaG9ydE1lbWJlchkCAG1pbnRlZ2VyTWVtYmVyGQTSamxvbmdNZW1iZXIbAAAAAhhxGgBrZmxvYXRNZW1iZXL6P8AAAGxkb3VibGVNZW1iZXL6QBAAAGxzdHJpbmdNZW1iZXJtc2NhbGFycyBldmVudGpibG9iTWVtYmVyQ2Zvb290aW1lc3RhbXBNZW1iZXLBGl/vDZBqc3RyaW5nRW51bWNGb29naW50RW51bQE="
                bodyMediaType: "application/cbor"
            }
        ]
    }
    {
        id: "RpcV2CborEventStreamResponseList"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Deserializes an event
            whose payload carries lists of scalars as CBOR arrays."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    listEvent: {
                        strings: ["first", "second"]
                        integers: [1, 2, 3]
                        timestamps: [1609502096, 1609588496]
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "listEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "o2dzdHJpbmdzgmVmaXJzdGZzZWNvbmRoaW50ZWdlcnODAQIDanRpbWVzdGFtcHOCwRpf7w2QwRpf8F8Q"
                bodyMediaType: "application/cbor"
            }
        ]
    }
    {
        id: "RpcV2CborEventStreamResponseMap"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Deserializes an event
            whose payload carries maps of scalars as CBOR maps with text string
            keys."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    mapEvent: {
                        strings: { alpha: "first", beta: "second" }
                        integers: { one: 1, two: 2 }
                        timestamps: { start: 1609502096, end: 1609588496 }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "mapEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "o2dzdHJpbmdzomVhbHBoYWVmaXJzdGRiZXRhZnNlY29uZGhpbnRlZ2Vyc6Jjb25lAWN0d28CanRpbWVzdGFtcHOiZXN0YXJ0wRpf7w2QY2VuZMEaX/BfEA=="
                bodyMediaType: "application/cbor"
            }
        ]
    }
    {
        id: "RpcV2CborEventStreamResponseStruct"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Deserializes an event
            whose payload nests a structure, which is a CBOR map keyed by member
            name like any other."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    structEvent: {
                        value: { stringMember: "nestedString", integerMember: 55, booleanMember: true }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "structEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWV2YWx1ZaNsc3RyaW5nTWVtYmVybG5lc3RlZFN0cmluZ21pbnRlZ2VyTWVtYmVyGDdtYm9vbGVhbk1lbWJlcvU="
                bodyMediaType: "application/cbor"
            }
        ]
    }
    {
        id: "RpcV2CborEventStreamResponseUnion"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Deserializes an event
            whose payload nests a union, serialized as a CBOR map with exactly
            one member set."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    unionEvent: {
                        value: { stringValue: "union event" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "unionEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWV2YWx1ZaFrc3RyaW5nVmFsdWVrdW5pb24gZXZlbnQ="
                bodyMediaType: "application/cbor"
            }
        ]
    }
    {
        id: "RpcV2CborEventStreamResponseMultipleEvents"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Dispatches a sequence of
            events to the correct union variants. The heartbeat event models an
            empty structure, so it carries no payload."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    scalarsEvent: {
                        booleanMember: true
                        byteMember: 7
                        shortMember: 300
                        integerMember: 11
                        longMember: 8000000001
                        floatMember: 3.25
                        doubleMember: 4.75
                        stringMember: "first message"
                        blobMember: "bar"
                        timestampMember: 1609588496
                        stringEnum: "Bar"
                        intEnum: 2
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "rG1ib29sZWFuTWVtYmVy9WpieXRlTWVtYmVyB2tzaG9ydE1lbWJlchkBLG1pbnRlZ2VyTWVtYmVyC2psb25nTWVtYmVyGwAAAAHc1lABa2Zsb2F0TWVtYmVy+kBQAABsZG91YmxlTWVtYmVy+kCYAABsc3RyaW5nTWVtYmVybWZpcnN0IG1lc3NhZ2VqYmxvYk1lbWJlckNiYXJvdGltZXN0YW1wTWVtYmVywRpf8F8QanN0cmluZ0VudW1jQmFyZ2ludEVudW0C"
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    heartbeatEvent: {}
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "heartbeatEvent" }
                }
                body: ""
            }
            {
                type: "response"
                params: {
                    listEvent: {
                        strings: ["third", "fourth"]
                        integers: [4, 5]
                        timestamps: [1609674896, 1609761296]
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "listEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "o2dzdHJpbmdzgmV0aGlyZGZmb3VydGhoaW50ZWdlcnOCBAVqdGltZXN0YW1wc4LBGl/xsJDBGl/zAhA="
                bodyMediaType: "application/cbor"
            }
        ]
    }
])

apply EventStreamResponseBlobPayload @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseBlobPayload"
        documentation: """
            EventStreamResponseBlobPayloadOutput has no members besides the
            event stream, so there are no non-stream members to serialize. This
            case pins the reading under which an initial-response event is sent
            anyway and its payload is the serialization of that empty set of
            members, an empty CBOR map, carrying an application/cbor
            :content-type. The competing reading, under which no initial
            response exists to be sent, is unresolved against it; see the file
            header. A blob @eventPayload is written as the raw message payload,
            neither CBOR nor base64, and takes the application/octet-stream
            content type. The modeled @eventHeader member is carried as a
            message header and does not appear in the payload."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    blobEvent: { contentType: "text/csv", data: "row1,row2" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "blobEvent" }
                    ":content-type": { string: "application/octet-stream" }
                    contentType: { string: "text/csv" }
                }
                body: "row1,row2"
                bodyMediaType: "application/octet-stream"
            }
        ]
    }
])

apply EventStreamResponseStructPayload @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseStructPayload"
        documentation: """
            EventStreamResponseStructPayloadOutput has no members besides the
            event stream, so there are no non-stream members to serialize. This
            case pins the reading under which an initial-response event is sent
            anyway and its payload is the serialization of that empty set of
            members, an empty CBOR map, carrying an application/cbor
            :content-type. The competing reading, under which no initial
            response exists to be sent, is unresolved against it; see the file
            header. A structure @eventPayload is written as the CBOR message
            payload and takes the application/cbor content type. The modeled
            @eventHeader member is carried as a message header and does not
            appear in the payload."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    structPayloadEvent: {
                        contentType: "custom-metadata"
                        payload: { stringMember: "payloadString", integerMember: 21, booleanMember: false }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "structPayloadEvent" }
                    ":content-type": { string: "application/cbor" }
                    contentType: { string: "custom-metadata" }
                }
                body: "o2xzdHJpbmdNZW1iZXJtcGF5bG9hZFN0cmluZ21pbnRlZ2VyTWVtYmVyFW1ib29sZWFuTWVtYmVy9A=="
                bodyMediaType: "application/cbor"
            }
        ]
    }
])

apply EventStreamResponseUnionPayload @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseUnionPayload"
        documentation: """
            EventStreamResponseUnionPayloadOutput has no members besides the
            event stream, so there are no non-stream members to serialize. This
            case pins the reading under which an initial-response event is sent
            anyway and its payload is the serialization of that empty set of
            members, an empty CBOR map, carrying an application/cbor
            :content-type. The competing reading, under which no initial
            response exists to be sent, is unresolved against it; see the file
            header. A union @eventPayload is written as the CBOR message payload
            and takes the application/cbor content type. The modeled
            @eventHeader member is carried as a message header and does not
            appear in the payload."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    unionPayloadEvent: {
                        contentType: "custom-metadata"
                        payload: { stringValue: "payloadUnion" }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "unionPayloadEvent" }
                    ":content-type": { string: "application/cbor" }
                    contentType: { string: "custom-metadata" }
                }
                body: "oWtzdHJpbmdWYWx1ZWxwYXlsb2FkVW5pb24="
                bodyMediaType: "application/cbor"
            }
        ]
    }
])

apply EventStreamResponseHeaders @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseHeaders"
        documentation: """
            EventStreamResponseHeadersOutput has no members besides the event
            stream, so there are no non-stream members to serialize. This case
            pins the reading under which an initial-response event is sent
            anyway and its payload is the serialization of that empty set of
            members, an empty CBOR map, carrying an application/cbor
            :content-type. The competing reading, under which no initial
            response exists to be sent, is unresolved against it; see the file
            header. Each @eventHeader member is carried as a typed event stream
            message header rather than in the payload, so the event stream
            framing fixes their encoding and this protocol does not. A string
            @eventPayload is written as raw text and takes the text/plain
            content type."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    headerEvent: {
                        stringHeader: "headerString"
                        integerHeader: 1234
                        booleanHeader: true
                        byteHeader: 9
                        shortHeader: 512
                        longHeader: 9000000000
                        timestampHeader: 1609502096
                        blobHeader: "header"
                        body: "event payload text"
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "headerEvent" }
                    ":content-type": { string: "text/plain" }
                    stringHeader: { string: "headerString" }
                    integerHeader: { integer: 1234 }
                    booleanHeader: { boolean: true }
                    byteHeader: { byte: 9 }
                    shortHeader: { short: 512 }
                    longHeader: { long: 9000000000 }
                    timestampHeader: { timestamp: 1609502096 }
                    blobHeader: { blob: "aGVhZGVy" }
                }
                body: "event payload text"
                bodyMediaType: "text/plain"
            }
        ]
    }
])

apply EventStreamError @eventStreamTests([
    {
        id: "RpcV2CborEventStreamErrorModeled"
        documentation: """
            EventStreamErrorOutput has no members besides the event stream, so
            there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. A modeled error event
            uses :message-type "exception" and names the union member in
            :exception-type, and resolves to the modeled error shape rather than
            a generic failure."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    streamError: { message: "stream failed", code: 57 }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "streamError" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omdtZXNzYWdlbXN0cmVhbSBmYWlsZWRkY29kZRg5"
                bodyMediaType: "application/cbor"
            }
        ]
        expectation: {
            failure: { errorId: StreamError }
        }
        appliesTo: "client"
    }
    {
        id: "RpcV2CborEventStreamErrorAfterMessage"
        documentation: """
            EventStreamErrorOutput has no members besides the event stream, so
            there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. An error event arriving
            after successfully delivered events still resolves to the modeled
            error shape, so the events preceding it must not mask it."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    messageEvent: { content: "delivered before failure", sequence: 66 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "messageEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omdjb250ZW50eBhkZWxpdmVyZWQgYmVmb3JlIGZhaWx1cmVoc2VxdWVuY2UYQg=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    streamError: { message: "stream failed", code: 57 }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "streamError" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omdtZXNzYWdlbXN0cmVhbSBmYWlsZWRkY29kZRg5"
                bodyMediaType: "application/cbor"
            }
        ]
        expectation: {
            failure: { errorId: StreamError }
        }
        appliesTo: "client"
    }
    {
        id: "RpcV2CborEventStreamErrorUnmodeled"
        documentation: """
            EventStreamErrorOutput has no members besides the event stream, so
            there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. Clients must handle a
            structured but unmodeled error, which uses :message-type "error"
            with the code and message carried entirely in headers and no
            payload. No specification fixes the value of either :error-code or
            :error-message, so only their presence is asserted."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                headers: {
                    ":message-type": { string: "error" }
                }
                forbidHeaders: [":content-type"]
                requireHeaders: [":error-code", ":error-message"]
                body: ""
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
    {
        id: "RpcV2CborEventStreamErrorModeledServer"
        documentation: """
            EventStreamErrorOutput has no members besides the event stream, so
            there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. A modeled error is sent
            as an event with :message-type "exception" naming the union member
            in :exception-type, with the error shape serialized as the CBOR
            payload."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    streamError: { message: "stream failed", code: 57 }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "streamError" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omdtZXNzYWdlbXN0cmVhbSBmYWlsZWRkY29kZRg5"
                bodyMediaType: "application/cbor"
            }
        ]
        appliesTo: "server"
    }
    {
        id: "RpcV2CborEventStreamErrorAfterMessageServer"
        documentation: """
            EventStreamErrorOutput has no members besides the event stream, so
            there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. A modeled error raised
            after events have already been sent is still sent as an exception
            event, following the events that preceded it."""
        protocol: rpcv2Cbor
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    messageEvent: { content: "delivered before failure", sequence: 66 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "messageEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omdjb250ZW50eBhkZWxpdmVyZWQgYmVmb3JlIGZhaWx1cmVoc2VxdWVuY2UYQg=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    streamError: { message: "stream failed", code: 57 }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "streamError" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omdtZXNzYWdlbXN0cmVhbSBmYWlsZWRkY29kZRg5"
                bodyMediaType: "application/cbor"
            }
        ]
        appliesTo: "server"
    }
])

apply EventStreamRequest @eventStreamTests([
    {
        id: "RpcV2CborEventStreamRequestSendMessage"
        documentation: """
            EventStreamRequestInput has no members besides the event stream, so
            no initial request is modeled and there are no non-stream members to
            serialize. This case pins the behavior of sending an initial-request
            event anyway, ahead of the stream's events, carrying the
            serialization of that empty set of members -- an empty CBOR map,
            written as the canonical definite-length 0xa0 -- plus an
            application/cbor :content-type. The streaming specification states
            that when an initial request is not modeled a client MUST NOT send
            one and a server MUST NOT expect one, and that a client or server
            MUST NOT fail when an initial message containing only optional
            members is not received. This case therefore asserts behavior the
            specification forbids a client to produce, and it does NOT establish
            conformance: a client that correctly omits the event fails it, and a
            client that passes it has not been shown to conform. Serializes a
            single struct-payload event onto a request stream as CBOR. An event
            streaming request sets Content-Type to the event stream media type,
            must not set a Content-Length, and must not name the operation in
            either target header."""
        protocol: rpcv2Cbor
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2CborCorpusTests/operation/EventStreamRequest"
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream", Accept: "application/cbor" }
            forbidHeaders: ["Content-Length", "X-Amz-Target", "X-Amzn-Target"]
        }
        initialRequestShape: InitialHttpRequest
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-request" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "outbound message" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWdjb250ZW50cG91dGJvdW5kIG1lc3NhZ2U="
                bodyMediaType: "application/cbor"
            }
        ]
    }
    {
        id: "RpcV2CborEventStreamRequestMultipleEvents"
        documentation: """
            EventStreamRequestInput has no members besides the event stream, so
            no initial request is modeled and there are no non-stream members to
            serialize. This case pins the behavior of sending an initial-request
            event anyway, ahead of the stream's events, carrying the
            serialization of that empty set of members -- an empty CBOR map,
            written as the canonical definite-length 0xa0 -- plus an
            application/cbor :content-type. The streaming specification states
            that when an initial request is not modeled a client MUST NOT send
            one and a server MUST NOT expect one, and that a client or server
            MUST NOT fail when an initial message containing only optional
            members is not received. This case therefore asserts behavior the
            specification forbids a client to produce, and it does NOT establish
            conformance: a client that correctly omits the event fails it, and a
            client that passes it has not been shown to conform. Serializes a
            sequence of events onto a request stream, closing with an
            empty-structure event that carries no payload."""
        protocol: rpcv2Cbor
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2CborCorpusTests/operation/EventStreamRequest"
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream", Accept: "application/cbor" }
            forbidHeaders: ["Content-Length", "X-Amz-Target", "X-Amzn-Target"]
        }
        initialRequestShape: InitialHttpRequest
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-request" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "outbound message" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWdjb250ZW50cG91dGJvdW5kIG1lc3NhZ2U="
                bodyMediaType: "application/cbor"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "second outbound message" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWdjb250ZW50d3NlY29uZCBvdXRib3VuZCBtZXNzYWdl"
                bodyMediaType: "application/cbor"
            }
            {
                type: "request"
                params: {
                    endStream: {}
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "endStream" }
                }
                body: ""
            }
        ]
    }
])

apply EventStreamInitialResponse @eventStreamTests([
    {
        id: "RpcV2CborEventStreamInitialResponse"
        documentation: """
            Non-stream output members are delivered ahead of the stream. Under
            rpcv2Cbor the operation's @httpHeader bindings are ignored, so
            every non-stream output member travels in the CBOR payload of an
            initial-response event rather than in HTTP response headers. This
            output has non-stream members and values are provided for them, so
            the initial-response event must be sent and the question the file
            header leaves open does not arise here."""
        protocol: rpcv2Cbor
        initialResponse: {
            code: 200
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream" }
            forbidHeaders: ["X-Session-Id", "X-Timeout"]
        }
        initialResponseParams: { sessionId: "session4a2f", timeout: 30 }
        initialResponseShape: InitialHttpResponse
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omlzZXNzaW9uSWRrc2Vzc2lvbjRhMmZndGltZW91dBge"
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    scalarsEvent: { stringMember: "message after initial response", integerMember: 77 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omxzdHJpbmdNZW1iZXJ4Hm1lc3NhZ2UgYWZ0ZXIgaW5pdGlhbCByZXNwb25zZW1pbnRlZ2VyTWVtYmVyGE0="
                bodyMediaType: "application/cbor"
            }
        ]
    }
])

apply EventStreamInitialRequest @eventStreamTests([
    {
        id: "RpcV2CborEventStreamInitialRequest"
        documentation: """
            Non-stream input members are sent ahead of the stream. Under
            rpcv2Cbor the operation's @httpHeader bindings are ignored, so
            every non-stream input member travels in the CBOR payload of an
            initial-request event rather than in HTTP headers."""
        protocol: rpcv2Cbor
        initialRequestParams: { requestId: "request-5f1c", priority: 7 }
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2CborCorpusTests/operation/EventStreamInitialRequest"
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream", Accept: "application/cbor" }
            forbidHeaders: ["Content-Length", "X-Amz-Target", "X-Amzn-Target", "X-Request-Id", "X-Priority"]
        }
        initialRequestShape: InitialHttpRequest
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-request" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omlyZXF1ZXN0SWRscmVxdWVzdC01ZjFjaHByaW9yaXR5Bw=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "after initial request" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWdjb250ZW50dWFmdGVyIGluaXRpYWwgcmVxdWVzdA=="
                bodyMediaType: "application/cbor"
            }
        ]
    }
])

apply EventStreamDuplex @eventStreamTests([
    {
        id: "RpcV2CborEventStreamDuplex"
        documentation: """
            A duplex stream carries both initial messages: the input members
            go out in an initial-request event and the output members come
            back in an initial-response event, each ahead of their own
            stream's events. Accept and Content-Type are both the event stream
            media type because both directions stream."""
        protocol: rpcv2Cbor
        initialRequestParams: { requestId: "duplex-3ba9" }
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2CborCorpusTests/operation/EventStreamDuplex"
            headers: {
                "smithy-protocol": "rpc-v2-cbor"
                "Content-Type": "application/vnd.amazon.eventstream"
                Accept: "application/vnd.amazon.eventstream"
            }
            forbidHeaders: ["Content-Length", "X-Amz-Target", "X-Amzn-Target", "X-Request-Id"]
        }
        initialRequestShape: InitialHttpRequest
        initialResponseParams: { sessionId: "duplex-session-77" }
        initialResponse: {
            code: 200
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream" }
            forbidHeaders: ["X-Session-Id"]
        }
        initialResponseShape: InitialHttpResponse
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-request" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWlyZXF1ZXN0SWRrZHVwbGV4LTNiYTk="
                bodyMediaType: "application/cbor"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "duplex outbound" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWdjb250ZW50b2R1cGxleCBvdXRib3VuZA=="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "oWlzZXNzaW9uSWRxZHVwbGV4LXNlc3Npb24tNzc="
                bodyMediaType: "application/cbor"
            }
            {
                type: "response"
                params: {
                    scalarsEvent: { stringMember: "duplex inbound", integerMember: 88 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "omxzdHJpbmdNZW1iZXJuZHVwbGV4IGluYm91bmRtaW50ZWdlck1lbWJlchhY"
                bodyMediaType: "application/cbor"
            }
        ]
    }
])
