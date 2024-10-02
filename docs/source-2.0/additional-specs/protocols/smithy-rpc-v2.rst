.. _smithy-rpc-v2-cbor:

===========================
Smithy RPC v2 CBOR protocol
===========================

This specification defines the ``smithy.protocols#rpcv2Cbor`` protocol. This
protocol is used to expose services that serialize RPC payloads as CBOR.

.. smithy-trait:: smithy.protocols#rpcv2Cbor
.. _smithy.protocols#rpcv2Cbor-trait:


------------------------------------
``smithy.protocols#rpcv2Cbor`` trait
------------------------------------

Summary
    Adds support for an RPC-based protocol over HTTP that sends requests and
    responses with CBOR payloads.
Trait selector
    ``service``
Value type
    Structure
See
    `Protocol tests <https://github.com/smithy-lang/smithy/tree/__smithy_version__/smithy-protocol-tests/model/rpcv2Cbor>`_

``smithy.protocols#rpcv2Cbor`` is a structure that supports the following
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

The following example defines a service that uses
``smithy.protocols#rpcv2Cbor``.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use smithy.protocols#rpcv2Cbor

    @rpcv2Cbor
    service MyService {
        version: "2020-07-02"
    }

The following example defines a service that requires the use of
``h2`` when using event streams.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use smithy.protocols#rpcv2Cbor

    @rpcv2Cbor(
        http: ["h2", "http/1.1"],
        eventStreamHttp: ["h2"]
    )
    service MyService {
        version: "2020-02-05"
    }

The following example defines a service that requires the use of
``h2`` or ``http/1.1`` when using event streams, where ``h2`` is
preferred over ``http/1.1``.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use smithy.protocols#rpcv2Cbor

    @rpcv2Cbor(
        http: ["h2", "http/1.1"],
        eventStreamHttp: ["h2", "http/1.1"]
    )
    service MyService {
        version: "2020-02-05"
    }

The following example defines a service that requires the use of
``h2`` for all requests, including event streams.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use smithy.protocols#rpcv2Cbor

    @rpcv2Cbor(http: ["h2"])
    service MyService {
        version: "2020-02-05"
    }


----------------
Supported traits
----------------

The ``smithy.protocols#rpcv2Cbor`` protocol supports the following traits
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
    * - :ref:`httpError <httpError-trait>`
      - A ``client`` error has a default status code of ``400``, and a
        ``server`` error has a default status code of ``500``. The
        ``httpError`` trait is used to define a custom status code.
    * - :ref:`requestCompression <requestCompression-trait>`
      - Indicates that an operation supports compressing requests from clients
        to services.


------------------
Protocol behaviors
------------------

Implementations of the ``smithy.protocols#rpcv2Cbor`` protocol comply with the
following rules:

~~~~~~~~
Requests
~~~~~~~~
 
Every request for the ``rpcv2Cbor`` protocol MUST contain a ``Smithy-Protocol``
header with the value of ``rpc-v2-cbor``.

Every request for the ``rpcv2Cbor`` protocol MUST be sent using the HTTP
``POST`` method. :ref:`HTTP binding traits <http-traits>` MUST be ignored when
building ``rpcv2Cbor`` requests if they are present.

Every request for the ``rpcv2Cbor`` protocol MUST be sent to a URL with the
following form: ``{prefix?}/service/{serviceName}/operation/{operationName}``

* The Smithy RPCv2 CBOR protocol will only use the last four segments of the
    URL when routing requests. For example, a service could use a ``v1`` prefix
    in the URL path, which would not affect the operation a request is routed
    to: ``v1/service/FooService/operation/BarOperation``
* The ``serviceName`` segment MUST be replaced by the :token:`shape name <smithy:Identifier>`
    of the service's :ref:`shape-id` in the Smithy model. The ``serviceName``
    produced by client implementations MUST NOT contain the namespace of the
    ``service`` shape. Service implementations SHOULD accept an absolute shape
    ID as the content of this segment with the ``#`` character replaced with a
    ``.`` character, routing it the same as if only the name was specified. For
    example, if the ``service``'s absolute shape ID is
    ``com.example#TheService``, a service should accept both ``TheService`` and
    ``com.example.TheService`` as values for the ``serviceName`` segment.
