.. _http-traits:

============================
HTTP protocol binding traits
============================

Smithy provides various HTTP binding traits that can be used by protocols to
explicitly configure HTTP request and response messages.

The members of an operation input, output, and errors MUST NOT be bound to
multiple HTTP message locations (e.g., a member cannot be bound to both a URI
label and to a header). Only top-level members of an operation's input, output,
and error structures are considered when serializing HTTP messages.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. _http-trait:

``http`` trait
==============

Summary
    Configures the HTTP bindings of an operation.
Trait selector
    ``operation``
Value type
    ``structure``

The ``http`` trait is a structure that supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - :ref:`method <http-method>`
      - ``string``
      - **Required**. The HTTP method of the operation.
    * - :ref:`uri <http-uri>`
      - ``string``
      - **Required**. The URI pattern of the operation. Labels defined in the
        URI pattern are used to bind operation input members to the URI.
    * - code
      - ``integer``
      - The HTTP status code of a successful response. Defaults to ``200`` if
        not provided.

The following example defines an operation that uses HTTP bindings:


.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @idempotent
        @http(method: "PUT", uri: "/{bucketName}/{key}", code: 200)
        operation PutObject {
            input: PutObjectInput
        }

        structure PutObjectInput {
            // Sent in the URI label named "key".
            @required
            @httpLabel
            key: ObjectKey,

            // Sent in the URI label named "bucketName".
            @required
            @httpLabel
            bucketName: String,

            // Sent in the X-Foo header
            @httpHeader("X-Foo")
            foo: String,

            // Sent in the query string as paramName
            @httpQuery("paramName")
            someValue: String,

            // Sent in the body
            data: MyBlob,

            // Sent in the body
            additional: String,
        }


.. _http-method:

method
------

The ``method`` property defines the HTTP method of the operation (e.g., "GET",
"PUT", "POST", "DELETE", "PATCH", etc). Smithy will use this value literally
and will perform no validation on the method. The ``method`` value SHOULD
match the ``operation`` production rule of :rfc:`7230#appendix-B`. This
property does not influence the safety or idempotency characteristics of an
operation.


.. _http-uri:

uri
---

The ``uri`` property defines the *request-target* of the operation in
*origin-form* as defined in :rfc:`7230#section-5.3.1`. The URI is a simple
pattern that Smithy uses to match HTTP requests to operations and to bind
components of the request URI to fields in the operations's input structure.
:dfn:`Patterns` consist of literal characters that MUST be matched in the
request URI and labels which are used to insert named components into the
request URI.

The resolved absolute URI of an operation is formed by combining the URI of
the operation with the endpoint of the service. (that is, the host and any 
base URL of where the service is deployed). For example, given a service 
endpoint of ``https://example.com/v1`` and an operation pattern of
``/myresource``, the resolved absolute URI of the operation is
``https://example.com/v1/myresource``.

The value provided for the ``uri`` property MUST adhere to the following
constraints:

#. MUST start with "/".
#. MUST NOT contain empty path segments (i.e., "//").
#. MUST NOT contain a fragment (i.e., "#").
#. MUST NOT end with "?".
#. MUST NOT contain dot-segments (i.e., ".." and ".").
#. MUST NOT case-sensitively conflict with other ``http`` / ``uri``
   properties.

::

    @readonly
    @http(method: "GET", uri: "/foo/{baz}")
    operation GetService {
        output: GetServiceOutput
    }


Literal character sequences
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Patterns with no labels will match only requests containing the exact literal
characters declared in the pattern, with the exception of trailing slashes
which are always optional.

Given an endpoint of ``https://yourhost`` and a pattern of ``/my/uri/path``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``https://yourhost/my/uri/path``
      - Yes
      - Exact match
    * - ``https://yourhost/my/uri/path/``
      - Yes
      - Trailing slashes are ignored
    * - ``https://yourhost/my/uri``
      - No
      - Missing "/path"
    * - ``https://yourhost/my/uri/other``
      - No
      - Found "/other" instead of "/path"
    * - ``https://yourhost/my/uri/path/other``
      - No
      - Trailing segment "/other"


Labels
~~~~~~

