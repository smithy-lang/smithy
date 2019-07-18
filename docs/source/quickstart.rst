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
        :ref:`protocol <protocols-trait>`.
    * - :ref:`document <document-type>`
      - An untyped JSON-like value.
    * - :ref:`list`
      - Homogenous collection of values.
    * - :ref:`set`
      - Unordered collection of unique homogenous values.
    * - :ref:`map`
      - Map data structure that maps string keys to homogenous values
    * - :ref:`structure`
      - Fixed set of named heterogenous members
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

.. tabs::

    .. code-tab:: smithy

        namespace example.weather

        /// Provides weather forecasts.
        /// Triple slash comments attach documentation to shapes.
        service Weather {
          version: "2006-03-01"
        }

    .. code-tab:: json

        {
            "smithy": "0.3.0",
            "example.weather": {
                "shapes": {
                    "Weather": {
                        "type": "service",
                        "version": "2006-03-01"
                    }
                }
            }
        }

.. admonition:: What's that syntax?
    :class: note

    Smithy models are defined using either the Smithy IDL or JSON. The JSON
    representation of a Smithy model is typically an artifact created by build
    tools to make them easier to use by other tooling.

Smithy is a *protocol agnostic* IDL, meaning the model defines the interface of
the API but not its serialization. A :ref:`protocol <protocols-trait>` defines
how clients and servers communicate and how messages are passed back and forth.
:ref:`Traits <traits>` can be used to influence how a protocol serializes
shapes.


Defining resources
==================

A resource is contained within a service or another resource. Resources have
identifiers, operations, and any number of child resources.

.. tabs::

    .. code-tab:: smithy

        namespace example.weather

        /// Provides weather forecasts.
        service Weather {
          version: "2006-03-01",
          resources: [City]
        }

        resource City {
          identifiers: { cityId: CityId },
          read: GetCity,
          list: ListCities,
        }

        // "pattern" is a trait.
        @pattern("^[A-Za-z0-9 ]+$")
        string CityId

    .. code-tab:: json

        {
            "smithy": "0.3.0",

            "example.weather": {
                "shapes": {
                    "Weather": {
                        "type": "service",
                        "version": "2006-03-01",
                        "resources": ["City"]
                    },
                    "City": {
                        "type": "resource",
                        "identifiers": {
                            "cityId": "CityId"
                        },
                        "read": "GetCity",
                        "list": "ListCities"
                    },
                    "CityId": {
                        "type": "string",
                        "pattern": "^[A-Za-z0-9 ]+$"
                    }
                }
            }
        }

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

