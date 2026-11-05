(client-guidance-http)=
# HTTP

A Smithy client sends serialized requests through an HTTP transport and receives
HTTP responses or transport errors. Implementations that provide their own HTTP
stack follow the same boundary.

The interfaces below illustrate this boundary. Implementations should use
established language-specific HTTP abstractions. They do not need to expose
connections, connection pools, or exchanges as public interfaces.

## HTTP transport integration

Smithy clients should allow users to configure the HTTP transport used to send
requests. This lets users select a transport that provides the required platform
integration, protocol versions, proxy behavior, or concurrency model.

An HTTP transport should implement the client's application-protocol-neutral
transport interface:

```java
public interface HttpClient
        extends ClientTransport<HttpRequest, HttpResponse> {}
```

The transport interface should accept per-attempt context in addition to the
request. Per-attempt context carries settings such as timeouts and cancellation
without placing execution concerns on the HTTP message itself.

Implementations should follow language conventions when representing a send as
a blocking call, future, promise, callback, or another construct. A blocking
interface can still support concurrency using facilities such as lightweight
threads, fibers, or an event loop. Smithy clients should not require every HTTP
transport to use the same concurrency mechanism.

### Transport compatibility

A Smithy client should verify that its protocol implementation and configured
transport use compatible request and response types. The client can enforce
compatibility through generic types, runtime type checks, or a message-exchange
descriptor.

### HTTP responses and transport errors

The HTTP transport should return every syntactically valid HTTP response,
including `3xx`, `4xx`, and `5xx` responses, as an `HttpResponse`. It should not
interpret a status code as a transport failure.

The transport should report failures that prevent it from producing an HTTP
response as transport errors. Examples include DNS resolution failure,
connection refusal, TLS negotiation failure, connection-acquisition timeout,
cancellation, and malformed transport data.

Transport errors should:

- Preserve the underlying cause when possible.
- Distinguish invalid requests from network failures and cancellation.
- Expose retry-relevant information without requiring retry strategies to
  recognize transport-specific exception types.
- Avoid retaining credentials or other sensitive request data unnecessarily.

See the [retry guidance](../retries.md) for the retry information exposed by
client errors.

### Lifecycle

An HTTP transport can own resources such as connection pools, sockets, event
loops, and executors. A transport that owns resources should provide an
idempotent close or shutdown operation. A client should close a transport only
if it owns that transport. It should not close a transport supplied by the user
unless the client contract explicitly transfers ownership.

## HTTP messages

{rfc}`9110` defines HTTP requests and responses as messages. Request and response
types should share common message properties:

```java
public interface HttpMessage extends AutoCloseable {
    HttpVersion httpVersion();

    HttpHeaders headers();

    DataStream body();
}
```

An HTTP message body should not be null. Messages without a body should use a
shared or inexpensive empty body.

Closing an HTTP message should close its body. Closing a message must be
idempotent if the language's resource conventions require idempotent close
operations.

### Requests

HTTP requests add a method and structured URI:

```java
public interface HttpRequest extends HttpMessage {
    String method();

    URI uri();
}
```

Requests should store the URI as a structured value rather than only as a
string. URI implementations should validate components and prevent ambiguous
authority parsing. Implementations should consider prohibiting URI user
information because it can lead to authority confusion and accidental
credential disclosure.

The URI fragment is not sent as part of an HTTP request target.

### Responses

HTTP responses add a status code:

```java
public interface HttpResponse extends HttpMessage {
    int statusCode();
}
```

A response may expose a reason phrase when the underlying HTTP version provides
one, but clients must not rely on reason phrases for protocol behavior.

### Message mutation

Serialization, endpoint resolution, authentication, and interceptors may each
need to modify an HTTP request. Mutations made for one attempt must not alter a
request retained by another attempt or by user code.

Implementations should use one of these approaches:

- Immutable messages whose mutation methods return a copy.
- Immutable messages with an explicit modifiable copy or builder.
- Mutable messages that are freshly copied at attempt boundaries.

Implementations that provide immutable and modifiable representations should
document whether conversion operations can return the current instance or
always create an isolated copy.

## HTTP fields

{rfc}`9110` collectively refers to header and trailer key-value pairs as
{rfc}`fields <9110#section-5>`.

Header names are case-insensitive and a header may contain multiple values.
Implementations should therefore represent headers as an iterable collection of
name-value pairs or as a map whose value type is a list:

