.. _http-traits:

===================
HTTP binding traits
===================

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
            key: ObjectKey

            // Sent in the URI label named "bucketName".
            @required
            @httpLabel
            bucketName: String

            // Sent in the X-Foo header
            @httpHeader("X-Foo")
            foo: String

            // Sent in the query string as paramName
            @httpQuery("paramName")
            someValue: String

            // Sent in the body
            data: MyBlob

            // Sent in the body
            additional: String
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

.. tabs::

    .. code-tab:: smithy

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


.. _http-uri-label:

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

#. Patterns MUST NOT be equivalent if they share a host.

   - Pattern ``/foo/bar`` and ``/foo/bar`` conflict.
   - Pattern ``/foo/{bar}`` and ``/foo/{baz}`` conflict regardless of any
     constraint traits on the label members.

#. A label and a literal SHOULD NOT both occupy the same segment in patterns
   which are equivalent to that point if they share a host.

   - ``/foo/bar/{baz}`` and ``/foo/baz/bam`` can coexist.
   - ``/foo/bar`` and ``/foo/{baz}/bam`` cannot coexist unless pattern
     traits prevent ``{baz}`` from evaluating to ``bar`` because the label
     occupies the same segment of another pattern with the same prefix.

#. A query string literal with no value and a query string literal with an
   empty value are considered equivalent. For example, ``/foo?baz`` and
   ``/foo?baz=`` are considered the same route.

#. Patterns MAY conflict if the operations use different hosts. Different hosts
   can be configured using the :ref:`endpoint-trait`'s ``hostPrefix`` property.

   - ``/foo/bar`` and ``/foo/{baz}/bam`` can coexist if one operation has no
     endpoint trait and the other specifies ``foo.`` as the ``hostPrefix``.
   - ``/foo/bar`` and ``/foo/{baz}/bam`` can coexist if one operation specifies
     ``foo.`` as the ``hostPrefix`` and the other specifies ``bar.`` as the
     ``hostPrefix``.


.. _httpError-trait:

``httpError`` trait
===================

Summary
    Defines an HTTP response code for an operation error.
Trait selector
    .. code-block:: none

        structure[trait|error]

    The ``httpError`` trait can only be applied to :ref:`structure <structure>`
    shapes that also have the :ref:`error-trait`.
Value type
    ``integer`` value representing the HTTP response status code
    (for example, ``404``).

The following example defines an error with an HTTP status code of ``404``.

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @httpError(404)
        structure MyError {}

.. rubric:: Default HTTP status codes

The ``httpError`` trait is used to set a *custom* HTTP response status code.
By default, error structures with no ``httpError`` trait use the default
HTTP status code of the :ref:`error-trait`.

* ``400`` is used for "client" errors
* ``500`` is used for "server" errors


.. _httpHeader-trait:

``httpHeader`` trait
====================

Summary
    Binds a structure member to an HTTP header.
Trait selector
    .. code-block:: none

        structure > :test(member > :test(boolean, number, string, timestamp,
                collection > member > :test(boolean, number, string, timestamp)))

    The ``httpHeader`` trait can be applied to ``structure`` members that
    target a ``boolean``, ``number``, ``string``, or ``timestamp``; or a
    ``structure`` member that targets a list/set of these types.
Value type
    ``string`` value defining a valid HTTP header field name according to
    :rfc:`section 3.2 of RFC7230 <7230#section-3.2>`. The value MUST NOT be
    empty and MUST be case-insensitively unique across all other members of
    the structure.
Conflicts with
   :ref:`httpLabel-trait`,
   :ref:`httpQuery-trait`,
   :ref:`httpQueryParams-trait`,
   :ref:`httpPrefixHeaders-trait`,
   :ref:`httpPayload-trait`,
   :ref:`httpResponseCode-trait`

.. rubric:: ``httpHeader`` serialization rules:

* When a :ref:`list <list>` shape is targeted, each member of the shape is
  serialized as a separate HTTP header either by concatenating the values
  with a comma on a single line or by serializing each header value on its
  own line.
