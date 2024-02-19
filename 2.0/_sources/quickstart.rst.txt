===========
Quick start
===========

This document is a tutorial that introduces the Smithy interface definition
language (IDL). By reading this tutorial, you will learn:

* How to create a Smithy model
* How to define :ref:`shapes <shapes>`, including :ref:`service`,
  :ref:`resource`, and :ref:`operation` shapes
* How to apply :ref:`traits <traits>` to shapes


What is Smithy?
===============

*Smithy* is an interface definition language and set of tools that allows
developers to build clients and servers in multiple languages. Smithy
models define a service as a collection of resources, operations, and shapes.
A Smithy model enables API providers to generate clients and servers in
various programming languages, API documentation, test automation, and
example code.


Shapes and traits
=================

Smithy models consist of shapes and traits. :ref:`Shapes <shapes>` are
instances of *types*. :ref:`Traits <traits>` are used to add more information
to shapes that might be useful for clients, servers, or documentation.

Smithy supports the following types:

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - :ref:`blob`
      - Uninterpreted binary data
    * - :ref:`boolean`
      - Boolean value type
    * - :ref:`string`
      - UTF-8 encoded string
    * - :ref:`byte`
      - 8-bit signed integer ranging from -128 to 127 (inclusive)
    * - :ref:`short`
      - 16-bit signed integer ranging from -32,768 to 32,767 (inclusive)
    * - :ref:`integer`
      - 32-bit signed integer ranging from -2^31 to (2^31)-1 (inclusive)
    * - :ref:`long`
      - 64-bit signed integer ranging from -2^63 to (2^63)-1 (inclusive)
    * - :ref:`float`
      - Single precision IEEE-754 floating point number
    * - :ref:`double`
      - Double precision IEEE-754 floating point number
    * - :ref:`bigInteger`
      - Arbitrarily large signed integer
    * - :ref:`bigDecimal`
      - Arbitrary precision signed decimal number
    * - :ref:`timestamp`
      - An instant in time with no UTC offset or timezone.
    * - :ref:`document`
      - An untyped JSON-like value.
    * - :ref:`list`
      - Homogeneous collection of values
    * - :ref:`map`
      - Map data structure that maps string keys to homogeneous values
    * - :ref:`structure`
      - Fixed set of named heterogeneous members
    * - :ref:`union`
      - Tagged union data structure that can take on several different,
        but fixed, types
    * - :ref:`service`
      - Entry point of an API that aggregates resources and operations together
    * - :ref:`operation`
      - Represents the input, output and possible errors of an API operation
    * - :ref:`resource`
      - An entity with an identity, set of operations, and child resources


Weather Service
===============

In order to demonstrate how Smithy models are defined, we will create a
weather service.

1. This service provides weather information for cities.
2. This service consists of ``City`` resources and ``Forecast`` resources.
3. The ``Weather`` service has many ``City`` resources, and a ``City``
   resource contains a single ``Forecast`` singleton resource.
4. This service closure contains the following operations:
   ``ListCities``, ``GetCity``, ``GetForecast``, ``GetCurrentTime``.

First, create a directory called `smithy-quickstart` with a `model` directory
and a weather model file such that your `smithy-quickstart` directory has the
following file structure:

.. code-block:: text

    smithy-quickstart/
    └── model/
        └── weather.smithy

.. tip::

    Run the following command to create the quickstart directory

    .. code-block:: text

        mkdir -p smithy-quickstart/model \
        && touch smithy-quickstart/model/weather.smithy \
        && cd smithy-quickstart

Next, we will start to model a ``Weather`` service in the `weather.smithy` file.
``Weather`` is a :ref:`service` shape that is defined inside of a :ref:`namespace <namespaces>`.

.. code-block:: smithy
    :caption: model/weather.smithy

    $version: "2"
    namespace example.weather

    /// Provides weather forecasts.
    /// Triple slash comments attach documentation to shapes.
    service Weather {
        version: "2006-03-01"
    }