* The ``operationName`` segment MUST be replaced by the name of the ``operation``
    shape in the Smithy. The ``operationName`` produced by client
    implementations MUST NOT contain the namespace of the ``operation`` shape.
    Service implementations MUST NOT accept an absolute shape ID as the content
    of this segment.

Requests for the ``rpcv2Cbor`` protocol MUST use the following behavior for
setting a ``Content-Type`` header:

* Buffered RPC requests: the value of the ``Content-Type`` header MUST be
    ``application/cbor``.
* Event streaming requests: the value of the ``Content-Type`` header MUST be
    ``application/vnd.amazon.eventstream``.
* Requests for operations with no defined input type (as in, they target the
    ``Unit`` shape) MUST NOT contain bodies in their HTTP requests. The
    ``Content-Type`` for the serialization format MUST NOT be set.

Requests for the ``rpcv2Cbor`` protocol MUST NOT contain an ``X-Amz-Target`` or
``X-Amzn-Target`` header. An ``rpcv2Cbor`` request is malformed if it contains
either of these headers. Server-side implementations MUST reject such requests
for security reasons.

Buffered RPC requests for the ``rpcv2Cbor`` protocol SHOULD include a
``Content-Length`` header. Event streaming requests MUST NOT specify a content
length (instead using ``Transfer-Encoding: chunked`` on HTTP/1.1).

Requests for the ``rpcv2Cbor`` protocol MUST use the following behavior for
setting an ``Accept`` header:

* For requests with event streaming responses: the value of the ``Accept``
    header MUST be ``application/vnd.amazon.eventstream``.
* For requests with all other response types: the value of the ``Accept``
    header MUST be ``application/cbor``.

Other forms of content streaming MAY be added in the future, utilizing
different values for ``Accept``.

In summary, the ``rpcv2Cbor`` protocol defines behavior for the following
headers for requests:

.. list-table::
    :header-rows: 1
    :widths: 18 18 64

    * - Header
      - Status
      - Description
    * - ``Smithy-Protocol``
      - Required
      - The value of ``rpc-v2-cbor``.
    * - ``Content-Type``
      - Required with request bodies
      - The value of ``application/cbor``. For event streaming requests, this
        is ``application/vnd.amazon.eventstream``.
    * - ``Content-Length``
      - Conditional
      - The standard ``Content-Length`` header defined by :rfc:`9110#section-8.6`.
        For event streaming requests, this MUST NOT be set.
    * - ``Accept``
      - Required
      - The value of ``application/cbor``. For requests with event streaming
        responses, this is ``application/vnd.amazon.eventstream``.


~~~~~~~~~
Responses
~~~~~~~~~

The status code for successful ``rpcv2Cbor`` responses MUST be ``200``.
 
The status code for error ``rpcv2Cbor`` MUST be determined by the following
steps:

1. If the :ref:`httpError <httpError-trait>` trait is set on the error shape,
   its value is used.
2. If the :ref:`error <error-trait>` trait is set to ``server``, the value
   MUST be ``500``.
3. Otherwise, the value MUST be ``400``.

Every response for the ``rpcv2Cbor`` protocol MUST contain a ``Smithy-Protocol``
header with the value of ``rpc-v2-cbor``. The response MUST match the value of
the ``Smithy-Protocol`` header from the request.

Responses for the ``rpcv2Cbor`` protocol MUST use the following behavior for
setting a ``Content-Type`` header:

* Buffered RPC responses: the value of the ``Content-Type`` header MUST be
  ``application/cbor``.
* Event streaming responses: the value of the ``Content-Type`` header MUST be
  ``application/vnd.amazon.eventstream``.
* Responses for operations with no defined output type (as in, they target the
  ``Unit`` shape) MUST NOT contain bodies in their HTTP responses. The
  ``Content-Type`` for the serialization format MUST NOT be set.

Buffered RPC responses for the ``rpcv2Cbor`` protocol SHOULD include a
``Content-Length`` header. Event streaming responses SHOULD NOT specify a
content length (instead using ``Transfer-Encoding: chunked`` on HTTP/1.1).

Responses for the ``rpcv2Cbor`` protocol SHOULD NOT contain the
``X-Amzn-ErrorType`` header. Type information is always serialized in the
payload. Clients MUST ignore this header. See `Operation error serialization`_
for information on the serialization of error responses.

