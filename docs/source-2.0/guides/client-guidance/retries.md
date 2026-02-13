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

## Retry interfaces

It is recommended to expose retry interfaces that aren't coupled to a particular
implementation or protocol. It is recommended to have a `RetryStrategy` that is
isolated from the state of individual requests alongside `RetryToken`s to
capture that state and pass it between attempts.

### Retry token

A `RetryToken` is a bundle of state that is created and passed between attempts
of a single request. It should indicate how long to wait until the next attempt,
but should allow each implementation to include whatever state they find
necessary. This could include the number of attempts that have been made, an
identifier for the request, or anything else that is necessary for the retry
strategy.

```java
public interface RetryToken {
    /**
     * @return the duration to wait until the next attempt is made.
     */
    Duration delay();
}
```

### Retry strategy

A `RetryStrategy` is where the logic of computing delays and determining if a
request should be retried lives. It encapsulates the state of a request in retry
tokens, which it creates and refreshes.

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

:::{note}

While the state of a request is intended to be included in the retry token, a
retry strategy may still need to manage some state that is shared across the
client. Be careful to ensure that access to that state is synchronized in order
to prevent race conditions.
:::

#### Using retry strategies

An initial retry token should be acquired at the beginning of a request, before
the first attempt is made. If an initial token cannot be acquired, the client
should still make an attempt.

If an attempt fails, the retry strategy is passed the retry token for the
attempt and given the exception raised by the attempt. If the retry strategy
determines that the failed attempt may be retried, it will return a refreshed
retry token to use for the next attempt. If the retry strategy determines that
the failed attempt may not be retried, it throws an exception.

If the request succeeds, the retry strategy is given the token so that it may
free up any resources it was using.

