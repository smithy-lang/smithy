$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Cbor
use smithy.test#InitialHttpRequest
use smithy.test#InitialHttpResponse
use smithy.test#eventStreamTests

// Edge cases for rpcv2Cbor event streams: the HTTP envelope on its own, and
// arbitrary precision inside an event payload. See eventstream-test.smithy for
// the body encoding and initial-response conventions that apply here too.
//
// RpcV2CborEventStreamRequestEnvelope is disabled, and is kept here verbatim
// so that it can be restored by uncommenting. The reference implementation
// responds to this request with an internal-failure status, because the event
// stream it carries holds zero frames. No specification makes an empty event
// stream a server error: the request is well formed, and both of the
// envelope's normative requirements are satisfied by it. The case is disabled
// pending resolution of that divergence.
//
// apply EventStreamRequest @eventStreamTests([
//     {
//         id: "RpcV2CborEventStreamRequestEnvelope"
//         documentation: """
//             A streaming request body uses the event stream media type, while
//             Accept stays application/cbor because this operation's response is
//             a buffered RPC response. An event streaming request must not set a
//             Content-Length, and no rpcv2Cbor request may name its operation in
//             a target header."""
//         protocol: rpcv2Cbor
//         initialRequest: {
//             method: "POST"
//             uri: "/service/RpcV2CborCorpusTests/operation/EventStreamRequest"
//             headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream", Accept: "application/cbor" }
//             forbidHeaders: ["Content-Length", "X-Amz-Target", "X-Amzn-Target"]
//         }
//         initialRequestShape: InitialHttpRequest
//     }
// ])
apply EventStreamResponse @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseEnvelope"
        documentation: """
            A buffered request that expects a streaming response sets Accept
            to the event stream media type while its own Content-Type stays
            application/cbor. Content-Length is permitted here, and indeed
            expected, because the request itself is buffered. The response
            envelope carries the event stream media type as its Content-Type,
            and the stream's own frames are covered elsewhere."""
        protocol: rpcv2Cbor
        initialRequest: {
            method: "POST"
            uri: "/service/RpcV2CborCorpusTests/operation/EventStreamResponse"
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/cbor", Accept: "application/vnd.amazon.eventstream" }
            forbidHeaders: ["X-Amz-Target", "X-Amzn-Target"]
        }
        initialRequestShape: InitialHttpRequest
        initialResponse: {
            code: 200
            headers: { "smithy-protocol": "rpc-v2-cbor", "Content-Type": "application/vnd.amazon.eventstream" }
        }
        initialResponseShape: InitialHttpResponse
    }
])

apply EventStreamResponse @eventStreamTests([
    {
        id: "RpcV2CborEventStreamResponseArbitraryPrecision"
        documentation: """
            EventStreamResponseOutput has no members besides the event stream,
            so there are no non-stream members to serialize. This case pins the
            reading under which an initial-response event is sent anyway and its
            payload is the serialization of that empty set of members, an empty
            CBOR map, carrying an application/cbor :content-type. The competing
            reading, under which no initial response exists to be sent, is
            unresolved against it; see the file header. An event payload keeps
            arbitrary precision the way a buffered body does: a bigInteger is
            CBOR tag 2 or tag 3 over a byte string holding the big-endian
            magnitude, and a bigDecimal is tag 4 over an [exponent, mantissa]
            pair whose mantissa here itself needs tag 2. Unlike the
            quoted-string rule of the JSON-based protocols, the tags are fixed
            normatively by this protocol's shape serialization table, so a bare
            integer or a float in place of either tag is a conformance failure:
            it routes the value through a double and loses precision. The
            [exponent, mantissa] pair is not scale-canonical, so compare the
            numeric value rather than the pair."""
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
                        bigIntegerMember: 1234567890123456789012345678901234567890
                        bigDecimalMember: 3.141592653589793238462643383279502884197
                    }
                }
                headers: {
                    ":message-type": { string: "event" }
                    ":event-type": { string: "scalarsEvent" }
                    ":content-type": { string: "application/cbor" }
                }
                body: "onBiaWdJbnRlZ2VyTWVtYmVywlEDoMkgdcDb87isvF+Wzj8K0nBiaWdEZWNpbWFsTWVtYmVyxII4JsJRCTt4vIi+kNk6woLlCuVp/WU="
                bodyMediaType: "application/cbor"
            }
        ]
        tags: ["arbitrary-precision"]
    }
])
