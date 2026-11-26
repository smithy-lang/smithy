(client-guidance-client-execution)=
# Client execution

A Smithy client brings together protocols, transports, endpoint resolution,
authentication, retries, plugins, interceptors, and modeled operation metadata
in an operation execution pipeline.

Implementations should express this pipeline using the conventions of their
programming language. They do not need to expose the internal execution
pipeline as public API.

## Client surface

Generated clients should provide a client type with a method for each operation.
Operation methods should accept the modeled input and return the modeled output
using the language's synchronous or asynchronous conventions.

```java
public interface WeatherClient extends AutoCloseable {
    GetForecastOutput getForecast(GetForecastInput input);

    GetForecastOutput getForecast(
            GetForecastInput input,
            RequestOverrideConfig overrides
    );
}
```

Typed operation methods are preferred because they are discoverable and preserve
the modeled input and output types. A client may additionally provide generic
operation dispatch for dynamic clients, test harnesses, or callers that select
an operation at runtime.

Client execution settings are not modeled operation input. Settings such as
timeouts, retry limits, authentication preferences, transport options, and
call-scoped plugins belong in a separate override mechanism.
If a language idiom combines modeled input and execution settings in one value,
execution settings should use an unambiguous reserved namespace and must not be
serialized as modeled input.

Clients should be safe to use for concurrent calls when the language supports
concurrent execution. This does not imply that operation inputs, outputs, body
streams, or user-provided components are safe for concurrent use.

## Client construction

A client should provide defaults for optional components and allow callers to
configure or replace the components described below. Construction can use a
builder, configuration object, constructor parameters, or the language's
standard client-construction pattern.

Construction should produce a fixed client configuration. An immutable
configuration makes it possible to share a client safely and to derive a
configuration for one call without changing later calls. Languages that use
mutable builders should produce an immutable or concurrency-safe resolved
configuration when construction completes.

Construction should:

- Apply and resolve [client plugins](plugins.md).
- Resolve model- or service-provided defaults.
- Select a protocol implementation.
- Validate that the protocol and transport use compatible message types when
  the language's type system does not enforce compatibility.
- Validate required endpoint and authentication configuration.
- Build the interceptor chain.
- Acquire resources owned by the client.

Validation that does not depend on operation input should happen during
construction. Invalid configurations should fail before an operation is sent.
If construction fails after acquiring resources, the client should release
those resources before propagating the failure.

### Configuration components

A resolved client configuration can contain these components when required by
the service and protocol:

| Component | Responsibility |
|---|---|
| Service metadata | Identifies the service and its modeled operations. |
| [Protocol](application-protocols/index.md) | Serializes operation input and deserializes responses. |
| [Transport](application-protocols/index.md) | Sends protocol messages and returns protocol responses. |
| [Endpoint resolver](endpoints.md) | Selects the endpoint for an operation attempt. |
| [Auth scheme resolver](identity-auth.md) | Selects a supported authentication scheme. |
| [Identity resolvers](identity-auth.md) | Resolve identities used by authentication schemes. |
| [Retry strategy](retries.md) | Controls retry permission, delay, and accounting. |
| [Plugins](plugins.md) | Configure and compose client features. |
| [Interceptors](interceptors.md) | Observe or modify defined execution stages. |
| [Context](context.md) | Carries typed extensible configuration and call state. |

Implementations should keep these responsibilities separate even when several
components are implemented by the same package. Implementations may support
non-HTTP protocol and transport abstractions.

Components shared by concurrent calls must be safe for that use. A component
that retains mutable execution state should use the client, execution, or
attempt scope that matches the lifetime of that state. Implementations with
call wrappers may also need call-scoped state.

### Call-scoped overrides

A client may provide a per-call override mechanism. Per-call overrides must not
mutate the resolved client configuration. The client should keep these
overrides separate from modeled input and give them precedence over the
corresponding client setting.

An override should derive an effective configuration for the operation.
Implementations can use immutable copies, copy-on-write overlays, or builders
that create an isolated configuration.

Not every client setting needs to be replaceable for a single call. A setting
should be overridable when its state and resources fit within the call
lifecycle. Per-call retry limits and transport timeouts fit this scope. A
client-owned event loop does not.

When a call-scoped override changes the protocol or transport, the client must
rebuild any cached pipeline state that depends on them. The client must also
revalidate compatibility when the language's type system does not enforce it.
Call-scoped plugins follow the resolution and deduplication rules in the
[plugin guidance](plugins.md).

The client should release resources acquired for an operation after its
execution finishes. If a call wrapper starts multiple executions, it should
release call-scoped resources after all delegations finish. A streaming output
retains the resources it needs until the caller releases that output.

## Execution scopes

Operation execution has two required nested scopes:

