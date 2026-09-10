(client-guidance-interceptors)=
# Interceptors

An **interceptor** is a general-purpose extension point that allows code to
observe or modify *specific* stages of a request execution, such as
serialization, signing, transmission, and deserialization. Interceptors may
also wrap an entire operation call. Interceptors are registered at client
creation time or per operation invocation.

The other sections of this guide describe specific extension points for known
use cases: retries, endpoint resolution, authentication, and transport. Those
interfaces are designed to handle tasks that every client implementation will
take on and which may need to be replaced or extended. In addition to those
specific extension points, it is recommended to provide interceptors as a more
general-purpose mechanism that allows users to implement a broader array of
extensions.

## Hooks

Interceptors inject logic through methods that are called at specific points in
the execution pipeline. These methods are called **hooks**.

Each hook provides either an **immutable** view of the current state or a
**mutable** view that allows modifications. It is important to maintain this
distinction, even in languages where immutability may not be enforceable. Hooks
often come in pairs: the mutable hook is called first so that all interceptors
can make their changes, then the immutable hook is called so that interceptors
can see the finalized state after all mutations have been applied.

Where possible, it is important to limit what can be mutated in a mutable hook
to only the properties that are relevant to that hook. This makes each hook
easier to reason about, and helps to ensure that a change in execution order
doesn't result in different behavior.

Hooks are intended to be lightweight, so blocking operations should not be
supported.

(client-guidance-interceptors-modify-before-call)=
### modifyBeforeCall

`modifyBeforeCall` is the first interceptor hook invoked for a call. It runs
once per call, before call wrappers and outside the execution and retry
lifecycles. The hook receives the operation input and current client
configuration and returns the configuration to use for the call.

The client invokes `modifyBeforeCall` on interceptors in registration order.
Each interceptor receives the configuration returned by the previous
interceptor. The final configuration is used to construct the execution
pipeline and resolve the interceptors that participate in the call. This allows
`modifyBeforeCall` to switch protocols for a single call, change other
call-scoped configuration, or add and remove interceptors.

Interceptors added by `modifyBeforeCall` participate in call wrapping and the
execution hooks, but do not receive `modifyBeforeCall` for the current call. An
error raised by this hook is propagated directly because execution has not yet
started.

### Call wrappers

Most interceptor hooks run at a fixed point in the execution pipeline. The
interceptor interface also includes call-wrapping hooks that enclose the entire
execution. Individual interceptor implementations may opt in to wrapping calls.

An interceptor opts in to call wrapping by returning `true` from
`interceptCalls`. Opted-in interceptors receive an `interceptCall` invocation
with an input hook and a continuation representing the remaining wrappers and
the normal execution pipeline. A wrapper may:

- Delegate once to continue normal execution.
- Delegate with a different operation input.
- Return an output without delegating.
- Delegate multiple times to implement behavior such as fallback or, where
  concurrent invocation is supported, hedging.
- Observe or transform the output or error produced by delegation.

Call wrappers compose by nesting. The first registered wrapper is the outermost
wrapper, unlike read and modify hooks, which iterate in registration order.
Wrappers run after `modifyBeforeCall` and outside the retry loop. Every
delegation to the continuation runs the normal execution pipeline, including
its retry loop.

Clients should resolve the opted-in wrappers when creating an interceptor chain
and provide a fast way to determine whether the chain contains wrappers. This
allows calls without wrappers to follow the normal execution path without
constructing a call-wrapper input hook or continuation.

The current client should be available through the input hook's context so that
a wrapper can re-enter the client when necessary. Re-entry starts a separate
call, including its call wrappers.

An error raised by a wrapper itself is propagated directly and is not processed
by execution completion hooks. Errors produced by a delegated execution are
processed by that execution's completion hooks before propagating back to the
wrapper.

### Hook sequence

The following is an ordered list of recommended hooks for one execution. The
sequence runs each time the call wrapper chain delegates to the normal execution
pipeline. If no call wrappers are configured, it runs once for the call. It is
also recommended to make the list of hooks modifiable, so that new hooks may be
added later.

:::{important}

In the following list, a **call** is one operation invocation made through the
client. An **execution** is one end-to-end delegation through the normal
pipeline. A call wrapper may cause a call to have zero, one, or multiple
executions. An **attempt** is a single try within an execution. There may be
multiple attempts if the request needs to be retried.

The **transport request** and **transport response** represent the serialized
requests and responses that are sent to and received from the service. For HTTP
protocols, these are HTTP requests and HTTP responses.
:::

1. **readBeforeExecution** *(immutable)* — The first thing called during an
   execution. This inspects the client state and the unmodified inputs to the
   operation.
2. **modifyBeforeSerialization** *(mutable)* — Modifies the input before it is
   serialized.
3. **readBeforeSerialization** *(immutable)* — Called immediately before the
   input is serialized.
4. **readAfterSerialization** *(immutable)* — Called immediately after the input
   is serialized.
5. **modifyBeforeRetryLoop** *(mutable)* — Modifies the transport request before
   the retry loop begins.