.. tabs::

    .. code-tab:: smithy

        resource City {
          identifiers: { cityId: CityId },
          read: GetCity,
          list: ListCities,
          resources: [Forecast],
        }

        resource Forecast {
          identifiers: { cityId: CityId },
          read: GetForecast,
        }

    .. code-tab:: json

        {
            "smithy": "0.3.0",
            "example.weather": {
                "shapes": {
                    "City": {
                        "type": "resource",
                        "identifiers": { "cityId": "CityId" },
                        "read": "GetCity",
                        "list": "ListCities",
                        "resources": ["Forecast"],
                    },
                    "Forecast": {
                        "type": "resource",
                        "type": "resource",
                        "identifiers": { "cityId": "CityId" },
                        "read": "GetForecast"
                    }
                }
            }
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

.. tabs::

    .. code-tab:: smithy

        @readonly
        operation GetCity(GetCityInput) -> GetCityOutput errors [NoSuchResource]

        structure GetCityInput {
          // "cityId" provides the identifier for the resource and
          // has to be marked as required.
          @required
          cityId: CityId
        }

        structure GetCityOutput {
          // "required" is used on output to indicate if the service
          // will always provide a value for the member.
          @required
          name: String,

          @required
          coordinates: CityCoordinates,
        }

        structure CityCoordinates {
          @required
          latitude: Float,

          @required
          longitude: Float,
        }

        // "error" is a trait that is used to specialize
        // a structure as an error.
        @error("client")
        structure NoSuchResource {
          @required
          resourceType: String
        }

    .. code-tab:: json

        {
            "smithy": "0.3.0",
            "example.weather": {
                "shapes": {
                    "GetCity": {
                        "type": "operation",
                        "input": "GetCityInput",
                        "output": "GetCityOutput",
                        "errors": ["NoSuchResource"]
                    },
                    "GetCityInput": {
                        "type": "structure",
                        "members": {
                            "cityId": {
                                "target": "CityId",
                                "required": true
                            }
                        }
                    },
                    "GetCityOutput": {
                        "type": "structure",
                        "members": {
                            "name": {
                                "target": "String",
                                "required": true
                            },
                            "coordinates": {
                                "target": "CityCoordinates",
                                "required": true
                            }
                        }
                    },
                    "CityCoordinates": {
                        "type": "structure",
                        "members": {
                            "latitude": {
                                "target": "Float",
                                "required": true
                            },
                            "longitude": {
                                "target": "Float",
                                "required": true
                            }
                        }
                    },
                    "NoSuchResource": {
                        "type": "structure",
                        "error": "client",
                        "members": {
                            "resourceType": {
                                "target": "String",
                                "required": true
                            }
                        }
                    }
                }
            }
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
cities, so there's no way we could provide a City identifier.

.. tabs::

    .. code-tab:: smithy

        /// Provides weather forecasts.
        @paginated(inputToken: "nextToken", outputToken: "nextToken",
                   pageSize: "pageSize")
        service Weather {
          version: "2006-03-01",
          resources: [City]
        }

        // The paginated trait indicates that the operation may
        // return truncated results. Applying this trait to the service
        // sets default pagination configuration settings on each operation.
        @paginated(items: "items")
        @readonly @collection
        operation ListCities(ListCitiesInput) -> ListCitiesOutput

        structure ListCitiesInput {
          nextToken: String,
          pageSize: Integer
        }

        structure ListCitiesOutput {
          nextToken: String,

          @required
          items: CitySummaries,
        }

        // CitySummaries is a list of CitySummary structures.
        list CitySummaries {
          member: CitySummary
        }

        // CitySummary contains a reference to a City.
        @references(city: { resource: City, service: Weather })
        structure CitySummary {
          @required
          cityId: CityId,

          @required
          name: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.3.0",
            "example.weather": {
                "shapes": {
                    "Weather": {
                        "type": "service",
                        "version": "2006-03-01",
                        "resources": ["City"],
                        "paginated": {"inputToken": "nextToken", "outputToken": "nextToken", "pageSize": "pageSize"}
                    },
                    "ListCities": {
                        "type": "operation",
                        "input": "ListCitiesInput",
                        "output": "ListCitiesOutput",
                        "readonly": true,
                        "paginated": {"items": "items"}
                    },
                    "ListCitiesInput": {
                        "type": "structure",
                        "members": {
                            "nextToken": {
                                "target": "String"
                            },
                            "pageSize": {
                                "target": "Integer"
                            }
                        }
                    },
                    "ListCitiesOutput": {
                        "type": "structure",
                        "members": {
                            "nextToken": {
                                "target": "String"
                            },
                            "items": {
                                "target": "CitySummaries",
                                "required": true
                            }
                        }
                    },
                    "CitySummaries": {
                        "type": "list",
                        "member": {
                            "target": "CitySummary"
                        }
                    },
                    "CitySummary": {
                        "type": "structure",
                        "members": {
                            "cityId": {
                                "target": "CityId",
                                "required": true
                            },
                            "name": {
                                "target": "String",
                                "required": true
                            }
                        }
                    }
                }
            }
        }

The ``ListCities`` operation is :ref:`paginated <paginated-trait>`, meaning
the results of invoking the operation can be truncated, requiring subsequent
calls to retrieve the entire list of results. It's usually a good idea to add
pagination to an API that lists resources because it can help prevent
operational issues in the future if the list grows to an unpredicted size.

The ``CitySummary`` structure defines a :ref:`reference <referencing-resources>`
to a ``City`` resource. This allows client tooling that supports resource
modeling to automatically dereference each City resource returned in the
output and invoke operations on them.

The above example refers to :ref:`prelude shapes <prelude>` like
``String`` that are automatically available in all Smithy models.

.. admonition:: Review
    :class: tip

    1. The ``list`` lifecycle operation is used to list resources.
    2. ``list`` operations should be :ref:`paginated-trait`.
    3. The :ref:`references-trait` links a structure to a resource.


Non-Lifecycle Operations
========================

Smithy supports operations that don't fit into the typical create, read,
update, delete, and list lifecycles. Operations can be added to any resource or
service shape with no special lifecycle designation using the ``operations``
property. The following operation gets the current time from the ``Weather``
service.


.. tabs::

    .. code-tab:: smithy

        /// Provides weather forecasts.
        @paginated(inputToken: "nextToken", outputToken: "nextToken",
                   pageSize: "pageSize")
        service Weather {
          version: "2006-03-01",
          resources: [City],
          operations: [GetCurrentTime]
        }

        @readonly
        operation GetCurrentTime() -> GetCurrentTimeOutput

        structure GetCurrentTimeOutput {
          @required
          time: Timestamp
        }

    .. code-tab:: json

        {
            "smithy": "0.3.0",
            "example.weather": {
                "shapes": {
                    "Weather": {
                        "type": "service",
                        "version": "2006-03-01",
                        "resources": ["City"],
                        "operations": ["GetCurrentTime"]
                    },
                    "GetCurrentTime": {
                        "type": "operation",
                        "output": "GetCurrentTimeOutput",
                        "readonly": true
                    },
                    "GetCurrentTimeOutput": {
                        "type": "structure",
                        "members": {
                            "time": {
                                "target": "Timestamp",
                                "required": true
                            }
                        }
                    }
                }
            }
        }


Next steps
==========

That's it! We just created a simple, read-only, ``Weather`` service.

1. Try adding a "create" lifecycle operation to ``City``.
2. Try adding a "delete" lifecycle operation to ``City``.
3. Try adding :ref:`HTTP binding traits <http-traits>` to the API.

There's plenty more to explore in Smithy. The
:ref:`Smithy specification <specification>` can teach you everything you need
to know about Smithy.


Complete example
================

.. tabs::

    .. code-tab:: smithy

        namespace example.weather

        /// Provides weather forecasts.
        @paginated(inputToken: "nextToken", outputToken: "nextToken",
                   pageSize: "pageSize")
        service Weather {
          version: "2006-03-01",
          resources: [City],
          operations: [GetCurrentTime]
        }

        resource City {
          identifiers: { cityId: CityId },
          read: GetCity,
          list: ListCities,
          resources: [Forecast],
        }

        resource Forecast {
          identifiers: { cityId: CityId },
          read: GetForecast,
        }

        // "pattern" is a trait.
        @pattern("^[A-Za-z0-9 ]+$")
        string CityId

        @readonly
        operation GetCity(GetCityInput) -> GetCityOutput errors [NoSuchResource]

        structure GetCityInput {
          // "cityId" provides the identifier for the resource and
          // has to be marked as required.
          @required
          cityId: CityId
        }

        structure GetCityOutput {
          // "required" is used on output to indicate if the service
          // will always provide a value for the member.
          @required
          name: String,

          @required
          coordinates: CityCoordinates,
        }

        // This structure is nested within GetCityOutput.
        structure CityCoordinates {
          @required
          latitude: Float,

          @required
          longitude: Float,
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
        @readonly @collection
        @paginated(items: "items")
        operation ListCities(ListCitiesInput) -> ListCitiesOutput

        structure ListCitiesInput {
          nextToken: String,
          pageSize: Integer
        }

        structure ListCitiesOutput {
          nextToken: String,

          @required
          items: CitySummaries,
        }

        // CitySummaries is a list of CitySummary structures.
        list CitySummaries {
          member: CitySummary
        }

        // CitySummary contains a reference to a City.
        @references(city: { resource: City, service: Weather })
        structure CitySummary {
          @required
          cityId: CityId,

          @required
          name: String,
        }

        @readonly
        operation GetCurrentTime() -> GetCurrentTimeOutput

        structure GetCurrentTimeOutput {
          @required
          time: Timestamp
        }

        @readonly
        operation GetForecast(GetForecastInput) -> GetForecastOutput

        // "cityId" provides the only identifier for the resource since
        // a Forecast doesn't have its own.
        structure GetForecastInput {
          @required
          cityId: CityId,
        }

        structure GetForecastOutput {
            chanceOfRain: Float
        }

    .. code-tab:: json

        {
            "smithy": "0.3.0",
            "example.weather": {
                "shapes": {
                    "Weather": {
                        "type":"service",
                        "version":"2006-03-01",
                        "operations":[
                            "GetCurrentTime"
                        ],
                        "resources":[
                            "City"
                        ],
                        "paginated": {"inputToken": "nextToken", "outputToken": "nextToken", "pageSize": "pageSize"}
                    },
                    "City": {
                        "type":"resource",
                        "identifiers": {
                            "cityId":"CityId"
                        },
                        "read":"GetCity",
                        "list":"ListCities",
                        "resources":[
                            "Forecast"
                        ]
                    },
                    "CityCoordinates": {
                        "type":"structure",
                        "members": {
                            "latitude": {
                                "target":"Float",
                                "required":true
                            },
                            "longitude": {
                                "target":"Float",
                                "required":true
                            }
                        }
                    },
                    "CityId": {
                        "type":"string",
                        "pattern":"^[A-Za-z0-9 ]+$"
                    },
                    "CitySummaries": {
                        "type":"list",
                        "member": {
                            "target":"CitySummary"
                        }
                    },
                    "CitySummary": {
                        "type":"structure",
                        "members": {
                            "cityId": {
                                "target":"CityId",
                                "required":true
                            },
                            "name": {
                                "target":"String",
                                "required":true
                            }
                        },
                        "references": {
                            "city": {
                                "resource":"City",
                                "service":"Weather"
                            }
                        }
                    },
                    "Forecast": {
                        "type":"resource",
                        "identifiers": {
                            "cityId":"CityId"
                        },
                        "read":"GetForecast"
                    },
                    "GetCity": {
                        "type":"operation",
                        "input":"GetCityInput",
                        "output":"GetCityOutput",
                        "errors":[
                            "NoSuchResource"
                        ],
                        "readonly":true
                    },
                    "GetCityInput": {
                        "type":"structure",
                        "members": {
                            "cityId": {
                                "target":"CityId",
                                "required":true
                            }
                        }
                    },
                    "GetCityOutput": {
                        "type":"structure",
                        "members": {
                            "coordinates": {
                                "target":"CityCoordinates",
                                "required":true
                            },
                            "name": {
                                "target":"String",
                                "required":true
                            }
                        }
                    },
                    "GetCurrentTime": {
                        "type":"operation",
                        "output":"GetCurrentTimeOutput",
                        "readonly":true
                    },
                    "GetCurrentTimeOutput": {
                        "type":"structure",
                        "members": {
                            "time": {
                                "target":"Timestamp",
                                "required":true
                            }
                        }
                    },
                    "GetForecast": {
                        "type":"operation",
                        "input":"GetForecastInput",
                        "output":"GetForecastOutput",
                        "readonly":true
                    },
                    "GetForecastInput": {
                        "type":"structure",
                        "members": {
                            "cityId": {
                                "target":"CityId",
                                "required":true
                            }
                        }
                    },
                    "GetForecastOutput": {
                        "type":"structure",
                        "members": {
                            "chanceOfRain": {
                                "target":"Float"
                            }
                        }
                    },
                    "ListCities": {
                        "type":"operation",
                        "input":"ListCitiesInput",
                        "output":"ListCitiesOutput",
                        "paginated": {"items":"items"},
                        "readonly":true,
                        "collection":true
                    },
                    "ListCitiesInput": {
                        "type":"structure",
                        "members": {
                            "nextToken": {
                                "target":"String"
                            },
                            "pageSize": {
                                "target":"Integer"
                            }
                        }
                    },
                    "ListCitiesOutput": {
                        "type":"structure",
                        "members": {
                            "items": {
                                "target":"CitySummaries",
                                "required":true
                            },
                            "nextToken": {
                                "target":"String"
                            }
                        }
                    },
                    "NoSuchResource": {
                        "type":"structure",
                        "members": {
                            "resourceType": {
                                "target":"String",
                                "required":true
                            }
                        },
                        "error":"client"
                    }
                }
            }
        }


.. _Tagged union: https://en.wikipedia.org/wiki/Tagged_union
