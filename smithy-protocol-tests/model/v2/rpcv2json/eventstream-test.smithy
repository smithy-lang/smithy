$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#InitialHttpRequest
use smithy.test#InitialHttpResponse
use smithy.test#eventStreamTests

apply EventStreamResponse @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamResponseScalars"
        documentation: "Deserializes an event whose implicit payload carries every scalar type"
        protocol: rpcv2Json
        events: [
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"booleanMember":true,"byteMember":9,"shortMember":512,"integerMember":1234,"longMember":9000000000,"floatMember":1.5,"doubleMember":2.25,"stringMember":"scalars event","blobMember":"Zm9v","timestampMember":1609502096,"stringEnum":"Foo","intEnum":1}"""
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "RpcV2JsonEventStreamResponseList"
        documentation: "Deserializes an event whose payload carries lists of scalars"
        protocol: rpcv2Json
        events: [
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"strings":["first","second"],"integers":[1,2,3],"timestamps":[1609502096,1609588496]}"""
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "RpcV2JsonEventStreamResponseMap"
        documentation: "Deserializes an event whose payload carries maps of scalars"
        protocol: rpcv2Json
        events: [
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"strings":{"alpha":"first","beta":"second"},"integers":{"one":1,"two":2},"timestamps":{"start":1609502096,"end":1609588496}}"""
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "RpcV2JsonEventStreamResponseStruct"
        documentation: "Deserializes an event whose payload nests a structure"
        protocol: rpcv2Json
        events: [
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"value":{"stringMember":"nestedString","integerMember":55,"booleanMember":true}}"""
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "RpcV2JsonEventStreamResponseUnion"
        documentation: "Deserializes an event whose payload nests a union"
        protocol: rpcv2Json
        events: [
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"value":{"stringValue":"union event"}}"""
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "RpcV2JsonEventStreamResponseMultipleEvents"
        documentation: """
            Dispatches a sequence of events to the correct union variants. The
            heartbeat event models an empty structure, so it carries no payload
            and therefore no :content-type header."""
        protocol: rpcv2Json
        events: [
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"booleanMember":true,"byteMember":7,"shortMember":300,"integerMember":11,"longMember":8000000001,"floatMember":3.25,"doubleMember":4.75,"stringMember":"first message","blobMember":"YmFy","timestampMember":1609588496,"stringEnum":"Bar","intEnum":2}"""
                bodyMediaType: "application/json"
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
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"strings":["third","fourth"],"integers":[4,5],"timestamps":[1609674896,1609761296]}"""
                bodyMediaType: "application/json"
            }
        ]
    }
])

apply EventStreamResponseBlobPayload @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamResponseBlobPayload"
        documentation: """
            A blob @eventPayload is written as the raw message payload, not
            base64-encoded, and takes the application/octet-stream content
            type. The modeled @eventHeader member is carried as a message
            header and does not appear in the payload."""
        protocol: rpcv2Json
        events: [
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

apply EventStreamResponseHeaders @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamResponseHeaders"
        documentation: """
            Each @eventHeader member is carried as a typed event stream message
            header rather than in the payload, and a string @eventPayload takes
            the text/plain content type."""
        protocol: rpcv2Json
        events: [
            {
                type: "response"
                params: {
                    headerEvent: {
                        stringHeader: "headerString"
                        integerHeader: 1234
                        booleanHeader: true
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

apply EventStreamResponseImplicitPayload @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamResponseImplicitPayload"
        documentation: """
            With no @eventPayload member, the remaining non-header members form
            an implicit document payload. The @eventHeader member is excluded
            from that payload and appears only as a message header."""
        protocol: rpcv2Json
        events: [
            {
                type: "response"
                params: {
                    dataEvent: {
                        requestId: "request-91"
                        content: "implicit payload content"
                        count: 33
                        nested: { stringMember: "nestedString", integerMember: 44, booleanMember: true }
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "dataEvent" }
                    ":content-type": { string: "application/json" }
                    requestId: { string: "request-91" }
                }
                body: """
                    {"content":"implicit payload content","count":33,"nested":{"stringMember":"nestedString","integerMember":44,"booleanMember":true}}"""
                bodyMediaType: "application/json"
            }
        ]
    }
])

apply EventStreamError @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamErrorModeled"
        documentation: """
            A modeled error event uses :message-type "exception" and names the
            union member in :exception-type, and resolves to the modeled error
            shape rather than a generic failure."""
        protocol: rpcv2Json
        events: [
            {
                type: "response"
                params: {
                    streamError: { message: "stream failed", code: 57 }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "streamError" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"message":"stream failed","code":57}"""
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: { errorId: StreamError }
        }
        appliesTo: "client"
    }
    {
        id: "RpcV2JsonEventStreamErrorAfterMessage"
        documentation: """
            An error event arriving after successfully delivered events still
            resolves to the modeled error shape, so the events preceding it
            must not mask it."""
        protocol: rpcv2Json
        events: [
            {
                type: "response"
                params: {
                    messageEvent: { content: "delivered before failure", sequence: 66 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "messageEvent" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"content":"delivered before failure","sequence":66}"""
                bodyMediaType: "application/json"
            }
            {
                type: "response"
                params: {
                    streamError: { message: "stream failed", code: 57 }
                }
                headers: {
                    ":message-type": { string: "exception" }
                    ":exception-type": { string: "streamError" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"message":"stream failed","code":57}"""
                bodyMediaType: "application/json"
            }
        ]
        expectation: {
            failure: { errorId: StreamError }
        }
        appliesTo: "client"
    }
    {
        id: "RpcV2JsonEventStreamErrorUnmodeled"
        documentation: """
            Clients must handle a structured but unmodeled error, which uses
            :message-type "error" with the code and message carried entirely in
            headers and no payload."""
        protocol: rpcv2Json
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "error" }
                    ":error-code": { string: "InternalFailure" }
                    ":error-message": { string: "an unknown error occurred" }
                }
            }
        ]
        expectation: {
            failure: {}
        }
        appliesTo: "client"
    }
])