```java
public interface HttpHeaders {
    static HttpHeaders of(Map<String, List<String>> headers) {
        return new UnmodifiableHttpHeaders(headers);
    }

    boolean contains(String name);

    String firstValue(String name);

    List<String> allValues(String name);

    Map<String, List<String>> toMap();
}
```

Implementations may preserve the original casing of a header name for display,
but lookup, replacement, and removal must be case-insensitive. Iteration should
preserve repeated values. Implementations should validate header names and
values before passing them to a transport.

Automatically joining repeated values at the HTTP message layer is unsafe.
{rfc}`9110#section-5` permits comma-separated combination only when a field's
grammar defines that behavior. Joining other fields can corrupt data. A
protocol implementation that understands a field's grammar may join or split
that field.

### Trailers

HTTP endpoints produce or receive trailers after a message body.
Implementations should model trailers separately from ordinary headers instead
of adding a position flag to header values.

The request message should expose trailers through a deferred provider that
resolves after the transport writes the request body. If the request can be
retried, the provider must either be replayable or provide a way to reset its
state.

The response message or body should expose trailers as deferred state. The state
should distinguish:

- Pending trailers while the body is still being received.
- Completed trailers, including completion with no trailer fields.
- Failed trailers when body transfer or trailer decoding fails.

The trailer API should distinguish access before completion from a completed
response that contains no trailers.

Request trailer support is transport-dependent. A transport that cannot send
trailers should expose that limitation and fail before starting the request
rather than silently dropping them.

## Message bodies

HTTP bodies need richer behavior than a bare iterable of bytes. Signing,
checksums, retries, compression, streaming, and connection reuse all depend on
body capabilities and lifecycle.

A body abstraction should expose the concepts illustrated below, using
language-appropriate types and names:

```java
public interface DataStream extends AutoCloseable {
    // Returns null or another sentinel when the length is unknown.
    Long contentLength();

    boolean isReplayable();

    boolean isAvailable();

    InputStream asInputStream();

    void discard();
}
```

Body implementations should support language-specific byte consumption
mechanisms such as blocking streams, byte buffers, channels, publishers,
iterators, or callbacks. The common contract should describe body semantics
without forcing every transport to use a single I/O mechanism.

### Content length

A body should distinguish a known zero length from an unknown length. A
transport that observes the complete request body must validate a declared
`Content-Length` against the supplied byte count. Premature end-of-file and
bytes beyond the declared length are request failures.

Before sending a request, the transport must reconcile metadata reported by the
body with the HTTP fields. It must not put conflicting `Content-Length` and
`Transfer-Encoding` framing on the wire.

### Replayability and availability

A replayable body supports consumption from its logical beginning for another
attempt. Implementations can replay a body by reopening a file, creating a new
publisher subscription, retaining immutable bytes, or rewinding a seekable
stream.

Availability answers whether a body can currently be consumed. After an
attempt, a replayable body is available if it can reopen or reset its source. A
one-shot body becomes unavailable after consumption starts.

Replayability should be an explicit capability rather than inferred only from
whether a low-level stream is seekable. A wrapper might replay a non-seekable
source by reopening it, while seeking an arbitrary stream might not return it
to the logical start of the request body.

### Consumption state and retries

Transports should retain enough request-body state to determine:

- Whether body transmission started.
- Whether the transport fully consumed the body.
- Whether the body can be reset or replayed.
- Whether an error occurred before any body bytes could have been sent.

A replayable body should be reset before another attempt. A non-replayable body
must not be retried after transmission might have started. A transport may
allow the original one-shot body to be used by a later attempt only when it can
reliably establish that the earlier attempt did not begin consuming or sending
it.

### Incremental bodies and backpressure

Implementations may support bodies that are produced while the request is being
sent. These implementations should:

- Have a bounded buffer or another backpressure mechanism.
- Distinguish temporary lack of data from end-of-file.
- Propagate producer failures to the transport.
- Allow the transport to detach when it stops consuming.
- Validate an expected length when one was provided.

When a streaming protocol assigns meaning to flush or chunk boundaries, body
and transport abstractions should preserve those boundaries.

### Closing and discarding

Closing and discarding a body can have different semantics:

- **Close** stops further use and may abort an active transfer.
- **Discard** drains or otherwise releases unread data so that the underlying
  connection can be reused when possible.

