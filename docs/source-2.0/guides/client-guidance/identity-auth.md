# Identity and Authentication

Operations supported by a Smithy service generally require a mechanism for
authenticating the caller's identity. This section describes how to model the
workflow of identity retrieval and request authentication in Smithy clients.

The Java-like interfaces in this section are illustrative behavioral contracts,
not mandatory source-compatible APIs. Implementations may adapt names, types,
synchronous or asynchronous execution, mutability, and lifecycle management to
the idioms of their language while preserving the behavior described here.

## Identity

An `Identity` represents **who the caller is**. The caller's identity could be
anonymous, a token, a public/private key pair, or another form of identity.

Identity types in the client runtime should conform to a common `Identity`
abstraction so that authentication components can reference them generically.
Scheme-specific APIs can then use concrete identity types, and the same identity
type can be reused by multiple schemes.

```java
public interface Identity {
    default Instant expirationTime() {
        return null;
    }
}

// for aws.auth#sigv4, aws.auth#sigv4a
public interface AwsCredentialsIdentity extends Identity {
    String accessKeyId();
    String secretAccessKey();

    default String sessionToken() {
        return null;
    }

    default String accountId() {
        return null;
    }
}

// for smithy.api#httpBearerAuth
public interface TokenIdentity extends Identity {
    String token();
}

// Username and password-based credentials.
public interface LoginIdentity extends Identity {
    String username();
    String password();
}

// for smithy.api#httpApiKeyAuth
public interface ApiKeyIdentity extends Identity {
    String apiKey();
}
```

## IdentityResolver

