(client-guidance-checksums)=
# Checksums

Checksums allow clients and services to detect corruption of a message payload.
Smithy traits can define when clients calculate request checksums, validate
response checksums, and where checksum values appear on the wire.

This guide describes an architecture for implementing request and response
checksums in Smithy clients. It applies to fixed checksum requirements, such as
{ref}`smithy.api#httpChecksumRequired <httpChecksumRequired-trait>`, and to
protocol-specific traits that support algorithm selection, streaming trailers,
or response validation, such as
{ref}`aws.protocols#httpChecksum <aws.protocols#httpChecksum-trait>`.
The trait specifications define the normative behavior. This guide focuses on
how clients can implement that behavior without coupling checksum algorithms to
protocols, transports, or generated service clients.

## Architecture

Checksum support should be divided into the following components:

1. A **checksum resolver** interprets the operation schema, operation input,
   client configuration, and transport message. It selects an algorithm,
   determines whether a checksum is required, and resolves protocol details
   such as field names, value encodings, and header or trailer placement.
2. A **checksum provider** creates incremental checksum implementations by
   algorithm name.
3. **Body decorators** calculate request checksums while a body is read and
   validate response checksums while a body is consumed.
4. **Pipeline integration** coordinates checksum resolution with serialization,
   payload transformations, authentication, retries, and deserialization.

The checksum resolver is protocol-specific. It owns details such as
`Content-MD5`, `x-amz-checksum-*`, and how modeled input members affect
selection. The provider and body decorators should be protocol-independent.

A checksum provider can use an interface similar to the following:

```java
public interface ChecksumProvider {
    boolean supports(String algorithm);

    Checksum create(String algorithm);
}

public interface Checksum {
    void update(ByteBuffer bytes);

    byte[] finish();
}
```

Each call to `create` returns new mutable checksum state. This allows an
implementation to use the same provider for concurrent calls, request retries,
and multiple response bodies.

Resolvers should produce an immutable plan that contains the information needed
by the rest of the pipeline. A request plan commonly contains:

- The selected algorithm.
- Whether the checksum is required or optional.
- The header or trailer name.
- The wire encoding of the checksum value.
- Whether the value is supplied before transmission or calculated while
  streaming.

A response plan commonly contains the selected algorithm, the expected value,
and the metadata location used to report validation. Plans allow protocol logic
to be tested independently from hashing and stream behavior.

## Client configuration

Client configuration should distinguish modeled requirements from user
preferences.

A client can expose configuration for:

- The default request checksum algorithm.
- Whether optional request checksums are calculated.
- Whether optional response checksums are validated.
- The checksum provider or algorithm implementations.

Configuration must not disable checksum behavior required by the protocol or
operation. An algorithm explicitly selected by an operation input should take
precedence over a client default. If a required or explicitly selected
algorithm is unsupported, the client should fail before transmitting the
request.

Some protocols use modeled input members to select a request algorithm or
enable response validation. The effective value must be available when the
input is serialized. A client can accomplish this by updating the input before
serialization or by storing the resolved value in execution
[context](context.md) and making it available to the serializer. The effective
value should also be visible to later checksum processing.

## Request checksums

Request checksums are calculated over the byte sequence defined by the
protocol. They should be applied after serialization and after any payload
transformations that the checksum is intended to cover.

For example, a protocol may require the checksum to cover compressed bytes,
uncompressed bytes, or bytes before transport-specific framing. The checksum
layer should not infer this ordering. The protocol integration should arrange
the body decorators so that the checksum observes the required byte sequence.

### Explicit checksum values

A protocol can allow callers to supply a checksum directly through a modeled
member or transport field. When an applicable checksum value is already
present, the resolver should follow the protocol's conflict behavior. Protocols
commonly preserve the supplied value and skip calculation.

The generic checksum layer should not replace, recalculate, or validate an
explicit value unless the protocol requires it. Field lookup must follow the
case and multiplicity rules of the transport. For HTTP, see the discussion of
fields in the [HTTP client guidance](application-protocols/http.md).

### Headers and trailers

A checksum sent in a header must be known before the request is transmitted.
The client can calculate it directly when the body is replayable. A one-shot
body must either be buffered, rejected, or handled using a protocol-supported
streaming mechanism.

A checksum sent in a trailer can be calculated incrementally as the request
body is transmitted. The decorator updates checksum state for each chunk and
emits the final value after the payload. This avoids reading the entire body
before transmission.

