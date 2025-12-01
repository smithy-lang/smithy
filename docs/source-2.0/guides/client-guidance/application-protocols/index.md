# Application Protocols

This section provides guidance on how to implement and integrate different
application protocols into a client.

Application protocols define how operations are transmitted over a network. The
most commonly used application protocol by Smithy clients is HTTP, but other
protocols like MQTT may also be used by Smithy clients and servers. When
designing a client, be careful to not couple any components to a particular
application protocol unless they interact explicitly with that protocol. For
example, an HTTP request serializer inherently needs to be coupled to HTTP, but
a JSON serializer does not.

(transport-clients)=
## Transport clients

Smithy clients and services have a common access pattern regardless of what
application protocol is being used: a client sends requests to a server and
receives responses. This can be represented by a simple interface:

```java
public interface ClientTransport<RequestT, ResponseT> {
    ResponseT send(Context context, RequestT request);
}
```

In addition to the request, it is recommended to introduce a context parameter
to the `send` method to allow the client to be configured for each request. It
is recommended to make this a generic context object rather than a type with
fixed properties. Leaving it unrestricted allows context to be passed into
custom `ClientTransport` implementations that may not be relevant to other
implementations.

## Navigation

```{toctree}
:maxdepth: 1

http
```