apply EventStreamRequest @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamRequestSendMessage"
        documentation: "Serializes a single struct-payload event onto a request stream"
        protocol: rpcv2Json
        events: [
            {
                type: "request"
                params: {
                    sendMessage: { content: "outbound message" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"content":"outbound message"}"""
                bodyMediaType: "application/json"
            }
        ]
    }
    {
        id: "RpcV2JsonEventStreamRequestMultipleEvents"
        documentation: """
            Serializes a sequence of events onto a request stream, closing with
            an empty-structure event that carries no payload and therefore no
            :content-type header."""
        protocol: rpcv2Json
        events: [
            {
                type: "request"
                params: {
                    sendMessage: { content: "outbound message" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"content":"outbound message"}"""
                bodyMediaType: "application/json"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "second outbound message" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"content":"second outbound message"}"""
                bodyMediaType: "application/json"
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
            }
        ]
    }
])

apply EventStreamInitialResponse @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamInitialResponse"
        documentation: """
            Non-stream output members are delivered ahead of the stream. Under
            rpcv2Json the operation's @httpHeader bindings are ignored, so they
            travel in the payload of an initial-response event rather than in
            HTTP response headers."""
        protocol: rpcv2Json
        initialResponse: {
            code: 200
            headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/vnd.amazon.eventstream" }
        }
        initialResponseParams: { sessionId: "session4a2f", timeout: 30 }
        initialResponseShape: InitialHttpResponse
        events: [
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"sessionId":"session4a2f","timeout":30}"""
                bodyMediaType: "application/json"
            }
            {
                type: "response"
                params: {
                    scalarsEvent: { stringMember: "message after initial response", integerMember: 77 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"stringMember":"message after initial response","integerMember":77}"""
                bodyMediaType: "application/json"
            }
        ]
    }
])

apply EventStreamInitialRequest @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamInitialRequest"
        documentation: """
            Non-stream input members are sent ahead of the stream. Under rpcv2Json
            the operation's @httpHeader bindings are ignored, so they travel in the
            payload of an initial-request event rather than in HTTP headers."""
        protocol: rpcv2Json
        initialRequestParams: { requestId: "request-5f1c", priority: 7 }
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2JsonCorpusTests/operation/EventStreamInitialRequest"
            headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/vnd.amazon.eventstream", Accept: "application/json" }
        }
        initialRequestShape: InitialHttpRequest
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-request" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"requestId":"request-5f1c","priority":7}"""
                bodyMediaType: "application/json"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "after initial request" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"content":"after initial request"}"""
                bodyMediaType: "application/json"
            }
        ]
    }
])

apply EventStreamDuplex @eventStreamTests([
    {
        id: "RpcV2JsonEventStreamDuplex"
        documentation: """
            A duplex stream carries both initial messages: the input members go out
            in an initial-request event and the output members come back in an
            initial-response event, each ahead of their own stream's events. Accept
            and Content-Type are both the event stream media type."""
        protocol: rpcv2Json
        initialRequestParams: { requestId: "duplex-3ba9" }
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2JsonCorpusTests/operation/EventStreamDuplex"
            headers: {
                "smithy-protocol": "rpc-v2-json"
                "Content-Type": "application/vnd.amazon.eventstream"
                Accept: "application/vnd.amazon.eventstream"
            }
        }
        initialRequestShape: InitialHttpRequest
        initialResponseParams: { sessionId: "duplex-session-77" }
        initialResponse: {
            code: 200
            headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/vnd.amazon.eventstream" }
        }
        initialResponseShape: InitialHttpResponse
        events: [
            {
                type: "request"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-request" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"requestId":"duplex-3ba9"}"""
                bodyMediaType: "application/json"
            }
            {
                type: "request"
                params: {
                    sendMessage: { content: "duplex outbound" }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "sendMessage" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"content":"duplex outbound"}"""
                bodyMediaType: "application/json"
            }
            {
                type: "response"
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "initial-response" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"sessionId":"duplex-session-77"}"""
                bodyMediaType: "application/json"
            }
            {
                type: "response"
                params: {
                    scalarsEvent: { stringMember: "duplex inbound", integerMember: 88 }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/json" }
                }
                body: """
                    {"stringMember":"duplex inbound","integerMember":88}"""
                bodyMediaType: "application/json"
            }
        ]
    }
])