Trailer support can require coordination between the protocol, authentication
scheme, and transport. The checksum resolver should determine the placement,
while the body decorator calculates the value. Transport or authentication
components should provide the framing required by the protocol.

### Authentication

Checksum fields and trailer declarations can be inputs to request signing. All
checksum metadata covered by authentication must be present before signing.

When a trailer checksum is calculated during transmission, the final value
cannot be known before signing. Protocol-specific integration should add the
trailer name, framing metadata, and any required signing sentinel before
signing, then calculate and emit the value while streaming.

A request checksum should not be added after signing unless the authentication
scheme explicitly excludes it or the protocol requires that ordering.

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
also be reset. A checksum decorator does not make a one-shot body replayable;
retry behavior for such a body remains subject to the client's streaming and
buffering policies.

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
checksum is present.

### Buffered responses

For a buffered response, the client can validate the body before
deserialization. Alternatively, it can give the deserializer a validating body
decorator and complete validation when the deserializer reaches the end of the
body.

A mismatch detected while processing an attempt should be represented as an
attempt error before attempt completion. This allows the retry strategy to
classify the error in the same way as other transport or deserialization
failures.

### Streaming responses

A streaming response should be validated without buffering the entire payload.
The client should return a body decorator that updates checksum state as the
caller reads. When the caller reaches the end of the stream, the decorator
compares the calculated value with the expected value.

This has two important consequences:

- Validation is not complete when the operation initially returns.
- A mismatch discovered after the operation returns cannot generally be retried
  transparently.

Closing or abandoning a response before reaching the end of the body does not
complete validation. Clients should not report successful validation in this
case. An implementation may drain a body on close only when doing so is
consistent with the language's normal stream behavior and resource limits.

Each response attempt must use new checksum state. A failed attempt's state
must not be reused for a later response.

## Validation metadata

Clients should provide a way to determine whether response validation occurred
and which algorithm was used. For streaming responses, the validation state can
change after the operation returns.

A useful state model distinguishes:

- Validation was not requested.
- No supported checksum was available.
- Validation is pending while a stream is consumed.
- Validation succeeded with a specific algorithm.
- Validation failed with a specific algorithm.

The exact representation is language-specific. It can be exposed through
response metadata, execution context, or a property on the validating stream.
Clients must not report validation as successful until the complete protected
payload has been consumed and compared.

## Errors

Checksum errors should identify whether the failure occurred while preparing a
request or validating a response and should include the selected algorithm.
Useful error categories include:

- A required or explicitly selected algorithm is unsupported.
- A checksum value has an invalid encoding or length.
- A request body cannot be read or buffered as required.
- A response checksum does not match the payload.
- Protocol-specific trailer or framing requirements cannot be satisfied.

The checksum layer should preserve the original stream or provider error as the
cause when possible.

## Pipeline integration

Checksum support is a reusable client feature rather than serializer,
transport, or generated service-client behavior. It can be installed by a
[client plugin](plugins.md) that registers
[interceptors](interceptors.md), checksum providers, and protocol-specific
resolvers.

The exact hooks depend on the client pipeline, but checksum integration
typically needs to:

- Resolve modeled defaults before serialization.
- Prepare a stable or replayable request body before the retry loop when
  possible.
- Add signed checksum metadata before authentication.
- Recreate attempt-specific checksum state before transmission.
- Wrap a response body before deserialization.
- Report validation results before attempt completion for non-streaming
  responses.

Serializers remain responsible for serializing modeled input values. They
should not calculate payload checksums. Transports remain responsible for
sending and receiving bytes. They can expose trailer and streaming primitives
without interpreting Smithy checksum traits.

## Testing

Checksum tests should cover protocol resolution separately from algorithm and
stream behavior. At minimum, test:

- Every required algorithm, including empty payloads and multiple chunk
  boundaries.
- Default, explicitly selected, unsupported, and user-supplied checksums.
- Header and trailer placement.
- Replayable, seekable, one-shot, asynchronous, and empty request bodies.
- Request body transformations such as compression and protocol framing.
- Authentication ordering and signed checksum metadata.
- Retries after partial request transmission.
- Matching, mismatching, malformed, absent, and unsupported response checksum
  values.
- Buffered and streaming responses, including partial consumption and early
  close.
- Validation state and algorithm metadata.

Protocol compliance tests should assert the normative wire behavior. Runtime
tests should additionally verify stream ownership, replayability, cleanup,
retry reset behavior, and error propagation.
