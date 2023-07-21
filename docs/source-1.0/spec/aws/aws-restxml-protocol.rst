.. _aws-restxml-protocol:

====================
AWS restXml protocol
====================

This specification defines the ``aws.protocols#restXml`` protocol.


.. smithy-trait:: aws.protocols#restXml
.. _aws.protocols#restXml-trait:

-------------------------------
``aws.protocols#restXml`` trait
-------------------------------

Summary
    Adds support for an HTTP-based protocol that sends XML requests and
    responses.
Trait selector
    ``service``
Value type
    Structure

``aws.protocols#restXml`` is a structure that supports the following
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
    * - noErrorWrapping
      - ``boolean``
      - Disables the wrapping of error properties in an ``ErrorResponse``
        XML element. See :ref:`operation error serialization <restXml-errors>`
        for more information.

Each entry in ``http`` and ``eventStreamHttp`` SHOULD be a valid
`Application-Layer Protocol Negotiation (ALPN) Protocol ID`_ (for example,
``http/1.1``, ``h2``, etc). Clients SHOULD pick the first protocol in the
list they understand when connecting to a service. A client SHOULD assume
that a service supports ``http/1.1`` when no ``http`` or ``eventStreamHttp``
values are provided.

The following example defines a service that uses ``aws.protocols#restXml``.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#restXml

        @restXml
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
                        "aws.protocols#restXml": {}
                    }
                }
            }
        }

The following example defines a service that requires the use of
``h2`` when using event streams.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#restXml

    @restXml(eventStreamHttp: ["h2"])
    service MyService {
        version: "2020-04-02"
    }

The following example defines a service that requires the use of
``h2`` or ``http/1.1`` when using event streams, where ``h2`` is
preferred over ``http/1.1``.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#restXml

    @restXml(eventStreamHttp: ["h2", "http/1.1"])
    service MyService {
        version: "2020-04-02"
    }

The following example defines a service that requires the use of
``h2`` for all requests, including event streams.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#restXml

    @restXml(http: ["h2"])
    service MyService {
        version: "2020-04-02"
    }


----------------
Supported traits
----------------

The ``aws.protocols#restXml`` protocol supports the following traits
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
    * - :ref:`httpPrefixHeaders <httpPrefixHeaders-trait>`
      - Binds a top-level input, output, or error member to a map of
        prefixed HTTP headers.
    * - :ref:`httpQuery <httpQuery-trait>`
      - Binds a top-level input structure member to a query string parameter.
    * - :ref:`httpQueryParams <httpQueryParams-trait>`
      - Binds a map of key-value pairs to query string parameters.
    * - :ref:`xmlAttribute <xmlAttribute-trait>`
      - Serializes an object property as an XML attribute rather than a nested
        XML element.
    * - :ref:`xmlFlattened <xmlFlattened-trait>`
      - By default, entries in lists, sets, and maps have values serialized in
        nested XML elements specific to their type. The ``xmlFlattened`` trait
        unwraps these elements into the containing structure.
    * - :ref:`xmlName <xmlName-trait>`
      - By default, the XML element names used in serialized structures are
        the same as a structure member name. The ``xmlName`` trait changes
        the XML element name to a custom value.
    * - :ref:`xmlNamespace <xmlNamespace-trait>`
      - Adds an xmlns namespace definition URI to XML element(s) generated
        for the targeted shape.
    * - :ref:`timestampFormat <timestampFormat-trait>`
      - Defines a custom timestamp serialization format.
    * - :ref:`requestCompression <requestCompression-trait>`
      - Indicates that an operation supports compressing requests from clients
        to services.

.. important::

    This protocol does not support document types.


------------
Content-Type
------------

The ``aws.protocols#restXml`` protocol uses a default ``Content-Type``
of ``application/xml``.

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
      - Undefined. Document shapes are not supported in this protocol.
    * - ``structure``
      - ``application/xml``
    * - ``union``
      - ``application/xml``


-----------------------
XML shape serialization
-----------------------

.. include:: aws-xml-serialization.rst.template


--------------------------
HTTP binding serialization
--------------------------

The ``aws.protocols#restXml`` protocol supports all of the HTTP binding traits
defined in the :ref:`HTTP protocol bindings <http-traits>` specification. The
serialization formats and behaviors described for each trait are supported as
defined in the ``aws.protocols#restXml`` protocol.


.. |quoted shape name| replace:: ``aws.protocols#restXml``
.. include:: non-numeric-floats.rst.template


.. _restXml-errors:

----------------------------
Error response serialization
----------------------------

Error responses in the ``restXml`` protocol are wrapped within an XML root
node named ``ErrorResponse`` by default. A nested element, named ``Error``,
contains the serialized error structure members, unless bound to another
location with HTTP protocol bindings.

Serialized error shapes MUST also contain an additional child element ``Code``
that contains only the :token:`shape name <smithy:Identifier>` of the error's
:ref:`shape-id`. This can be used to distinguish which specific error has been
serialized in the response.

.. code-block:: xml

    <ErrorResponse>
        <Error>
            <Type>Sender</Type>
            <Code>InvalidGreeting</Code>
            <Message>Hi</Message>
            <AnotherSetting>setting</AnotherSetting>
        </Error>
        <RequestId>foo-id</RequestId>
    </ErrorResponse>

The ``noErrorWrapping`` setting of the ``restXml`` protocol trait disables
using this additional nested XML element as the root node.

.. code-block:: xml

    <Error>
        <Type>Sender</Type>
        <Code>InvalidGreeting</Code>
        <Message>Hi</Message>
        <AnotherSetting>setting</AnotherSetting>
        <RequestId>foo-id</RequestId>
    </Error>

Error responses contain the following nested elements:

* ``Error``: A container for the encountered error.
* ``Type``: One of "Sender" or "Receiver"; whomever is at fault from the
  service perspective.
* ``Code``: The :token:`shape name <smithy:Identifier>` of the error's
  :ref:`shape-id`.
* ``RequestId``: Contains a unique identifier for the associated request.

In the above example, ``Message``, and ``AnotherSetting`` are additional,
hypothetical members of the serialized error structure.


-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/smithy-lang/smithy/tree/main/smithy-aws-protocol-tests/model/restXml

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.

*TODO: Add event stream handling specifications.*

.. _`Application-Layer Protocol Negotiation (ALPN) Protocol ID`: https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#alpn-protocol-ids

.. include:: error-rename.rst.template