- An **execution** is one end-to-end traversal of the execution pipeline.
- An **attempt** is one try within an execution that may transmit a request.

Without call wrappers, an operation invocation starts one execution. Each
execution contains one or more attempts when retries are enabled.

```text
operation invocation
└── execution
    ├── serialization
    ├── attempt 1
    ├── retry delay
    └── attempt 2
```

An implementation may add a **call** scope when it supports
{ref}`call-wrapping interceptors
<client-guidance-interceptors-call-wrappers>`. A call is one operation
invocation made through the client. A call wrapper can return without starting
an execution or start multiple executions for behavior such as fallback or
hedging.

State must use the narrowest scope that gives it the required behavior:

| Scope | Example state |
|---|---|
| Client | Resolved plugins, protocol, transport, shared retry capacity. |
| Call, when supported | Effective configuration, call context, call-owned resources. |
| Execution | Retry state, serialized baseline request, final result. |
| Attempt | Attempt number, resolved endpoint, identity, signature, response. |

A client must not store execution or attempt state in shared mutable fields
unless that state is designed for concurrent access. The same rule applies to
call state when an implementation supports call wrappers.

### Context

Each execution should have an isolated [context](context.md) derived from the
client configuration. The client configuration exposed through the context
should be immutable. Integrations can add execution state to the context using
typed or otherwise collision-resistant keys.

Implementations with call wrappers may derive multiple execution contexts from
one call-scoped context. Values limited to one execution or attempt must not
leak into another delegation.

Attempt-scoped context should expose values required by components and
interceptors, such as the attempt number, retry state, selected endpoint,
identity, and service request identifier. Sensitive identity and signing data
should not be exposed more broadly than necessary.

## Preparing an operation

Before execution begins, a client should:

1. Start with the resolved client configuration.
2. Apply call-scoped plugins and explicit overrides.
3. Resolve an effective configuration in which explicit overrides take
   precedence.
4. Revalidate and rebuild configuration-dependent pipeline state.
5. Start the execution.

An implementation may provide `modifyBeforeCall` and call wrappers as an outer
call lifecycle. If provided, `modifyBeforeCall` runs after call-scoped plugins
are applied, explicit overrides retain their precedence, and wrappers are
resolved from the effective configuration. Each wrapper delegation starts an
execution. An error raised before a wrapper delegates does not enter
execution-completion hooks.

## Execution pipeline

The pipeline should orchestrate replaceable components rather than implement
their behavior directly. Use the following flow. The
{ref}`interceptor guidance <client-guidance-interceptors-hook-sequence>`
defines the exact hook sequence and hook mutation contracts.

### Initialize the execution

At the start of an execution:

1. Initialize execution state and the first attempt number.
2. Invoke `readBeforeExecution`.
3. Invoke `modifyBeforeSerialization`.
4. Invoke `readBeforeSerialization`.
5. Serialize the input into a transport request.
6. Invoke `readAfterSerialization`.
7. Invoke `modifyBeforeRetryLoop`.
8. Ask the retry strategy whether the initial attempt may proceed.

This pipeline serializes once per execution, outside the retry loop.
Attempt-specific behavior such as endpoint application, signing, and retry
headers belongs inside the attempt loop.

The request produced before the retry loop is the baseline request for the
execution. Mutations made for one attempt must not contaminate the baseline or
later attempts. Implementations can preserve this invariant with immutable
messages, explicit copies, or a transport-specific retry preparation
operation.

The retry strategy may delay or reject the execution before its first attempt.
Retry strategies should receive the configured retry scope and modeled
properties relevant to retry behavior.

### Prepare an attempt

Each attempt should begin by restoring or deriving an attempt request from the
execution's baseline request and updating attempt-scoped context.

The attempt then:

1. Invokes `readBeforeAttempt`.
2. Obtains the authentication scheme and identity for the attempt.
3. Obtains the endpoint for the attempt.
4. Applies endpoint and explicit request overrides.
5. Invokes `modifyBeforeSigning` and `readBeforeSigning`.
6. Signs the request.
7. Invokes `readAfterSigning`.

Authentication, identity, and endpoint resolution must follow the precedence
and compatibility rules in the
{ref}`identity and authentication guidance <client-guidance-identity-auth-order>`.
Clients should resolve the endpoint and identity for each attempt because
identities can expire and endpoints can change. An implementation that resolves
either once per execution must ensure that the value remains valid for every
attempt. Every attempt must receive a fresh signature. Implementations may
overlap independent asynchronous work as long as observable ordering and
property precedence are preserved.

The client should apply an endpoint to the serialized request without discarding
protocol bindings or previous interceptor changes. It should apply explicit
per-call request overrides at a documented point before signing so that the
signature covers the request that it will transmit.

