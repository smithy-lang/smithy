# Retrying Requests

Operation requests might fail for a number of reasons that are unrelated to the
input parameters, such as a transient network issue or excessive load on the
service. This guide gives recommendations on how Smithy clients can
automatically retry failed requests in those cases, and how a robust system for
pluggable retry strategies can be implemented.

## Why is a retry system recommended?

If transient failures are surfaced to Smithy client users, they will likely
implement their own retry behavior. This hand-written retry behavior may not
include important risk mitigations that reduce the impact of outages.

When a service begins to degrade, clients may notice behavior changes, such as
an elevated number of server errors or connection failures. Users will naturally
want to retry these failed requests. Unfortunately, these retries have the
effect of increasing the load on the service, which may cause the service to
further degrade, resulting in even more retries.

These sorts of events are called **retry storms**, and are often the result of
poorly managed retry behavior. While there is no perfect retry behavior,
strategies will inevitably improve over time as the scale of systems grows and
new cascading failure conditions are observed. The right interface reflecting
the problem domain can make sure the right extension points are available for
future expansion.

## Retry behaviors

The most basic retry behavior is a simple loop with no delays between attempts.
This is the most likely behavior to contribute to retry storms, but a simple
delay between attempts can be just as bad because it can result in spikes of
requests from the same system.

Instead of a fixed delay, using **exponential backoff** to produce delays that
are longer each time balances the desire to get a quick success with the desire
to give the service more time to recover. Adding some randomness to that delay
(known as **jitter**) can result in a smoother request load. This strategy,
called **exponential backoff with jitter**, is relatively common. In AWS SDKs
this is the `standard` retry mode.