Patterns MAY contain label placeholders. :dfn:`Labels` consist of label name
characters surrounded by open and closed braces (i.e., "{label_name}" is a
label and ``label_name`` is the label name). The label name corresponds to a
top-level operation input structure member name. Every label MUST have a
corresponding input member, the input member MUST be marked as
:ref:`required-trait`, the input member MUST have the :ref:`httpLabel-trait`,
and the input member MUST reference a string, byte, short, integer, long,
float, double, bigDecimal, bigInteger, boolean, or timestamp.

Labels MUST adhere to the following constraints:

#. Labels MUST NOT appear in the query string.
#. Each label MUST span an entire path segment (e.g., "/{foo}/bar" is valid,
   and "/{foo}bar" is invalid).

A pattern of ``/my/uri/{label}`` will match any URI that begins with
``/my/uri/`` followed by any string not including an additional path segment
("/").

Given a pattern of ``/my/uri/{label}`` and an endpoint of ``http://yourhost``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``http://yourhost/my/uri/foo``
      - Yes
      - "/my/uri/" matches and "foo" is captured as ``label``.
    * - ``http://yourhost/my/uri/foo/``
      - Yes
      - "/my/uri/" matches and "foo" is captured as ``label``. The trailing
        "/" is ignored.
    * - ``http://yourhost/my/uri/bar``
      - Yes
      - "/my/uri/" matches and "bar" is captured as ``label``.
    * - ``http://yourhost/my/uri``
      - No
      - "/my/uri" matches but is missing a segment for ``label``.
    * - ``http://yourhost/my/uri/foo/bar``
      - No
      - Found a trailing segment "/bar".

Any number of labels can be included within a pattern, provided that they are
not immediately adjacent and do not have identical label names. Given a
pattern of ``/my/uri/{label1}/{label2}`` and an endpoint of
``http://yourhost``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``http://yourhost/my/uri/foo/bar``
      - Yes
      - Matches literal "/my/uri/", "foo" is captured as ``label1``, and "bar"
        is captured as ``label2``.
    * - ``http://yourhost/my/uri/bar/baz/``
      - Yes
      - Matches literal "/my/uri/", "bar" is captured as ``label1``, and "baz"
        is captured as ``label2``.
    * - ``http://yourhost/my/uri/foo``
      - No
      - Matches literal "/my/uri/" but is missing a segment for ``label2``.
    * - ``http://yourhost/my/uri``
      - No
      - Matches literal "/my/uri/" but is missing segments for ``label1`` and
        ``label2``.
    * - ``http://yourhost/my/uri/foo/bar/baz``
      - No
      - Matches literal "/my/uri/", "bar" is captured as ``label1``, and "baz"
        is captured as ``label2``, but contains an additional segment "baz".


Query string literals
~~~~~~~~~~~~~~~~~~~~~

Components of the query string can be matched literally in the URI pattern.
The query string portion of a pattern MUST NOT contain labels.

Literals can be in the form of required keys without values. Given a pattern
of ``/path?requiredKey`` and an endpoint of ``http://yourhost``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``http://yourhost/path?requiredKey``
      - Yes
      - Matches literal "/path" and contains a "requiredKey" query string
        parameter.
    * - ``http://yourhost/path?other&requiredKey``
      - Yes
      - Matches literal "/path" and contains a "requiredKey" query string
        parameter.
    * - ``http://yourhost/path``
      - No
      - Matches literal "/path" but does not contain the "requiredKey" query
        string parameter.
    * - ``http://yourhost/path?``
      - No
      - Matches literal "/path" but does not contain the "requiredKey" query
        string parameter.
    * - ``http://yourhost/path?otherKey``
      - No
      - Matches literal "/path" but does not contain the "requiredKey" query
        string parameter.

Literal query string parameters can be matched with required key-value pairs.
Given a pattern of ``/path?requiredKey=requiredValue`` and an endpoint of
``http://yourhost``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``http://yourhost/path?requiredKey=requiredValue``
      - Yes
      - Matches literal "/path" and contains a query string parameter named
        "requiredKey" with a value of "requiredValue".
    * - ``http://yourhost/path?other&requiredKey=requiredValue``
      - Yes
      - Matches literal "/path" and contains a query string parameter named
        "requiredKey" with a value of "requiredValue". "other" is disregarded
        or bound to another input member.
    * - ``http://yourhost/path``
      - No
      - Does not contain a query string parameter named "requiredValue".
    * - ``http://yourhost/path?``
      - No
      - Does not contain a query string parameter named "requiredValue".
    * - ``http://yourhost/path?requiredKey=otherValue``
      - No
      - Contains a query string parameter named "requiredValue" but its value
        is not "requiredValue" .