In summary, the ``rpcv2Cbor`` protocol defines behavior for the following
headers for responses:

.. list-table::
    :header-rows: 1
    :widths: 18 18 64

    * - Header
      - Status
      - Description
    * - ``Smithy-Protocol``
      - Required
      - The value of ``rpc-v2-cbor``.
    * - ``Content-Type``
      - Required with response bodies
      - The value of ``application/cbor``. For event streaming responses, this
        is ``application/vnd.amazon.eventstream``.
    * - ``Content-Length``
      - Conditional
      - The standard ``Content-Length`` header defined by :rfc:`9110#section-8.6`.
        For event streaming requests, this SHOULD NOT be set.


-------------------
Shape serialization
-------------------

The ``smithy.protocols#rpcv2Cbor`` protocol serializes all shapes into a CBOR
document body with no HTTP bindings supported. The following table shows how
to convert each shape type:

.. list-table::
    :header-rows: 1
    :widths: 25 75

    * - Smithy type
      - CBOR representation
    * - ``blob``
      - :rfc:`Major type 2 (byte string) <8949#section-3.1-2.5>`
    * - ``boolean``
      - :rfc:`Major type 7 (simple) <8949#section-3.1-2.15>`
    * - ``byte``
      - :rfc:`Major type 0 (unsigned integer) <8949#section-3.1-2.1>` or
        :rfc:`major type 1 (negative integer) <8949#section-3.1-2.3>`
    * - ``short``
      - :rfc:`Major type 0 (unsigned integer) <8949#section-3.1-2.1>` or
        :rfc:`major type 1 (negative integer) <8949#section-3.1-2.3>`
    * - ``integer``
      - :rfc:`Major type 0 (unsigned integer) <8949#section-3.1-2.1>` or
        :rfc:`major type 1 (negative integer) <8949#section-3.1-2.3>`
    * - ``long``
      - :rfc:`Major type 0 (unsigned integer) <8949#section-3.1-2.1>` or
        :rfc:`major type 1 (negative integer) <8949#section-3.1-2.3>`
    * - ``float``
      - :rfc:`Major type 7 (float) <8949#section-3.1-2.15>`
    * - ``double``
      - :rfc:`Major type 7 (float) <8949#section-3.1-2.15>`
    * - ``bigDecimal``
      - :rfc:`Major type 6 <8949#section-3.1-2.13>`, value
        :rfc:`tags 2 (unsigned bignum) or 3 (negative bignum) <8949#section-3.4>`
    * - ``bigInteger``
      - :rfc:`Major type 6 <8949#section-3.1-2.13>`, value
        :rfc:`tag 4 (decimal fraction) <8949#section-3.4>`
    * - ``string``
      - :rfc:`Major type 3 (text string) <8949#section-3.1-2.7>`
    * - ``timestamp``
      - See `Timestamp type serialization`_.
    * - ``document``
      - Document types are not currently supported in this protocol.
    * - ``list``
      - :rfc:`Major type 4 (array of data items) <8949#section-3.1-2.9>`
    * - ``map``
      - :rfc:`Major type 5 (map of pairs of data items) <8949#section-3.1-2.11>`,
        each key/value pair is a pair in the type
    * - ``structure``
      - :rfc:`Major type 5 (map of pairs of data items) <8949#section-3.1-2.11>`,
        each member is the key to a pair in the type
    * - ``union``
      - :rfc:`Major type 5 (map of pairs of data items) <8949#section-3.1-2.11>`,
        each member is the key to a pair in the type.

        A union is serialized identically as a ``structure``
        shape, but only a single member can be set to a non-null value.
        Deserializers MUST ignore an unrecognized ``__type`` member if present.

Values that are ``null`` MUST be omitted from wire contents where not subject
to `default value serialization`_ rules.

If an implementation does not support arbitrary precision (``bigInteger`` and
``bigDecimal`` Smithy types), it MUST fail when attempting to deserialize a
value of that type.

.. note::
    The ``undefined`` CBOR value, CBOR :rfc:`major type 7 <8949#section-3.1-2.15>`
    :rfc:`value 23 <8949#section-3.3>`, is not supported. Clients SHOULD NOT
    serialize to this CBOR value and SHOULD deserialize this CBOR value to
    ``null``. Servers MUST NOT serialize to this CBOR value and MUST
    deserialize this CBOR value to ``null``.