* ``boolean`` values are serialized as ``true`` or ``false``.
* ``string`` values with a :ref:`mediaType-trait` are always base64 encoded.
* ``timestamp`` values are serialized using the ``http-date``
  format by default, as defined in the ``IMF-fixdate`` production of
  :rfc:`7231#section-7.1.1.1`. The :ref:`timestampFormat-trait` MAY be used
  to use a custom serialization format.

.. rubric:: Do not put too much data in HTTP headers

While there is no limit placed on the length of an HTTP header field, many
HTTP client and server implementations enforce limits in practice.
Carefully consider the maximum allowed length of each member that is bound
to an HTTP header.


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
    Binds an operation input structure member to an
    :ref:`HTTP label <http-uri-label>` so that it is used as part of an
    HTTP request URI.
Trait selector
    .. code-block:: none

        structure > member[trait|required] :test(> :test(string, number, boolean, timestamp))

    The ``httpLabel`` trait can be applied to ``structure`` members marked
    with the :ref:`required-trait` that target a ``string``, ``number``,
    ``boolean``, or ``timestamp``.
Value type
    Annotation trait.
Conflicts with
    :ref:`httpHeader-trait`,
    :ref:`httpQuery-trait`,
    :ref:`httpQueryParams-trait`,
    :ref:`httpPrefixHeaders-trait`,
    :ref:`httpPayload-trait`,
    :ref:`httpResponseCode-trait`

The following example defines an operation that send an HTTP label named
``foo`` as part of the URI of an HTTP request:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @readonly
        @http(method: "GET", uri: "/{foo}")
        operation GetStatus {
            input: GetStatusInput
            output: GetStatusOutput
        }

        structure GetStatusInput {
            @required
            @httpLabel
            foo: String
        }

.. rubric:: Relationship to :ref:`http-trait`

When a structure is used as the input of an operation, any member of the
structure with the ``httpLabel`` trait MUST have a corresponding
:ref:`URI label <http-uri-label>` with the same name as the member.
``httpLabel`` traits are ignored when serializing operation output or errors.

.. rubric:: Applying the ``httpLabel`` trait to members

* ``httpLabel`` can only be applied to structure members that are marked as
  :ref:`required <required-trait>`.
* If the corresponding URI label in the operation is not greedy, then the
  ``httpLabel`` trait MUST target a member that targets a ``string``,
  ``byte``, ``short``, ``integer``, ``long``, ``float``, ``double``,
  ``bigDecimal``, ``bigInteger``, ``boolean``, or ``timestamp``.
* If the corresponding URI label in the operation is greedy, then the
  ``httpLabel`` trait MUST target a member that targets a ``string`` shape.

.. rubric:: ``httpLabel`` serialization rules:

- ``boolean`` values are serialized as ``true`` or ``false``.
- ``timestamp`` values are serialized as an :rfc:`3339` string by default
  (for example, ``1985-04-12T23:20:50.52Z``, and with percent-encoding,
  ``1985-04-12T23%3A20%3A50.52Z``). The :ref:`timestampFormat-trait`
  MAY be used to use a custom serialization format.