An `IdentityResolver` retrieves a single type of identity. Identity resolution
accepts a
[client context](https://smithy.io/2.0/guides/client-guidance/context.html)
containing properties that can affect resolution.

Identity resolution has three possible outcomes:

1. An identity is successfully resolved.
2. An expected identity-specific failure occurs, such as an identity source not
   being configured. This outcome is returned as a result without an identity
   and allows a resolver chain to continue.
3. An unexpected or unrecoverable failure occurs, such as malformed
   configuration or a runtime failure from which resolution cannot recover.
   The resolver raises an error, which must not be treated as an expected miss.

The `IdentityResolver` interface may be modeled as follows:

```java
public interface IdentityResolver<IdentityT extends Identity> {
    IdentityResult<IdentityT> resolveIdentity(Context properties);
    Class<IdentityT> identityType();

    default void invalidate(IdentityT rejectedIdentity) {}
}

public final class IdentityResult<IdentityT extends Identity> {
    private final IdentityT identity;
    private final String error;
    private final Class<?> resolver;

    public static <IdentityT extends Identity> IdentityResult<IdentityT> success(IdentityT identity) {
        return new IdentityResult<>(Objects.requireNonNull(identity));
    }

    public static <IdentityT extends Identity> IdentityResult<IdentityT> notFound(
            Class<?> resolver,
            String error
    ) {
        return new IdentityResult<>(
                Objects.requireNonNull(resolver),
                Objects.requireNonNull(error));
    }

    private IdentityResult(IdentityT identity) {
        this.identity = identity;
        this.error = null;
        this.resolver = null;
    }

    private IdentityResult(Class<?> resolver, String error) {
        this.identity = null;
        this.error = error;
        this.resolver = resolver;
    }

    public IdentityT identity() {
        return identity;
    }

    public String error() {
        return error;
    }

    /**
     * Get the identity resolver class that could not resolve an identity.
     *
     * @return the identity resolver.
     */
    public Class<?> resolver() {
        return resolver;
    }

    /**
     * Get the resolved identity, or throw if no identity is present.
     *
     * @return the resolved identity.
     */
    public IdentityT unwrap() {
        if (identity == null) {
            throw new IdentityNotFoundException(
                    "Unable to resolve an identity: " + error
                            + " (" + resolver.getName() + ")");
        }
        return identity;
    }

    @Override
    public String toString() {
        if (identity != null) {
            return "IdentityResult[identity=" + identity.getClass().getName() + ']';
        }
        return "IdentityResult[error='" + error + "', resolver=" + resolver.getName() + ']';
    }
}
```

`IdentityResolver::invalidate` signals that an identity used for a request was
rejected. This guide defines the generic invalidation contract, whose default
implementation is a no-op. The Modular AWS Credential Chains SEP applies this
contract to AWS credential chains and defines request-pipeline integration and
propagation. The Credential Refresh Behavior SEP defines invalidation triggers
and the resulting AWS credential cache behavior.

There may be multiple valid implementations of an identity resolver for a given
type. For example, one implementation of an `IdentityResolver<TokenIdentity>`
may read the bearer token from the system environment, while another may read
it from application configuration.

The client configuration for a service should typically make an applicable
`IdentityResolver` configurable for every authentication scheme supported by
the service. Implementations may register resolvers by identity type, auth
scheme ID, modeled trait, or another mechanism. An auth scheme is responsible
for locating or adapting an applicable resolver from the configured resolver
repository. This permits multiple auth schemes to share an identity resolver
and allows dynamically discovered schemes to select resolvers using runtime
model or schema information.

```java
ClientConfig config = ClientConfig.builder()
        .addIdentityResolver(sigV4CredentialResolver)
        .addIdentityResolver(bearerTokenResolver)
        // other configuration...
        .build();
```

## Signer

A `Signer` **authenticates a request using an identity**. Signing includes both
generating cryptographic signatures and attaching non-cryptographic
authentication material, such as a bearer token.

For example, a signer authenticates an outgoing HTTP request using a bearer
token by placing the token in the `Authorization` header.

At a minimum, signing requires a request and an identity. A signer may mutate
the request or return a modified copy. It may also return signature metadata
needed by later processing, such as event stream signing. The behavior depends
on the language idioms and implementation, though implementations should bias
towards the most performant approach with the least amount of allocations.

```java
public interface Signer<RequestT, IdentityT extends Identity>
        extends AutoCloseable {

    SignResult<RequestT> sign(RequestT request, IdentityT identity, Context properties);

    @Override
    default void close() {}
}

public record SignResult<RequestT>(RequestT signedRequest, String signature) {
    public SignResult(RequestT signedRequest) {
        this(signedRequest, "");
    }
}
```

Signers may be shared, created per client, or created per request attempt.
Clients should close a signer when its configured lifetime ends to release any
state like caches, scratch buffers, etc. Stateless signers may use the default
no-op `close` implementation.

## AuthScheme

An `AuthScheme` describes the components needed to authenticate a request using
a single authentication scheme:

```java
public interface AuthScheme<RequestT, IdentityT extends Identity> {
    /**
     * Unique identifier for this auth scheme. Corresponds to the ID
     * of a Smithy IDL auth trait, for example "smithy.api#httpBearerAuth".
     */
    ShapeId schemeId();

    /**
     * The identity type used by this authentication scheme.
     */
    Class<IdentityT> identityClass();

    /**
     * The request type this authentication scheme can sign.
     */
    Class<RequestT> requestClass();

    /**
     * Provides an identity resolver for this authentication scheme.
     * This API can return null to indicate that an identity
     * resolver for this scheme's identity type is not available in the
     * current client environment.
     */
    default IdentityResolver<IdentityT> identityResolver(IdentityResolvers resolvers) {
        return resolvers.identityResolver(identityClass());
    }

    /**
     * Provides default properties used when resolving an identity.
     */
    default Context getIdentityProperties(Context operationContext) {
        return Context.empty();
    }

    /**
     * Provides default properties used when signing a request.
     */
    default Context getSignerProperties(Context operationContext) {
        return Context.empty();
    }

    /**
     * Provides a signer for this authentication scheme. An implementation
     * might retrieve the signer from client configuration or provide its own
     * implementation directly.
     */
    Signer<RequestT, IdentityT> signer();

    /**
     * Creates a signer used to sign event stream frames.
     */
    default <F extends Frame<?>> FrameProcessor<F> eventSigner(
            IdentityT identity,
            Context context,
            String seedSignature
    ) {
        return FrameProcessor.identity();
    }
}

public interface IdentityResolvers {
    <IdentityT extends Identity> IdentityResolver<IdentityT> identityResolver(
            Class<IdentityT> identityClass);
}
```

This example indexes resolvers by identity type. Implementations may instead
index them by auth scheme ID, modeled trait, or another form of runtime
metadata. The `AuthScheme::identityResolver` method is the adaptation point
between the repository's lookup strategy and the resolver required by a
specific scheme.

Implementations that construct clients from runtime models or dynamic schemas
may also provide `AuthScheme` factories discoverable by scheme ID. These
factories map modeled authentication traits to runtime `AuthScheme`
implementations.

```java
public interface AuthSchemeFactory<TraitT extends Trait> {
    /**
     * The ID of the authentication scheme created by this factory.
     */
    ShapeId schemeId();

    /**
     * Creates an authentication scheme from its modeled trait.
     */
    AuthScheme<?, ?> createAuthScheme(TraitT trait);
}

// Nested in HttpApiKeyAuthScheme.
public static final class Factory implements AuthSchemeFactory<HttpApiKeyAuthTrait> {
    @Override
    public ShapeId schemeId() {
        return HttpApiKeyAuthTrait.ID;
    }

    @Override
    public AuthScheme<?, ?> createAuthScheme(HttpApiKeyAuthTrait trait) {
        return new HttpApiKeyAuthScheme(
                trait.getName(),
                trait.getIn(),
                trait.getScheme().orElse(null));
    }
}
```

A list of supported `AuthScheme`s should be configurable by the end user of the
Smithy client. The default client configuration should typically preload the
service's supported `AuthScheme`s.

If multiple configured auth schemes use the same scheme ID, the most recently
registered implementation replaces the previous one. This allows user
configuration to deterministically replace a default implementation.

```java
// in this example, the service supports some combination of
// smithy.api#httpBearerAuth and aws.auth#sigv4
public MyServiceClientConfig defaultConfig() {
    MyServiceClientConfig.Builder builder = MyServiceClientConfig.builder();

    builder.putSupportedAuthSchemes(new DefaultHttpBearerAuthScheme());
    builder.putSupportedAuthSchemes(new DefaultSigV4AuthScheme());

    // ...

    return builder.build();
}
```

## AuthSchemeResolver

Smithy allows clients to model operations that support **multiple**
authentication schemes. A Smithy client can therefore have multiple
`AuthScheme`s available at runtime. The `AuthSchemeResolver` selects the
appropriate `AuthScheme` for an operation call.

Like the client's `EndpointResolver`, an `AuthSchemeResolver` is a runtime
component that can be shared by generated and dynamic clients. It accepts
`AuthSchemeResolverParams`, which provide the protocol, operation, and request
context needed to resolve authentication schemes. The operation contains its
effective auth scheme IDs and can expose schema information needed by a dynamic
resolver.

```java
public interface AuthSchemeResolver {
    /**
     * Returns authentication scheme options in priority order.
     */
    List<AuthSchemeOption> resolveAuthScheme(AuthSchemeResolverParams params);
}

public final class AuthSchemeResolverParams {
    private final ShapeId protocolId;
    private final ApiOperation<?, ?> operation;
    private final Context context;

    private AuthSchemeResolverParams(Builder builder) {
        this.protocolId = Objects.requireNonNull(builder.protocolId, "protocolId is null");
        this.operation = Objects.requireNonNull(builder.operation, "operation is null");
        this.context = Objects.requireNonNullElseGet(builder.context, Context::create);
    }

    public ShapeId protocolId() {
        return protocolId;
    }

    public ApiOperation<?, ?> operation() {
        return operation;
    }

    public Context context() {
        return context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ShapeId protocolId;
        private ApiOperation<?, ?> operation;
        private Context context;

        public Builder protocolId(ShapeId protocolId) {
            this.protocolId = protocolId;
            return this;
        }

        public Builder operation(ApiOperation<?, ?> operation) {
            this.operation = operation;
            return this;
        }

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public AuthSchemeResolverParams build() {
            return new AuthSchemeResolverParams(this);
        }
    }
}
```

### AuthSchemeOption

An `AuthSchemeOption` represents a **possible** `AuthScheme` for the current
request. Identity and signer properties use the extensible, type-safe property
bag described in the
[client context](https://smithy.io/2.0/guides/client-guidance/context.html)
guidance.

```java
public record AuthSchemeOption(
        ShapeId schemeId,
        Context identityPropertyOverrides,
        Context signerPropertyOverrides
) {
    public AuthSchemeOption {
        Objects.requireNonNull(schemeId, "schemeId cannot be null");
        Objects.requireNonNull(
                identityPropertyOverrides,
                "identityPropertyOverrides cannot be null");
        Objects.requireNonNull(
                signerPropertyOverrides,
                "signerPropertyOverrides cannot be null");
    }

    public AuthSchemeOption(ShapeId schemeId) {
        this(schemeId, Context.empty(), Context.empty());
    }
}
```

Identity and signer property overrides must not be null. An option with no
overrides uses empty contexts.

After invoking the `AuthSchemeResolver`, the Smithy client should choose the
first `AuthSchemeOption` whose scheme it supports. A scheme is considered
supported if the client has registered an `AuthScheme` with the same ID and an
identity resolver for the scheme is available. If the runtime uses multiple
transport message representations, the scheme must also support the
representation used by the current request.

Support must be checked before evaluating the scheme's default properties or
attempting to resolve an identity. Scheme selection checks whether a resolver
is available, not whether that resolver will ultimately produce an identity.
Once a scheme is selected, an identity resolution failure causes the request
attempt to fail rather than falling through to another auth scheme. Fallback
between identity sources belongs within an identity resolver chain.

This may be implemented as follows:

```java
public void resolveAndSelectAuthScheme(OperationContext ctx) {
    AuthSchemeResolver resolver = ctx.getAuthSchemeResolver();
    AuthSchemeResolverParams params = AuthSchemeResolverParams.builder()
            .protocolId(ctx.getProtocolId())
            .operation(ctx.getOperation())
            .context(ctx.getContext())
            .build();

    List<AuthSchemeOption> options = resolver.resolveAuthScheme(params);
    SelectedAuthScheme selected = selectAuthScheme(ctx, options);
    if (selected == null) {
        throw new OperationException("no available auth schemes");
    }

    ctx.setSelectedAuthScheme(selected);
}

private SelectedAuthScheme selectAuthScheme(OperationContext ctx, List<AuthSchemeOption> options) {
    IdentityResolvers identityResolvers = ctx.identityResolvers();

    for (AuthSchemeOption option : options) {
        // condition 1: the client has an auth scheme with an ID matching
        // this option
        AuthScheme<?, ?> found = ctx.getAuthSchemesById().get(option.schemeId());
        if (found == null) {
            continue;
        }

        // condition 2: the scheme supports the current transport message type
        if (!found.requestClass().isInstance(ctx.getRequest())) {
            continue;
        }

        // condition 3: the client has an identity resolver configured for
        // this auth scheme
        //
        // note that we are only checking whether there is an identity
        // resolver, not whether it can actually provide an identity
        IdentityResolver<?> identityResolver = found.identityResolver(identityResolvers);
        if (identityResolver == null) {
            continue;
        }

        return new SelectedAuthScheme(
            found,
            identityResolver,
            option.identityPropertyOverrides(),
            option.signerPropertyOverrides()
        );
    }

    return null;
}

record SelectedAuthScheme(
    AuthScheme<?, ?> scheme,
    IdentityResolver<?> identityResolver,
    Context identityPropertyOverrides,
    Context signerPropertyOverrides
) {}
```

## Order of Operations

For every request attempt within the retry loop, the Smithy client should
perform the following authentication flow:

1. Resolve the auth scheme options (`AuthSchemeResolver::resolveAuthScheme`)
   and select the first supported scheme.
2. Use the `IdentityResolver` selected with the `AuthScheme`.
3. Merge identity properties using the following precedence, where later
   values override earlier values:

   1. Auth scheme defaults.
   2. Auth scheme option overrides.

4. Resolve the identity (`IdentityResolver::resolveIdentity`) using the merged
   identity properties.
5. Resolve the endpoint.
6. Form the final signer properties by merging the following sources, where
   later values override earlier values:

   1. Auth scheme defaults.
   2. Auth scheme option overrides.
   3. Endpoint-provided signer property overrides.

7. Retrieve the `Signer` from the selected `AuthScheme`
   (`AuthScheme::signer`) and sign the request using the merged signer
   properties.

The signer property ordering above defines merge precedence, not evaluation
timing. Implementations may invoke `AuthScheme::getSignerProperties` while
selecting the auth scheme or later before signing. Auth scheme defaults should
not depend on the resolved identity or endpoint; endpoint-derived values should
be supplied as endpoint-provided signer property overrides.

For protocols that require signed event streams, the auth scheme may
additionally provide an event signer initialized with the resolved identity,
merged signer properties, and the signature returned when signing the initial
request.

## FAQ

### What about operations with no authentication?

"Anonymous" (no authentication) is not an explicitly modeled auth scheme.
Rather, it is a consequence of an operation having an empty
[`@auth`](https://smithy.io/2.0/spec/authentication-traits.html#smithy-api-auth-trait)
trait list (`@auth([])`), or of using the
[`@optionalAuth`](https://smithy.io/2.0/spec/authentication-traits.html#optionalauth-trait)
trait. In the auth scheme knowledge index, this is represented as the synthetic
`smithy.api#noAuth` scheme ID.

Clients can treat anonymous authentication as a first-class auth scheme without
much special-casing. Its `Identity` implementation can model no additional
properties, its `IdentityResolver` can return a static anonymous identity, and
its `Signer` can be a no-op. These components should always be available
without user configuration.

Operations with `@optionalAuth` support both authenticated and anonymous
access. The client should include anonymous auth as a candidate in the auth
scheme resolution for these operations, typically as the lowest-priority
option.

Clients should avoid exposing configuration for anonymous identity resolvers
and signers because doing so provides no additional value to the caller and
unnecessarily expands the API surface.

### What about auth schemes returned by endpoint resolution?

Some existing clients support endpoint rules that return auth scheme IDs and
signer property overrides. These rules may augment the selected auth scheme or
replace it after endpoint resolution, potentially requiring the client to
resolve another identity.

Endpoint-driven auth scheme replacement is deprecated compatibility behavior.
New services and implementations should select auth schemes through the
`AuthSchemeResolver`. Endpoint resolution should only contribute
endpoint-specific signer property overrides for the selected scheme.

### Why does `AuthScheme::identityResolver` accept a parameter, but `AuthScheme::signer` does not?

`AuthScheme` implementations commonly exist in a static, hand-written runtime,
while configured identity resolvers vary between clients.

The client can expose its configured resolvers through a repository keyed by
identity type:

```java
public final class ClientIdentityResolvers implements IdentityResolvers {
    private final Map<Class<?>, IdentityResolver<?>> resolvers = new HashMap<>();

    public ClientIdentityResolvers(List<IdentityResolver<?>> resolvers) {
        for (IdentityResolver<?> resolver : resolvers) {
            this.resolvers.put(resolver.identityType(), resolver);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <IdentityT extends Identity> IdentityResolver<IdentityT> identityResolver(
            Class<IdentityT> identityClass
    ) {
        return (IdentityResolver<IdentityT>) resolvers.get(identityClass);
    }
}
```

This is only a suggested approach, and the implementer need not expose the
interfaces this way. Resolver registration may instead be keyed by auth scheme
ID, modeled trait, or another implementation-specific mechanism. An
`AuthScheme` may also adapt a shared resolver into the scheme-specific resolver
it returns.

Signers are often supplied by the shared runtime, but are not required to be
constant or stateless. An implementation may allow the caller to configure a
signer, create one per client, or create one per request attempt. If a signer
holds resources, the implementation should provide an appropriate lifecycle
mechanism.

## Appendix: IdentityResolver idioms

### Resolver Chains

If a Smithy client supports retrieving an identity from multiple sources, the
implementer may use a resolver chain that tries each resolver in sequence and
returns the first successful result. An expected miss allows the chain to
continue. An unexpected or unrecoverable error must stop the chain and be
exposed to the caller.

```java
public class IdentityResolverChain<IdentityT extends Identity> implements IdentityResolver<IdentityT> {
    private final Class<IdentityT> identityType;
    private final List<IdentityResolver<IdentityT>> resolvers;

    public IdentityResolverChain(List<IdentityResolver<IdentityT>> resolvers) {
        this.resolvers = List.copyOf(
                Objects.requireNonNull(resolvers, "resolvers cannot be null"));
        if (this.resolvers.isEmpty()) {
            throw new IllegalArgumentException("Cannot chain an empty resolver list");
        }

        this.identityType = this.resolvers.get(0).identityType();
        for (IdentityResolver<IdentityT> resolver : this.resolvers) {
            if (!identityType.equals(resolver.identityType())) {
                throw new IllegalArgumentException(
                        "All resolvers in a chain must resolve the same identity type");
            }
        }
    }

    @Override
    public Class<IdentityT> identityType() {
        return identityType;
    }

    @Override
    public IdentityResult<IdentityT> resolveIdentity(Context properties) {
        List<IdentityResult<?>> errors = new ArrayList<>();

        for (IdentityResolver<IdentityT> resolver : resolvers) {
            IdentityResult<IdentityT> result = resolver.resolveIdentity(properties);
            if (result.error() == null) {
                return result;
            }
            errors.add(result);
        }

        return IdentityResult.notFound(
            IdentityResolverChain.class,
            "Attempted resolvers: " + errors
        );
    }

    @Override
    public void invalidate(IdentityT rejectedIdentity) {
        for (IdentityResolver<IdentityT> resolver : resolvers) {
            resolver.invalidate(rejectedIdentity);
        }
    }
}
```

### Resolver Caching

If the process of retrieving an identity is a resource-intensive or otherwise
expensive operation, such as one that requires an external service call, the
implementer may wrap the identity resolver in a cache. A cache must preserve
resolution semantics for any context properties that affect identity resolution
and must define appropriate concurrency and lifecycle behavior.

AWS credential caching has additional requirements that are intentionally not
repeated here. The Modular AWS Credential Chains SEP applies
`IdentityResolver::invalidate` to AWS credential chains and defines
request-pipeline integration and propagation. The Credential Refresh Behavior
SEP defines AWS credential caching, refresh windows, refresh backoff,
concurrency, invalidation triggers, identity matching, and cache behavior.
