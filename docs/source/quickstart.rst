===========
Quick start
===========

This document is a tutorial that introduces the Smithy interface definition
language (IDL). By reading this tutorial, you will learn:

* How to create a Smithy model
* How to define :ref:`shapes <shapes>`, including :ref:`service`,
  :ref:`resource`, and :ref:`operation` shapes
* How to apply :ref:`traits <traits>` to shapes

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


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
    * - blob
      - Uninterpreted binary data
    * - boolean
      - Boolean value type
    * - string
      - UTF-8 encoded string
    * - byte
      - 8-bit signed integer ranging from -128 to 127 (inclusive)
    * - short
      - 16-bit signed integer ranging from -32,768 to 32,767 (inclusive)
    * - integer
      - 32-bit signed integer ranging from -2^31 to (2^31)-1 (inclusive)
    * - long
      - 64-bit signed integer ranging from -2^63 to (2^63)-1 (inclusive)
    * - float
      - Single precision IEEE-754 floating point number
    * - double
      - Double precision IEEE-754 floating point number
    * - bigInteger
      - Arbitrarily large signed integer
    * - bigDecimal
      - Arbitrary precision signed decimal number
    * - timestamp
      - An instant in time with no UTC offset or timezone. The
        serialization of a timestamp is determined by a
        :ref:`protocol <protocolDefinition-trait>`.
    * - document type
      - An untyped JSON-like value.
    * - :ref:`list`
      - Homogeneous collection of values
    * - :ref:`set`
      - Unordered collection of unique homogeneous values
    * - :ref:`map`
      - Map data structure that maps string keys to homogeneous values
    * - :ref:`structure`
      - Fixed set of named heterogeneous members
    * - :ref:`union`
      - `Tagged union`_ data structure that can take on several different,
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

``Weather`` is a :ref:`service` shape that is defined inside of a
:ref:`namespace <namespaces>`.

.. code-block:: smithy

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

.. admonition:: Review
    :class: tip

    1. The ``resources`` property binds resources to service and resource
       shapes.
    2. Resources can define identifiers.
    3. Child resources must define the same identifiers as their parents,
       and they can also define additional identifiers.


Defining operations
===================

The ``create``, ``read``, ``update``, ``delete``, and ``list`` properties of a
resource are used to define the :ref:`lifecycle operations <lifecycle-operations>`
of a resource. Lifecycle operations are the canonical methods used to read and
transition the state of a resource using well-defined semantics. Defining
lifecycle operations helps automated tooling reason about your API.

Let's define the operation used to "read" a ``City``.

.. code-block:: smithy

    @readonly
    operation GetCity {
        input: GetCityInput
        output: GetCityOutput
        errors: [NoSuchResource]
    }

    structure GetCityInput {
        // "cityId" provides the identifier for the resource and
        // has to be marked as required.
        cityId: CityId!
    }

    structure GetCityOutput {
        // Required is used on output to indicate if the service
        // will always provide a value for the member.
        name: String!
        coordinates: CityCoordinates!
    }

    structure CityCoordinates {
        latitude: Float!
        longitude: Float!
    }

    // "error" is a trait that is used to specialize
    // a structure as an error.
    @error("client")
    structure NoSuchResource {
        resourceType: String!
    }

And define the operation used to "read" a ``Forecast``.

.. code-block:: smithy

    @readonly
    operation GetForecast {
        input: GetForecastInput
        output: GetForecastOutput
    }

    // "cityId" provides the only identifier for the resource since
    // a Forecast doesn't have its own.
    structure GetForecastInput {
        cityId: CityId!
    }

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

    /// Provides weather forecasts.
    @paginated(
        inputToken: "nextToken"
        outputToken: "nextToken"
        pageSize: "pageSize"
    )
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

    structure ListCitiesInput {
        nextToken: String
        pageSize: Integer
    }

    structure ListCitiesOutput {
        nextToken: String
        items: CitySummaries!
    }

    // CitySummaries is a list of CitySummary structures.
    list CitySummaries {
        member: CitySummary
    }

    // CitySummary contains a reference to a City.
    @references([{resource: City}])
    structure CitySummary {
        cityId: CityId!
        name: String!
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
        output: GetCurrentTimeOutput
    }

    structure GetCurrentTimeOutput {
        time: Timestamp!
    }


Building the Model
==================

Now that you have a model, you'll want to build it and generate things from it.
Building the model creates projections of the model, applies plugins to
generate artifacts, runs validation, and creates a JAR that contains the
filtered projection. The `Smithy Gradle Plugin`_ is the best way to get started
building a Smithy model. First, create a ``smithy-build.json`` file:

