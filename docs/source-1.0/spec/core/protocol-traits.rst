===============
Protocol traits
===============

Serialization and protocol traits define how data is transferred over
the wire.

.. smithy-trait:: smithy.api#protocolDefinition
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
       * - noInlineDocumentSupport
         - ``boolean``
         - If set to ``true``, indicates that this protocol does not support
           ``document`` type shapes. A service that uses such a protocol
           MUST NOT contain any ``document`` shapes in their service closure.

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
            "smithy": "1.0",
            "shapes": {
                "smithy.example#WeatherService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "traits": {
                        "smithy.example#jsonExample": {},
                        "smithy.example#xmlExample": {}
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


.. smithy-trait:: smithy.api#jsonName
.. _jsonName-trait:

------------------
``jsonName`` trait
------------------

Summary
    Allows a serialized object property name in a JSON document to differ from
    a structure or union member name used in the model.
Trait selector
    ``:is(structure, union) > member``

    *Any structure or union member*
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
            "smithy": "1.0",
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

.. note::

    No two members of the same structure or union can use the
    same case-sensitive ``@jsonName``.


.. smithy-trait:: smithy.api#mediaType
.. _mediaType-trait:

-------------------
``mediaType`` trait
-------------------

Summary
    Describes the contents of a blob or string shape using a design-time
    media type as defined by :rfc:`6838` (for example, ``application/json``).
Trait selector
    ``:is(blob, string)``

    *Any blob or string*
Value type
    ``string``

The following example defines a ``video/quicktime`` blob:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @mediaType("video/quicktime")
        blob VideoData

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#VideoData": {
                    "type": "blob",
                    "traits": {
                        "smithy.api#mediaType": "video/quicktime"
                    }
                }
            }
        }

.. rubric:: Use cases

The primary function of the ``mediaType`` trait is to send open content
data over the wire inside of values that are isolated from the rest of
a payload using exact representations of customer provided data. While the
model does define the serialization format of values able to be stored in a
shape at design-time using a media type, models are not required to define
any kind of schema for the shape.

The ``mediaType`` trait can be used to aid tools in documentation,
validation, special-cased helpers to serialize and deserialize media type
contents in code, assigning a fixed Content-Type when using
:ref:`HTTP bindings <http-traits>`, etc.

.. rubric:: Comparisons to document types

The serialization format of a shape marked with the ``@mediaType`` trait is
an important part of its contract. In contrast, document types are
serialized in a protocol-agnostic way and can only express data types as
granular as the JSON-type system. Design-time media types are preferred over
document types when the exact bytes of a value are required for an
application to function.


.. smithy-trait:: smithy.api#timestampFormat
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

By default, the serialization format of a timestamp is implicitly determined by
the :ref:`protocol <protocolDefinition-trait>` of a service; however, the
serialization format can be explicitly configured in some protocols to
override the default format using the ``timestampFormat`` trait.

.. rubric:: Timestamp formats

Smithy defines the following built-in timestamp formats:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Format
      - Description
    * - date-time
      - Date time as defined by the ``date-time`` production in
        :rfc:`3339#section-5.6` with optional millisecond precision but no
        UTC offset (for example, ``1985-04-12T23:20:50.520Z``). Values that
        are more granular than millisecond precision SHOULD be truncated to
        fit millisecond precision. Deserializers SHOULD parse ``date-time``
        values that contain offsets gracefully by normalizing them to UTC.
    * - http-date
      - An HTTP date as defined by the ``IMF-fixdate`` production in
        :rfc:`7231#section-7.1.1.1` (for example,
        ``Tue, 29 Apr 2014 18:30:38 GMT``). A deserializer that encounters an
        ``http-date`` timestamp with fractional precision SHOULD fail to
        deserialize the value (for example, an HTTP server SHOULD return a 400
        status code).
    * - epoch-seconds
      - Also known as Unix time, the number of seconds that have elapsed since
        00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970,
        with optional millisecond precision (for example, ``1515531081.123``).
        Values that are more granular than millisecond precision SHOULD be
        truncated to fit millisecond precision.

.. rubric:: Resolving timestamp formats

The following steps are taken to determine the serialization format of a
:ref:`member <member>` that targets a timestamp:

1. Use the ``timestampFormat`` trait of the member, if present.
2. Use the ``timestampFormat`` trait of the shape, if present.
3. Use the default format of the protocol.

.. important::

    This trait SHOULD NOT be used unless the intended serialization format of
    a timestamp differs from the default protocol format. Using this trait too
    liberally can cause other tooling to improperly interpret the timestamp.
