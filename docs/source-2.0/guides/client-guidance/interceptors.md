(client-guidance-interceptors)=
# Interceptors

An **interceptor** is a general-purpose extension point that allows code to
observe or modify *specific* stages of a request execution: serialization,
signing, transmission, deserialization, and so on. Interceptors are registered
at client creation time or per operation invocation.

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

### Hook sequence

The following is an ordered list of recommended hooks. It is also recommended to
make the list of hooks modifiable, so that new hooks may be added later.

:::{important}

In the following list, an **execution** is one entire end-to-end invocation of
an operation. An **attempt** is a single try within that execution. There may be
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

### Error behavior

Errors raised in hooks are handled consistently. The behavior depends on where
in the pipeline the hook is called:

- **Hooks called once per execution** (`readBeforeExecution`,
  `readAfterExecution`): Errors are collected across all interceptors before any
  further action is taken. If multiple interceptors raise errors, the last error
  wins and earlier ones are logged and dropped.

- **Most other hooks**: An error immediately jumps execution to
  `modifyBeforeAttemptCompletion` (if inside the retry loop) or
  `modifyBeforeCompletion` (if outside), with the error set as the result.

- **`readAfterAttempt`**: Errors are collected the same way as
  `readBeforeExecution`. After all interceptors have been called, the
  [retry strategy](#client-guidance-retries) decides whether to retry or proceed
  to `modifyBeforeCompletion`.

## Interfaces

### Hook input types

Each hook receives a typed input object that contains only the data available at
that stage of the pipeline, along with a [context object](#typed-context). Using
typed inputs prevents interceptors from accidentally accessing data that doesn't
exist yet (for example, trying to read the transport response before a request
has been sent).

```java
// Available from readBeforeExecution and modifyBeforeSerialization onward.
// Always contains the operation input.
public class InputHook<I, O> {
    public I input() { ... }
    public Context context() { ... }
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
    public O output() { ... }
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

Mutable hooks should always return the message, whether or not it was modified.
If no modification is needed, they return the original value unchanged.

```java
public interface Interceptor {

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

## Example

The following interceptor adds a tracing header to HTTP requests when running
inside AWS Lambda. It uses `modifyBeforeTransmit` because the header needs to be
added to the transport request after signing. Adding it before signing would
cause the signature to include the header. That would be fine, but this
particular header is added after signing in practice.

```java
public class AddTraceHeader implements ClientInterceptor {
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

## Why interceptors instead of middleware?

Middleware is a common pattern for building request pipelines, and it works well
as an internal implementation strategy. As a public extension point, however, it
has a significant drawback: middleware can modify control flow. A middleware
component can wrap the next stage in its own retry loop, short-circuit the
pipeline entirely, or call the next stage multiple times. This makes it
impossible to reason about the behavior of the pipeline as a whole when
third-party middleware is present.

Interceptors deliberately cannot modify control flow. They can observe and
modify messages, but the pipeline itself always executes in the same order. This
makes the behavior of the client predictable and easier to reason about. It also
makes it safe to add new behavior to the pipeline without risking unexpected
interactions with existing interceptors.
