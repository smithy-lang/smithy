.. _aws-restjson1-protocol:

======================
AWS restJson1 protocol
======================

This specification defines the ``aws.protocols#restJson1`` protocol. This
protocol is used to expose services that serialize payloads as JSON and
utilize features of HTTP like configurable HTTP methods, URIs, and
status codes.

.. smithy-trait:: aws.protocols#restJson1
.. _aws.protocols#restJson1-trait:

---------------------------------
``aws.protocols#restJson1`` trait
---------------------------------

Summary
    A :ref:`protocol definition trait <protocolDefinition-trait>` that
    configures a service to support the ``aws.protocols#restJson1``
    protocol.
Trait selector
    ``service``
Value type
    Structure

``aws.protocols#restJson1`` is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - http
      - ``[string]``
      - The priority ordered list of supported HTTP protocol versions.
    * - eventStreamHttp
      - ``[string]``
      - The priority ordered list of supported HTTP protocol versions
        that are required when using :ref:`event streams <event-streams>`
        with the service. If not set, this value defaults to the value
        of the ``http`` member. Any entry in ``eventStreamHttp`` MUST
        also appear in ``http``.

Each entry in ``http`` and ``eventStreamHttp`` SHOULD be a valid
`Application-Layer Protocol Negotiation (ALPN) Protocol ID`_ (for example,
``http/1.1``, ``h2``, etc). Clients SHOULD pick the first protocol in the
list they understand when connecting to a service. A client SHOULD assume
that a service supports ``http/1.1`` when no ``http`` or ``eventStreamHttp``
values are provided.

The following example defines a service that uses ``aws.protocols#restJson1``.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#restJson1

        @restJson1
        service MyService {
            version: "2020-04-02"
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-04-02",
                    "traits": {
                        "aws.protocols#restJson1": {}
                    }
                }
            }
        }

The following example defines a service that requires the use of
``h2`` when using event streams.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#restJson1

    @restJson1(
        http: ["h2", "http/1.1"],
        eventStreamHttp: ["h2"]
    )
    service MyService {
        version: "2020-04-02"
    }

The following example defines a service that requires the use of
``h2`` or ``http/1.1`` when using event streams, where ``h2`` is
preferred over ``http/1.1``.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#restJson1

    @awsJson1_1(
        http: ["h2", "http/1.1"],
        eventStreamHttp: ["h2", "http/1.1"]
    )
    service MyService {
        version: "2020-04-02"
    }

The following example defines a service that requires the use of
``h2`` for all requests, including event streams.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#restJson1

    @restJson1(http: ["h2"])
    service MyService {
        version: "2020-04-02"
    }


----------------
Supported traits
----------------

The ``aws.protocols#restJson1`` protocol supports the following traits
that affect serialization:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Trait
      - Description
    * - :ref:`cors <cors-trait>`
      - Indicates that the service supports CORS.
    * - :ref:`endpoint <endpoint-trait>`
      - Configures a custom operation endpoint.
    * - :ref:`hostLabel <hostLabel-trait>`
      - Binds a top-level operation input structure member to a label in
        the hostPrefix of an endpoint trait.
    * - :ref:`http <http-trait>`
      - Configures the HTTP bindings of an operation. An operation that
        does not define the ``http`` trait is ineligible for use with
        this protocol.
    * - :ref:`httpError <httpError-trait>`
      - A ``client`` error has a default status code of ``400``, and a
        ``server`` error has a default status code of ``500``. The
        ``httpError`` trait is used to define a custom status code.
    * - :ref:`httpHeader <httpHeader-trait>`
      - Binds a top-level input, output, or error structure member to
        an HTTP header instead of the payload.
    * - :ref:`httpLabel <httpLabel-trait>`
      - Binds a top-level input structure member to a URI label instead
        of the payload.
    * - :ref:`httpPayload <httpPayload-trait>`
      - Binds a top-level input or output structure member as the payload
        of a request or response.

        .. important::

            This protocol only permits the :ref:`httpPayload-trait` to be applied to
            members that target structures, documents, strings, blobs, or unions.

    * - :ref:`httpPrefixHeaders <httpPrefixHeaders-trait>`
      - Binds a top-level input, output, or error member to a map of
        prefixed HTTP headers.
    * - :ref:`httpQuery <httpQuery-trait>`
      - Binds a top-level input structure member to a query string parameter.
    * - :ref:`httpQueryParams <httpQueryParams-trait>`
      - Binds a map of key-value pairs to query string parameters.
    * - :ref:`jsonName <jsonName-trait>`
      - By default, the JSON property names used in serialized structures are
        the same as a structure member name. The ``jsonName`` trait changes
        the JSON property name to a custom value.
    * - :ref:`timestampFormat <timestampFormat-trait>`
      - Defines a custom timestamp serialization format.
    * - :ref:`requestCompression <requestCompression-trait>`
      - Indicates that an operation supports compressing requests from clients
        to services.


------------
Content-Type
------------