.. _greedy-labels:

Greedy labels
~~~~~~~~~~~~~

A :dfn:`greedy label` is a label suffixed with the ``+`` qualifier that can be
used to match more than one path segment. At most, one greedy label may exist
in any path pattern, and if present, it MUST be the last label in the pattern.
Greedy labels MUST be bound to a string shape.

Given a pattern of ``/my/uri/{label+}`` and an endpoint of ``http://yourhost``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``http://yourhost/my/uri/foo/bar``
      - Yes
      - Matches literal "/my/uri/", and "foo/bar" is captured as ``label``.
    * - ``http://yourhost/my/uri/bar/baz/``
      - Yes
      - Matches literal "/my/uri/", and "bar/baz" is captured as ``label``.
    * - ``http://yourhost/my/uri/foo/bar/baz``
      - Yes
      - Matches literal "/my/uri/", and "foo/bar/baz" is captured as ``label``.
    * - ``http://yourhost/my/uri``
      - No
      - Matches literal "/my/uri/" but does not contain a segment to match
        ``label``.

Greedy matching can be used to capture the whole URI to a label, which results
in every request for a particular HTTP method being captured. For example, this
can be modeled with a pattern of ``/{label+}``.

Segments in the middle of a URI can be captured using greedy labels. Given a
pattern of ``/prefix/{label+}/suffix`` and an endpoint of ``https://yourhost``:

.. list-table::
    :header-rows: 1
    :widths: 40 10 50

    * - Request URI
      - Matches?
      - Reason
    * - ``http://yourhost/prefix/foo/suffix``
      - Yes
      - Matches literal "/prefix", captures "foo" in greedy ``label``, and
        matches literal "/suffix".
    * - ``http://yourhost/prefix/foo/bar/suffix``
      - Yes
      - Matches literal "/prefix", captures "foo/bar" in greedy ``label``, and
        matches literal "/suffix".
    * - ``http://yourhost/prefix/foo/bar``
      - No
      - Matches literal "/prefix", but does not contain the trailing literal
        "/suffix".
    * - ``http://yourhost/foo/bar/suffix``
      - No
      - Does not match the literal "/prefix".


Pattern Validation and Conflict Avoidance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Smithy validates the patterns within a service against each other to ensure
that no two patterns conflict with each other for the same HTTP method. To
prevent ambiguity when matching requests for different operations, the
following rules are in place:

#. All labels MUST be delimited by '/' characters.

   - ``/{foo}/{bar}`` is legal
   - ``/{foo}{bar}`` is illegal
   - ``/{foo}bar/{bar}`` is illegal
   - ``/{foo}a{bar}`` is illegal

#. At most, one greedy label MAY exist per pattern.

   - ``/{foo}/{bar+}`` is legal
   - ``/{foo+}/{bar+}`` is illegal

#. If present, a greedy pattern MUST be the last label in a pattern.

   - ``/{foo}/{bar+}`` is legal
   - ``/{foo+}/{bar}`` is illegal

#. Patterns MUST NOT be equivalent.

   - Pattern ``/foo/bar`` and ``/foo/bar`` conflict.
   - Pattern ``/foo/{bar}`` and ``/foo/{baz}`` conflict regardless of any
     constraint traits on the label members.

#. A label and a literal SHOULD NOT both occupy the same segment in patterns
   which are equivalent to that point.

   - ``/foo/bar/{baz}`` and ``/foo/baz/bam`` can coexist.
   - ``/foo/bar`` and ``/foo/{baz}/bam`` cannot coexist unless pattern
     traits prevent ``{baz}`` from evaluating to ``bar`` because the label
     occupies the same segment of another pattern with the same prefix.

#. A query string literal with no value and a query string literal with an
   empty value are considered equivalent. For example, ``/foo?baz`` and
   ``/foo?baz=`` are considered the same route.


.. _httpError-trait:

``httpError`` trait
===================

Summary
    Defines an HTTP response code for an operation error.
Trait selector
    .. code-block:: none

        structure[trait|error]

    *Structure shapes that also have the error trait*
Value type
    ``integer`` value (e.g., ``404``).

