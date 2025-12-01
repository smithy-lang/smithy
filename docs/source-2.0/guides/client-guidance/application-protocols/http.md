# HTTP

HTTP is the most common application protocol used by Smithy clients. This guide
provides advice on how to integrate and expose HTTP clients.

## Configuration

Smithy clients should allow their users to configure the HTTP client that the
Smithy client uses to send requests. Users may want to change the client used to
something that has different performance characteristics or support for features
that they need.

## HTTP interfaces

Smithy clients should provide interfaces for HTTP clients that standardize how
the Smithy client interacts with the HTTP client, allowing any HTTP client to be
used as long as it implements the interface.

### Clients

It is recommended to make HTTP clients implementations of
[`ClientTransport`](#transport-clients).

```java
public interface HttpClient implements ClientTransport<HttpRequest, HttpResponse> {
    HttpResponse send(Context context, HttpRequest request);
}
```

#### Context

HTTP clients don't have many common context parameters, but they should check
the context for a request timeout setting and use it if it's present.

```java
/**
 * This utility class holds shared context key definitions that are useful
 * for HTTP implementations.
 */
public final class HttpContext {
    public static final Context.Key<Duration> HTTP_REQUEST_TIMEOUT = Context.key("HTTP.RequestTimeout");

    // This is a utility class that is not intended to be constructed, so it
    // has a private constructor.
    private HttpContext() {}
}
```

### Requests and Responses

{rfc}`9110` discusses HTTP requests and responses collectively as "messages",
and it can be useful to encode their shared features in a shared interface.

```java
public interface HttpMessage {
    /**
     * Get the headers of the message.
     *
     * @return headers.
     */
    HttpFields headers();

    /**
     * Get the body of the message, or null.
     *
     * @return the message body or null.
     */
    DataStream body();
}
```

Requests introduce the `method` and `uri` properties.

```java
public interface HttpRequest extends HttpMessage {
    /**
     * Get the method of the request.
     *
     * @return the method.
     */
    String method();

    /**
     * Get the URI of the request.
     *
     * @return the request URI.
     */
    URI uri();
}
```

Responses introduce a status code.

```java
public interface HttpResponse extends HttpMessage {
    /**
     * Get the status code of the response.
     *
     * @return the status code.
     */
    int statusCode();
}
```

### Fields

Most users who have interacted with HTTP directly are familiar with the concept
of headers. Headers were originally introduced in HTTP/1.0 and, since then, the
concept of key/value pairs has expanded to include trailers and other arbitrary
metadata. As of {rfc}`9110`, these key/value pairs are exclusively referred to
as {rfc}`fields <9110#section-5>`.

When designing HTTP interfaces for Smithy clients, be careful to understand
field semantics. In particular, it is important to understand that field keys
are case-insensitive and may appear more than once in an HTTP message. Since
field keys may appear more than once, it is recommended that they are
represented as an iterable collection of pairs or as a map whose value type is a
list. This allows protocol implementations to safely handle joining and
splitting.

It is recommended to have utilities to convert fields to and from maps. Fields
are often conceptualized as maps, so providing these utilities allows users to
access fields in a more comfortable way without sacrificing correctness.

```java
public interface HttpFields extends Iterable<Map.Entry<String, List<String>>> {
    /**
     * Create an HttpFields instance from a map.
     *
     * @param fields Field map to use as a data source.
     * @return the created fields.
     */
    static HttpFields of(Map<String, List<String>> fields) {
        // This constructs a theoretical default implementation of the
        // HttpFields interface that creates an unmodifiable copy of the given
        // map.
        return fields.isEmpty() ? UnmodifiableHttpFields.EMPTY : new UnmodifiableHttpFields(fields);
    }

    /**
     * Convert the HttpFields to a map.
     *
     * @return the fields as a map.
     */
    Map<String, List<String>> toMap();

    /**
     * Check if the given field is case-insensitively present.
     *
     * @param name Name of the field to check.
     * @return true if the field is present.
     */
    default boolean containsField(String name) {
        return !getAllValues(name).isEmpty();
    }

    /**
     * Get the first field value of a specific field by case-insensitive name.
     * 
     * Smithy clients know whether a given field should have a single value or
     * a list value. This helper method simplifies usage for fields with a
     * single value.
     *
     * @param name Name of the field to get.
     * @return the matching field value, or null if not found.
     */
    default String getFirstValue(String name) {
        var list = getAllValues(name);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Get the values of a specific field by case-insensitive name.
     *
     * @param name Name of the field to get the values of.
     * @return the values of the field, or an empty list.
     */
    List<String> getAllValues(String name);
}
```

#### Implementation recommendations

It is not recommended to automatically attempt to join values for a given field
key at the HTTP layer. {rfc}`9110#section-5` allows field values to be joined
with a comma, but doing so automatically can introduce data corruption if one of
the field values already includes a comma. {rfc}`Section 5.6 <9110#section-5.6>`
includes productions that can help to handle those edge cases, but whether they
are used or not is up to the protocol definition.