- Reserved characters defined in :rfc:`section 2.2 of RFC3986 <3986#section-2.2>`
  and the `%` itself MUST be percent-encoded_ (that is, ``:/?#[]@!$&'()*+,;=%``).
- However, if the label is greedy, then "/" MUST NOT be percent-encoded
  because greedy labels are meant to span multiple path segments.

.. rubric:: ``httpLabel`` is only used on input

``httpLabel`` is ignored when resolving the HTTP bindings of an operation's
output or an error. This means that if a structure that contains members
marked with the ``httpLabel`` trait is used as the top-level output structure
of an operation, then those members are sent as part of the
:ref:`protocol-specific document <http-protocol-document-payloads>` sent in
the body of the response.


.. _httpPayload-trait:

``httpPayload`` trait
=====================

Summary
    Binds a single structure member to the body of an HTTP message.
Trait selector
    .. code-block:: none

        structure > :test(member > :test(string, blob, structure, union, document, list, set, map))

    The ``httpPayload`` trait can be applied to ``structure`` members that
    target a ``string``, ``blob``, ``structure``, ``union``, ``document``,
    ``set``, ``map``, or ``list``.
Value type
    Annotation trait.
Conflicts with
    :ref:`httpLabel-trait`, :ref:`httpQuery-trait`, :ref:`httpQueryParams-trait`,
    :ref:`httpHeader-trait`, :ref:`httpPrefixHeaders-trait`,
    :ref:`httpResponseCode-trait`
Structurally exclusive
    Only a single structure member can be bound to ``httpPayload``.

The following example defines an operation that returns a ``blob`` of binary
data in a response:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @readonly
        @http(method: "GET", uri: "/random-binary-data")
        operation GetRandomBinaryData {
            output: GetRandomBinaryDataOutput
        }

        structure GetRandomBinaryDataOutput {
            @required
            @httpHeader("Content-Type")
            contentType: String

            @httpPayload
            content: Blob
        }

.. _http-protocol-document-payloads:

.. rubric:: Protocol-specific document payloads

By default, all structure members that are not bound as part of the HTTP
message are serialized in a protocol-specific document sent in the body of
the message (e.g., a JSON object). The ``httpPayload`` trait can be used to
bind a single top-level operation input, output, or error structure member to
the body of the HTTP message. Multiple members of the same structure MUST NOT
be bound to ``httpPayload``.

.. rubric:: Binding members to ``httpPayload``

If the ``httpPayload`` trait is present on the structure referenced by the
input of an operation, then all other structure members MUST be bound with
the :ref:`httpLabel-trait`, :ref:`httpHeader-trait`,
:ref:`httpPrefixHeaders-trait`, :ref:`httpQueryParams-trait`, or :ref:`httpQuery-trait`.

If the ``httpPayload`` trait is present on the structure referenced by the
output of an operation or a structure targeted by the :ref:`error-trait`,
then all other structure members MUST be bound to a :ref:`httpHeader-trait`
or :ref:`httpPrefixHeaders-trait`.

.. rubric:: Serialization rules

#. When a string or blob member is referenced, the raw value is serialized
   as the body of the message.
#. When a :ref:`structure <structure>`, :ref:`union <union>`, :ref:`list <list>`,
   :ref:`set <set>`, :ref:`map <map>`, or document type is targeted,
   the shape value is serialized as a :ref:`protocol-specific <protocolDefinition-trait>`
   document that is sent as the body of the message.


.. _httpPrefixHeaders-trait:

``httpPrefixHeaders`` trait
===========================

Summary
    Binds a map of key-value pairs to prefixed HTTP headers.
Trait selector
    .. code-block:: none

        structure > member
        :test(> map > member[id|member=value] > :test(string, collection > member > string))

    The ``httpPrefixHeaders`` trait can be applied to ``structure`` members
    that target a ``map`` of ``string``, or a ``map`` of ``list``/``set`` of
    ``string``.
Value type
    ``string`` value that defines the prefix to prepend to each header field
    name stored in the targeted map member. For example, given a prefix value
    of of "X-Amz-Meta-" and a map key entry of "Baz", the resulting header
    field name serialized in the message is "X-Amz-Meta-Baz".
Conflicts with
   :ref:`httpLabel-trait`, :ref:`httpQuery-trait`, :ref:`httpQueryParams-trait`,
   :ref:`httpHeader-trait`, :ref:`httpPayload-trait`,
   :ref:`httpResponseCode-trait`
Structurally exclusive
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
            key: String
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

.. rubric:: Disambiguation of ``httpPrefixHeaders``

In order to differentiate ``httpPrefixHeaders`` from other headers, when
``httpPrefixHeaders`` are used, no other :ref:`httpHeader-trait` bindings can
start with the same prefix provided in ``httpPrefixHeaders`` trait. If
``httpPrefixHeaders`` is set to an empty string, then no other members can be
bound to ``headers``.


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

    The ``httpQuery`` trait can be applied to ``structure`` members that
    target a ``string``, ``number``, ``boolean``, or ``timestamp``; or a
    ``list``/``set`` of these types.
Value type
    A non-empty ``string`` value that defines the name of the query string
    parameter. The query string parameter name MUST be case-sensitively unique
    across all other members marked with the ``httpQuery`` trait.
Conflicts with
   :ref:`httpLabel-trait`, :ref:`httpHeader-trait`, :ref:`httpQueryParams-trait`,
   :ref:`httpPrefixHeaders-trait`, :ref:`httpPayload-trait`,
   :ref:`httpResponseCode-trait`

The following example defines an operation that optionally sends the
``color``, ``shape``, and ``size`` query string parameters in an HTTP
request:

.. tabs::

    .. code-tab:: smithy

        @readonly
        @http(method: "GET", uri: "/things")
        operation ListThings {
            input: ListThingsInput
            output: ListThingsOutput, // omitted for brevity
        }

        structure ListThingsInput {
            @httpQuery("color")
            color: String

            @httpQuery("shape")
            shape: String

            @httpQuery("size")
            size: Integer
        }

.. rubric:: Serialization rules

* "&" is used to separate query string parameter key-value pairs.
* "=" is used to separate query string parameter names from values.
* Reserved characters in keys and values as defined in :rfc:`section 2.2 of RFC3986 <3986#section-2.2>` and `%`
  MUST be percent-encoded_ (that is, ``:/?#[]@!$&'()*+,;=%``).
* ``boolean`` values are serialized as ``true`` or ``false``.
* ``timestamp`` values are serialized as an :rfc:`3339`
  ``date-time`` string by default (for example, ``1985-04-12T23:20:50.52Z``,
  and with percent-encoding, ``1985-04-12T23%3A20%3A50.52Z``). The
  :ref:`timestampFormat-trait` MAY be used to use a custom serialization
  format.
* :ref:`list` and :ref:`set <set>` members are serialized by adding multiple
  query string parameters to the query string using the same name. For
  example, given a member bound to ``foo`` that targets a list of strings
  with a value of ``["a", "b"]``, the value is serialized in the query string
  as ``foo=a&foo=b``.

.. important:: Percent-encoding is an implementation detail

    The encoding and serialization rules of shapes defined in a Smithy model are
    implementation details. When designing clients, servers, and other kinds of
    software based on Smithy models, the format in which the value of a member
    is serialized SHOULD NOT be a concern of the end-user. As such, members bound
    to the query string MUST be automatically percent-encoded when serializing
    HTTP requests and automatically percent-decoded when deserializing HTTP
    requests.

.. rubric:: ``httpQuery`` is only used on input

``httpQuery`` is ignored when resolving the HTTP bindings of an operation's
output or an error. This means that if a structure that contains members
marked with the ``httpQuery`` trait is used as the top-level output structure
of an operation, then those members are sent as part of the
:ref:`protocol-specific document <http-protocol-document-payloads>` sent in
the body of the response.

.. rubric:: Do not put too much data in the query string

While there is no limit placed on the length of an `HTTP request line`_,
many HTTP client and server implementations enforce limits in practice.
Carefully consider the maximum allowed length of each member that is bound to
an HTTP query string or path.

.. _httpQueryParams-trait:

``httpQueryParams`` trait
=========================

Summary
    Binds a map of key-value pairs to query string parameters.
Trait selector
    .. code-block:: none

        structure > member
        :test(> map > member[id|member=value] > :test(string, collection > member > string))

    The ``httpQueryParams`` trait can be applied to ``structure`` members
    that target a ``map`` of ``string``, or a ``map`` of ``list``/``set`` of
    ``string``.

Value type
    Annotation trait.
Conflicts with
   :ref:`httpLabel-trait`, :ref:`httpHeader-trait`, :ref:`httpQuery-trait`,
   :ref:`httpPrefixHeaders-trait`, :ref:`httpPayload-trait`,
   :ref:`httpResponseCode-trait`

The following example defines an operation that optionally sends the
target input map as query string parameters in an HTTP request:

.. tabs::

    .. code-tab:: smithy

        @readonly
        @http(method: "GET", uri: "/things")
        operation ListThings {
            input: ListThingsInput
            output: ListThingsOutput, // omitted for brevity
        }

        structure ListThingsInput {
            @httpQueryParams()
            myParams: MapOfStrings
        }

        map MapOfStrings {
            key: String
            value: String
        }

.. rubric:: ``httpQueryParams`` is only used on input

``httpQueryParams`` is ignored when resolving the HTTP bindings of an operation's
output or an error. This means that if a structure that contains members
marked with the ``httpQueryParams`` trait is used as the top-level output structure
of an operation, then those members are sent as part of the
:ref:`protocol-specific document <http-protocol-document-payloads>` sent in
the body of the response.

.. rubric:: Serialization rules

See the :ref:`httpQuery-trait` serialization rules that define how the keys and values of the
target map will be serialized in the request query string. Key-value pairs in the target map
are treated like they were explicitly bound using the :ref:`httpQuery-trait`, including the
requirement that reserved characters MUST be percent-encoded_.

If a member with the ``httpQueryParams`` trait and a member with the :ref:`httpQuery-trait`
conflict, clients MUST use the value set by the member with the :ref:`httpQuery-trait` and
disregard the value set by ``httpQueryParams``. For example, given the following model:

.. tabs::

    .. code-tab:: smithy

        @http(method: "POST", uri: "/things")
        operation PutThing {
            input: PutThingInput
        }

        structure PutThingInput {
            @httpQuery
            @required
            thingId: String,

            @httpQueryParams
            tags: MapOfStrings
        }

        map MapOfStrings {
            key: String,
            value: String
        }

And given the following input to ``PutThing``:

.. code-block:: json

    {
        "thingId": "realId",
        "tags": {
            "thingId": "fakeId",
            "otherTag": "value"
        }
    }

An example HTTP request would be serialized as:

::

    POST /things?thingId=realId&otherTag=value
    Host: <server>

.. _httpResponseCode-trait:

``httpResponseCode`` trait
==========================

Summary
    Binds a structure member to the HTTP response status code so that an
    HTTP response status code can be set dynamically at runtime to something
    other than ``code`` of the :ref:`http-trait`.
Trait selector
    .. code-block:: none

        structure > member :test(> integer)

    The ``httpResponseCode`` trait can be applied to ``structure`` members
    that target an ``integer``.
Value type
    Annotation trait.
Conflicts with
   :ref:`httpLabel-trait`, :ref:`httpHeader-trait`,
   :ref:`httpPrefixHeaders-trait`, :ref:`httpPayload-trait`,
   :ref:`httpQuery-trait`, :ref:`httpQueryParams-trait`,

.. rubric:: ``httpResponseCode`` use cases

Marking an output ``structure`` member with this trait can be used to provide
different response codes for an operation, like a 200 or 201 for a PUT
operation.

.. rubric:: ``httpResponseCode`` is only used on output

``httpResponseCode`` is ignored when resolving the HTTP bindings of an
operation's input structure. This means that if a structure that contains
members marked with the ``httpResponseCode`` trait is used as the top-level
input structure of an operation, then those members are sent as part of the
:ref:`protocol-specific document <http-protocol-document-payloads>` sent in
the body of the request.

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
   3. If the member has the ``httpQueryParams`` trait, serialize the values into
      the HTTP request as query string parameters.
   4. If the member has the ``httpHeader`` trait, serialize the value in an
      HTTP header using the value of the ``httpHeader`` trait.
   5. If the member has the ``httpPrefixHeaders`` trait and the value is a map,
      serialize the map key value pairs as prefixed HTTP headers.
   6. If the member has the ``httpPayload`` trait, serialize the value as the
      body of the request.
   7. If the member has no bindings, serialize the key-value pair as part of a
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
            messages: MessageStream
        }

        @streaming
        union MessageStream {
            message: Message
        }

        structure Message {
            message: String
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
        message: Message
    }

    structure Message {
        message: String
    }


.. _percent-encoded: https://tools.ietf.org/html/rfc3986#section-2.1
.. _HTTP request line: https://tools.ietf.org/html/rfc7230.html#section-3.1.1
