# Identity and Authentication

Operations supported by a Smithy service generally require a mechanism for
authenticating the client's identity. This section describes how to model the
workflow of identity retrieval and request authentication in Smithy clients.

## Identity

An Identity is an entity representing **who the caller is**. Abstractly, the
caller's identity could be anonymous, a token, a public/private key, etc.

Identity types in the client runtime should typically extend from a base
interface that exposes a single value: the expiration time of the identity. The
API should then branch per auth scheme. Sometimes an Identity type may be
re-used across multiple schemes.

```java
public interface Identity {
	Instant expiration();
}

// for smithy.api#bearerTokenAuth
public interface BearerTokenIdentity extends Identity {
	String token();
}

// for aws.auth#sigv4, aws.auth#sigv4a
public interface SigV4CredentialIdentity extends Identity {
	String accessKeyId();
	String secretAccessKey();
	String sessionToken();
}
```

## IdentityResolver

An IdentityResolver implements the retrieval of a single type of identity.

The IdentityResolver interface should be modeled as follows:

```java
public interface IdentityResolver<TIdentity extends Identity> {
    TIdentity resolve(Object properties);
}
```

There may be multiple valid implementations of an identity resolver for a given
type. For example, one implementation of an IdentityResolver<BearerTokenIdentity>
may source the bearer token from the system environment, while another may
source the token from in-code configuration.

The client configuration for a service should typically include a field that
allows configuration of an IdentityResolver for every auth scheme supported by
the service.

```java
public class MyServiceClientConfig {
    private final IdentityResolver<SigV4CredentialIdentity> sigV4CredentialResolver;
    private final IdentityResolver<BearerTokenIdentity> bearerTokenResolver;

    // other configuration...
}
```

## Signer

A Signer is an entity representing **a way to generate a signature for a
request**. A signature is metadata attached to a request that will be sent to
the service in order to allow the service to authenticate the Smithy client
caller's identity.

For example, an operation that uses a bearer token for
identification would "sign" an outgoing HTTP request by presenting the value of
the token in the Authorization header.

All signers require, at a minimum, a request to sign and an identity with which
to sign. A Signer should be modeled like so:

```java
// This example interface modifies the transport message in-place with the
// signature. Depending on the surrounding runtime, the caller may instead wish to
// model the Signer as an interface that returns a copied, modified transport
// message instead.
public interface Signer<TIdentity extends Identity, TMessage extends Message> {
    void sign(TIdentity identity, TMessage message, Object properties);
}
```

## AuthScheme

An AuthScheme self-describes a single flow (combination of an identity resolver
and signer) through which a Smithy client authenticates a request:

```java
public interface AuthScheme<TIdentity extends Identity, TMessage extends Message> {
    /**
     * Unique identifier for this auth scheme. Typically corresponds to the ID of a
     * Smithy IDL auth trait, e.g. "smithy.api#httpBearerAuth".
     */
	String schemeId();

    /**
     * Provides an Identity Resolver for this authentication scheme.
     * This API can return a nullish value indicating that an identity
     * resolver of this scheme's type is not available in the current client
     * environment.
     */
    IdentityResolver<TIdentity> identityResolver(IdentityResolverConfig config);

    /**
     * Provides a Signer for this authentication scheme from an unspecified
     * source. An actual implementation might source the Signer from client
     * configuration, or perhaps provide its own implementation directly.
     */
    Signer<TIdentity, TMessage> signer();
}

public interface IdentityResolverConfig {
    IdentityResolver<?> getIdentityResolver(String schemeId);
}
```

A list of supported AuthSchemes should be configurable by the end-user of the
Smithy client. The default "constructor" for a Smithy client should typically
pre-load a list of supported AuthSchemes on client configuration.

```java
// in this example, the service supports some combination of
// smithy.api#httpBearerAuth and aws.auth#sigv4
public MyServiceClientConfig defaultConfig() {
    MyServiceClientConfig.Builder builder = MyServiceClientConfig.builder();

    builder.addAuthScheme(new DefaultHttpBearerAuthScheme());
    builder.addAuthScheme(new DefaultSigV4AuthScheme());

    // ...

    return builder.build();
}
```

## AuthSchemeResolver

The Smithy IDL allows clients to model operations which support **multiple**
authentication schemes. Correspondingly, a Smithy client may be loaded with
multiple AuthSchemes at runtime. The AuthSchemeResolver is the entity through
which the appropriate AuthScheme is selected and employed for a given operation
call.

