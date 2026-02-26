# Endpoint Resolution

Smithy clients need to know which server to connect to when making requests.
This server location is specified by a URI. That URI, bundled with any
additional context needed to make requests, is called an **endpoint**. This
guide shows you how to implement flexible endpoint resolution that can handle
different configuration options and request parameters.

## Progressive flexibility

For simple services using HTTP protocols, there may only be one URI to address.
When creating a Smithy client intended for such services, the obvious solution
to endpoint resolution is to expose a simple configuration option that takes the
entire URI.

As services scale, that simple configuration strategy may become cumbersome to
customers. Services with geographically-distributed deployment regions may wish
to have a simplified way to refer to those regions that customers can use as an
alternative. There may additionally be other factors that complicate endpoint
resolution, such as alternative endpoints dedicated to IPv6, cellular
architecture, operation-specific endpoints, and so on. To support these needs,
it is recommended that Smithy clients allow endpoint resolution to be flexible,
without sacrificing the ability to easily specify a URI.

## Interfaces

### Endpoint

A resolved endpoint should consist of a URI, context, and an optional list of
auth schemes that the endpoint supports.

```java
public interface Endpoint {
    /**
     * The endpoint URI.
     *
     * @return URI of the endpoint.
     */
    URI uri();

    /**
     * @return Context relevant to the endpoint.
     */
    Context context();

    /**
     * @return a list of auth scheme overrides for the endpoint.
     */
    List<EndpointAuthScheme> authSchemes();
}
```

#### Context

Endpoints may require additional context that impacts the request outside of
setting the URI. This context carries metadata that tells the client how to
modify requests for this specific endpoint. For example, some AWS use cases
require that additional headers be added to HTTP requests for certain endpoints.

```java
public static final Context.Key<Map<String, List<String>>> HEADERS = Context.key("Endpoint headers");
```

#### Auth schemes

Services may need to differentiate auth schemes based on the endpoint, so
endpoints should be able to specify a more restricted subset of auth schemes
that are valid for that endpoint. For example, a service may need to require a
specific auth scheme in a given region due to regulatory requirements. In other
regions it may prefer to use a less expensive auth scheme by default.

Even if a given endpoint supports all the same auth schemes that the service
supports, it may need to provide endpoint-specific configuration for those auth
schemes.

To support these cases, it is recommended that an endpoint contain an optional
list of supported auth schemes along with context. An empty or unset list
indicates that the endpoint has no special auth settings.

```java
/**
 * An authentication scheme supported for the endpoint.
 */
public interface EndpointAuthScheme {
    /**
     * The ID of the auth scheme (e.g., "aws.auth#sigv4").
     *
     * @return the auth scheme ID.
     */
    String authSchemeId();

    /**
     * @return Context relevant to the auth scheme.
     */
    Context context();
}
```

(endpoint-resolver)=
### Endpoint Resolver

It is recommended that the endpoint resolver itself be a simple function that
takes a predefined set of input parameters and returns an endpoint. This allows
implementations to be as simple or complex as they like.

```java
/**
 * Resolves an endpoint for an operation.
 */
@FunctionalInterface
public interface EndpointResolver {
    /**
     * Resolves an endpoint using the provided parameters.
     *
     * @param params The parameters used during endpoint resolution.
     * @return a resolved endpoint.
     */
    Endpoint resolveEndpoint(EndpointResolverParams params);

    /**
     * Creates an endpoint resolver that returns a statically-defined URI.
     * 
     * @param uri The URI to always return.
     * @return a new endpoint resolver that always returns the given URI.
     */
    static EndpointResolver staticUri(URI uri) { ... }
}
```

To further simplify the common case of defining a static URI, it is recommended
to provide helper methods that implement the interface for common URI types.

#### Resolver parameters

It is recommended to make the endpoint resolver parameters a dedicated type so
that they may be constructed and passed around more simply. They should include
information about the operation being invoked, the actual input to the
operation, and any context available when resolving the endpoint.

```java
/**
 * Encapsulates endpoint resolver parameters.
 */
public final class EndpointResolverParams {
    /**
     * Get the information for the operation to resolve the endpoint for.
     *
     * @return information about the operation being invoked.
     */
    public ApiOperation<?, ?> operation() {
        return operation;
    }

    /**
     * Input value for the client call the endpoint is being resolved for.
     *
     * @return the input given to the operation.
     */
    public Object inputValue() {
        return inputValue;
    }

    /**
     * Context available when resolving the endpoint.
     *
     * @return the context for the operation invocation.
     */
    public Context context() {
        return context;
    }
}
```

:::{note}

These endpoint resolver parameters take the input of each request, meaning that
endpoints must be resolved on each operation invocation rather than being
resolved when constructing a client. This allows for maximum flexibility while
still allowing individual resolver implementations to return a cached result if
they don't need to resolve so often.

:::

:::{admonition} TODO - Define ApiOperation
:class: note

`ApiOperation` will be defined later in a separate document. At a minimum, it
should contain the operation's ID as well as the list of effective auth schemes
that the operation supports.
:::

## Configuration

It is recommended that Smithy clients provide a configuration option to set an
`EndpointResolver`.

Depending on the language or use case of the client, a static `endpointUri`
configuration option may also be provided. If a static URI is provided this way,
it should take precedence over any configured `EndpointResolver`.
