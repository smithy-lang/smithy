(typed-context)=
# Context

When implementing a Smithy client, you will almost certainly need to pass
contextual information throughout the request lifecycle. **Context refers to
data that needs to be shared and tracked across different stages of processing a
single operation (such as retry counts, authentication tokens, or timing
information).

Client plugins in particular need a way to store and retrieve this information
during operation invocations, since they cannot modify the core request pipeline
directly. This guide provides guidance on how context objects can be safely
implemented and exposed to maintain type safety while allowing for
extensibility.

## Implementation

This context could be defined explicitly as a structure with defined properties,
but that would quickly become bloated, unwieldy, and difficult to extend without
changing the core library code.

It is better to instead use an open map (such as `Map<String, Object>` in Java,
`dict` in Python, or similar structures in other languages). A given string key
still maps to a specific value type and serves the same purpose, but now the
context is open to extension without changing core library code.

While this sort of open map usage may be common in some languages, the lack of
type safety is a significant problem. To retain type safety, it is recommended
to use an interface that encodes the value type within the key itself using
generics or similar type system features.

```java
/**
 * A typed context bag.
 */
public interface Context {

    /**
     * A key wrapper that tracks the value type it is expected to be assigned
     * to.
     */
    final class Key<T> {
        private final String name;

        /**
         * @param name Name of the value.
         */
        public Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static <T> Key<T> key(String name) {
        return new Key<>(name);
    }

    /**
     * Set a property. If it was already present, it is overridden.
     *
     * @param key   Property key.
     * @param value Value to set.
     * @param <T>   Value type.
     */
    <T> void put(Key<T> key, T value);


    /**
     * Get a property.
     *
     * @param key Property key to retrieve the value for.
     * @return    the value, or null if not present.
     * @param <T> Value type.
     */
    <T> T get(Key<T> key);
}
```

Typed keys can then be statically defined and shared. A compiler or type checker
will validate that usage is correct, catching type mismatches at compile time
rather than runtime. These should be defined in the packages that primarily use
them.

For example, imagine if retry tracking were extracted to a client plugin. It
could define a static `RETRY_COUNT` property that can be exposed for use by
other plugins.

```java
public final class RetryTracker {
    // Any interested client plugin could use this context key to have
    // type-safe and typo-safe access to the context property.
    public static final Context.Key<Integer> RETRY_COUNT = Context.key("retry-count");

    public void onAttempt(Context context) {
        Integer count = context.get(RETRY_COUNT);
        if (count == null) {
            context.put(RETRY_COUNT, 0);
        }
        context.put(RETRY_COUNT, count + 1);
    }
}
```

## Lifecycle

Each operation invocation should create its own context object. This prevents
unintentionally leaking context into other requests and reduces the chances of
concurrency issues.

Smithy clients should pass this context object to any integration hooks.
[TODO: link to interceptors documentation.] There should be at least one hook at
the beginning of the request pipeline to enable client plugins to populate the
context as soon as possible.