If an implementation exposes both operations, their behavior should be
documented and idempotent. If only close is exposed, closing must still release
transport resources even when it prevents connection reuse.

## Sending requests

The transport is responsible for translating an `HttpRequest` and its body into
valid HTTP framing. Protocol-specific details should not leak into serializers
unless the serializer is explicitly responsible for them.

### Request targets and redirects

The transport should derive the origin-form request target from the structured
URI for direct requests and use the form required by the configured proxy when
proxying.

HTTP transports used by Smithy clients should not automatically follow
redirects. The transport should return redirect responses to the client
pipeline. An automatic redirect can replay a one-shot body, send signed headers
to another authority, change the request method, or bypass the client's retry
and endpoint policies.

### Request framing

Transports should support known-length and unknown-length bodies using framing
appropriate for the negotiated HTTP version. HTTP/1.1 may use chunked transfer
encoding for unknown-length bodies. HTTP/2 and later versions use their native
message framing.

Transports may implement `Expect: 100-continue` to avoid sending a large body
when the server will reject the request from its headers. Handling must account
for final responses that arrive before or during body transmission.

### Bidirectional streaming

HTTP protocol versions and transports differ in their ability to send a request
body while receiving a response:

- HTTP/1.1 permits early responses, but a transport may finish writing the
  request before exposing the response body.
- HTTP/2 supports independent request and response streams on an exchange.

An implementation should expose bidirectional streaming only when the
transport and negotiated protocol can provide it. Client correctness must not
depend on HTTP/2 being selected unless the operation explicitly requires that
capability.

## Receiving responses

An HTTP transport may buffer a response body or return a streaming body.
Implementations should base buffering policy on known body size, configured
limits, and the requirements of the protocol operation.

### Header delivery and body completion

For a streaming response, the transport may return the `HttpResponse` after the
final response headers arrive while body transfer continues. This means:

- A successful send does not imply that the response body will finish
  successfully.
- Later reads may raise body transfer errors.
- Response trailers are not available until body completion.
- Transport resources remain associated with the response until its body is
  consumed, discarded, closed, or fails.

Interim `1xx` responses are not the final operation response. The transport
should process them according to HTTP semantics and return the final response.

### Bounded buffering and backpressure

Transports should avoid unbounded automatic buffering. A streaming response
body should use bounded buffering and apply backpressure when the consumer is
slower than the network.

Implementations may provide separate limits for:

- The largest body buffered automatically.
- Buffered bytes waiting for a streaming consumer.
- Total response bytes.
- Bytes produced after content decoding or decompression.

If a configured limit is exceeded, the response body and any dependent trailer
state should fail with the same underlying error.

### Content decoding

A transport may transparently decode content encodings such as `gzip`.
Implementations that do so should ensure that response metadata describes the
representation delivered to the caller. Stale `Content-Encoding` and
`Content-Length` fields should not describe the encoded representation after
the transport decodes it.

Implementations that enforce a response-size limit must apply it after content
decoding. A compressed response must not bypass the limit by expanding after
decoding.

## Per-attempt options

HTTP transports should support per-attempt options through typed context or
transport-specific options. Per-attempt values should override client-level
defaults, which in turn override transport defaults.

Per-attempt options include these settings when the transport supports them:

| Option | Scope |
|---|---|
| Request timeout | Time until a response is available, with behavior defined separately for buffered and streaming responses. |
| Connection-acquisition timeout | Time waiting for transport capacity before opening or leasing a connection or stream. |
| Connect timeout | Time establishing a new network connection. It does not apply when a pooled connection is reused. |
| TLS negotiation timeout | Time completing TLS negotiation on a new secure connection. |
| Read timeout | Maximum inactivity while receiving response data. |
| Write timeout | Maximum inactivity while sending request data. |
| Cancellation | A signal that stops acquisition, active transfer work, and later streaming-body work. |
| Proxy | Proxy selection or an override for the attempt. |
| Response limits | Buffer and total-size limits applied while receiving and decoding the body. |

Each transport should expose only the options it implements. Implementations
should not add settings to the common HTTP interface when they are meaningful
to only one HTTP engine.

### Timeout semantics

Documentation for each timeout should specify its start point, completion point,
and behavior when disabled.

For buffered responses, a request timeout may cover the complete response body.
For streaming responses, the documented timeout scope determines its end point.
A response-availability timeout ends when final headers and the streaming body
are returned. A full-response timeout remains active until body completion. A
read-inactivity timeout or call-level deadline can govern subsequent body
consumption when the request timeout ends at header delivery.

