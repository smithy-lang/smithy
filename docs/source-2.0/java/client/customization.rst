===========================
Customizing Client Behavior
===========================

Request-level overrides
-----------------------

Smithy Java supports client configuration overrides that are applied to a single request.
Create and use an override as follows:

.. code-block:: java

    var requestOverride = RequestOverrideConfig.builder()
        .protocol(...)
        .addIdentityResolver(...)
        // etc...
        .build()

    var fooOutput = client.callFoo(fooInput, requestOverride);

Each generated client will contain a client-specific ``RequestOverride`` class that includes any custom
configuration settings. To get a client-specific override builder, use the ``.requestOverrideBuilder`` on the
generated client:

.. code-block:: java

    var overrideBuilder = MyClient.requestOverrideBuilder();
    var override = overrideBuilder.customSetting("foo").build();
    var output = client.createFoo(input, override);

Interceptors
------------

Interceptors allow “injecting” logic into specific stages of the request execution pipeline.
Logic injection is done with hooks that the interceptor implements.

The following hooks are supported:

* **readBeforeExecution** - called at the start of an execution, before the client does anything else
* **modifyBeforeSerialization** - called before the input message is serialized into a transport message
* **readBeforeSerialization** - called before the input is serialized into a transport request
* **readAfterSerialization** - called after the input message is marshalled into a protocol-specific request
* **modifyBeforeRetryLoop** - called before the retry loop is entered that can be used to modify and return a new request
* **readBeforeAttempt** - called before each attempt at sending the transmission * request message to the service.
* **modifyBeforeSigning** - called before the request is signed; this method can modify and return a new request
* **readBeforeSigning** - called before the transport request message is signed
* **readAfterSigning** - called after the transport request message is signed
* **modifyBeforeTransmit** - called before the transport request message is sent to the service
* **readBeforeTransmit** - called before the transport request message is sent to the * service
* **readAfterTransmit** - called after the transport request message is sent to the service and a transport response message is received
* **modifyBeforeDeserialization** - called before the response is deserialized
* **readBeforeDeserialization** - called before the response is deserialized
* **readAfterDeserialization** - called after the transport response message is deserialized
* **modifyBeforeAttemptCompletion** - called when an attempt is completed. This method can
  modify and return a new output or error matching the currently executing operation
* **readAfterAttempt** - called when an attempt is completed
* **modifyBeforeCompletion** - called when an execution is completed
* **readAfterExecution** - called when an execution is completed

Interceptors implement the ``ClientInterceptor`` interface and override one or more hook methods.

Interceptors can be registered for all calls on a client:

.. code-block:: java

    var client = MyClient.builder()
        .addInterceptor(new MyInterceptor())
        .build();

Or for a single request:

.. code-block:: java

    var requestOverride = RequestOverrideConfig.builder()
        .addInterceptor(new MyInterceptor())
        .build()

    var fooOutput = client.callFoo(fooInput, requestOverride);

Plugins
-------

Plugins implement the ``ClientPlugin`` interface to modify client configuration when the client is created or when
an operation is called (if added to a ``RequestOverrideConfig``).

Plugins may set ``IdentityResolvers``, ``EndpointResolvers``, ``Interceptors``, ``AuthSchemeResolvers``,
and other client configuration in a repeatable way.

.. tip::

    Create one or more common plugins for your organization to apply a standard configuration to generated clients.


To apply a plugins to a client at runtime, use the ``addPlugin`` method on the client builder:

.. code-block:: java

    var client = MyClient.builder()
        .addPlugin(new MyPlugin())
        .build();

.. admonition:: Important
    :class: note

    Plugins are run once at client build time if added to the client builder, or each time a request is made if
    added through a ``RequestOverrideConfig``.

Default plugins
^^^^^^^^^^^^^^^

Plugins can be applied by default at client instantiation. To apply a plugin by default, add the plugin’s
fully qualified name to the ``defaultPlugins``` setting to your :ref`smithy-build <smithy-build>` configuration:

.. code-block:: json
    :caption: smithy-build.json

    "java-client-codegen": {
         // ...
         "defaultPlugins": [
            "fully.qualified.plugin.name.MyPlugin"
         ]
    }

.. admonition:: Important
    :class: note

    Plugins must have a public, zero-arg constructor defined. The code generator will check for an
    empty constructor when resolving default plugins and fail if one is not found.

