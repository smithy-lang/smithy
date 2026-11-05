(client-guidance-dns)=
# DNS Resolution

The Domain Name System (DNS) maps hostnames to network addresses that a
transport can use to establish connections.

:::{important}

Smithy clients should delegate DNS resolution, caching, address selection, and
connection failover to their configured transport, language runtime, or
operating system by default. Clients should provide custom DNS behavior only
when those facilities cannot satisfy a concrete requirement.

Implementations should use system resolution or an existing DNS library instead
of implementing the DNS protocol directly.

:::

System and transport resolvers incorporate behavior outside the DNS protocol
itself. This can include host files, local caching services, VPN configuration,
split-horizon DNS, and organizational network policy. Bypassing them can produce
different results from other applications on the same host.

A client that accepts a user-provided transport should not resolve endpoint
hostnames before passing requests to that transport. Doing so can bypass the
transport's connection pool, proxy behavior, address selection, and DNS cache.

## Responsibility boundaries

Endpoint resolution, DNS resolution, and connection establishment solve
different problems:

| Component | Responsibility |
|---|---|
| Endpoint resolver | Selects a URI and associated endpoint metadata for an operation. |
| DNS resolver | Maps the URI hostname to one or more network addresses. |
| Connection strategy | Chooses when and how to attempt connections to those addresses. |
| Transport | Owns connection pooling, proxies, sockets, TLS, and protocol negotiation. |

An [endpoint resolver](endpoints.md) should not perform DNS queries to determine
whether an endpoint is available. It selects the endpoint described by the
service model and client configuration. DNS resolution occurs later, when the
transport needs to establish a connection.

A DNS resolver should not open connections or choose an endpoint. It returns
the addresses known for a hostname. Address ordering can express a preference,
but the connection strategy determines whether it attempts addresses
sequentially, interleaved, or concurrently.

A pooled connection does not require another DNS lookup for every request.
The transport resolves a hostname when it needs a new connection and lacks a
usable cached result.

## Transport integration

A transport can provide DNS resolution, caching, IPv4 and IPv6 address
selection, and connection failover. Clients using such a transport do not need
a Smithy-specific DNS interface.

If a transport defines its own resolver abstraction, it should allow callers
to provide an implementation and keep resolution independent from connection
establishment. A Smithy implementation can adapt user-provided resolvers to an
existing transport abstraction rather than define a parallel Smithy-specific
interface.

DNS configuration belongs to the transport that consumes the results. A
generated service client should expose transport configuration instead of
duplicating transport-specific DNS settings.

Custom DNS behavior is warranted only for:

- Deterministic hostname mappings in tests or local development.
- A transport that owns socket creation and needs access to every resolved
  address.
- Address rotation or failover requirements that the configured transport cannot
  provide.
- Environments that require a specific resolver library or network policy.

Observing DNS timing or failures does not require a custom resolver when the
transport exposes that telemetry.

## Optional resolver interface

A transport that needs custom resolution should keep the interface small. For
example:

```java
public final class DnsRequest {
    public String hostname() { ... }
}

public interface DnsResolver {
    List<NetworkAddress> resolve(DnsRequest request);

    default void reportFailure(
            DnsRequest request,
            NetworkAddress address
    ) {}

    default void purge(DnsRequest request) {}

    default void purgeAll() {}
}
```

`resolve` should return every usable address in preference order, rather than
selecting a single address or limiting each address family to one result. It
should raise an error when resolution fails or produces no usable addresses.

Addresses should use the language or transport's native parsed representation
when that representation is available. Returning parsed addresses avoids
repeated parsing and prevents ambiguity around IPv6 text formatting.

`reportFailure`, `purge`, and `purgeAll` are optional capabilities. Stateless
resolvers and resolvers whose caching is owned by the operating system can
implement them as no-ops. A resolver that supports failure feedback should
scope it to the hostname and address rather than treating an address as
globally unhealthy.

A resolver shared by concurrent requests must be safe for concurrent use.
Resolution should follow the transport's concurrency model. An asynchronous
transport should not run a blocking resolver on its event-loop thread.

### Resolution inputs

For TCP connections, an HTTP client needs only a hostname to resolve addresses.
Implementations should avoid exposing DNS record types, socket flags, or
protocol-specific options unless the transport uses them.

`DnsRequest` should initially contain only the hostname. It can later add
transport-relevant information without requiring DNS features that clients do
not currently use.

## Hostname identity

Resolving a hostname changes the socket destination, not the identity of the
endpoint. The transport must continue to use the original hostname for:

- The HTTP request authority and `Host` field.
- TLS Server Name Indication (SNI).
- TLS certificate hostname verification.
- Connection-pool and origin identity.

The resolved address is only the socket destination. Rewriting a request URI to
use the address is not a general-purpose substitute for transport-level DNS
integration.

A transport can provide a connect-to or static-resolution option that connects
to an address while preserving the original hostname. A Smithy implementation
can use that option to adapt a custom resolver when the transport preserves the
required HTTP and TLS semantics.