Timeouts and cancellation are distinct. A cancellation should preserve its
application-supplied cause when one is available.

## Protocol versions and capabilities

HTTP transports should document the protocol versions they offer and the
conditions for fallback. For TLS connections, transports should use ALPN to
select from supported versions and fall back only when the version policy
permits it.

Transports may allow users to require a particular protocol version, but must
fail when the negotiated version does not satisfy that requirement.
Version-policy APIs should be extensible rather than assuming HTTP/1.1 and
HTTP/2 are the only versions an implementation will ever support.

Capabilities such as bidirectional streaming, request trailers, response
trailers, and multiplexing depend on both the transport and negotiated protocol.
Implementations should define the behavior of each supported capability instead
of requiring a universal capability API.

## Connection management

Connection management is an implementation detail of an HTTP transport.
Implementations may expose connection managers, pools, connections, and
exchanges when callers need to configure or observe them. Smithy clients should
not require those public types.

A transport that manages connections should:

- Pool or reuse connections only when their route and security properties are
  compatible.
- Include scheme, authority, port, proxy, and TLS configuration when determining
  whether a connection is safe to reuse.
- Bound concurrent work globally, per route, or at both scopes.
- Apply backpressure or fail with a connection-acquisition timeout when
  capacity is exhausted.
- Allow acquisition waits to be cancelled.
- Validate idle connections before reuse when the underlying HTTP engine does
  not already do so.
- Evict connections after protocol or network failures that make reuse unsafe.
- Remove connections that exceed an idle lifetime.
- Account for HTTP/2 stream capacity separately from physical connection count.
- Release owned resources during shutdown.

Implementations should choose FIFO, LIFO, least-loaded, round-robin, or
protocol-specific load balancing based on their HTTP engine and target runtime.
Smithy guidance does not standardize the connection selection algorithm.

### Connection release

For HTTP/1.1, connection reuse requires a reusable connection and a fully
consumed or discarded response body. Closing a partially consumed response may
require closing the connection.

For multiplexed protocols, closing one response body should terminate or reset
only its stream when possible, not unrelated exchanges sharing the physical
connection.

User code must have a documented way to release a response without loading its
remaining body into memory.

## TLS and proxies

HTTP transports must enable TLS peer and hostname verification by default. They
should use the runtime or platform's secure TLS defaults and trust store unless
the user explicitly supplies another configuration.

Custom trust stores, client certificates, TLS providers, and cipher
configuration are transport-specific concerns. Implementations should expose
them through transport-specific extensions instead of requiring them on every
Smithy HTTP interface. APIs that weaken verification should require an explicit
unsafe option and document the risk.

Connection pools must include proxy selection in the identity of a reusable
route. HTTPS proxying should establish a tunnel before negotiating end-to-end
TLS with the target. Proxy credentials must not be forwarded to the target
service.

## Observability

An HTTP transport may expose events or metrics for request execution, DNS
resolution, connection acquisition, connection establishment, TLS negotiation,
body transfer, and connection reuse.

For streaming responses, implementations should distinguish final-header
arrival from body completion. A request-complete event should not report
success merely because response headers were returned. It should complete when
the body is consumed, discarded, or closed, or when the body fails.

Telemetry must not include authorization fields, cookies, sensitive query
parameters, request bodies, or other credentials by default.

## Testing HTTP transports

Transport tests should focus on observable behavior rather than a particular
connection implementation. A transport test suite should cover each supported
behavior:

- Case-insensitive and repeated headers.
- Known, unknown, empty, and incorrectly declared body lengths.
- Replayable and one-shot request bodies.
- Incremental request production and backpressure.
- Early and interim responses during request upload.
- Buffered and streaming responses.
- Slow consumers and bounded response buffering.
- Closing, discarding, and fully consuming response bodies.
- Response-body failures after headers have been returned.
- Request and response trailers.
- Cancellation during acquisition, upload, download, and body consumption.
- Acquisition, connect, request, read, and write timeouts.
- Redirect responses without automatic following.
- Connection reuse and eviction.
- Pool exhaustion and idle cleanup.
- HTTP/1.1 and HTTP/2 framing and lifecycle differences.
- Protocol negotiation failures.
- Proxy isolation.
- TLS verification.
- Encoded responses and decompression limits.