6. *(retry loop)*
   1. **readBeforeAttempt** *(immutable)* — The first thing called inside the
      retry loop.
   2. **modifyBeforeSigning** *(mutable)* — Can modify the transport request
      before signing.
   3. **readBeforeSigning** *(immutable)* — Called immediately before signing.
   4. **readAfterSigning** *(immutable)* — Called immediately after signing.
   5. **modifyBeforeTransmit** *(mutable)* — Can modify the transport request
      before it is sent.
   6. **readBeforeTransmit** *(immutable)* — Called immediately before the
      request is sent.
   7. **readAfterTransmit** *(immutable)* — Called immediately after the
      transport response is received.
   8. **modifyBeforeDeserialization** *(mutable)* — Can modify the transport
      response before deserialization.
   9. **readBeforeDeserialization** *(immutable)* — Called immediately before
      deserialization.
   10. **readAfterDeserialization** *(immutable)* — Called immediately after
       deserialization.
   11. **modifyBeforeAttemptCompletion** *(mutable)* — Can modify the output or
       error before the attempt ends.
   12. **readAfterAttempt** *(immutable)* — The last thing called inside the
       retry loop.
7. **modifyBeforeCompletion** *(mutable)* — Can modify the output or error
   before the execution ends.
8. **readAfterExecution** *(immutable)* — The last thing called during an
   execution.

### Error handling

Error handling behavior depends on where in the pipeline the hook is called and
whether or not the hook is mutable.

#### Error accumulation

When an immutable hook is being executed, all interceptors are always invoked
and any errors thrown by those interceptors are accumulated before any further
action is taken. The last error thrown by an interceptor is re-thrown. If the
language supports suppressed errors, the other errors should be suppressed. If
not, the other errors should be logged and dropped. The may also be attached to
the raised error as metadata.

When a mutable hook is being executed, the first error thrown is immediately
forwarded on and no other interceptors are invoked.

#### Control flow

When an error is raised in a hook outside the retry loop, execution immediately
jumps to `modifyBeforeCompletion`. When an error is raised outside the retry
loop, execution immediately jumps to `modifyBeforeAttemptCompletion`.

## Interfaces

### Hook input types