The pipeline should close a signer that owns attempt-scoped resources after
signing. If signing fails, it should preserve the signing failure and attach any
cleanup failure when the language supports cause chaining.

### Transmit and deserialize

After signing, the attempt:

1. Invokes `modifyBeforeTransmit` and `readBeforeTransmit`.
2. Sends the request through the configured transport.
3. Invokes `readAfterTransmit`.
4. Invokes `modifyBeforeDeserialization` and
   `readBeforeDeserialization`.
5. Deserializes the response into modeled output or a modeled error.
6. Invokes `readAfterDeserialization`.

The client should pass every syntactically valid transport response to the
protocol implementation. The transport should report failures that prevented it
from returning a response as transport errors rather than manufacturing modeled
service errors.

The protocol implementation should preserve modeled errors separately from
transport, configuration, authentication, and interceptor failures. When the
transport or protocol exposes retry metadata, the client should attach it to a
common retry-relevant error representation rather than require the retry
strategy to recognize implementation-specific error types.

### Complete an attempt

Attempt completion runs for both successful and failed attempts:

1. Invoke `modifyBeforeAttemptCompletion` with the current output or error.
2. Invoke `readAfterAttempt`.
3. Classify the resulting outcome for retry.

A completion interceptor can transform an output, transform an error, recover
from an error by returning a valid output, or turn an output into an error.
After completion hooks finish, an attempt result should contain either a valid
output or an error, not an ambiguous empty result.

Retry classification occurs after attempt-completion interceptors so that the
retry strategy observes the final attempt outcome. For an outcome that could be
retried, the client should:

1. Establish that the transport and request body can be retried before
   consuming retry capacity.
2. Ask the retry strategy whether another attempt is permitted and obtain any
   delay.
3. Release or prepare the previous response and request resources.
4. Wait for the retry delay while honoring cancellation and deadlines.
5. Increment the attempt number and begin another attempt.

Retry preparation should account for whether the request body was started,
whether it was fully consumed, and whether it can be replayed. The transport
may need to discard or close a response before its connection can be reused.
Failure to prepare safely stops the retry even if the retry strategy otherwise
permits another attempt.

Signing must run again for a retry. Endpoint and identity resolution should run
again. Serialization and `modifyBeforeRetryLoop` must not run again.

An explicit per-call maximum should bound the attempts made for that call.
Implementations can treat it as a cap on the configured strategy or use it to
derive a call-specific strategy. It must not bypass retry-capacity decisions.

### Complete the execution

When no additional attempt will be made:

1. Report the final outcome to the retry strategy for success or quota
   accounting.
2. Release execution-scoped retry state.
3. Invoke `modifyBeforeCompletion`.
4. Finalize execution observability.
5. Invoke `readAfterExecution`.
6. Return the output or propagate the final error.

Execution-completion hooks should run whenever an execution started, including
when serialization, initial retry permission, or another pre-attempt stage
fails. Failures raised before the execution starts, such as an error from
`modifyBeforeCall` or a call wrapper that never delegates, are outside this
lifecycle.

Observability exporters and callbacks should not change the operation outcome.
An implementation that buffers telemetry until execution completion should
log and drop dispatch failures unless its public contract explicitly says
otherwise.

## Error control flow

The pipeline should track the current output and error as execution progresses.
When a new error supersedes an existing error, the implementation should retain
the previous error as a suppressed error, cause, or diagnostic attachment when
the language supports it. Otherwise, it should log the displaced error without
exposing sensitive data.

Use these error transitions:

- A failure after execution starts but before an attempt begins jumps to
  execution completion.
- A failure during an attempt jumps to attempt completion.
- A failure during attempt completion becomes the attempt outcome and is then
  classified for retry.
- A failure during retry classification or delay ends the retry loop and jumps
  to execution completion.
- A failure during execution completion becomes the final result.

Read-only boundary hooks such as `readBeforeExecution`, `readBeforeAttempt`,
`readAfterAttempt`, and `readAfterExecution` should invoke every registered
interceptor even if one fails. If several fail, the latest failure is
authoritative and earlier failures should be retained diagnostically when
the language supports cause chaining. Other hooks can fail fast as described in the
{ref}`interceptor error guidance <client-guidance-interceptors-error-handling>`.

Clients must classify cancellation requested through the execution context as
terminal. Cleanup and completion hooks must still run for a cancelled
execution.

## Streaming operations

For a streaming output, deserialization can complete by returning a modeled
output that owns a response body stream. Execution completion then represents
handoff of the stream to the caller, not completion of all network I/O.

The client should:

- Transfer ownership of the response stream explicitly.
- Keep resources required by the stream alive until it is consumed or closed.
- Close or discard the response when deserialization or retry preparation
  fails.
- Document where errors that occur after handoff are reported.
- Avoid automatically retrying after returning a streaming output.