Like the client's EndpointResolver, an AuthSchemeResolver and its inputs are
typically code-generated for a particular modeled service.

The AuthSchemeResolver accepts data from the request (e.g. request members) and
client (e.g. SigV4 "region") and returns a list of authentication scheme options
that the SDK should use when authenticating that request:

```java
public interface MyServiceAuthSchemeResolver {
    List<AuthSchemeOption> resolveAuthSchemes(MyServiceAuthSchemeResolverInput input);
}

public interface AuthSchemeResolverInput {
    // The single universal input to auth scheme resolution, since Smithy
    // operations can be modeled to have a specific preference order of AuthSchemes
    // that differs from the list at the service level.
	String operation();
}

public interface MyServiceAuthSchemeResolverInput extends AuthSchemeResolverInput {
    // The additional parameters exposed by methods here are entirely
    // unspecified and will vary based on a service's specific needs.
    //
    // A SigV4-enabled service, for example, might include a "region" parameter
    // that specifies the discrete partition of the service (a geographic
    // location, or perhaps a deployment stage) for which the Smithy client is making
    // the request.
}
```

### AuthSchemeOption

An AuthSchemeOption represents a **possible** AuthScheme that the current
request may use:

``` java
public interface AuthSchemeOption {
    /**
     * The unique ID of the scheme to use.
     */
    String schemeId();

    /**
     * Opaque container for additional context to be passed to the scheme's
     * IdentityResolver when retrieving Identity.
     */
    Object identityProperties();

    /**
     * Opaque container for additional context to be passed to the scheme's
     * Signer when signing the request. These allow the Signer to use
     * information from the request, client, etc. in the signature generation process.
     *
     * For example, a SigV4-enabled service would encode the "region" parameter
     * into its signer properties.
     */
    Object signerProperties();
}
```

After calling into the AuthSchemeResolver, the Smithy client should choose the
first AuthScheme in the resulting list of options that it supports. A scheme is
considered "supported" if the client is configured with a scheme with its ID
**and** if the identity resolver for the scheme is available.

This may be implemented like the following:

```java
public void resolveAuthScheme(OperationContext ctx) {
    AuthSchemeResolver resolver = ctx.getAuthSchemeResolver();
    MyServiceAuthSchemeResolverInput input = MyServiceAuthSchemeResolverInput.builder()
        .operation(ctx.getOperationName())
        .build();

    List<AuthSchemeOption> options = resolver.resolveAuthScheme(input);
    SelectedAuthScheme selected = selectAuthScheme(ctx, options);
    if (selected == null) {
        throw new OperationException("no available auth schemes");
    }

    ctx.setSelectedAuthScheme(selected);
}

private void selectAuthScheme(OperationContext ctx, List<AuthSchemeOption> options) {
    MyServiceClientConfig config = ctx.clientConfig();

    for (AuthScheme option : options) {
        // condition 1: the client has an auth scheme with an id matching the
        // one of this option
        AuthScheme<?, ?> found = ctx.getAuthSchemes().stream()
            .filter(it -> it.schemeId() == option.schemeId())
            .findFirst();
        if (found.isEmpty()) {
            continue;
        }

        // condition 2: the client has an identity resolver configured for this auth scheme
        //
        // note that we are only checking whether there IS an identity resolver, not whether
        // it can actually provide an identity
        IdentityResolver resolver = scheme.getIdentityResolver(config);
        if (resolver == null) {
            continue;
        }

        return new SelectedAuthScheme(
            found.get(),
            option.identityProperties(),
            option.signerProperties()
        );
    }

    return null;
}

record SelectedAuthScheme(
    AuthScheme<?, ?> scheme,
    Object identityProperties,
    Object signerProperties
) {}
```

## Order of Operations

The Smithy client should conduct the auth flow in an operation call as follows:

1. Auth scheme resolution (AuthSchemeResolver::resolveAuthScheme) is called
   **within** the retry loop. The selected auth scheme and identity / signer
   properties are stored in operation context for future use.
1. (Endpoint resolution)[TODO: NEED ANCHOR] is called. The signer properties
   from the resolved endpoint are **merged** into the ones sourced from auth
   scheme resolution.
1. Retrieve the IdentityResolver from the previously-resolved AuthScheme
   (AuthScheme::identityResolver). Identity resolution
   (IdentityResolver::resolve) is called with the identity properties sourced from
   scheme resolution.