.. admonition:: What's that syntax?
    :class: note

    Smithy models are defined using either the :ref:`Smithy IDL <idl>`
    or the :ref:`JSON AST <json-ast>`. The JSON AST representation of a model
    is typically an artifact created by build tools to make them easier to
    use by other tooling.

    * ``//`` is used for comments
    * ``///`` is used to add :ref:`documentation <documentation-comment>`
      to the following shape.
    * Keywords like ``service`` and ``structure`` start the definition of a
      shape.


Defining resources
==================

A resource is contained within a service or another resource. Resources have
identifiers, operations, and any number of child resources.

.. code-block:: smithy
    :caption: model/weather.smithy

    $version: "2"
    namespace example.weather

    /// Provides weather forecasts.
    service Weather {
        version: "2006-03-01"
        resources: [City]
    }

    resource City {
        identifiers: { cityId: CityId }
        read: GetCity
        list: ListCities
    }

    // "pattern" is a trait.
    @pattern("^[A-Za-z0-9 ]+$")
    string CityId

Because the ``Weather`` service contains many cities, the ``City`` resource
defines an :ref:`identifier <resource-identifiers>`. *Identifiers* are used
to refer to a specific resource within a service. The "identifiers" property
is a mapping of identifier names to the shape to use for that identifier. If
the input structure of an operation uses the same names and targeted shapes
as the ``identifiers`` property of the resource, the structure is
:ref:`automatically configured <implicit-identifier-bindings>` to work with
the resource so that input members of the operation are used to provide the
identity of the resource.

Each ``City`` has a single ``Forecast``. This can be defined by adding the
``Forecast`` to the ``resources`` property of the ``City``.

.. code-block:: smithy
    :caption: model/weather.smithy

    resource City {
        identifiers: { cityId: CityId }
        read: GetCity
        list: ListCities
        resources: [Forecast]
    }

    resource Forecast {
        identifiers: { cityId: CityId }
        read: GetForecast
    }

Child resources must define the exact same identifiers property of their
parent, but they are allowed to add any number of additional identifiers if
needed. Because there is only one forecast per city, no additional identifiers
were added to the identifiers property that isn't present on the ``City``
resource.

The state of a resource is represented through its
:ref:`properties <resource-properties>`. ``City`` contains coordinates, and
``Forecast`` has a chance of rain represented as a float. Input and output
members of resource operations map to resource properties or identifiers to
perform updates on or examine the state of a resource.

.. code-block:: smithy
    :caption: model/weather.smithy

    resource City {
        identifiers: { cityId: CityId }
        properties: { coordinates: CityCoordinates }
        read: GetCity
        list: ListCities
        resources: [Forecast]
    }

    structure GetCityOutput for City {
        $coordinates
    }

    resource Forecast {
        identifiers: { cityId: CityId }
        properties: { chanceOfRain: Float }
        read: GetForecast
    }

    structure GetForecastOutput for Forecast {
        $chanceOfRain
    }

.. admonition:: Review
    :class: tip

    1. The ``resources`` property binds resources to service and resource
       shapes.
    2. Resources can define identifiers.
    3. Child resources must define the same identifiers as their parents,
       and they can also define additional identifiers.
    4. Resources can define properties.
    5. Resource properties are set, modified, or read through lifecycle
       operations.

.. seealso::

    The :ref:`target elision syntax <idl-target-elision>` for an easy way to
    define structures that reference resource identifiers and properties
    without having to repeat the target definition.


Defining operations
===================

The ``put``, ``create``, ``read``, ``update``, ``delete``, and ``list``
properties of a resource are used to define the :ref:`lifecycle operations
<lifecycle-operations>` of a resource. Lifecycle operations are the canonical
methods used to read and transition the state of a resource using well-defined
semantics. Defining lifecycle operations helps automated tooling reason about
your API.