The ``httpError`` trait can only be applied to structures that also have the
:ref:`error-trait`.

By default, error structures with no ``httpError`` trait use the default
HTTP status code of the :ref:`error-trait` value. The ``httpError``
trait can be used to set a custom HTTP response status code.


.. tabs::

    .. code-tab:: smithy

        @error("client")
        @httpError(404)
        structure MyError {}


.. _httpHeader-trait:

``httpHeader`` trait
====================

Summary
    Binds a structure member to an HTTP header.
Trait selector
    .. code-block:: none

        structure > :test(member > :test(boolean, number, string, timestamp,
                collection > member > :test(boolean, number, string, timestamp)))

    *Structure members that target boolean, number, string, or timestamp; or a structure member that targets a list/set of these types*
Value type
    ``string`` value defining the field name of the HTTP header. The value
    MUST NOT be empty and MUST be case-insensitively unique across all other
    members of the structure.
Conflicts with
   :ref:`httpLabel-trait`,
   :ref:`httpQuery-trait`,
   :ref:`httpPrefixHeaders-trait`,
   :ref:`httpPayload-trait`

Serialization rules:

* The header field name MUST be compatible with :rfc:`7230#section-3.2`.
* When a :ref:`list` shape is targeted, each member of the shape is serialized
  as a separate HTTP header either by concatenating the values with a comma on a
  single line or by serializing each header value on its own line.
* boolean values are serialized as ``true`` or ``false``.
* string values with a :ref:`mediaType-trait` are base64 encoded.
* timestamp values are serialized using the ``http-date``
  format as defined in the ``IMF-fixdate`` production of
  :rfc:`7231#section-7.1.1.1`. The :ref:`timestampFormat-trait` MAY be used
  to use a custom serialization format.

.. note::

    While there is no limit placed on the length of an HTTP header field, many
    HTTP client and server implementations enforce limits in practice.
    Smithy models SHOULD carefully consider the maximum allowed length of each
    member that is bound to an HTTP header.


.. _restricted-headers:

Restricted HTTP headers
-----------------------

Various HTTP headers are highly discouraged for the ``httpHeader`` and
``httpPrefixHeaders`` traits.

.. list-table::
    :header-rows: 1
    :widths: 25 75

    * - Header
      - Reason
    * - Authorization
      - This header should be populated by
        :ref:`authentication traits <authDefinition-trait>`.
    * - Connection
      - This is controlled at a lower level by the HTTP client or server.
    * - Content-Length
      - HTTP clients and servers are responsible for providing a
        Content-Length header.
    * - Expect
      - This is controlled at a lower level by the HTTP client.
    * - Host
      - The Host header is controlled by the HTTP client, not the model.
    * - Max-Forwards
      - This is controlled at a lower level by the HTTP client.
    * - Proxy-Authenticate
      - This header should be populated by
        :ref:`authentication traits <authDefinition-trait>`.
    * - Server
      - The Server header is controlled by the HTTP server, not the model.
    * - TE
      - This is controlled at a lower level by the HTTP client and server.
    * - Trailer
      - This is controlled at a lower level by the HTTP client and server.
    * - Transfer-Encoding
      - This is controlled at a lower level by the HTTP client and server.
    * - Upgrade
      - This is controlled at a lower level by the HTTP server.
    * - User-Agent
      - Setting a User-Agent is the responsibility of an HTTP client.
    * - WWW-Authenticate
      - This header should be populated by
        :ref:`authentication traits <authDefinition-trait>`.
    * - X-Forwarded-For
      - X-Forwarded-For is an implementation detail of HTTP that does not
        need to be modeled.


.. _httpLabel-trait:

``httpLabel`` trait
===================

Summary
    Binds an operation input structure member to an HTTP label.
Trait selector
    .. code-block:: none

        structure > member[trait|required] :test(> :test(string, number, boolean, timestamp))

    *Required structure members that target a string, number, boolean, or timestamp*
Value type
    Annotation trait.
Conflicts with
    :ref:`httpHeader-trait`,
    :ref:`httpQuery-trait`,
    :ref:`httpPrefixHeaders-trait`,
    :ref:`httpPayload-trait`

``httpLabel`` members MUST be marked as :ref:`required-trait`.

When a structure is associated with an operation, any member of the structure
with the ``httpLabel`` trait MUST have a corresponding URI label with the same
name as the member. ``httpLabel`` traits are ignored when serializing the
output or an error of an operation.