1. Retrieve the Signer from the previously-resolved AuthScheme
   (AuthScheme::signer). Request signing (Signer::sign) is called with the
   merged signer properties from scheme and endpoint resolution.

## FAQ

### Wow, this seems like a lot. Do I really need all of these abstractions just to decide how to set an Authorization header?

The example set of interfaces provided in this listing represents the most
rigorous possible solution for implementing Identity & Auth. These interfaces
support multiple authentication schemes, _across_ multiple identity types and
transport message types. The Smithy client implementor is free to simplify
along any of those dimensions as fits their service needs.

As always, APIs should be designed or future-proofed with respect to backwards
compatibility of future client releases at the implementor's discretion.

### What about operations with no authentication?

"Anonymous" (no authentication) is explicitly modeled via `@smithy.api#noAuth`.
If you treat it as such (just like any other auth scheme) it does not typically
require much special-casing. Its Identity implementation can simply model no
additional properties, its IdentityResolver can return a static "anonymous"
identity, and its Signer can be a no-op.

We do recommend you avoid exposing configuration for "anonymous" identity
resolvers and signers, since doing so provides no additional value to the
caller and bloats your API surface.

### Why does the example AuthScheme::identityResolver accept a parameter, but ::signer doesn't?

This relates to how the boundaries of different components intersect.
Typically, the AuthScheme construct in the Smithy client exists in the static,
hand-written runtime that generated clients share. Conversely, client
configuration is typically generated per-service.

Thus it is necessary to join the two with an "adapter" interface implemented by
the generated client structure, which the static runtime component can call into:

```java
// code-generated
public class MyServiceClientConfig implements IdentityResolverConfig {
    // generated on the config structure because the service modeled support for both
    // aws.auth#sigv4 and smithy.api#httpBearerAuth
    private final IdentityResolver<SigV4CredentialIdentity> sigV4CredentialResolver;
    private final IdentityResolver<BearerTokenIdentity> bearerTokenResolver;

    // other configuration...

    public IdentityResolver<?> getIdentityResolver(String schemeId) {
        return switch (schemeId) {
            case "aws.auth#sigv4" -> sigV4CredentialsResolver;
            case "smithy.api#httpBearerAuth" -> bearerTokenResolver;
            case "smithy.api#noAuth" -> new AnonymousIdentityResolver();
            default -> null;
        };
    }
}
```

This is only the suggested approach and the implementor need not expose the
interfaces this way. For example, the AuthScheme implementation could instead
accept an opaque configuration object in its constructor and retrieve the
appropriate identity resolver without an input parameter using reflection.

The signer is instead a "constant" implementation detail of an auth scheme and
its implementation is also expected to exist in the runtime. The implementor
could instead choose to allow the caller to directly configure a specific
signer on client config, and implement a SignerConfig interface which the
AuthScheme accepts as an input for ::signer.

## Appendix: IdentityResolver idioms

### Resolver Chains

If a Smithy client supports retrieval of an Identity type from multiple
sources, the implementor may elect to model a "resolver chain" which tries each
type of resolver in sequence, returning the results of the first one that
successfully provides an Identity.

```java
public class IdentityResolverChain<TIdentity extends Identity> implements IdentityResolver<TIdentity> {
    private final IdentityResolver<TIdentity>[] resolvers;

    public IdentityResolverChain(IdentityResolver<TIdentity>... resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public TIdentity resolve(Object properties) throws ResolveIdentityException {
        for (IdentityResolver<TIdentity> resolver : resolvers) {
            try {
                return resolver.resolve(properties);
            } catch (ResolveIdentityException ignored) {
            }
        }

        throw new ResolveIdentityException("no resolvers in chain returned an identity");
    }
}
```

### Resolver Caching

If the process of retrieving an identity is a resource-intensive or otherwise
expensive operation (for example, if retrieving the identity requires an
external service call) then the implementor may wish to wrap a client's
identity resolver in a caching mechanism.

```java
public class IdentityResolverCache<TIdentity extends Identity> implements IdentityResolver<TIdentity> {
    private final IdentityResolver<TIdentity> resolver;

    private TIdentity cached;

    public IdentityResolverCache(IdentityResolver<TIdentity> resolver) {
        this.resolver = resolver;
    }

    @Override
    public synchronized TIdentity resolve(Object properties) throws ResolveIdentityException {
        Instant now = Instant.now();
        if (cached != null && (cached.expiration() == null || now.isBefore(cached.expiration()))) {
            return cached;
        }

        cached = resolver.resolve(properties);
        return cached;
    }
}
```