The ``aws.protocols#restJson1`` protocol uses a default Content-Type
of ``application/json``.

Input or output shapes that apply the :ref:`httpPayload-trait` on one of
their top-level members MUST use a Content-Type that is appropriate for
the payload. The following table defines the expected Content-Type header
for requests and responses based on the shape targeted by the member marked
with the ``httpPayload`` trait:

.. list-table::
    :header-rows: 1
    :widths: 30 70

    * - Targeted shape
      - Content-Type
    * - Has :ref:`mediaType-trait`
      - Use the value of the ``mediaType`` trait if present.
    * - ``string``
      - ``text/plain``
    * - ``blob``
      - ``application/octet-stream``
    * - ``document``
      - ``application/json``
    * - ``structure``
      - ``application/json``
    * - ``union``
      - ``application/json``


------------------------
JSON shape serialization
------------------------

.. list-table::
    :header-rows: 1
    :widths: 25 75

    * - Smithy type
      - JSON type
    * - ``blob``
      - JSON ``string`` value that is base64 encoded.
    * - ``boolean``
      - JSON boolean
    * - ``byte``
      - JSON number
    * - ``short``
      - JSON number
    * - ``integer``
      - JSON number
    * - ``long``
      - JSON number
    * - ``float``
      - JSON number for numeric values. JSON strings for ``NaN``, ``Infinity``,
        and ``-Infinity``
    * - ``double``
      - JSON number for numeric values. JSON strings for ``NaN``, ``Infinity``,
        and ``-Infinity``
    * - ``bigDecimal``
      - JSON number. Unfortunately, this protocol serializes bigDecimal
        shapes as a normal JSON number. Many JSON parsers will either
        truncate the value or be unable to parse numbers that exceed the
        size of a double.
    * - ``bigInteger``
      - JSON number. Unfortunately, this protocol serializes bigInteger
        shapes as a normal JSON number. Many JSON parsers will either
        truncate the value or be unable to parse numbers that exceed the
        size of a double.
    * - ``string``
      - JSON string
    * - ``timestamp``
      - JSON number (default). This protocol uses ``epoch-seconds``, also
        known as Unix timestamps, in JSON payloads represented as a double.
        However, the :ref:`timestampFormat <timestampFormat-trait>` MAY be
        used to customize timestamp serialization.
    * - ``document``
      - Any JSON value
    * - ``list``
      - JSON array
    * - ``set``
      - JSON array. A set is serialized identically as a ``list`` shape,
        but only contains unique values.
    * - ``map``
      - JSON object
    * - ``structure``
      - JSON object. Each member value provided for the structure is
        serialized as a JSON property where the property name is the same
        as the member name. The :ref:`jsonName-trait` can be used to serialize
        a property using a custom name. A null value MAY be provided or
        omitted for a :ref:`boxed <box-trait>` member with no observable
        difference.
    * - ``union``
      - JSON object. A union is serialized identically as a ``structure``
        shape, but only a single member can be set to a non-null value.


--------------------------
HTTP binding serialization
--------------------------

The ``aws.protocols#restJson1`` protocol supports all of the HTTP binding traits
defined in the :ref:`HTTP protocol bindings <http-traits>` specification. The
serialization formats and behaviors described for each trait are supported as
defined in the ``aws.protocols#restJson1`` protocol.


.. |quoted shape name| replace:: ``aws.protocols#restJson1``
.. include:: non-numeric-floats.rst.template


.. restJson1-errors:

-----------------------------
Operation error serialization
-----------------------------

Error responses in the ``restJson1`` protocol are serialized identically to
standard responses with one additional component to distinguish which error
is contained. New server-side protocol implementations MUST use a header field
named ``X-Amzn-Errortype``. Clients MUST accept any one of the following: an
additional header with the name ``X-Amzn-Errortype``, a body field with the
name ``__type``, or a body field named ``code``. The value of this component
SHOULD contain the :token:`shape name <smithy:Identifier>` of the error's
:ref:`shape-id`. The value of this component SHOULD NOT include the
:ref:`shape-id`'s namespace.

Legacy server-side protocol implementations sometimes include additional
information in this value. New server-side protocol implementations SHOULD NOT
populate this value with anything but the shape name. All client-side
implementations SHOULD support sanitizing the value to retrieve the
disambiguated error type using the following steps:

1. If a ``:`` character is present, then take only the contents **before** the
   first ``:`` character in the value.
2. If a ``#`` character is present, then take only the contents **after** the
   first ``#`` character in the value.

All of the following error values resolve to ``FooError``:

* ``FooError``
* ``FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/``
* ``aws.protocoltests.restjson#FooError``
* ``aws.protocoltests.restjson#FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/``


-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/smithy-lang/smithy/tree/main/smithy-aws-protocol-tests/model/restJson1

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.

*TODO: Add event stream handling specifications.*

.. _`Application-Layer Protocol Negotiation (ALPN) Protocol ID`: https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#alpn-protocol-ids

.. include:: error-rename.rst.template