Each hook receives a typed input object that contains only the data available at
that stage of the pipeline, along with a [context object](#typed-context). Using
typed inputs prevents interceptors from accidentally accessing data that doesn't
exist yet (for example, trying to read the transport response before a request
has been sent).

```java
// Available to modifyBeforeCall.
// Contains the operation input and current client configuration.
public class CallHook<I, O> {
    public I input() { ... }
    public ClientConfig config() { ... }
}

// Available to interceptCall and from readBeforeExecution onward.
// Always contains the operation input.
public class InputHook<I, O> {
    public I input() { ... }
    public Context context() { ... }
    public InputHook<I, O> withInput(I input) { ... }
}

// Available from readAfterSerialization and modifyBeforeRetryLoop onward.
// Adds the protocol-specific request.
public class RequestHook<I, O, RequestT> extends InputHook<I, O> {
    public RequestT request() { ... }
}

// Available from readAfterTransmit and modifyBeforeDeserialization onward.
// Adds the protocol-specific response.
public class ResponseHook<I, O, RequestT, ResponseT> extends RequestHook<I, O, RequestT> {
    public ResponseT response() { ... }
}

// Available from readAfterDeserialization and modifyBeforeAttemptCompletion onward.
// Adds the deserialized output (may be null if the attempt failed).
public class OutputHook<I, O, RequestT, ResponseT> extends ResponseHook<I, O, RequestT, ResponseT> {
    public Optional<O> output() { ... }

    public Optional<RuntimeException> error() { ... }
    
    /**
     * If an exception {@code e} is provided, throw it, otherwise return the output value.
     *
     * @param e Error to potentially rethrow.
     * @return the output value.
     */
    public O forward(RuntimeException e) {
        if (e != null) {
            throw e;
        }
        return output;
    }
}
```

The context object on each hook is the same instance for the entire execution,
so data stored in it by one hook is available to all subsequent hooks. See the
[context guide](#typed-context) for details.

### Interceptor interface

It is highly recommended to design the interceptor interface so that
implementations only need to override the hooks they care about. In Java, this
means providing default no-op implementations for every method. Clients in other
languages may prefer to use abstract classes or similar features.

Mutable hooks should always return the value they receive, whether or not it was
modified. If no modification is needed, they return the original value
unchanged.

```java
public interface Interceptor {

    default ClientConfig modifyBeforeCall(CallHook<?, ?> hook) {
        return hook.config();
    }

    default boolean interceptCalls() {
        return false;
    }

    default <I, O> O interceptCall(InputHook<I, O> hook, NextCall<I, O> next) {
        return next.invoke(hook);
    }

    @FunctionalInterface
    interface NextCall<I, O> {
        O invoke(InputHook<I, O> hook);
    }

    default void readBeforeExecution(InputHook<?, ?> hook) {}

    default <I> I modifyBeforeSerialization(InputHook<I, ?> hook) {
        return hook.input();
    }

    default void readBeforeSerialization(InputHook<?, ?> hook) {}

    default void readAfterSerialization(RequestHook<?, ?, ?> hook) {}

    default <RequestT> RequestT modifyBeforeRetryLoop(RequestHook<?, ?, RequestT> hook) {
        return hook.request();
    }

    default void readBeforeAttempt(RequestHook<?, ?, ?> hook) {}

    default <RequestT> RequestT modifyBeforeSigning(RequestHook<?, ?, RequestT> hook) {
        return hook.request();
    }

    default void readBeforeSigning(RequestHook<?, ?, ?> hook) {}

    default void readAfterSigning(RequestHook<?, ?, ?> hook) {}

    default <RequestT> RequestT modifyBeforeTransmit(RequestHook<?, ?, RequestT> hook) {
        return hook.request();
    }

    default void readBeforeTransmit(RequestHook<?, ?, ?> hook) {}

    default void readAfterTransmit(ResponseHook<?, ?, ?, ?> hook) {}

    default <ResponseT> ResponseT modifyBeforeDeserialization(ResponseHook<?, ?, ?, ResponseT> hook) {
        return hook.response();
    }

    default void readBeforeDeserialization(ResponseHook<?, ?, ?, ?> hook) {}

    default void readAfterDeserialization(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {}

    default <O> O modifyBeforeAttemptCompletion(OutputHook<?, O, ?, ?> hook, RuntimeException error) {
        return hook.forward(error);
    }

    default void readAfterAttempt(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {}

    default <O> O modifyBeforeCompletion(OutputHook<?, O, ?, ?> hook, RuntimeException error) {
        return hook.forward(error);
    }

    default void readAfterExecution(OutputHook<?, ?, ?, ?> hook, RuntimeException error) {}
}
```

:::{admonition} Error modeling
:class: important

In these interfaces, errors are provided separately from the `OutputHook` type.
This is a minor optimization that eliminates the need to allocate new `OutputHook`
instances when errors are thrown.

In languages that have a native union type (such as Python) or a native `Result`
type (such as Rust), the usability benefits of integrating the error into the
`OutputHook` may be preferrable.

:::

## Example

The following interceptor adds a tracing header to HTTP requests when running
inside AWS Lambda. It uses `modifyBeforeTransmit` because the header needs to be
added to the transport request after signing. Adding it before signing would
cause the signature to include the header. That would be fine, but this
particular header is added after signing in practice.

```java
public class AddTraceHeader implements Interceptor {
    private final String traceId;

    public AddTraceHeader(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public <RequestT> RequestT modifyBeforeTransmit(RequestHook<?, ?, RequestT> hook) {
        return hook.mapRequest(HttpRequest.class, h -> {
            if (h.request().headers().hasHeader("x-amzn-trace-id")) {
                return h.request();
            }
            return h.request().toBuilder()
                .withReplacedHeader("x-amzn-trace-id", List.of(traceId))
                .build();
        });
    }
}
```

`mapRequest` is a convenience method on `RequestHook` that applies a mapping
function only if the request is of the expected type. This keeps the interceptor
from failing if it is used with a non-HTTP protocol.

## Configuring interceptors

Interceptors should be configurable for the whole client, in which case they
apply to every operation invocation made by that client. Interceptors configured
this way can determine which operation is being executed based on the input if
they need to apply to only a subset of operations.

Reusable client features should register their interceptors through
[client plugins](plugins.md).

```java
MyServiceClient client = MyServiceClient.builder()
    .addInterceptor(new AddTraceHeader(traceId))
    .build();
```

Interceptors should also be configurable for a single operation execution. This
is particularly important for things like debugging or profiling specific parts
of a code base.

```java
client.getObject(GetObjectInput.builder()
    .bucket("example")
    .key("my-object")
    .addInterceptor(new AddTraceHeader(traceId))
    .build());
```

### Execution order

When multiple interceptors are configured, they should be called in a
deterministic order because the order they are called in can impact the
execution of the operation.

The recommended ordering is:

1. Interceptors that are configured on the client by default. This includes
   interceptors that are added during code generation.
2. Interceptors that are configured on the client which are not applied by
   default.
3. Interceptors configured for a single operation execution.

Read and modify hooks iterate in this order. Call wrappers preserve the same
resolved order but compose by nesting, so the first registered wrapper is the
outermost and the last registered wrapper is the innermost.

## Why interceptors instead of middleware?

Middleware is a common pattern for building request pipelines, and it works well
as an internal implementation strategy. As a public extension point, however, it
allows middleware to modify control flow at arbitrary stages. This makes it
difficult to reason about the pipeline as a whole when third-party middleware
is present.

Most interceptor hooks deliberately cannot modify control flow. They observe or
modify values at fixed pipeline stages while preserving the client's execution
order. `interceptCall` is a narrowly scoped exception at the operation call
boundary. It may choose whether or how many times to delegate the entire
execution, but once delegated, the serialization, retry, signing, transmission,
and deserialization stages retain their defined ordering.