An IP literal is already a network address. The transport should bypass DNS
resolution for IP literals. URI parsing remains responsible for details such as
brackets around IPv6 literals.

## Address selection and connections

DNS resolution can return multiple IPv4 and IPv6 addresses. The resolver should
preserve the ordering produced by the system or resolver library unless it
implements an intentional, documented address policy.

Connection establishment should be prepared to try more than one address. A
failed connection to one address does not imply that resolution failed or that
the other addresses are unavailable.

IPv4 and IPv6 connection racing, known as Happy Eyeballs, is a connection
strategy rather than a DNS resolver feature. Transports that implement it should
follow {rfc}`8305` or a platform facility that provides equivalent behavior.
Resolver interfaces should return the ordered address set without prescribing
the racing algorithm.

Failure feedback should come from the component that attempted the connection.
A resolver must not infer address health from HTTP response status codes.
Implementations that temporarily deprioritize a failed address should use
bounded failure state and make the address eligible again after a bounded
cooldown or a successful refresh.

## Caching

When DNS is delegated to the transport, runtime, or operating system, that
component should retain ownership of its cache. Adding another cache in the
Smithy client can extend effective lifetimes, interfere with address rotation,
and make cache invalidation unpredictable.

A custom resolver that owns a cache should:

- Bound cache size and resource usage.
- Coalesce concurrent lookups for the same hostname.
- Distinguish successful results from negative or transient failures when the
  underlying resolver provides that information.
- Avoid treating a transient resolution failure as a permanent negative result.
- Document whether stale addresses can be used when refresh fails.
- Provide explicit invalidation only when callers have a concrete need for it.

DNS record TTLs are not available through every system resolver API. If the
underlying API provides TTLs, a custom cache should honor them. If it does not,
an application-level cache duration is a local policy rather than the
authoritative DNS TTL. Cache documentation should identify it as a local policy.

Resolver implementations own background refresh, stale-address retention, and
eviction policies. The transport and deployment environment determine these
policies.

## Retries and connection pools

An operation retry that reuses a healthy pooled connection does not perform DNS
resolution.

When a retry requires a new connection, the connection strategy should
try other addresses from the current result before forcing another lookup. A
transport can re-resolve after connection failures when the cached result has
expired or its retry policy requests fresh results.

Clients should not unconditionally purge DNS state or perform a new query for
every operation attempt. Retry behavior must also respect the operation's
deadline and avoid multiplying DNS and connection attempts without bounds.

DNS failures and connection failures should remain distinguishable so that
retry policies and diagnostics can classify them accurately.

## Proxies and redirects

Proxy configuration determines whether the transport resolves the proxy
hostname, the origin hostname, or both. Custom DNS integration must preserve the
transport's proxy semantics rather than resolving and replacing every hostname
in advance.

When a redirect changes the origin hostname, any custom resolution must be
applied to the new hostname. Redirect handling must continue to preserve the
new origin's HTTP and TLS identity.

## Timeouts and cancellation

The transport should bound DNS resolution with a connection or operation
deadline. It can expose a separate DNS timeout, include resolution in its
connection timeout, or use a single operation deadline. Transport documentation
should define which deadline covers resolution and apply it to every lookup.

Asynchronous transports should support cancellation while resolution is
pending when their resolver APIs allow it. If a system resolver cannot be
cancelled, cancellation should prevent its eventual result from starting a
connection after the request has been abandoned.

## Security

Implementations should treat DNS results as untrusted network input. Resolving a
hostname does not establish the identity of the remote peer. Authentication and
TLS certificate verification must continue to use the original hostname.

Features that restrict requests to particular networks must validate resolved
addresses, not only the hostname text. They should repeat that validation after
redirects and later resolutions so that DNS changes cannot bypass the
restriction.

Explicit static hostname mappings should take precedence only when configured
for the transport or call where they are intended. They should not silently
replace system resolution globally.

## Observability

Transports should make DNS failures distinguishable from connection, TLS, and
HTTP failures. DNS telemetry can record:

- Resolution duration.
- Whether a result came from a cache, when that information is available.
- The number and address families of returned addresses.
- Failure category.

Observability callbacks must not delay resolution or change control flow.
Implementations should treat hostnames and resolved addresses as customer
network information and record them only when the telemetry policy permits it.

## Testing

A static or fake resolver can test the following behavior without public DNS:

- IPv4-only, IPv6-only, and mixed address results.
- Multiple addresses and connection failover.
- Empty results and resolution errors.
- IP literals bypassing resolution.
- Preservation of HTTP authority, TLS SNI, and certificate verification.
- Pooled connections that do not resolve on every request.
- Retries that use another known address without unbounded re-resolution.
- Proxy and redirect behavior.
- Resolver timeouts, cancellation, and concurrent calls.
- Cache hits, expiry, invalidation, and negative results when custom caching is
  implemented.
- Temporary address deprioritization when failure feedback is implemented.

Integration tests involving a real resolver should use controlled local
infrastructure. Tests that depend on public DNS records are vulnerable to
network availability, caching, and record changes outside the test's control.