~~~~~~~~~~~~~~~~~~~~~~~~~~
Numeric type serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~
 
Numeric types (``byte``, ``short``, ``integer``, ``long``, ``float``, and ``double``)
SHOULD be serialized into the smallest possible CBOR type representation that
can contain its value. For example, a field modeled as a ``long`` with a value of
1 should be sent in a single byte.

Floating-point numeric types (``float`` and ``double``) MAY be serialized into
non-floating-point numeric types (``byte``, ``short``, ``integer``, and ``long``) if
and only if the conversion would not cause a loss of precision. For example, a
field modeled as a ``float`` with a value of 256 may be sent as an integer.

As support for half-precision floating-point values is inconsistent across
implementations, floating-point numeric types SHOULD NOT serialize into a
half-precision (16 bit) numeric CBOR values. Implementations MUST be capable of
deserializing half-precision numeric CBOR values (including -Infinity,
Infinity, and NAN) into their Smithy type representation.

Numeric types MUST be deserialized into their Smithy type representation. For
example, a field modeled as a ``long`` with a single byte value of 1 must be
deserialized into a long.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Timestamp type serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Timestamps are serialized as :rfc:`major type 6 <8949#section-3.1-2.13>`, value
:rfc:`tag 1 (epoch-based date/time) <8949#section-3.4>` to indicate the tagged
value is an epoch timestamp. Values are either :rfc:`major type 0 (unsigned integer) <8949#section-3.1-2.1>`
or :rfc:`major type 1 (negative integer) <8949#section-3.1-2.3>` for integer
values or :rfc:`major type 7 <8949#section-3.1-2.15>`,
:rfc:`value 25 (half-precision float), 26 (single-precision float), or 27 (double-precision float) <8949#section-3.3>`
for floating-point values.

As support for half-precision floating-point values is inconsistent across
implementations, timestamp types SHOULD NOT serialize into a half-precision
(16 bit) numeric CBOR value for the tagged value.

This protocol uses ``epoch-seconds``, also known as Unix timestamps, with
millisecond (1/1000th of a second) resolution. The :ref:`timestampFormat <timestampFormat-trait>`
MUST NOT be respected to customize timestamp serialization.


~~~~~~~~~~~~~~~~~~~~~~~~~~~
Default value serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~~

To avoid information disclosure, service serializers SHOULD omit the default
value of structure members that are marked with the :ref:`internal trait <internal-trait>`.

Client deserializers SHOULD attempt to error correct structures that omit a
:ref:`required <required-trait>` member by filling in a default zero value for
the member. Error correction allows clients to continue to function while the
server is in error.

----------------------------------
Operation response deserialization
----------------------------------
 
Clients MUST use the following rules to interpret responses and determine how
to deserialize them:

1. If the response does not have the same ``Smithy-Protocol`` header as the
    request, it MUST be considered malformed. Clients who receive a malformed
    response MUST handle it (i.e. throw a reasonable exception) based solely on
    the HTTP response code. No attempt should be made to interpret the response
    body or headers.
2. If the response code is ``200``, the request is successful and the response
    payload SHALL be deserialized as the ``output`` shape defined on the
    operation.
3. Finally, if the response code is not ``200``, the response payload is an
    exception and SHALL be deserialized according to `Operation error serialization`_

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Operation error serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Error responses in the ``rpcv2Cbor`` protocol MUST be serialized identically to
standard responses with one additional component to distinguish which error is
contained: a body field named ``__type``. This value of this component MUST
contain the error’s absolute :ref:`shape-id`.

By default, all error shapes have a ``message`` field containing an
error-specific message meant for human consumers of API errors. Services MUST
support generating this field and serializing it when responding. Clients MUST
support generating this field and deserializing it from responses.

The ``Code`` response body field and ``code`` response body field MUST NOT be
used to distinguish which error is contained in a response.

Clients who receive a malformed error response MUST handle it based solely on
the HTTP response code (i.e. throw a reasonable exception). No attempt should
be made to interpret the response body or headers.

.. include:: ../../aws/protocols/error-rename-simple.rst.template

.. _`Application-Layer Protocol Negotiation (ALPN) Protocol ID`: https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#alpn-protocol-ids