``httpLabel`` traits can only be applied to structure members that are marked
as :ref:`required-trait`.

If the corresponding URI label in the operation is not greedy, then the
``httpLabel`` trait MUST target a string, byte, short, integer, long, float,
double, bigDecimal, bigInteger, boolean, or timestamp. If the
corresponding URI label in the operation is greedy, then the ``httpLabel``
trait MUST target a string shape.

Serialization rules:

- boolean is serialized as ``true`` or ``false``.
- timestamp values are serialized as an :rfc:`3339` string
  (e.g., ``1985-04-12T23:20:50.52Z``). The :ref:`timestampFormat-trait` MAY
  be used to use a custom serialization format.
- Unless the label is greedy, "/" MUST be percent encoded.


.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @readonly
        @http(method: "GET", uri: "/{foo}")
        operation GetStatus {
            input: GetStatusInput,
            output: GetStatusOutput
        }

        structure GetStatusInput {
            @required
            @httpLabel
            foo: String
        }


.. _httpPayload-trait:

``httpPayload`` trait
=====================

Summary
    Binds a single structure member to the body of an HTTP request.
Trait selector
    .. code-block:: none

        structure > :test(member > :test(string, blob, structure, union, document))

    *Structure members that target a string, blob, structure, union, or document*
Value type
    Annotation trait.
Conflicts with
    :ref:`httpLabel-trait`, :ref:`httpQuery-trait`,
    :ref:`httpHeader-trait`, :ref:`httpPrefixHeaders-trait`

By default, all structure members that are not bound as part of the HTTP
message are serialized in a protocol-specific document sent in the body of
the message (e.g., a JSON object). The ``httpPayload`` trait can be used to
bind a single top-level operation input, output, or error structure member to
the body of the HTTP message. Multiple members of the same structure MUST NOT
be bound to ``httpPayload``.

If the ``httpPayload`` trait is present on the structure referenced by the
input of an operation, then all other structure members MUST be bound with
the :ref:`httpLabel-trait`, :ref:`httpHeader-trait`,
:ref:`httpPrefixHeaders-trait`, or :ref:`httpQuery-trait`.

If the ``httpPayload`` trait is present on the structure referenced by the
output of an operation or a structure targeted by the :ref:`error-trait`,
then all other structure members MUST be bound to a :ref:`httpHeader-trait`
or :ref:`httpPrefixHeaders-trait`.

Serialization rules:

#. When a string or blob member is referenced, the raw value is serialized
   as the body of the message.
#. When a :ref:`structure <structure>`, :ref:`union <union>`, or
   document type is targeted, the shape value is serialized
   as a :ref:`protocol-specific <protocolDefinition-trait>` document that is
   sent as the body of the message.


.. _httpPrefixHeaders-trait:

``httpPrefixHeaders`` trait
===========================

Summary
    Binds a map of key-value pairs to prefixed HTTP headers.
Trait selector
    .. code-block:: none

        structure > member
        :test(> map > member[id|member=value] > :test(string, collection > member > string))

    *Structure member that targets a map of strings, or a map of list/set of strings*
Value type
    ``string`` value that defines the prefix to prepend to each header field
    name stored in the targeted map member. For example, given a prefix value
    of of "X-Amz-Meta-" and a map key entry of "Baz", the resulting header
    field name serialized in the message is "X-Amz-Meta-Baz".
Conflicts with
   :ref:`httpLabel-trait`, :ref:`httpQuery-trait`,
   :ref:`httpHeader-trait`, :ref:`httpPayload-trait`

In order to differentiate ``httpPrefixHeaders`` from other headers, when
``httpPrefixHeaders`` are used, no other :ref:`httpHeader-trait` bindings can
start with the same prefix provided in ``httpPrefixHeaders`` trait. If
``httpPrefixHeaders`` is set to an empty string, then no other members can be
bound to ``headers``.

Only a single structure member can be bound to ``httpPrefixHeaders``.

Given the following Smithy model:


.. tabs::

    .. code-tab:: smithy

        @readonly
        @http(method: "GET", uri: "/myOperation")
        operation MyOperation {
            input: MyOperationInput
        }

        structure MyOperationInput {
            @httpPrefixHeaders("X-Foo-")
            headers: StringMap
        }

        map StringMap {
            key: String,
            value: String
        }

