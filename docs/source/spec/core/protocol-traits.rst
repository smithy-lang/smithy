===============
Protocol traits
===============

Serialization and protocol traits define how data is transferred over
the wire.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. _protocolDefinition-trait:

----------------------------
``protocolDefinition`` trait
----------------------------

Summary
    A meta-trait that marks a trait as a protocol definition trait. Traits
    that are marked with this trait are applied to service shapes to
    define the protocols supported by a service. A client MUST understand
    at least one of the protocols in order to successfully communicate
    with the service.
Trait selector
    ``[trait|trait]``
Value type
    An object with the following properties:

    .. list-table::
       :header-rows: 1
       :widths: 10 23 67

       * - Property
         - Type
         - Description
       * - traits
         - [:ref:`shape-id`]
         - List of shape IDs that protocol implementations MUST understand
           in order to successfully use the protocol. Each shape MUST exist
           and MUST be a trait. Code generators SHOULD ensure that they
           support each listed trait.

Smithy is protocol agnostic, which means it focuses on the interfaces and
abstractions that are provided to end-users rather than how the data is sent
over the wire. In Smithy, a *protocol* is a named set of rules that defines
the syntax and semantics of how a client and server communicate. This
includes the application layer protocol of a service (for example, HTTP)
and the serialization formats used in messages (for example, JSON). Traits
MAY be used to influence how messages are serialized (for example,
:ref:`jsonName-trait` and :ref:`xmlAttribute-trait`).

The following example defines a service that supports both the hypothetical
``jsonExample`` and ``xmlExample`` protocols.

.. tabs::

    .. code-tab:: smithy

        /// An example JSON protocol.
        @protocolDefinition
        @trait(selector: "service")
        structure jsonExample {}

        /// An example XML protocol.
        @protocolDefinition
        @trait(selector: "service")
        structure xmlExample {}

        @jsonExample
        @xmlExample
        service WeatherService {
            version: "2017-02-11",
        }

    .. code-tab:: json

        {
            "smithy": "1.0.0",
            "shapes": {
                "smithy.example#WeatherService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "traits": {
                        "smithy.example#jsonExample": true,
                        "smithy.example#xmlExample": true
                    }
                },
                "smithy.example#jsonExample": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#documentation": "An example JSON protocol."
                        "smithy.api#protocolDefinition": {},
                        "smithy.api#trait": {
                            "selector": "service"
                        }
                    }
                },
                "smithy.example#xmlExample": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#documentation": "An example JSON protocol."
                        "smithy.api#protocolDefinition": {},
                        "smithy.api#trait": {
                            "selector": "service"
                        }
                    }
                }
            }
        }

Because protocol definitions are just specialized shapes, they can also
support configuration settings.

.. code-block:: smithy

    @protocolDefinition
    @trait(selector: "service")
    structure configurableExample {
        @required
        version: String
    }

    @configurableExample(version: "1.0")
    service WeatherService {
        version: "2017-02-11",
    }


.. _jsonName-trait:

------------------
``jsonName`` trait
------------------

Summary
    Allows a serialized object property name in a JSON document to differ from
    a structure member name used in the model.
Trait selector
    ``member:of(structure)``

    *Any structure member*
Value type
    ``string``

Given the following structure definition,

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @jsonName("Foo")
            foo: String,

            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#jsonName": "Foo"
                            }
                        },
                        "bar": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"

the JSON representation of the value would be serialized with the
following document:

.. code-block:: json

    {
        "Foo": "abc",
        "bar": "def"
    }


.. _mediaType-trait:

-------------------
``mediaType`` trait
-------------------

Summary
    Describes the contents of a blob or string shape using a media type as
    defined by :rfc:`6838` (e.g., "video/quicktime").
Trait selector
    ``:test(blob, string)``

    *Any blob or string*
Value type
    ``string``

The ``mediaType`` can be used in tools for documentation, validation,
automated conversion or encoding in code, automatically determining an
appropriate Content-Type for an HTTP-based protocol, etc.

The following example defines a video/quicktime blob:

.. tabs::

    .. code-tab:: smithy

        @mediaType("video/quicktime")
        blob VideoData


.. _timestampFormat-trait:

-------------------------
``timestampFormat`` trait
-------------------------

Summary
    Defines a custom timestamp serialization format.
Trait selector
    ``:test(timestamp, member > timestamp)``

    *timestamp or member that targets a timestamp*
Value type
    ``string``

The serialization format of a timestamp shape is normally dictated by the
:ref:`protocol <protocolDefinition-trait>` of a service. In order to
interoperate with other web services or frameworks, it is sometimes
necessary to use a specific serialization format that differs from the
protocol.

Smithy defines the following built-in timestamp formats:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Format
      - Description
    * - date-time
      - Date time as defined by the ``date-time`` production in
        `RFC3339 section 5.6 <https://xml2rfc.tools.ietf.org/public/rfc/html/rfc3339.html#anchor14>`_
        with no UTC offset (for example, ``1985-04-12T23:20:50.52Z``).
    * - http-date
      - An HTTP date as defined by the ``IMF-fixdate`` production in
        :rfc:`7231#section-7.1.1.1` (for example,
        ``Tue, 29 Apr 2014 18:30:38 GMT``).
    * - epoch-seconds
      - Also known as Unix time, the number of seconds that have elapsed since
        00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970,
        with decimal precision (for example, ``1515531081.1234``).

.. important::

    This trait SHOULD NOT be used unless the intended serialization format of
    a timestamp differs from the default protocol format. Using this trait too
    liberally can cause other tooling to improperly interpret the timestamp.

See :ref:`timestamp-serialization-format` for information on how to
determine the serialization format of a timestamp.