The following is a simplified example of what it looks like to use the
`RetryStrategy` interface to implement a retryable request loop.

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
        if (retryToken.delay() != null) {
            Thread.sleep(retryToken.delay());
        }

        Result result = null;
        try {
            result = send(serializedRequest);
        } catch (Exception e) {
            // If the request fails, attempt to refresh the retry token.
            try {
                retryToken = this.retryStrategy.refreshRetryToken(retryToken, e);
            } catch (TokenAcquisitionFailedException retryError) {
                // If the token can't be refreshed, the request fails, so the
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
* Whether they are a timeout error. That is, whether the error is a result of
  the service not responding within the transport client's defined timeout
  limit. For HTTP protocols, a `504` status code could also indicate this.
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
    default boolean isThrottle() {
        return false;
    }

    /**
     * Check if the error is a timeout error.
     *
     * @return the error type.
     */
    default boolean isTimeout() {
        return false;
    }

    /**
     * Get the amount of time to wait before retrying.
     *
     * @return the time to wait before retrying, or null if no hint for a
     *     retry-after was detected.
     */
    default Duration retryAfter() {
        return null;
    }

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
        super(message);
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

## Retry behaviors

The most basic retry behavior is a simple loop with no delays between attempts.
This is the most likely behavior to contribute to retry storms, but a simple
delay between attempts can be just as bad because it can result in spikes of
requests from the same system.

Instead of a fixed delay, using **exponential backoff** to produce delays that
are exponentially longer each time balances the desire to get a quick success
with the desire to give the service more time to recover. In particular, making
the backoff exponential instead of linear (for example, 1->2->4->8 instead of
1->2->3->4) results in the first few attempts still happening relatively
quickly. This means that temporary issues with the network won't delay requests
much. If attempts keep failing, the exponentially increasing backoff gives a
struggling service more time to recover.

Exponential backoff doesn't solve all problems. If a large number of clients
make a request at the same time, the service might struggle to respond to any of
them. If they all retry at the same time, this might make the problem worse.
Exponential backoff does not prevent this since all the clients will be using
it, and so the problem will re-occur. To solve this problem, we add a random
factor to the delay known as **jitter**. This results in a smoother load,
preventing another flood of requests and making it easier for the service to
recover.

The combination of these strategies is known as **exponential backoff with
jitter**, and is relatively common.

More advanced retry behavior may be implemented by using a
[token bucket](https://en.wikipedia.org/wiki/Token_bucket) on top of exponential
backoff with jitter to dynamically adjust retry behavior in response to changing
service conditions.

In short: if an attempt fails, a retry is only performed if a whole token can be
removed from the bucket. If an attempt succeeds, a fraction of a token is put
into the bucket. When the bucket is empty, no retries will be performed. This
means that the total number of requests to a service will drop significantly as
the service degrades, improving its ability to recover. The bucket fills back up
as the service failure rate goes down, once again allowing retries to be
performed. In AWS SDKs, this is how the `standard` retry mode works.

These are only a few possible retry behaviors a client may have, but they
demonstrate some of the potential needs of a retry system.

### Example retry strategy

The following is an example retry strategy that implements exponential backoff
with jitter alongside a token bucket. This strategy adds extra cost for timeout
errors since they may indicate a more highly degraded service.

Aside from delay, the retry token also tracks the number of attempts that have
been made. This is necessary because this strategy imposes a maximum attempt
count, and also because the delay is calculated in part based on how many
attempts have been made.

```java
public record AwsStandardRetryToken(int attempts, Duration delay) implements RetryToken {
}
```

```java
public final class AwsStandardRetryStrategy implements RetryStrategy {
    // These values are not prescriptive. They are static in this example for the
    // sake of simplicity, but making them configurable is ideal.
    private static final int RETRY_COST = 5;
    private static final int TIMEOUT_COST = 10;
    private static final int SUCCESS_REFUND = 1;

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_BACKOFF = 20;
    private static final int MAX_CAPACITY = 500;

    // The token bucket is integrated into this retry strategy in this example,
    // but in a real client it may be better to have it be its own type so
    // that it can be shared, and so that managing concurrency is simpler.
    private int tokens = MAX_CAPACITY;

    // Be careful to consider concurrency when designing retry strategies.
    // When there are multiple threads accessing the token bucket, proper
    // synchronization is essential to prevent race conditions.
    private final Object tokensLock = new Object();

    @Override
    public RetryToken acquireInitialToken() {
        // This returns successfully even if the token bucket is empty. This is
        // because an initial attempt will always be performed anyway, and
        // returning successfully here will ensure that the retry strategy is
        // checked if that initial attempt fails. By that point, the token bucket
        // may no longer be empty.
        return new AwsStandardRetryToken(0, null);
    }

    @Override
    public RetryToken refreshRetryToken(RetryToken token, Throwable failure) {
        // First, ensure that the provided token is of the correct type.
        if (!(token instanceof AwsStandardRetryToken standardToken)) {
            throw new IllegalArgumentException("Invalid token provided for refresh.");
        }

        // Next, check to see if the maximum number of attempts has already
        // been exceeded.
        if (standardToken.attempts >= MAX_ATTEMPTS) {
            throw new TokenAcquisitionFailedException("Max attempts exhausted.");
        }

        // Examine the exception thrown by the operation to determine an
        // appropriate delay, if any.
        return switch (failure) {
            // If the exception thrown by the operation includes retryability
            // information, use that to inform retry behavior.
            case RetryInfo retryInfo when retryInfo.isRetrySafe() != RetrySafety.NO -> {
                // Attempt to consume tokens from the token bucket to "pay"
                // for the retry.
                consumeTokens(retryInfo.isTimeout());
                yield backoff(standardToken, retryInfo.retryAfter());
            }

            // If the exception does not have retry info, but does have more
            // general error info, that can also be used. This assumes that
            // a server error is likely retryable and that a client error
            // likely is not.
            case ErrorInfo errorInfo when errorInfo.fault() == ErrorFault.SERVER -> {
                consumeTokens(false);
                yield backoff(standardToken);
            }
            default -> throw new TokenAcquisitionFailedException("Exception not retryable.");
        };
    }

    /**
     * Consumes tokens to "pay" for a retry.
     *
     * @param isTimeout whether the retry is in response to a timeout error,
     *     which will require more tokens.
     *
     * @throws TokenAcquisitionFailedException if there are not enough tokens
     *     in the bucket to pay for the retry.
     */
    private void consumeTokens(boolean isTimeout) {
        synchronized (tokensLock) {
            int cost = isTimeout ? TIMEOUT_COST : RETRY_COST;

            if (this.tokens < cost) {
                throw new TokenAcquisitionFailedException("Token bucket exhausted.");
            }

            this.tokens -= cost;
        }
    }

    /**
     * Computes a backoff with exponential backoff and jitter, capped at 20 seconds.
     *
     * @param token the previous token.
     */
    private AwsStandardRetryToken backoff(AwsStandardRetryToken token) {
        return new AwsStandardRetryToken(token.attempts + 1, computeDelay(token.attempts));
    }

    /**
     * Computes a backoff with exponential backoff and jitter, capped at 20 seconds.
     *
     * @param token the previous token.
     * @param suggested the delay suggested by the service, which will serve as
     *     the minimum delay.
     */
    private AwsStandardRetryToken backoff(AwsStandardRetryToken token, Duration suggested) {
        // Compute the backoff as normal. If it is longer than the suggested
        // backoff from the service, use it. Otherwise, use the suggested
        // backoff.
        Duration computedDelay = computeDelay(token.attempts);
        Duration finalDelay = computedDelay.toMillis() < suggested.toMillis() ? suggested : computedDelay;
        return new AwsStandardRetryToken(token.attempts + 1, finalDelay);
    }

    /**
     * Computes the delay with exponential backoff and jitter, capped at 20 seconds.
     *
     * @param attempts the number of attempts made so far.
     * @return the computed delay duration.
     */
    private Duration computeDelay(int attempts) {
        // First compute the exponential backoff.
        double backoff = Math.pow(2, attempts);

        // Next, cap it at 20 seconds.
        backoff = Math.min(backoff, MAX_BACKOFF);

        // Finally, add jitter and expand to milliseconds.
        double backoffMillis = Math.random() * backoff * 1000;
        return Duration.ofMilliseconds((long) backoffMillis);
    }

    @Override
    public void recordSuccess(RetryToken token) {
        synchronized (tokensLock) {
            // When a successful request is made, refill the token bucket unless it
            // is already at maximum capacity.
            if (this.tokens < MAX_CAPACITY) {
                this.tokens += SUCCESS_REFUND;
            }
        }
    }
}
```