Let's define the operation used to "read" a ``City``.

.. code-block:: smithy
    :caption: model/weather.smithy

    @readonly
    operation GetCity {
        input: GetCityInput
        output: GetCityOutput
        errors: [NoSuchResource]
    }

    @input
    structure GetCityInput for City {
        // "cityId" provides the identifier for the resource and
        // has to be marked as required.
        @required
        $cityId
    }

    @output
    structure GetCityOutput {
        // "required" is used on output to indicate if the service
        // will always provide a value for the member.
        @required
        @notProperty
        name: String

        @required
        $coordinates
    }

    structure CityCoordinates {
        @required
        latitude: Float

        @required
        longitude: Float
    }

    // "error" is a trait that is used to specialize
    // a structure as an error.
    @error("client")
    structure NoSuchResource {
        @required
        resourceType: String
    }

And define the operation used to "read" a ``Forecast``.

.. code-block:: smithy
    :caption: model/weather.smithy

    @readonly
    operation GetForecast {
        input: GetForecastInput
        output: GetForecastOutput
    }

    // "cityId" provides the only identifier for the resource since
    // a Forecast doesn't have its own.
    @input
    structure GetForecastInput {
        @required
        cityId: CityId
    }

    @output
    structure GetForecastOutput {
        chanceOfRain: Float
    }

.. admonition:: Review
    :class: tip

    1. Operations accept and return structured messages.
    2. Operations are bound to service shapes and resource shapes.
    3. Operations marked as :ref:`readonly-trait` indicate the operation
       has no side effects.
    4. Operations should define the :ref:`errors <error-trait>` it can return.


Listing resources
=================

There are many ``City`` resources contained within the ``Weather`` service.
The :ref:`list lifecycle operation <list-lifecycle>` can be added to the
``City`` resource to list all of the cities in the service. The list operation
is a :ref:`collection operation <collection-operations>`, and as such, MUST NOT
bind the identifier of a ``City`` to its input structure; we are listing
cities, so there's no way we could provide a ``City`` identifier.

.. code-block:: smithy
    :caption: model/weather.smithy

    /// Provides weather forecasts.
    @paginated(inputToken: "nextToken", outputToken: "nextToken",
               pageSize: "pageSize")
    service Weather {
        version: "2006-03-01"
        resources: [City]
    }

    // The paginated trait indicates that the operation may
    // return truncated results. Applying this trait to the service
    // sets default pagination configuration settings on each operation.
    @paginated(items: "items")
    @readonly
    operation ListCities {
        input: ListCitiesInput
        output: ListCitiesOutput
    }

    @input
    structure ListCitiesInput {
        nextToken: String
        pageSize: Integer
    }

    @output
    structure ListCitiesOutput {
        nextToken: String

        @required
        items: CitySummaries
    }

    // CitySummaries is a list of CitySummary structures.
    list CitySummaries {
        member: CitySummary
    }

    // CitySummary contains a reference to a City.
    @references([{resource: City}])
    structure CitySummary {
        @required
        cityId: CityId

        @required
        name: String
    }

The ``ListCities`` operation is :ref:`paginated <paginated-trait>`, meaning
the results of invoking the operation can be truncated, requiring subsequent
calls to retrieve the entire list of results. It's usually a good idea to add
pagination to an API that lists resources because it can help prevent
operational issues in the future if the list grows to an unpredicted size.

The ``CitySummary`` structure defines a :ref:`reference <references-trait>`
to a ``City`` resource. This gives tooling a better understanding of the
relationships in your service.

The above example refers to :ref:`prelude shapes <prelude>` like
``String`` that are automatically available in all Smithy models.

.. admonition:: Review
    :class: tip

    1. The ``list`` lifecycle operation is used to list resources.
    2. ``list`` operations should be :ref:`paginated-trait`.
    3. The :ref:`references-trait` links a structure to a resource.
    4. Prelude shapes can help DRY up models.