More advanced retry behavior may be implemented by using a
[token bucket](https://en.wikipedia.org/wiki/Token_bucket) on top of exponential
backoff with jitter to dynamically adjust retry behavior in response to changing
service conditions. In short: if an attempt succeeds, some fraction of a token
is dropped in the bucket. If an attempt fails, a retry is only performed if a
whole token can be removed from the bucket. This results in the total number of
requests to a service dropping as the service degrades, improving its ability to
recover. When the service recovers, the token bucket fills back up and load
returns to normal. In AWS SDKs, this is the `adaptive` retry mode.

These are only a few possible retry behaviors a client may have, but they
demonstrate some of the potential needs of the retry system.

## Retry interfaces

It is recommended to implement retry behavior in a `RetryStrategy` that produces
`RetryToken`s to pass state between attempts. Passing state through tokens in
this way allows the `RetryStrategy` implementation to be isolated from the state
of an individual request.

### Retry token

The retry token itself should indicate a delay to wait before the next attempt
is made, but it may otherwise contain any state that is necessary for the retry
strategy.

```java
public interface RetryToken {
    /**
     * @return the duration to wait until the next attempt is made.
     */
    public Duration delay();
}
```

### Retry strategy

The `RetryStrategy` creates and refreshes retry tokens. When an attempt fails,
the retry strategy is passed the retry token for the attempt and given the
exception raised by the attempt. If a failed attempt may be retried, the retry
strategy will return a refreshed retry token to use for the next attempt. If a
failed attempt may not be retried, it throws an exception. If a request
succeeds, the retry strategy is given the token so that it may free up any
resources it was using.

```java
public interface RetryStrategy {
    /**
     * Invoked before the first request attempt.
     *
     * @throws TokenAcquisitionFailedException if a token cannot be acquired.
     */
    RetryToken acquireInitialToken();

    /**
     * Invoked before each subsequent (non-first) request attempt.
     *
     * @throws IllegalArgumentException if the provided token was not issued by
     *     this strategy or the provided token was already used for a previous
     *     refresh or success call.
     * 
     * @throws TokenAcquisitionFailedException if a token cannot be acquired.
     */
    RetryToken refreshRetryToken(RetryToken token, Throwable failure);

    /**
     * Invoked after an attempt succeeds.
     *
     * @throws IllegalArgumentException if the provided token was not issued by
     *     this strategy or the provided token was already used for a previous
     *     refresh or success call.
     */
    void recordSuccess(RetryToken token);
}
```

### Retryable errors

Request attempts can throw many different types of exceptions and it is not
reasonable to expect retry strategy implementations to be aware of them all. For
example, different HTTP clients may expose response status codes in different
ways. Beyond that, a Smithy client may not even be using HTTP as a transport. It
is therefore recommended to use an interface to standardize the information that
is relevant to retry strategies. Retry strategies can then use that information
if it is available while still attempting to handle exceptions that don't have
that information available.

In particular, exceptions should indicate:

* Whether they are safe to retry. For example, a failure to connect to the
  service or a temporary service error may be retryable while an error
  relating to invalid input or authentication failure is not retryable.
* Whether they are a throttling error. That is, whether they are an error
  returned by a service specifically to indicate that too many requests have
  been made recently. For HTTP protocols, for example, a `429` status code
  indicates this.
* A minimum time to wait until the next request, if that information is
  available. For HTTP protocols, for example, this could be indicated by the
  [`Retry-After`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Retry-After)
  header. A retry strategy may choose a delay that is longer than this, but
  should not choose a delay that is shorter than this since it is information
  directly from the service.

```java
/**
 * Provides retry-specific information about an error.
 */
public interface RetryInfo {
    /**
     * Get the decision about whether it's safe to retry the encountered error.
     *
     * <p>If the decision is {@link RetrySafety#YES}, it does not mean that a
     * retry will occur, but rather that a retry is allowed to occur.
     *
     * @return whether it's safe to retry.
     */
    RetrySafety isRetrySafe();

    /**
     * Check if the error is a throttling error.
     *
     * @return the error type.
     */
    boolean isThrottle();

    /**
     * Get the amount of time to wait before retrying.
     *
     * @return the time to wait before retrying, or null if no hint for a
     *     retry-after was detected.
     */
    Duration retryAfter();

    /**
     * Whether it's safe to retry.
     */
    public enum RetrySafety {
        /**
         * Yes, it is safe to retry this error.
         */
        YES,

        /**
         * No, a retry should not be made because it isn't safe to retry.
         */
        NO,

        /**
         * Not enough information is available to determine if a retry is safe.
         */
        MAYBE
    }
}
```

Additionally, Smithy's [`error` trait](#error-trait) indicates whether an error
is the fault of the client or the server. It is highly recommended to include
this information in code-generated exception classes. It is also recommended to
allow this information to be provided for other kinds of exceptions. For
example, a 400-level status code in an HTTP response indicates a client error,
while a 500-level status code indicates a server error. This is shown below as a
separate interface since it is relevant to more than just retries.

```java
public interface ErrorInfo {
    /**
     * Indicates if a client or server is at fault for the error, if known.
     */
    ErrorFault fault();

    enum ErrorFault {
        /**
         * The client is at fault for this error (e.g., it omitted a required
         * parameter or sent an invalid request).
         */
        CLIENT,

        /**
         * The server is at fault (e.g., it was unable to connect to a
         * database, or other unexpected errors occurred).
         */
        SERVER,

        /**
         * The fault isn't necessarily client or server.
         */
        OTHER;
    }
}
```

Errors that are defined in the Smithy model should have all the properties of
`RetryInfo` statically generated or settable. For example, the following
demonstrates a modeled error and what it might look like as a generated Java
exception class.

```smithy
@httpError(429)
@error("client")
@retryable(throttling: true)
structure ThrottlingError {
    message: String
}
```

```java
public class RetryAfterException extends RuntimeException implements ErrorInfo, RetryInfo {
    private final String message;

    private Duration retryAfter = null;

    public RetryAfterException(String message) {
        super(message)
        this.message = message;
    }

    // This value is code generated based on the error trait.
    public ErrorFault fault() {
        return ErrorFault.CLIENT;
    }

    // This value is code generated based on the retryable trait. If that trait
    // is not present, this should be NO for "client" errors or MAYBE for
    // "server" errors.
    public RetrySafety isRetrySafe() {
        return RetrySafety.YES;
    }

    // This value is code generated based on the retryable trait.
    public boolean isThrottle() {
        return true;
    }

    public Duration retryAfter() {
        return this.retryAfter;
    }

    // This would be called by the deserializer. For HTTP protocols, for
    // instance, it would be called if a `retry-after` header is present in the
    // response.
    public void setRetryAfter(Duration duration) {
        this.retryAfter = duration;
    }

    // This is the modeled message property.
    public String message() {
        return this.message;
    }
}
```

## Example request loop

The following is a simplified example of what it looks like to use a
`RetryStrategy` to implement a retryable request loop.

```java
/**
 * A simplified example of what a retryable request loop looks like.
 * 
 * @param serializedRequest a request that has been fully serialized and is
 *     ready to send.
 * 
 * @return a successful result.
 */
public Result request(SerializedRequest serializedRequest) {
    // First acquire the initial retry token. If a token cannot be acquired,
    // make only one attempt without retries.
    RetryToken retryToken;
    try {
        retryToken = this.retryStrategy.acquireInitialToken();
    } catch (TokenAcquisitionFailedException e) {
        return send(serializedRequest);
    }

    // Make attempts until the request succeeds or the retry strategy throws
    // an exception. Notably, each retry strategy is responsible for controlling
    // the maximum number of attempts.
    while (true) {

        // Wait for the indicated delay duration. Even the initial token may
        // include a delay.
        Thread.sleep(retryToken.delay());

        Result result = null;
        try {
            result = send(serializedRequest);
        } catch (Exception e) {
            // Otherwise attempt to refresh the token.
            try {
                retryToken = this.retryStrategy.refreshRetryToken(retryToken, e);
            } catch (TokenAcquisitionFailedException retryError) {
                // If the token can't be acquired, the request fails, so the
                // original exception needs to be propagated. Logging the reason
                // the retry failed is advisable.
                throw e;
            }
        }

        // If the result was successful, inform the retry strategy. This allows
        // it to free up any resources if necessary.
        if (result != null) {
            this.retryStrategy.recordSuccess(retryToken);
            return result;
        }
    }
}
```

Note that this code does not attempt to inspect the exceptions. It instead
passes them directly to the retry strategy, which then handles any information
in the exception that is relevant to it.
