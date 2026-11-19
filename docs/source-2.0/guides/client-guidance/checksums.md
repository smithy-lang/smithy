(client-guidance-checksums)=
# Checksums

Checksums allow clients and services to detect corruption of a message payload.
Smithy traits can define when clients calculate request checksums, validate
response checksums, and where checksum values appear on the wire.

Smithy clients implement fixed checksum requirements, such as
{ref}`smithy.api#httpChecksumRequired <httpChecksumRequired-trait>`, and
protocol-specific traits that support algorithm selection, streaming trailers,
or response validation, such as
{ref}`aws.protocols#httpChecksum <aws.protocols#httpChecksum-trait>`.
Implementations must follow the trait specifications for normative behavior.
They should separate checksum algorithms from protocols, transports, and
generated service clients.

## Architecture

Implementations should divide checksum support into the following components:

1. A **checksum resolver** interprets the operation schema, operation input,
   client configuration, and transport message. It selects an algorithm,
   determines whether a checksum is required, and resolves protocol details
   such as field names, value encodings, and header or trailer placement.
2. **Checksum algorithms** identify implementations by the names used in Smithy
   traits and create independent incremental checksum state.
3. **Body decorators** calculate request checksums while a body is read and
   validate response checksums while a body is consumed.
4. **Pipeline integration** coordinates checksum resolution with serialization,
   payload transformations, authentication, retries, and deserialization.

The checksum resolver is protocol-specific. It owns details such as
`Content-MD5`, `x-amz-checksum-*`, and how modeled input members affect
selection. Checksum algorithms and body decorators should be
protocol-independent.

### Checksum interfaces

The following Java-like interfaces illustrate the required algorithm and state
capabilities:

```java
public interface ChecksumAlgorithm {
    /**
     * Returns the ID matched against algorithm names in Smithy traits.
     */
    String algorithmId();

    /**
     * Creates new, independent checksum state.
     */
    Checksum createChecksum();
}

public interface Checksum {
    /**
     * Returns the constant raw digest length in bytes.
     */
    int digestLength();

    /**
     * Returns an independent copy of the current checksum state.
     */
    Checksum copy();

    /**
     * Returns the digest of all data passed to update without resetting state.
     */
    byte[] digest();

    /**
     * Marks the current state so it can be restored by reset. The mark becomes
     * invalid after more than readLimit bytes are passed to update.
     */
    void mark(long readLimit);

    /**
     * Restores marked or initial checksum state.
     */
    void reset();

    /**
     * Adds another chunk of data to this checksum.
     */
    void update(ByteBuffer bytes);
}
```

Checksum operations are synchronous. Streaming clients call `update` with each
chunk as it becomes available rather than buffering the complete payload.

The resolver matches `algorithmId` against the algorithm names used in Smithy
traits, such as `CRC32C`. Each call to `createChecksum` returns new mutable
state. This allows the same algorithm implementation to be used for concurrent
calls, request retries, and multiple response bodies.

`digestLength` is the constant length of the raw digest in bytes. The protocol
resolver combines this length with the selected wire encoding to determine the
encoded value length when needed. Checksum implementations should not report
protocol-specific encoded lengths.

`digest` returns the checksum of all bytes passed to `update` and must not reset
the running state. Calling `digest`, updating with another chunk, and calling
`digest` again produces the checksum of all chunks. A one-shot convenience API
can be provided, but should be implemented compositionally using
`createChecksum`, `update`, and `digest`.

`copy`, `mark`, and `reset` allow checksum state to track replayable or
checkpointed streams. Languages should use their idiomatic copying and stream
marking conventions. A copy must be independent of the original state. When a
mark is active, resetting restores the state associated with that mark.
Otherwise, resetting restores the initial state.

The checksum implementation should return raw multi-byte values in network byte
order. Protocol integration then applies an encoding such as base64.

`update` must leave its input buffer's position, limit, and contents unchanged.
Decorators should also pass an independent duplicate so that a custom
implementation cannot change cursor state later observed by the transport. A
read-only duplicate prevents content changes.

Resolvers should produce an immutable plan that contains the information needed
by the rest of the pipeline. A request plan can contain these fields, depending
on the protocol:

- The selected algorithm.
- The source of the selection, such as an operation requirement, explicit input
  value, client default, or SDK default.