Non-Lifecycle Operations
========================

Smithy supports operations that don't fit into the typical create, read,
update, delete, and list lifecycles. Operations can be added to any resource or
service shape with no special lifecycle designation using the ``operations``
property. The following operation gets the current time from the ``Weather``
service.


.. code-block:: smithy
    :caption: model/weather.smithy

    /// Provides weather forecasts.
    @paginated(inputToken: "nextToken", outputToken: "nextToken",
               pageSize: "pageSize")
    service Weather {
        version: "2006-03-01"
        resources: [City]
        operations: [GetCurrentTime]
    }

    @readonly
    operation GetCurrentTime {
        input: GetCurrentTimeInput
        output: GetCurrentTimeOutput
    }

    @input
    structure GetCurrentTimeInput {}

    @output
    structure GetCurrentTimeOutput {
        @required
        time: Timestamp
    }


Building the Model
==================

Now that you have a model, you'll want to build it and generate additional
artifacts from it. Building the model creates projections of the model, applies plugins to
generate artifacts, and runs validation.

.. tab:: Smithy CLI

    .. admonition:: Install required tools
        :class: tip

        Before you proceed, make sure you have the :ref:`Smithy CLI installed <cli_installation>`.

    To build a Smithy model using the :ref:`the Smithy CLI <smithy-cli>`,
    create a :ref:`smithy-build.json <smithy-build-json>` file in the ``smithy-quickstart`` directory:

    .. code-block:: json
        :caption: smithy-build.json

        {
            // Version of the smithy-build.json file specification
            "version": "1.0",
            // Location to search for Smithy model source files
            "sources": ["model"]
        }

    Next, run ``smithy build``. That's it! We just created a simple, read-only, ``Weather`` service.