And given the following input to ``MyOperation``:

.. code-block:: json

    {
        "headers": {
            "first": "hi",
            "second": "there"
        }
    }

An example HTTP request would be serialized as:

::

    GET /myOperation
    Host: <server>
    X-Foo-first: hi
    X-Foo-second: there


.. _httpQuery-trait:

``httpQuery`` trait
===================

Summary
    Binds an operation input structure member to a query string parameter.
Trait selector
    .. code-block:: none

        structure > member
        :test(> :test(string, number, boolean, timestamp),
              > collection > member > :test(string, number, boolean, timestamp))

    *Structure members that target string, number, boolean, or timestamp; or a list/set of said types*
Value type
    ``string`` value defining the name of the query string parameter. The
    query string value MUST NOT be empty. This trait is ignored when
    resolving the HTTP bindings of an operation's output or an error.
Conflicts with
   :ref:`httpLabel-trait`, :ref:`httpHeader-trait`,
   :ref:`httpPrefixHeaders-trait`, :ref:`httpPayload-trait`

Serialization rules:

* "&" is used to separate query string parameters.
* "=" is used to separate query string parameter names from values.
* Query string keys and values MUST be percent-encoded_ so that they conform to
  the ``query`` grammar defined in :rfc:`3986#section-3.4`. Characters that are
  valid as part of the query string MUST NOT be percent encoded. For example,
  a value of ``foo/baz%20`` serialized in a query string would become
  ``foo/baz%2520``. However, ``&`` MUST be percent-encoded when present in a
  query string value.
* Multiple members of a structure MUST NOT case-sensitively target the same
  query string parameter.
* boolean values are serialized as ``true`` or ``false``.
* timestamp values are serialized as an :rfc:`3339`
  ``date-time`` string (e.g., ``1985-04-12T23:20:50.52Z``). The
  :ref:`timestampFormat-trait` MAY be used to use a custom serialization
  format.
* :ref:`list` members are serialized by adding multiple query string parameters
  to the query string using the same name. For example, given a member bound
  to ``foo`` that targets a list of strings with a value of ``["a", "b"]``,
  the value is serialized in the query string as ``foo=a&foo=b``.

.. note::

    While there is no limit placed on the length of an `HTTP request line`_,
    many HTTP client and server implementations enforce limits in practice.
    Smithy models SHOULD carefully consider the maximum allowed length of each
    member that is bound to an HTTP query string or path.

.. _httpResponseCode-trait:

``httpResponseCode`` trait
==========================

Summary
    Indicates that the structure member represents an HTTP response
    status code.
Trait selector
    ``structure > member :test(> integer)``
Value type
    Annotation trait.

The value MAY differ from the HTTP status code provided on the response.
Explicitly modeling this as a field can be helpful for services that wish to
provide different response codes for an operation, like 200 or 201 for a PUT
operation.

.. _cors-trait:

``cors`` trait
==============

Summary
    Defines how a service supports cross-origin resource sharing
Trait selector
    ``service``
Value type
    ``structure``

The ``cors`` trait is a structure that supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - origin
      - ``string``
      - The origin from which browser script-originating requests will be
        allowed. Defaults to ``*``.
    * - maxAge
      - ``integer``
      - The maximum number of seconds for which browsers are allowed to cache
        the results of a preflight ``OPTIONS`` request. Defaults to ``600``, the
        maximum age permitted by several browsers. Set to ``-1`` to disable
        caching entirely.
    * - additionalAllowedHeaders
      - ``list<string>``
      - The names of headers that should be included in the
        ``Access-Control-Allow-Headers`` header in responses to preflight
        ``OPTIONS`` requests. This list will be used in addition to the names of
        all request headers bound to an input data member via the
        :ref:`httpHeader-trait`, as well as any headers required by the protocol
        or authentication scheme.
    * - additionalExposedHeaders
      - ``list<string>``
      - The names of headers that should be included in the
        ``Access-Control-Expose-Headers`` header in all responses sent by the
        service. This list will be used in addition to the names of all request
        headers bound to an output data member via the :ref:`httpHeader-trait`,
        as well as any headers required by the protocol or authentication
        scheme.

Adding a ``cors`` trait with its value set to an empty object enables
cross-origin resource sharing for all origins and allows browser scripts access
to all headers to which data is bound in the model, as well as any headers used
by the protocol and authentication scheme.