.. code-block:: json

    {
        "version": "1.0"
    }

Then create a new ``build.gradle.kts`` file:

.. code-block:: kotlin

    plugins {
        id("software.amazon.smithy").version("0.5.3")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation("software.amazon.smithy:smithy-model:__smithy_version__")
    }

    configure<software.amazon.smithy.gradle.SmithyExtension> {
        // Uncomment this to use a custom projection when building the JAR.
        // projection = "foo"
    }

    // Uncomment to disable creating a JAR.
    //tasks["jar"].enabled = false

Finally, copy the weather model to ``model/weather.smithy`` and
then run ``gradle build`` (make sure you have `gradle installed`_).

Next steps
==========

That's it! We just created a simple, read-only, ``Weather`` service.

1. Try adding a "create" lifecycle operation to ``City``.
2. Try adding a "delete" lifecycle operation to ``City``.
3. Try adding :ref:`HTTP binding traits <http-traits>` to the API.
4. Try adding :ref:`tags <tags-trait>` to shapes and filtering them out with
   :ref:`excludeShapesByTag <excludeShapesByTag-transform>`.

There's plenty more to explore in Smithy. The
:ref:`Smithy specification <specification>` can teach you everything you need
to know about Smithy models. :ref:`Building Smithy Models <building-models>`
can teach you more about the build process, including how to use
transformations, projections, plugins, and more. For more sample build
configurations, see the `examples directory`_ of the Smithy Gradle plugin
repository.

Complete example
================

If you followed all the steps in this guide, you should have three files, laid
out like so::

    .
    ├── build.gradle.kts
    ├── model
    │   └── weather.smithy
    └── smithy-build.json

The ``smithy-build.json`` should have the following contents:

.. code-block:: json

    {
        "version": "1.0"
    }

The ``build.gradle.kts`` should have the following contents:

.. code-block:: kotlin

    plugins {
        id("software.amazon.smithy").version("0.5.3")
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        implementation("software.amazon.smithy:smithy-model:__smithy_version__")
    }

    configure<software.amazon.smithy.gradle.SmithyExtension> {
        // Uncomment this to use a custom projection when building the JAR.
        // projection = "foo"
    }

    // Uncomment to disable creating a JAR.
    //tasks["jar"].enabled = false

Finally, the complete ``weather.smithy`` model should look like:

.. tabs::

    .. code-tab:: smithy

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
            read: GetForecast
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

        structure GetCityInput {
            // "cityId" provides the identifier for the resource and
            // has to be marked as required.
            cityId: CityId!
        }

        structure GetCityOutput {
            // Required is used on output to indicate if the service
            // will always provide a value for the member.
            name: String!
            coordinates: CityCoordinates!
        }

        // This structure is nested within GetCityOutput.
        structure CityCoordinates {
            latitude: Float!
            longitude: Float!
        }

        // "error" is a trait that is used to specialize
        // a structure as an error.
        @error("client")
        structure NoSuchResource {
            resourceType: String!
        }

        // The paginated trait indicates that the operation may
        // return truncated results.
        @readonly
        @paginated(items: "items")
        operation ListCities {
            input: ListCitiesInput
            output: ListCitiesOutput
        }

        structure ListCitiesInput {
            nextToken: String
            pageSize: Integer
        }

        structure ListCitiesOutput {
            nextToken: String
            items: CitySummaries!
        }

        // CitySummaries is a list of CitySummary structures.
        list CitySummaries {
            member: CitySummary
        }

        // CitySummary contains a reference to a City.
        @references([{resource: City}])
        structure CitySummary {
            cityId: CityId!
            name: String!
        }

        @readonly
        operation GetCurrentTime {
            output: GetCurrentTimeOutput
        }

        structure GetCurrentTimeOutput {
            time: Timestamp!
        }

        @readonly
        operation GetForecast {
            input: GetForecastInput
            output: GetForecastOutput
        }

        // "cityId" provides the only identifier for the resource since
        // a Forecast doesn't have its own.
        structure GetForecastInput {
            cityId: CityId!
        }

        structure GetForecastOutput {
            chanceOfRain: Float
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "example.weather#Weather": {
                    "type": "service",
                    "version": "2006-03-01",
                    "operations": [
                        {
                            "target": "example.weather#GetCurrentTime"
                        }
                    ],
                    "resources": [
                        {
                            "target": "example.weather#City"
                        }
                    ],
                    "traits": {
                        "smithy.api#documentation": "Provides weather forecasts.",
                        "smithy.api#paginated": {
                            "inputToken": "nextToken",
                            "outputToken": "nextToken",
                            "pageSize": "pageSize"
                        }
                    }
                },
                "example.weather#City": {
                    "type": "resource",
                    "identifiers": {
                        "cityId": {
                            "target": "example.weather#CityId"
                        }
                    },
                    "read": {
                        "target": "example.weather#GetCity"
                    },
                    "list": {
                        "target": "example.weather#ListCities"
                    },
                    "resources": [
                        {
                            "target": "example.weather#Forecast"
                        }
                    ]
                },
                "example.weather#CityCoordinates": {
                    "type": "structure",
                    "members": {
                        "latitude": {
                            "target": "smithy.api#Float",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        },
                        "longitude": {
                            "target": "smithy.api#Float",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "example.weather#CityId": {
                    "type": "string",
                    "traits": {
                        "smithy.api#pattern": "^[A-Za-z0-9 ]+$"
                    }
                },
                "example.weather#CitySummaries": {
                    "type": "list",
                    "member": {
                        "target": "example.weather#CitySummary"
                    }
                },
                "example.weather#CitySummary": {
                    "type": "structure",
                    "members": {
                        "cityId": {
                            "target": "example.weather#CityId",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        },
                        "name": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#references": [
                            {
                                "resource": "example.weather#City"
                            }
                        ]
                    }
                },
                "example.weather#Forecast": {
                    "type": "resource",
                    "identifiers": {
                        "cityId": {
                            "target": "example.weather#CityId"
                        }
                    },
                    "read": {
                        "target": "example.weather#GetForecast"
                    }
                },
                "example.weather#GetCity": {
                    "type": "operation",
                    "input": {
                        "target": "example.weather#GetCityInput"
                    },
                    "output": {
                        "target": "example.weather#GetCityOutput"
                    },
                    "errors": [
                        {
                            "target": "example.weather#NoSuchResource"
                        }
                    ],
                    "traits": {
                        "smithy.api#readonly": {}
                    }
                },
                "example.weather#GetCityInput": {
                    "type": "structure",
                    "members": {
                        "cityId": {
                            "target": "example.weather#CityId",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "example.weather#GetCityOutput": {
                    "type": "structure",
                    "members": {
                        "name": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        },
                        "coordinates": {
                            "target": "example.weather#CityCoordinates",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "example.weather#GetCurrentTime": {
                    "type": "operation",
                    "output": {
                        "target": "example.weather#GetCurrentTimeOutput"
                    },
                    "traits": {
                        "smithy.api#readonly": {}
                    }
                },
                "example.weather#GetCurrentTimeOutput": {
                    "type": "structure",
                    "members": {
                        "time": {
                            "target": "smithy.api#Timestamp",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "example.weather#GetForecast": {
                    "type": "operation",
                    "input": {
                        "target": "example.weather#GetForecastInput"
                    },
                    "output": {
                        "target": "example.weather#GetForecastOutput"
                    },
                    "traits": {
                        "smithy.api#readonly": {}
                    }
                },
                "example.weather#GetForecastInput": {
                    "type": "structure",
                    "members": {
                        "cityId": {
                            "target": "example.weather#CityId",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "example.weather#GetForecastOutput": {
                    "type": "structure",
                    "members": {
                        "chanceOfRain": {
                            "target": "smithy.api#Float"
                        }
                    }
                },
                "example.weather#ListCities": {
                    "type": "operation",
                    "input": {
                        "target": "example.weather#ListCitiesInput"
                    },
                    "output": {
                        "target": "example.weather#ListCitiesOutput"
                    },
                    "traits": {
                        "smithy.api#paginated": {
                            "items": "items"
                        },
                        "smithy.api#readonly": {}
                    }
                },
                "example.weather#ListCitiesInput": {
                    "type": "structure",
                    "members": {
                        "nextToken": {
                            "target": "smithy.api#String"
                        },
                        "pageSize": {
                            "target": "smithy.api#Integer"
                        }
                    }
                },
                "example.weather#ListCitiesOutput": {
                    "type": "structure",
                    "members": {
                        "nextToken": {
                            "target": "smithy.api#String"
                        },
                        "items": {
                            "target": "example.weather#CitySummaries",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "example.weather#NoSuchResource": {
                    "type": "structure",
                    "members": {
                        "resourceType": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#error": "client"
                    }
                }
            }
        }

.. _examples directory: https://github.com/awslabs/smithy-gradle-plugin/tree/main/examples
.. _Tagged union: https://en.wikipedia.org/wiki/Tagged_union
.. _Smithy Gradle Plugin: https://github.com/awslabs/smithy-gradle-plugin/
.. _gradle installed: https://gradle.org/install/
