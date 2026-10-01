# SSE Framing for Event Streams

[**Draft**]

**Author**: Akash Goel
**Created**: 2026/07/23

## Abstract

This document proposes `smithy.protocols#sseJson`, a protocol that frames
Smithy event streams as Server-Sent Events (`text/event-stream`) with JSON
event payloads. The existing `@streaming` union model is reused unchanged;
only the wire framing differs from existing protocols. No changes to the
core specification, the event stream model, or existing protocols are
required.

## Motivation

SSE is the dominant contract for streaming LLM and agent output over HTTP.
OpenAI-style APIs, the [AG-UI protocol](https://docs.ag-ui.com/concepts/events),
and [MCP's Streamable HTTP transport](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports)
all use `text/event-stream`, because browsers consume it natively
(`EventSource`, `fetch`), it traverses ordinary HTTP infrastructure, and it
is human-readable on the wire.

Smithy's event stream model is a natural fit for these services: a
`@streaming` union of named event structures is exactly the shape of an SSE
stream. However, every current protocol binds event streams to
`application/vnd.amazon.eventstream`, a binary encoding that browsers and
the agent-tooling ecosystem do not speak. Teams modeling agent services in
Smithy today must either exclude their streaming endpoints from the model or
hand-write the streaming layer outside of generated code.

Comparable IDLs have already addressed this. TypeSpec ships SSE support as
a dedicated library ([`@typespec/sse`](https://typespec.io/docs/libraries/sse/reference/),
[microsoft/typespec#4513](https://github.com/microsoft/typespec/pull/4513))
using the same union-of-named-events shape proposed here. OpenAPI 3.2 added
[`itemSchema`](https://spec.openapis.org/oas/v3.2.0.html#media-type-object)
to model `text/event-stream` responses per-event
([OAI/OpenAPI-Specification#4554](https://github.com/OAI/OpenAPI-Specification/pull/4554)).

## Background

The Smithy specification already treats event stream framing as a protocol
concern:

> The serialization format and framing of messages sent over event streams
> is defined by the protocol.
> — [Event streams](https://smithy.io/2.0/spec/streaming.html)

The `rpcv2Cbor` and `rpcv2Json` specifications reserve room for exactly this
kind of addition:

> Other forms of content streaming MAY be added in the future, utilizing
> different values for `Accept`.

[SSE](https://html.spec.whatwg.org/multipage/server-sent-events.html) is a
WHATWG-specified, line-oriented text format. Each event is a block of
`field: value` lines terminated by a blank line. The relevant fields are
`event` (event type name), `data` (payload, possibly multi-line), `id`
(event identifier for resumption), and `retry` (reconnection delay hint).

Connect RPC evaluated and rejected SSE as a general streaming framing
([connectrpc/connect-go#772](https://github.com/connectrpc/connect-go/issues/772))
because SSE is text-only and server-to-client only. Those constraints shape
the goals below: this proposal targets response event streams with
non-binary payloads and does not replace `application/vnd.amazon.eventstream`.

## Goals and non-goals

**Goals**

1. Allow a Smithy service to model an SSE response stream using the existing
   `@streaming` union, with deterministic, specified wire mapping.
2. Keep the mapping compatible with plain `EventSource` consumers: a browser
   client with no Smithy tooling can consume a compliant stream.
3. Reuse existing machinery: HTTP bindings for the request,
   `smithy.test#eventStreamTests` for conformance.

**Non-goals**

1. Replacing or deprecating `application/vnd.amazon.eventstream`.
2. Input or bidirectional event streams. SSE is server-to-client;
   operations with input event streams are invalid under this protocol.
3. Binary event payloads. SSE is a text format; base64-wrapping blobs is
   wasteful and better served by the existing binary framing.
4. Modeling arbitrary third-party SSE APIs (e.g. matching OpenAI's exact
   wire format). The mapping is Smithy-defined; compatibility with specific
   external contracts can be evaluated later.

## High-level summary

A new protocol trait in the `smithy.protocols` namespace:

```smithy
$version: "2.0"

namespace smithy.example

use smithy.protocols#sseJson

@sseJson
service ChatService {
    version: "2026-07-23"
    operations: [Converse]
}

operation Converse {
    input := {
        message: String
    }
    output := {
        events: ConverseStream
    }
}

@streaming
union ConverseStream {
    delta: MessageDelta
    toolUse: ToolUseEvent
    done: CompletionSummary
    throttled: ThrottledError
}
```

A response is framed as:

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-store

event: delta
data: {"text":"Hel"}

event: delta
data: {"text":"lo"}

event: done
data: {"tokens":42}
```

## Proposal

### The `smithy.protocols#sseJson` trait

```smithy
/// An HTTP-based protocol that serializes buffered request/response
/// payloads as JSON and frames response event streams as Server-Sent
/// Events (text/event-stream) with JSON event payloads.
@trait(selector: "service")
@protocolDefinition(traits: [
    cors, endpoint, hostLabel, http, httpError, httpHeader, httpLabel,
    httpPayload, httpQuery, httpQueryParams, httpResponseCode
])
@traitValidators(
    "sseJson.NoInputEventStreams": {
        selector: "service ~> operation -[input]-> structure > member > union[trait|streaming]"
        message: "This protocol does not support input event streams; SSE is server-to-client only."
    }
    "sseJson.NoEventHeaders": {
        selector: "service ~> union[trait|streaming] > member > structure > member[trait|eventHeader]"
        message: "This protocol does not support event headers."
    }
    "sseJson.NoBinaryEventPayloads": {
        selector: "service ~> union[trait|streaming] > member :test(> structure > member[trait|eventPayload] > blob)"
        message: "This protocol does not support binary event payloads."
    }
)
structure sseJson {}
```

Whether buffered (non-streaming) operations follow restJson1-style HTTP
bindings (as sketched above) or rpcv2Json-style RPC semantics is the main
open decision; see FAQ. The event stream framing below is independent of
that choice.

### Event serialization

Each event stream event is serialized as one SSE event:

* The `event` field MUST be the event stream union member name
  (e.g. `event: delta`).
* The `data` field MUST be the event structure serialized as a single-line
  JSON object, following the same JSON serialization rules as the protocol's
  buffered payloads. If the member has an `@eventPayload` string member, the
  raw string is used; multi-line strings are split across `data:` lines per
  the WHATWG algorithm.
* The `id` field MAY be set by servers. When set, clients MUST expose it and
  SHOULD send `Last-Event-ID` on reconnection. Semantics of resumption are
  service-specific and out of scope.
* SSE comment lines (`: keep-alive`) MAY be sent by servers and MUST be
  ignored by clients.

### Errors

Modeled errors that are members of the event stream union are serialized
like any other event: `event: <memberName>`, JSON body in `data`. Client
codegen deserializes them into the corresponding exception type and
terminates the stream, matching existing event stream error behavior.

Errors that occur before the stream begins use normal (buffered) protocol
error serialization with an appropriate HTTP status code.

### Unknown events

A client receiving an `event` name that is not a member of the union MUST NOT
fail the stream. Implementations that support unknown variants surface it as
the `$unknown` variant; others discard it. This preserves the ability of
services to add event types backward-compatibly.

### Stream termination

The stream ends when the server closes the connection. This proposal does
not define an in-band terminal sentinel (such as OpenAI's `data: [DONE]`):
services that need a terminal signal model it as an ordinary event
(e.g. `done` above). This follows OpenAPI's position that sentinels are
application semantics, not framing
([discussion](https://github.com/OAI/OpenAPI-Specification/discussions/5096)),
and avoids TypeSpec's need for a special `@terminalEvent` construct. Client
codegen SHOULD distinguish orderly close from transport failure.

### Content negotiation

Requests for operations with response event streams MUST send
`Accept: text/event-stream`. Responses MUST set
`Content-Type: text/event-stream`. This uses the extension point already
reserved by the rpcv2 specifications.

### Protocol compliance tests

Conformance is expressed with the existing
[`smithy.test#eventStreamTests`](https://smithy.io/2.0/additional-specs/event-stream-protocol-compliance-tests.html)
trait; a `smithy-protocol-tests` module accompanies the protocol, following
the rpcv2Cbor precedent.

## Backward compatibility

This is purely additive: a new protocol trait in `smithy-protocol-traits`
and a new protocol specification document. No existing protocol, trait, or
model changes meaning. Services can advertise `sseJson` alongside existing
protocols, subject to the usual multi-protocol resolution rules.

## Guidance on code generation

Implementations reuse their existing event stream abstractions and swap the
framing layer: the typed union surface, per-event serde, and the
async-iteration interface exposed to callers are unchanged; only the
marshaller/unmarshaller for the wire format is new.
Servers SHOULD disable response buffering and flush per event. Clients in
browser environments MAY be generated over `fetch` streaming rather than
`EventSource` to support POST requests and typed errors.

## FAQ

### Why a new protocol instead of a framing option on existing protocols?

A member on existing protocols (like `eventStreamHttp`) would retroactively
touch every implementation of those protocols and imply support obligations
for AWS SDKs. A separate protocol trait keeps the blast radius small and
lets languages adopt it independently. TypeSpec reached the same conclusion,
shipping SSE as a separate library rather than in its core HTTP support.
If maintainers prefer a framing modifier instead, the event mapping in this
document is unchanged.

### Should this be rest-shaped or rpc-shaped for buffered operations?

Open question for maintainers. Agent/UI services typically want
`@http`-bound routes (rest-shaped), which is what the sketch shows. An
alternative is `smithy.protocols#rpcv2Json` gaining SSE as an alternate
`Accept` value, which its specification already anticipates.

### Why JSON-only payloads?

Every motivating consumer (browsers, AG-UI, MCP) uses JSON in `data`.
A future `sseCbor`-style protocol is not precluded but has no known demand.

### How does this relate to `application/vnd.amazon.eventstream`?

It doesn't replace it. The binary encoding supports headers, binary
payloads, checksums, and bidirectional streams; SSE supports none of those
and is chosen only for ecosystem interoperability of server-to-client
text streams.
