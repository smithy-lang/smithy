===================
Configuring Clients
===================

Smithy Java clients are composed of several components which are configurable at runtime:

* :ref:`Protocols <java-client-protocols>` - Defines how to serialize and deserialize a client request.
* :ref:`Transports <java-client-transports>` - Manages connections and the sending of data on the wire.
* :ref:`Auth Schemes <java-client-authSchemes>` - Adds authentication/authorization information to a client request.
* :ref:`Endpoint Resolvers <java-client-endpoints>` - Resolves the service endpoint to call.
* :ref:`Context Properties <java-client-context>` - Sets typed properties on the client.

.. _java-client-protocols:

---------
Protocols
---------

A protocol defines how to (de)serialize and bind data into a request. For example, an HTTP+JSON protocol
would handle the serialization of data into the HTTP message body as JSON and might bind some data to the
HTTP message headers.

Configure a protocol
^^^^^^^^^^^^^^^^^^^^

The Smithy IDL is protocol-agnostic and allows a service to support any number of protocols for transport and
(de)serialization of data. Like the Smithy IDL, Smithy Java clients are also protocol-agnostic and allow users
to configure a protocol at runtime.

To set a protocol at runtime, add the protocol to the client builder:

.. code-block:: java
    :caption: Set protocol at runtime

    var client = MyGeneratedClient.builder()
        .protocol(new MyProtocol())
        .build();

.. admonition:: Important
    :class: note

    The input/output types of the configured protocol must be compatible with the configured transport.

Set a default protocol
^^^^^^^^^^^^^^^^^^^^^^

While users can set protocols at runtime, a default protocol should be set when generating the client:

To configure the client plugin with a default protocol, add the protocol’s fully qualified Smithy ID to the protocol setting in the smithy-build.json configuration file:

.. code-block:: json
    :caption: smithy-build.json

    "plugins": {
        "java-client-codegen": {
          // Set the default protocol for the client.
          "protocol": "smithy.protocols#rpcv2Cbor",
          // ... additional settings
        }
    }


A service declares compatible protocols by applying the corresponding protocol trait on the service shape.
For example, if a service supports the rpcV2 protocol, it MUST have the protocol trait applied:

.. code-block:: smithy
    :caption: model.smithy

    @rpcV2Cbor // <- Protocol trait
    service MyService()

.. tip::

    The rpcv2-cbor protocol is a generic binary protocol and is good choice for services that want a fast, compact data format.

Provided protocols
^^^^^^^^^^^^^^^^^^

The Smithy Java framework provides the following protocols:

.. list-table::
    :header-rows: 1
    :widths: 20 5 30 35 10

    * - Name
      - Type
      - Trait
      - Description
      - Package
    * - rpcv2Cbor
      - Smithy
      - ``smithy.protocols#rpcv2Cbor``
      - HTTP RPC protocol that sends requests and responses with CBOR payloads.
      - ``client-rpcv2-cbor``
    * - AWS JSON 1.1
      - AWS
      - ``aws.protocols#awsJson1_1``
      - HTTP protocol that sends "POST" requests and responses with JSON payloads.
      - ``aws-client-awsjson``
    * - AWS Rest JSON 1.0
      - AWS
      - ``aws.protocols#restJson1``
      - HTTP protocol that sends requests and responses with JSON payloads
      - ``aws-client-awsjson``
    * - AWS Rest XML
      - AWS
      - ``aws.protocols#restXml``
      - HTTP protocol that sends requests and responses with XML payloads.
      - ``aws-client-restxml``

Writing custom protocols
^^^^^^^^^^^^^^^^^^^^^^^^^

To create a custom protocol, implement the ``ClientProtocol`` interface from the client-core package.

.. tip::

    If you are writing a service that uses a custom HTTP protocol, you may extend the ``HttpClientProtocol``
    and use one of the codecs provided by Smithy Java to get started.

Protocols are discovered via Service Provider Interface (SPI).  To use a custom protocol, you
must implement a protocol factory that implements ``ClientProtocolFactory``.
Once you have defined your factory, add it’s fully qualified name to the service provider file
(``META-INF/services/software.amazon.smithy.java.runtime.client.core.ClientProtocolFactory``).
As a reminder, make sure the custom protocol trait is applied to the service shape.

Codec‘s are used by client and server protocols for generic (de)serialization of types into wire data, such as JSON
Protocols SHOULD use an appropriate codec for (de)serialization where possible.
Smithy Java provides XML, JSON, and CBOR codecs.