Telemetry that measures complete stream transfer should wrap or observe the
stream rather than treat `readAfterExecution` as the end of I/O.

For streaming input, retry safety depends on whether the stream is replayable
and how much of it the transport consumed. Event-stream operations may
establish a stricter retry boundary once the request stream starts. The client
must not retry a non-replayable input stream.

## Concurrency, deadlines, and cancellation

The execution API should follow the language's concurrency model. Blocking
methods, futures, promises, callbacks, fibers, and lightweight threads can all
implement the same pipeline semantics.

Deadlines and cancellation should be carried through the execution context and
observed by:

- Retry delays.
- Endpoint and identity resolution when they can block.
- Signing operations that perform I/O.
- Connection acquisition and transport I/O.
- Call wrappers that start additional executions.

An overall call deadline applies across every execution and retry attempt.
Attempt-specific timeouts must not extend the remaining call deadline.

Hedged or concurrent wrapper delegations require independently usable request
bodies and execution state. A wrapper must not concurrently consume a single
non-replayable input stream or share mutable attempt state between
delegations.

## Resource ownership and client lifecycle

A client that owns resources should provide a close or shutdown operation using
the conventions of its language. Close should be idempotent when the language's
resource conventions expect idempotence.

Ownership must be explicit:

- The client closes resources it created and owns.
- The client does not close user-supplied components unless configuration
  explicitly transfers ownership.
- The attempt pipeline releases attempt-scoped resources at attempt completion.
- The execution pipeline releases execution-scoped resources at execution
  completion.
- An implementation with call wrappers releases call-scoped resources after
  wrappers finish.
- The streaming output releases transferred resources when the caller closes
  it.

If cleanup fails while another failure is already propagating, the original
failure should remain authoritative and the cleanup failure should be
suppressed or attached. If no prior failure exists, a cleanup failure can be
reported to the caller.

## Extensibility and module boundaries

[Plugins](plugins.md) configure the client and install features.
[Interceptors](interceptors.md) observe and modify defined execution stages.
Resolvers and strategies replace specific behaviors. Implementations should
prefer these bounded extension points over exposing a generalized mutable
middleware stack.

Service-specific code generation integrations should express runtime behavior
through plugins and modeled metadata. Generated operation methods should remain
thin delegates to a shared execution implementation rather than generate a
distinct pipeline architecture for each service.

Implementations should separate runtime components so that users do not need
unrelated dependencies. Backend-specific implementations should be optional
dependencies when the language's packaging model supports them. Implementations
are not required to reproduce a fixed API-module and implementation-module
hierarchy. Package and module boundaries should follow language conventions.

A statically generated client should not require the complete source model or
Smithy JSON AST to invoke an operation. It can retain compact runtime schemas
and metadata needed for generic serialization, endpoint resolution,
authentication, interceptors, or other runtime behavior. Implementations may
also provide explicitly dynamic clients that load a model at runtime.

## Observability

Implementations should expose stable lifecycle boundaries to observability
integrations. Observability integrations can measure:

- Total call and execution duration.
- Attempt count and attempt duration.
- Retry delays and final retry outcome.
- Serialization and deserialization duration.
- Endpoint and identity resolution duration.
- Signing duration.
- Transport acquisition and I/O duration.

Telemetry should identify the service and operation and can include the attempt
number, protocol, outcome, error type, and service request identifier when
available. Attribute names and values should have bounded cardinality.
Credentials, signatures, authorization fields, and unclassified modeled data
must not be recorded.

## Testing client execution

Client execution tests should cover:

- Construction with defaults and with each replaceable component.
- Construction-time validation and cleanup after construction failure.
- Typed operation methods and any generic dispatch API.
- Protocol and transport compatibility when runtime validation is required.
- Client configuration immutability.
- Isolation between concurrent calls.
- Call-scoped override precedence and plugin deduplication.
- Call wrappers that invoke zero, one, and multiple executions when supported.
- The complete interceptor sequence for success and failure paths.
- Serialization occurring once across multiple retry attempts.
- Endpoint and identity validity across retry attempts.
- Fresh signing for each attempt.
- Attempt request isolation.
- Retry permission, delay, outcome accounting, and exhaustion.
- Per-call attempt limits.
- Request replayability and transport retry preparation.
- Modeled errors, transport errors, interceptor errors, and cancellation.
- Deadline enforcement during resolution, transmission, and retry delay.
- Cleanup failures with and without a prior failure.
- Client-, execution-, and attempt-scoped resource ownership.
- Call-scoped resource ownership when call wrappers are supported.
- Streaming input and output handoff, failure, and close behavior.

Conformance tests should use deterministic fake protocols, transports,
resolvers, identities, clocks, and retry strategies. Tests that depend on a
public service or real retry delays are slower and make lifecycle ordering
harder to verify.