.. tab:: Gradle

    .. admonition:: Install required tools
        :class: tip

        Before you proceed, make sure you have `gradle installed`_.


    To build a Smithy model using the :ref:`Smithy Gradle Plugin <smithy-gradle-plugin>`,
    first, create a gradle build script file in the ``smithy-quickstart`` directory:

    .. tab:: Kotlin

        .. code-block:: kotlin
            :caption: build.gradle.kts

            plugins {
                `java-library`
                id("software.amazon.smithy.gradle.smithy-jar").version("0.10.0")
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

    .. tab:: Groovy

        .. code-block:: groovy
            :caption: build.gradle

            plugins {
                id 'java-library'
                id 'software.amazon.smithy.gradle.smithy-jar' version '0.10.0'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

    Next, create a :ref:`smithy-build.json <smithy-build-json>` file in the
    ``smithy-quickstart`` directory:

    .. code-block:: json
        :caption: smithy-build.json

        {
            // Version of the smithy-build.json file specification
            "version": "1.0"
        }

    Finally, run ``gradle build``. That's it! We just created a simple, read-only, ``Weather`` service.


Next steps
==========

1. Try adding a "create" lifecycle operation to ``City``.
2. Try adding a "delete" lifecycle operation to ``City``.
3. Try adding :ref:`HTTP binding traits <http-traits>` to the API.
4. Try adding :ref:`tags <tags-trait>` to shapes and filtering them out with
   :ref:`excludeShapesByTag <excludeShapesByTag-transform>`.
5. Follow the :ref:`Using Code Generation Guide <using-code-generation>`
   to generate code for the ``Weather`` service.

There's plenty more to explore in Smithy.

* The :ref:`Smithy specification <smithy-specification>` can teach you
  everything you need to know about Smithy models.
* :ref:`The Smithy CLI <smithy-cli>` is the easiest way to do interesting
  things with Smithy models like code generation, creating different
  versions of a model for different audiences, and more.
* The `Smithy Examples <https://github.com/smithy-lang/smithy-examples>`_
  repo on GitHub provides various example models and package layouts that
  show how to use tools like the Smithy CLI or
  :ref:`Gradle plugin <smithy-gradle-plugin>`.


Complete example
================



.. note::

    You can clone a working version of this quickstart example using the
    :ref:`Smithy CLI <smithy-cli>` ``init`` command.

    .. tab:: Quickstart with Smithy CLI

        .. code-block::

            smithy init -o <output_directory>

    .. tab:: Quickstart with Gradle

        .. code-block::

            smithy init -t quickstart-gradle -o <output_directory>

If you followed all the steps in this guide, your working directory should be laid out like so:

.. tab:: Smithy CLI

    .. code-block:: text

        .
        ├── smithy-build.json
        └── model
            └── weather.smithy

.. tab:: Gradle

    .. code-block:: text

        .
        ├── build.gradle.kts
        ├── smithy-build.json
        └── model
            └── weather.smithy

The complete ``weather.smithy`` model should look like:

.. code-block:: smithy

    $version: "2"
    namespace example.weather

    /// Provides weather forecasts.
    @paginated(
        inputToken: "nextToken"
        outputToken: "nextToken"
        pageSize: "pageSize"
    )
    service Weather {
        version: "2006-03-01"
        resources: [City]
        operations: [GetCurrentTime]
    }

    resource City {
        identifiers: { cityId: CityId }
        read: GetCity
        list: ListCities
        resources: [Forecast]
    }

    resource Forecast {
        identifiers: { cityId: CityId }
        read: GetForecast,
    }

    // "pattern" is a trait.
    @pattern("^[A-Za-z0-9 ]+$")
    string CityId

    @readonly
    operation GetCity {
        input: GetCityInput
        output: GetCityOutput
        errors: [NoSuchResource]
    }

    @input
    structure GetCityInput {
        // "cityId" provides the identifier for the resource and
        // has to be marked as required.
        @required
        cityId: CityId
    }

    @output
    structure GetCityOutput {
        // "required" is used on output to indicate if the service
        // will always provide a value for the member.
        @required
        name: String

        @required
        coordinates: CityCoordinates
    }

    // This structure is nested within GetCityOutput.
    structure CityCoordinates {
        @required
        latitude: Float

        @required
        longitude: Float
    }

    // "error" is a trait that is used to specialize
    // a structure as an error.
    @error("client")
    structure NoSuchResource {
        @required
        resourceType: String
    }

    // The paginated trait indicates that the operation may
    // return truncated results.
    @readonly
    @paginated(items: "items")
    operation ListCities {
        input: ListCitiesInput
        output: ListCitiesOutput
    }

    @input
    structure ListCitiesInput {
        nextToken: String
        pageSize: Integer
    }

    @output
    structure ListCitiesOutput {
        nextToken: String

        @required
        items: CitySummaries
    }

    // CitySummaries is a list of CitySummary structures.
    list CitySummaries {
        member: CitySummary
    }

    // CitySummary contains a reference to a City.
    @references([{resource: City}])
    structure CitySummary {
        @required
        cityId: CityId

        @required
        name: String
    }

    @readonly
    operation GetCurrentTime {
        input: GetCurrentTimeInput
        output: GetCurrentTimeOutput
    }

    @input
    structure GetCurrentTimeInput {}

    @output
    structure GetCurrentTimeOutput {
        @required
        time: Timestamp
    }

    @readonly
    operation GetForecast {
        input: GetForecastInput
        output: GetForecastOutput
    }

    // "cityId" provides the only identifier for the resource since
    // a Forecast doesn't have its own.
    @input
    structure GetForecastInput {
        @required
        cityId: CityId
    }

    @output
    structure GetForecastOutput {
        chanceOfRain: Float
    }

.. _examples directory: https://github.com/awslabs/smithy-gradle-plugin/tree/main/examples
.. _Smithy Gradle Plugin: https://github.com/awslabs/smithy-gradle-plugin/
.. _gradle installed: https://gradle.org/install/