- Whether a checksum must be sent.
- The failure policy when the selected algorithm or required placement cannot
  be supported.
- The header or trailer name.
- The wire encoding of the checksum value.
- The raw and wire-encoded checksum value lengths.
- Whether the value is supplied before transmission or calculated while
  streaming.
- The component that owns any required trailer or payload framing.

A response plan contains the selected algorithm, the expected value from the
response fields, and the metadata location used to report validation. Plans
allow protocol logic to be tested independently from hashing and stream behavior.

Whether a checksum is required and whether resolution must fail are separate
decisions. For example, a protocol can make request checksums optional while a
caller explicitly selects an algorithm. If that selected algorithm is
unsupported, the request must fail rather than silently omit the checksum. A
plan should therefore carry an explicit failure policy or enough selection
source information for the resolver to derive one. A single `required` boolean
is not sufficient.

## Client configuration

Client configuration should distinguish modeled requirements from user
preferences.

A client can expose configuration for:

- The default request checksum algorithm.
- Whether optional request checksums are calculated.
- Whether optional response checksums are validated.
- The configured checksum algorithm implementations.
- The maximum amount of a request body that can be buffered to calculate a
  checksum header.
- The policy for a one-shot body whose size is unknown or exceeds that limit,
  such as using a protocol-supported trailer or failing the request.

Checksum algorithms should be configured as an ordered collection. Adding an
algorithm whose `algorithmId` matches an already-configured algorithm replaces
the previous implementation. This allows customers or runtime plugins to
override a default implementation, such as replacing a software checksum with
a hardware-accelerated implementation. Clients should allow the configured
algorithms to be retrieved in registration order.

```java
void addChecksumAlgorithm(ChecksumAlgorithm algorithm);

List<ChecksumAlgorithm> checksumAlgorithms();
```

Configuration must not disable checksum behavior required by the protocol or
operation. An algorithm explicitly selected by an operation input should take
precedence over a client default. If a required or explicitly selected
algorithm is unsupported, the client should fail before transmitting the
request.

The protocol determines which checksum placements are valid. A client body
preparation policy decides whether an eligible body is reused, buffered within
the configured limit, sent with a supported streaming checksum, or rejected.
This policy must not perform unbounded buffering. An unknown-length body should
not be read into memory or temporary storage without an explicit, finite
limit.

Protocols can use modeled input members to select a request algorithm or enable
response validation. The effective value must be available when the input is
serialized. A client can accomplish this by updating the input before
serialization or by storing the resolved value in execution
[context](context.md) and making it available to the serializer. The effective
value should also be visible to later checksum processing.

## Request checksums

The checksum decorator should calculate a request checksum over the byte
sequence defined by the protocol. Protocol integration should place the
decorator after serialization and after any payload transformations that the
checksum covers.

A protocol can require the checksum to cover compressed bytes, uncompressed
bytes, or bytes before transport-specific framing. Protocol integration must
arrange the body decorators so that the checksum observes the required byte
sequence.

### Explicit checksum values

A protocol can allow callers to supply a checksum directly through a modeled
member or transport field. When an applicable checksum value is already
present, the resolver should follow the protocol's conflict behavior. The
resolver should preserve the supplied value and skip calculation when the
protocol defines that behavior.

The generic checksum layer should not replace, recalculate, or validate an
explicit value unless the protocol requires it. Field lookup must follow the
case and multiplicity rules of the transport. For HTTP, see the discussion of
fields in the [HTTP client guidance](application-protocols/http.md).

### Headers and trailers

A checksum sent in a header must be known before the request is transmitted.
The client can calculate it directly when the body is replayable. For a
one-shot body, the configured body preparation policy must choose between
bounded buffering, a protocol-supported streaming placement, and rejection.
The resolver determines which placements the protocol permits. The body
preparation policy applies the configured size limit and fallback behavior.

A checksum sent in a trailer can be calculated incrementally as the request
body is transmitted. The decorator updates checksum state for each chunk and
emits the final value after the payload. This avoids reading the entire body
before transmission. The resolver can use `digestLength` to calculate the
encoded trailer value length before transmission so that the framing owner can
determine the complete encoded body length when the protocol requires it.

Trailer support can require coordination between the protocol, authentication
scheme, and transport. The checksum resolver should determine the placement,
while the body decorator calculates the value. Transport or authentication
components should provide the framing required by the protocol.

