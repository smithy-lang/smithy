==============
Client Plugins
==============

Smithy Java provides a number of client plugins that add functionality to generated clients.

----------
MockPlugin
----------

The ``MockPlugin`` intercepts client requests and returns pre-defined mock responses, shapes, or exceptions.
The plugin facilitates testing client request/responses without the need to set up a mock server.

Usage
^^^^^

Add the ``mock-client-plugin`` package as a dependency:

.. code-block:: java
    :caption: build.gradle.kts

    dependencies {
        implementation("software.amazon.smithy.java.client.http.mock:mock-client-plugin:__smithy_java_version__")
    }

Use the plugin to return canned responses from the http client:

.. code-block:: java

    // (1) Create a response queue and add a set of canned responses that will be returned
    //     from client in the order in which they were added to the queue.
    var mockQueue = new MockQueue();
    mockQueue.enqueue(
        HttpResponse.builder()
            .statusCode(200)
            .body(DataStream.ofString("{\"id\":\"1\"}"))
        .build()
    );
    mockQueue.enqueue(
        HttpResponse.builder()
            .statusCode(429)
            .body(DataStream.ofString("{\"__type\":\"InvalidSprocketId\"}"))
        .build()
    );

    // (2) Create a MockPlugin instance using the request queue created above.
    var mockPlugin = MockPlugin.builder().addQueue(mockQueue).build();

    // (3) Create a client instance that uses the MockPlugin.
    var client = SprocketClient.builder()
            .addPlugin(mockPlugin)
            .build();

    // (4) Call client to get the first canned response from the queue.
    var response = client.createSprocket(CreateSprocketRequest.builder().id(2).build());

    // (5) Inspect the HTTP requests that were sent to the client.
    var requests = mock.getRequests();

---------------
UserAgentPlugin
---------------

The ``UserAgentPlugin`` adds a default ``User-Agent`` header to an HTTP request if none is set.

.. note::

    This plugin is applied by default by all built-in HTTP transports.

The added agent header has the form:

.. code-block::

    smithy-java/<smithy> ua/<ua> os/<family>#<version> lang/java#<version> m/<features>

.. list-table::
    :header-rows: 1
    :widths: 20 10 70

    * - Property
      - Example
      - Description
    * - ``smithy``
      - ``0.0.1``
      - Smithy Java version in use by client in SemVer format.
    * - ``ua``
      - ``2.1``
      - Version of the ``User-Agent`` metadata
    * - ``os-family``
      - ``macos``
      - Operating system client is running on
    * - ``version``
      - ``14.6.1``
      - version of OS or Language the client is running on.
    * - ``features``
      - ``a,b``
      - Comma-separated list of feature Ids

Feature IDs
^^^^^^^^^^^

Feature ID’s are set via the ``CallContext#FEATURE_IDS`` context key.
To add a new feature ID, update the FEATURE_IDS context key within an interceptor or in the client builder

.. code-block:: java

    // (1) Get the existing feature ids
    Set<FeatureId> features = context.get(CallContext.FEATURE_IDS);

    // (2) Update with a new feature
    features.add(new FeatureId() {
        @Override
        public String getShortName() {
            return "short-name";
        }
    });

A pair of ``app/{id}`` is added if ``CallContext#APPLICATION_ID`` is set, or a value is set in
the ``aws.userAgentAppId`` system property, or the value set in the ``AWS_SDK_UA_APP_ID`` environment variable.
See the `App ID <https://docs.aws.amazon.com/sdkref/latest/guide/feature-appid.html>`_ guide for more information.
