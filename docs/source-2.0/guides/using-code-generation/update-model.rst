.. _update-model:

=========================
Updating the Smithy Model
=========================

The weather service model is currently a high-level description of the Weather API,
and doesn't contain any low-level details about how the data is sent over the wire.
However, in order to communicate with the Weather API, a client needs to know some
of these details. For example, how should a client serialize requests? Is the server
using HTTP or something else?

Smithy makes it simple to add this information to a model via :ref:`traits`. Let's
update the weather service model with the details needed for generating a client.

Specifying a protocol
=====================

In Smithy, `protocols` define how a client and server communicate, and are modeled by
:doc:`protocol traits </spec/protocol-traits>`. A service's supported protocols can be
defined by applying protocol traits to the service shape. To generate code, at least
one protocol trait is required, and the code generator has to know how to generate code
for that protocol.

In this example, the weather service will use the
:ref:`AWS restJson1 protocol <aws-restjson1-protocol>`, which is a RESTful protocol
that sends JSON in structured payloads over HTTP.

The protocol trait, :ref:`@aws.protocols#restJson1 <aws.protocols#restJson1-trait>`,
is provided by the `smithy-aws-traits` package, so first add a dependency to your Smithy
project:

.. tab:: Smithy CLI

    .. code-block:: json
        :caption: smithy-build.json

        {
            "...": "..."
            "maven": {
                "dependencies": [
                    "software.amazon.smithy:smithy-aws-traits:__smithy_version__"
                ]
            },
            "...": "..."
        }

.. tab:: Gradle

    .. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            dependencies {
                ...
                implementation("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
            }

    .. tab:: Groovy

         .. code-block:: groovy
            :caption: build.gradle

            dependencies {
                ...
                implementation 'software.amazon.smithy:smithy-aws-traits:__smithy_version__'
            }


Now, import the :ref:`@aws.protocols#restJson1 <aws.protocols#restJson1-trait>` trait
and apply it to the ``Weather`` service shape:

.. code-block:: smithy
    :caption: weather.smithy

    $version: "2"
    namespace example.weather

    use aws.protocols#restJson1

    /// Provides weather forecasts.
    @paginated(
        inputToken: "nextToken"
        outputToken: "nextToken"
        pageSize: "pageSize"
    )

    @restJson1
    service Weather {
        version: "2006-03-01"
        resources: [City]
        operations: [GetCurrentTime]
    }

Adding HTTP bindings
====================

In Smithy, HTTP can be configured by applying :ref:`HTTP binding traits <http-traits>`
to operation shapes. HTTP protocols can use these traits to generate code that formats
HTTP messages properly.

First, configure the HTTP method, request URI, and the status code of a successful
response with the :ref:`@http trait <http-trait>`.

.. code-block:: smithy

    @readonly
    @http(code: 200, method: "GET", uri: "/cities/{cityId}")
    operation GetCity {
        input: GetCityInput
        output: GetCityOutput
        errors: [NoSuchResource]
    }

    @paginated(items: "items")
    @readonly
    @http(code: 200, method: "GET", uri: "/cities")
    operation ListCities {
        input: ListCitiesInput
        output: ListCitiesOutput
    }

    @readonly
    @http(code: 200, method: "GET", uri: "/currentTime")
    operation GetCurrentTime {
        input: GetCurrentTimeInput
        output: GetCurrentTimeOutput
    }

    @readonly
    @http(code: 200, method: "GET", uri: "/forecast/{cityId}")
    operation GetForecast {
        input: GetForecastInput
        output: GetForecastOutput
    }

The URI patterns for the ``GetCity`` and ``GetForecast`` operations each use an HTTP label to
bind the ``cityId`` member of the operation input structure to the request URI. Let's specify
the members that should be bound to the URIs using the :ref:`@httpLabel trait <httplabel-trait>`:

.. code-block:: smithy

    @input
    structure GetCityInput {
        // "cityId" provides the identifier for the resource and
        // has to be marked as required.
        @required
        @httpLabel
        cityId: CityId
    }

    @input
    structure GetForecastInput {
        @required
        @httpLabel
        cityId: CityId
    }

For the ``ListCities`` operation, include the ``nextToken`` and ``pageSize`` input members
in the request URI as query parameters using the :ref:`@httpQuery trait <httpquery-trait>`:

.. code-block:: smithy

    @input
    structure ListCitiesInput {
        @httpQuery("nextToken")
        nextToken: String
        @httpQuery("pageSize")
        pageSize: Integer
    }