### Trailer ownership

Each request must have exactly one component that owns trailer and payload
framing. The request plan should identify that owner or identify an existing
framing layer with which the checksum feature must integrate.

The checksum decorator observes payload bytes and produces a final checksum
value. It should not independently add chunk framing when an authentication
scheme or transport already owns that framing. If an existing signing
implementation already wraps the payload, checksum integration should register
its trailer declaration and value producer with that implementation. The
transport must not then add a second framing layer.

Pipeline integration should detect incompatible or duplicate ownership and
fail before transmission. Double-wrapping a body can change the protected
bytes, invalidate content lengths or signatures, and emit duplicate trailer
declarations.

### Authentication

Checksum fields and trailer declarations can be inputs to request signing. All
checksum metadata covered by authentication must be present before signing.

When a trailer checksum is calculated during transmission, the final value
cannot be known before signing. Protocol-specific integration should add the
trailer name, framing metadata, and any required signing sentinel before
signing, then calculate and emit the value while streaming.

A request checksum should not be added after signing unless the authentication
scheme explicitly excludes it or the protocol requires that ordering.

The request ordering is:

1. Resolve modeled algorithm defaults and selections that must be visible
   during serialization.
2. Serialize the request and apply payload transformations that precede the
   checksum.
3. Resolve the final checksum placement, failure policy, and framing owner, and
   prepare the body according to the configured buffering policy.
4. Calculate and set any fixed checksum header. For trailer checksums, set the
   trailer declaration, framing metadata, and required signing sentinel.
5. Sign the request with those headers and declarations already present.
6. Transmit the request. A streaming decorator calculates the final checksum
   value over the protected bytes and the selected framing owner emits that
   value in the trailer.

Only the final trailer value is deferred until transmission. The existence and
name of the trailer, any checksum sentinel, and all metadata covered by the
signature must be fixed before signing.

### Replayability and retries

Calculating a header checksum consumes the body unless the stream can be read
without changing its position. Clients should preserve the body position or
replace the body with a replayable representation before entering the retry
loop.

When the protected bytes are identical for every attempt, a client should
calculate a header checksum once and reuse it. If an attempt-specific
transformation changes those bytes, the checksum must be recalculated before
that attempt is signed and transmitted.

Streaming checksum decorators contain mutable state and must be reset or
recreated for every attempt. The underlying body and any trailer producer must
also be reset. A checksum decorator does not make a one-shot body replayable.
Retry behavior for such a body remains subject to the client's streaming and
buffering policies.

Checksum state and body position must be restored together. A retry that
restarts the body from the beginning should use a reset or newly created
checksum. A stream that resumes from a marked checkpoint can restore matching
checksum state using `mark` and `reset`, or an independent state captured with
`copy`. A non-seekable body that can only be replayed from the beginning must
use new checksum state and recompute the checksum from the beginning.

## Response checksums

Response checksum validation begins after the transport response and its fields
are available. The resolver should:

1. Determine whether validation is enabled.
2. Find checksum values allowed by the operation and protocol.
3. Filter them to algorithms supported by the client.
4. Select one value according to the protocol's selection rules.
5. Create a validating body decorator.

The resolver should not invent an algorithm preference when the protocol
defines one. It should also follow the protocol's behavior when no supported
checksum is present. For `aws.protocols#httpChecksum`, response checksum values
are read from HTTP headers.

### Buffered responses

For a buffered response, the client can validate the body before
deserialization. Alternatively, it can give the deserializer a validating body
decorator and complete validation when the deserializer reaches the end of the
body.

The client should represent a mismatch detected while processing an attempt as
an attempt error before attempt completion. This allows the retry strategy to
classify the error in the same way as other transport or deserialization
failures.

### Streaming responses

A client should validate a streaming response without buffering the entire
payload. Each received chunk flows to checksum calculation and to the caller. A
client can implement this using a validating body decorator that updates
checksum state as the caller reads, or using an idiomatic stream tee that
exposes one branch to validation and one to the caller. Either approach must
preserve normal backpressure and resource bounds. When the caller reaches the
end of the stream, the validation path compares the calculated value with the
expected value.

Streaming validation changes completion and retry behavior:

- The operation can return while validation remains pending.
- A mismatch discovered afterward reaches the caller through the stream and
  falls outside the normal attempt retry loop.

Closing or abandoning a response before reaching the end of the body leaves
validation incomplete. Clients should report success only after comparing the
complete body. An implementation may drain a body on close only when doing so
is consistent with the language's normal stream behavior and resource limits.

Each response attempt must use new checksum state. A failed attempt's state
must not be reused for a later response.

## Validation metadata

Clients should provide a way to determine whether response validation occurred
and which algorithm was used. For streaming responses, the validation state can
change after the operation returns.

Validation metadata should distinguish:

- Disabled because the caller did not request validation.
- Skipped because the response contained no supported checksum.
- Validation is pending while a stream is consumed.
- Validation succeeded with a specific algorithm.
- Validation failed with a specific algorithm.

The representation is language-specific. Clients can expose it through response
metadata, execution context, or a property on the validating stream. Clients
must not report validation as successful until they consume and compare the
complete protected payload.

## Errors

Checksum errors should identify whether the failure occurred while preparing a
request or validating a response and should include the selected algorithm.
Checksum error categories include:

- A required or explicitly selected algorithm is unsupported.
- A checksum value has an invalid encoding or length.
- A request body cannot be read or buffered as required.
- A request body exceeds the configured buffering limit and no permitted
  streaming placement is available.
- A response checksum does not match the payload.
- Protocol-specific trailer or framing requirements cannot be satisfied.
- More than one component attempts to own trailer or payload framing.

The checksum layer should preserve the original stream or algorithm
implementation error as the cause when the language supports cause chaining.

## Pipeline integration

Checksum support is a reusable client feature rather than serializer,
transport, or generated service-client behavior. It can be installed by a
[client plugin](plugins.md) that registers
[interceptors](interceptors.md), checksum algorithms, and protocol-specific
resolvers.

Checksum integration should use client pipeline hooks to:

- Resolve modeled defaults before serialization.
- Resolve the selection source and failure policy independently from whether a
  checksum is required.
- Prepare a stable or replayable request body before the retry loop when the body
  fits within the configured buffering limit.
- Select exactly one trailer or payload-framing owner.
- Add fixed checksum headers, trailer declarations, signing sentinels, and
  other signed checksum metadata before authentication.
- Recreate attempt-specific checksum state before transmission.
- Produce only the final trailer value during transmission.
- Wrap a response body before deserialization.
- Report validation results for non-streaming responses before the pipeline
  completes the attempt.

Serializers remain responsible for serializing modeled input values. They
should not calculate payload checksums. Transports remain responsible for
sending and receiving bytes. They can expose trailer and streaming primitives
without interpreting Smithy checksum traits.

## Testing

Checksum tests should cover protocol resolution separately from algorithm and
stream behavior. Tests should cover:

- Every required algorithm, including empty payloads and multiple chunk
  boundaries.
- Constant raw digest lengths and their wire-encoded lengths when required for
  framing.
- Incremental updates, non-resetting `digest` calls, independent copies, and
  mark and reset behavior.
- Network-byte-order output for multi-byte checksum values.
- Algorithm registration order and replacement by matching `algorithmId`.
- Default, explicitly selected, unsupported, and user-supplied checksums.
- Optional checksums with an explicitly selected unsupported algorithm, which
  must exercise the plan's failure policy.
- Header and trailer placement.
- Header checksum buffering below, at, and above the configured limit,
  including unknown-length bodies.
- Replayable, seekable, one-shot, asynchronous, and empty request bodies.
- Custom checksum implementations that attempt to advance or otherwise mutate the
  byte buffer passed to `update`.
- Request body transformations such as compression and protocol framing.
- Authentication ordering, including fixed checksum headers and trailer
  declarations before signing and only the final trailer value during
  transmission.
- Existing authentication or transport framing, including detection of
  duplicate trailer ownership and prevention of double-wrapped bodies.
- Retries after partial request transmission.
- Matching, mismatching, malformed, absent, and unsupported response checksum
  values.
- Buffered and streaming responses, including partial consumption and early
  close.
- Validation state and algorithm metadata.

Protocol compliance tests should assert the normative wire behavior. Runtime
tests should additionally verify stream ownership, replayability, cleanup,
retry reset behavior, and error propagation.