The default settings are not compatible with certain authentication schemes
(e.g., ``http-basic``) that rely on browser-managed credentials. Services using
such authentication schemes MUST designate a single origin from which
cross-origin, credentialed requests will be accepted.


Serializing HTTP messages
=========================

The following steps are taken to serialize an HTTP request given a map of
parameters:

1. Set the HTTP method to the ``method`` property of the :ref:`http-trait`
   of the operation.
2. Set the URI of the HTTP request to the ``uri`` property of the ``http``
   trait.
3. Iterate over all of the key-value pairs of the parameters and find the
   corresponding structure member by name:

   1. If the member has the ``httpLabel`` trait, expand the value into the URI.
   2. If the member has the ``httpQuery`` trait, serialize the value into the
      HTTP request as a query string parameter.
   3. If the member has the ``httpHeader`` trait, serialize the value in an
      HTTP header using the value of the ``httpHeader`` trait.
   4. If the member has the ``httpPrefixHeaders`` trait and the value is a map,
      serialize the map key value pairs as prefixed HTTP headers.
   5. If the member has the ``httpPayload`` trait, serialize the value as the
      body of the request.
   6. If the member has no bindings, serialize the key-value pair as part of a
      protocol-specific document sent in the body of the request.

The following steps are taken to serialize an HTTP response given a map of
parameters:

1. If serializing the output of an operation, set the status code of the
   response to the ``code`` property of the :ref:`http-trait`.
2. If serializing an error and the the :ref:`httpError-trait` is present,
   set the status code of the response to its value. Otherwise, set the status
   code to 400 if the error trait is "client" or to 500 if the error trait is
   "server".
3. Iterate over all of the key-value pairs of the parameters and find the
   corresponding structure member by name:

   1. If the member has the ``httpHeader`` trait, serialize the value in an
      HTTP header using the value of the ``httpHeader`` trait.
   2. If the member has the ``httpPrefixHeaders`` trait and the value is a map,
      serialize the map key value pairs as prefixed HTTP headers.
   3. If the member has the ``httpPayload`` trait, serialize the value as the
      body of the response.
   4. If the member has no bindings, serialize the key-value pair as part of a
      protocol-specific document sent in the body of the response.


Event streams
=============

When using :ref:`event streams <event-streams>` and HTTP bindings, the
:ref:`httpPayload <httppayload-trait>` trait MUST be applied to any input or
output member that targets a shape marked with the :ref:`streaming-trait`.

The following example defines an operation that uses an input event stream
and HTTP bindings:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @http(method: "POST", uri: "/messages")
        operation PublishMessages {
            input: PublishMessagesInput
        }

        structure PublishMessagesInput {
            @httpPayload
            messages: MessageStream,
        }

        @streaming
        union MessageStream {
            message: Message,
        }

        structure Message {
            message: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#PublishMessages": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PublishMessagesInput"
                    },
                    "traits": {
                        "smithy.api#http": {
                            "uri": "/messages",
                            "method": "POST"
                        }
                    }
                },
                "smithy.example#PublishMessagesInput": {
                    "type": "structure",
                    "members": {
                        "messages": {
                            "target": "smithy.example#MessageStream",
                            "traits": {
                                "smithy.api#httpPayload": {}
                            }
                        }
                    }
                },
                "smithy.example#MessageStream": {
                    "type": "union",
                    "members": {
                        "message": {
                            "target": "smithy.example#Message"
                        }
                    }
                },
                "smithy.example#Message": {
                    "type": "structure",
                    "members": {
                        "message": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
        }

The following is **invalid** because the operation has the ``http`` trait
and an input member is marked with the ``streaming`` trait but not
marked with the ``httpPayload`` trait:

.. code-block:: smithy

    namespace smithy.example

    @http(method: "POST", uri: "/messages")
    operation InvalidOperation {
        input: InvalidOperationInput
    }

    structure InvalidOperationInput {
        invalid: MessageStream, // <-- Missing the @httpPayload trait
    }

    @streaming
    union MessageStream {
        message: Message,
    }

    structure Message {
        message: String,
    }


.. _percent-encoded: https://tools.ietf.org/html/rfc3986#section-2.1
.. _HTTP request line: https://tools.ietf.org/html/rfc7230.html#section-3.1.1