When writing a custom protocol, we recommend writing compliance tests, which are used to validate the protocol across
multiple language implementations. The ``protocol-test-harness`` package provides a `JUnit5 <https://junit.org/junit5/>`_
test harness for running protocol compliance tests with Smithy Java.

.. _java-client-transports:

----------
Transports
----------

Transports manage connections, and handle the sending/receiving of serialized requests/responses.

``ClientTransport``'s can also configure default functionality like adding a user-agent header for HTTP request
by modifying the client builder using the ``configureClient`` method.

.. admonition:: Important
    :class: note

    When overriding the ``configureClient`` method of a ``ClientTransport``, you need to also call the ``configureClient``
    method of the ``MessageExchange``, if you want it to take effect. This allows for transports to override
    or even completely remove ``MessageExchange``-wide functionality.

Transport discovery
^^^^^^^^^^^^^^^^^^^

Transport implementations can be discovered by client code generators and dynamic clients via SPI.
To make a transport implementation discoverable, implement the ``ClientTransportFactory`` service provider.

If no transport is set on a client, the client will attempt to resolve a transport compatible with the current protocol
from the discoverable transport implementations.

Setting a default transport
^^^^^^^^^^^^^^^^^^^^^^^^^^^

To set a default transport, add the following to your :ref:`smithy-build.json <smithy-build>`:

.. code-block:: json
    :caption: smithy-build.json

    "java-client-codegen": {
        //...
        "transport": {
            "http-java": {}
        }
    }

.. admonition:: Important
    :class: note

    Transports MUST implement the ``ClientTransportFactory`` service provider to
    be discoverable by the code generation plugin.

Provided transports
^^^^^^^^^^^^^^^^^^^

* **http-java** - Uses the ``java.net.http.HttpClient`` to send and receive HTTP messages.
                  This transport is provided by the ``client-http`` module.


.. _java-client-authSchemes:

------------
Auth schemes
------------

Auth schemes add authentication/authorization information to a client request. An auth scheme is composed of:

1. Scheme ID - A unique identifier for the auth scheme. It SHOULD correspond to the ID of a Smithy trait
defining an auth scheme (see: https://smithy.io/2.0/spec/authentication-traits.html#smithy-api-authdefinition-trait).
2. Identity resolver - An API to acquire the customer's identity.
3. Signer - An API to sign requests using the resolved identity.

Auth Schemes may be registered by the client at runtime. To register an auth scheme,
use the ``putSupportedAuthSchemes`` method:

.. code-block:: java

    var client = MyClient.builder()
        .putSupportedAuthSchemes(new MyAuthScheme())
        .build()

Automatic registration
^^^^^^^^^^^^^^^^^^^^^^

The Client code generation plugin can discover auth schemes on the classpath. If a discovered auth scheme’s ID matches
an auth scheme ID in the Smithy model it will be automatically registered in the generated client.

To add an auth scheme automatically to a generated client based on a trait in the model, the auth scheme must provide
an ``AuthSchemeFactory`` implementation and register that implementation via SPI. Smithy Java client codegen will
automatically search the classpath for relevant ``AuthSchemeFactory``` implementations and attempt to match those with
a corresponding trait in the model.

Effective auth schemes
^^^^^^^^^^^^^^^^^^^^^^

Operations may have one or more “effective auth schemes” that could be used to authenticate a request.
Auth scheme traits applied to the service shape are inherited by all service operations unless those
operations have the auth trait applied.

The ``@auth`` trait define a priority-ordered list of authentication schemes supported by a service or operation.
When applied to a service, it defines the default authentication schemes of every operation in the service.
When applied to an operation, it defines the list of all authentication schemes supported by the operation,
overriding any auth trait specified on a service.

.. code-block:: smithy
    :caption: model.smithy

    @httpBasicAuth
    @httpDigestAuth
    @httpBearerAuth
    service MyService {
        version: "2020-01-29"
        operations: [
            OperationA
            OperationB
        ]
    }

    // This operation does not have the @auth trait and is bound to a service
    // without the @auth trait. The effective set of authentication schemes it
    // supports are: httpBasicAuth, httpDigestAuth and httpBearerAuth
    operation OperationA {}

    // This operation does have the @auth trait and is bound to a service
    // without the @auth trait. The effective set of authentication schemes it
    // supports are: httpDigestAuth.
    @auth([httpDigestAuth])
    operation OperationB {}

https://smithy.io/2.0/spec/authentication-traits.html#smithy-api-auth-trait
See :ref:`@auth <auth-trait>` trait for a more thorough discussion on how auth schemes are resolved.

Identity resolution
^^^^^^^^^^^^^^^^^^^

To use an auth scheme in a client, the client must register a corresponding identity resolver
that provides a compatible identity class. Auth schemes can provide a default resolver themselves
or clients can register resolvers via the client builder or via a client plugin.

.. tip::

    Multiple identity resolvers can be chained together using the IdentityResolver.chain method.

Provided auth schemes
^^^^^^^^^^^^^^^^^^^^^

A number of auth schemes are provided by default in the ``client-http`` package. These include:

*  :ref:`httpBearerAuth <httpBearerAuth-trait>` - Supports HTTP Bearer authentication as defined in RFC 6750.
*  :ref:`httpApiKeyAuth <httpApiKeyAuth-trait>` - Supports HTTP authentication using an API key sent in a header or query string parameter.
*  :ref:`httpBasicAuth <httpBasicAuth-trait>` - Supports HTTP Basic authentication as defined in RFC 2617.

Add the ``client-http`` package as a dependency of your project to make these auth schemes available in your service.

Worked example: Adding HTTP API key authentication
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Consider a Smithy modeled API for a service, ``ExampleService``. We would like to enable users of our generated SDK
to authenticate to the API using an API key sent via an ``x-api-key`` HTTP header.
Smithy Java already provides an ``httpApiKeyAuth`` auth scheme that we can use to allow
this API key authentication.

Before we can add any auth scheme implementations to our generated client we must first add the associated
:ref:`@httpApiKeyAuth <httpApiKeyAuth-trait>` scheme trait to our service model.

.. code-block:: smithy
    :caption: model.smithy

    namespace com.example

    @httpApiKeyAuth(name: "X-Api-Key", in: "header") // <- Add auth scheme trait
    service ExampleService {
        version: "2025-05-05"
        // ...
    }

Authentication schemes are effectively part of your services interface and so (outside of testing)
SHOULD always be modeled in your Smithy model using a trait. See the :ref:`@authDefinition <authDefinition-trait>`
trait for more information on how to define a custom auth scheme in your Smithy model.

Now that we have added our auth trait to the Smithy model we need to add a corresponding auth scheme implementation
to our client’s dependencies. The ``client-http package`` provides an ``HttpApiKeyAuthScheme`` implementation corresponding
to the ``@httpApiKeyAuth`` trait.

.. code-block:: kotlin
    :caption: build.gradle.kts

    dependencies {
        // Add the HTTP auth scheme to the classpath
        implementation("software.amazon.smithy.java:client-http:__smithy_java_version__")
        // ...
    }

The ``HttpApiKeyAuthScheme`` class implements the ``AuthSchemeFactory`` service provider, making the auth scheme
discoverable by the client codegen plugin.

The built-in Smithy Java HTTP auth schemes all require one or more compatible ``IdentityResolver``
to be register with the client. This resolver will handle actually fetching the clients credentials.
The ``HttpApiKeyAuthScheme`` scheme needs an identity resolver that returns an ``ApiKeyIdentity``.
For testing purposes we will provide a static resolver as follows:

.. code-block:: java

    var client = ExampleService.builder()
            .addIdentityResolver(
                IdentityResolver.of(new ApiKeyIdentity.create("example-api-key"))
            )
            .build()

Or, we could create a custom resolver that resolves the ``ApiKeyIdentity`` from an environment variable, ``EXAMPLE_API_KEY``:

.. code-block:: java
    :caption: Environment variable identity resolver implementation

    public final class EnvironmentVariableIdentityResolver implements IdentityResolver<ApiKeyIdentity> {
        private static final String API_KEY_PROPERTY = "EXAMPLE_API_KEY"

        @Override
        public Class<ApiKeyIdentity> identityType() {
            return ApiKeyIdentity.class;
        }

        @Override
        public CompletableFuture<IdentityResult<AwsCredentialsIdentity>> resolveIdentity(AuthProperties requestProperties) {
            String apiKey = System.getenv(API_KEY_PROPERTY);

            if (apiKey == null || apiKey.isEmpty())
                return CompletableFuture.completedFuture(
                    IdentityResult.ofError(getClass(), "Could not find API KEY")
                );
            }

            return CompletableFuture.completedFuture(IdentityResult.of(ApiKeyIdentity.create(apiKey)));
        }

Smithy Java also allows identity resolvers to be chained together if we want to check multiple locations for the client.

.. code-block:: java
    :caption: Chaining identity resolvers

    IdentityResolver.chain(List.of(new FirstResolver(), new SecondResolver())

.. _java-client-endpoints:

-----------------
Endpoint resolver
-----------------

Endpoint resolvers determine the endpoint to use for an operation. For example, an endpoint resolver could
determine what subdomain to use, i.e. ``us-east-2.myservice.com`` based on a region setting on the client.

To set a static endpoint for a client use the following client builder setter:

.. code-block:: java

    client.builder()
        .endpointResolver(EndpointResolver.staticResolver("https://example.com"))
        .build()

.. tip::

    Create a common endpoint resolver for your organization that can be shared across clients.

.. _java-client-context:

-------------------
Context properties
-------------------

Smithy Java clients allow users to add any configurations to a typed property bag, via the putConfig method.
The properties are tied to a typed key and used by client pipeline components such as request signers.

For example, a ``REGION`` property might need to be set on the client in order for a ``Sigv4`` request signer to correctly function.
Configuration parameters can be set on a client using a typed property key via the putConfig method:

.. code-block:: java
    :caption: Setting REGION context property

    static Context.Key<String> MY_PROPERTY = Context.key("a config property");

    ...

    var client = MyClient.builder()
        .putContext(MY_PROPERTY, "value")
        .build();

Custom builder setters
^^^^^^^^^^^^^^^^^^^^^^

For common settings on a client, it is often desirable to use specifically-named setter methods such as .region("us-east-1")
rather than requiring users to know the specific context property to use for a configuration parameter.
The ``ClientSetting`` interface can be used to create a custom setter that for client builders.

For example we would write a custom setting as:

.. code-block:: java
    :caption: custom client setting implementation

    public interface CustomSetting<B extends Client.Builder<?, B>> extends ClientSetting<B> {
        Context.Key<String> MY_PROPERTY = Context.key("A custom string configuration property");

        default B custom(String custom) {
            return putConfig(MY_PROPERTY, custom);
        }
    }

.. tip::

    If a config property is required, make sure to validate that it exists using a default plugin (see below)

A client setting can then be added to our generated clients using the defaultSettings setting in the smithy-build.json file:

.. code-block:: json
    :caption: smithy-build.json

    "java-client-codegen": {
        //...
        "defaultSettings": [
            "com.example.settings.CustomSetting"
        ],
        //...
    }

Now we can use our new setting as follows:

.. code-block:: java

    var client = MyClient.builder()
        .custom("that was easy!")
        .build();

.. tip::

    Default settings are typically paired with a default plugin to add the configuration and behavior of a feature
    (respectively) to a client by default.

Composing settings
^^^^^^^^^^^^^^^^^^

Some features require multiple custom settings. Because custom settings are simply Java interfaces, we can compose them.

For example, the SigV4 auth scheme requires that a region setting and clock setting be set on a client as well
as an additional settings, the signing name of the service. We can define the ``SigV4Settings`` interface as follows:

.. code-block:: java
    :caption: composed setting

    public interface SigV4Settings<B extends ClientSetting<B>>
        extends ClockSetting<B>, RegionSetting<B> {

        /**
         * Service name to use for signing. For example {@code lambda}.
         */
        Context.Key<String> SIGNING_NAME = Context.key("Signing name to use for computing SigV4 signatures.");

        /**
         * Signing name to use for the SigV4 signing process.
         *
         * <p>The signing name is typically the name of the service. For example {@code "lambda"}.
         *
         * @param signingName signing name.
         */
        default B signingName(String signingName) {
            // Validation of the signing name
            if (signingName == null || signingName.isEmpty()) {
                throw new IllegalArgumentException("signingName cannot be null or empty");
            }
            return putConfig(SIGNING_NAME, signingName);
        }
    }

When the ``SigV4Settings`` interface is added to the codegen configuration as a default setting it will also add the
``ClockSetting`` and ``RegionSetting`` setters to the generated client’s builder.

.. tip::

    Create a custom setting class for your organization that aggregates all common settings for your clients.
    This minimizes the number of code generation configurations you need to provide to create a functional client.
    Add new settings to the aggregate setting to add them to clients without changing the codegen configuration.

