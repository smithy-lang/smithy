.. _aws-query-protocol:

==================
AWS query protocol
==================

This specification defines the ``aws.protocols#awsQuery`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#awsQuery-trait:

--------------------------------
``aws.protocols#awsQuery`` trait
--------------------------------

Summary
    Adds support for an HTTP protocol that sends "POST" requests in the body
    as ``x-www-form-urlencoded`` strings and responses in XML documents.
Trait selector
    ``service [trait|xmlNamespace]``

    *Service shapes with the xmlNamespace trait*
Value type
    Annotation trait.
See
    `Protocol tests <https://github.com/awslabs/smithy/tree/__smithy_version__/smithy-aws-protocol-tests/model/awsQuery>`_

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#awsQuery

        @awsQuery
        service MyService {
            version: "2020-02-05"
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-02-05",
                    "traits": {
                        "aws.protocols#awsQuery": {}
                    }
                }
            }
        }

----------------
Supported traits
----------------

The ``aws.protocols#awsQuery`` protocol supports the following traits
that affect serialization:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Trait
      - Description
    * - :ref:`cors <cors-trait>`
      - Indicates that the service supports CORS.
    * - :ref:`xmlAttrubute <xmlAttribute-trait>`
      - Serializes an object property as an XML attribute rather than a nested
        XML element.
    * - :ref:`xmlFlattened <xmlFlattened-trait>`
      - By default, entries in lists, sets, and maps have values serialized in
        nested elements specific to their type. The ``xmlFlattened`` trait
        unwraps these elements into the containing structure.
    * - :ref:`xmlName <xmlName-trait>`
      - By default, the XML element names and form-urlencoded key segments used
        in serialized structures are the same as a structure member name. The
        ``xmlName`` trait changes the XML element name to a custom value.
    * - :ref:`xmlNamespace <xmlNamespace-trait>`
      - Adds an xmlns namespace definition URI to XML element(s) generated
        for the targeted shape.
    * - :ref:`timestampFormat <timestampFormat-trait>`
      - Defines a custom timestamp serialization format.

.. important::

    This protocol does not support document types.


-----------------
Protocol Behavior
-----------------

Every request for the ``awsQuery`` protocol MUST be sent to the
root URL (``/``) using the HTTP "POST" method.

The ``awsQuery`` protocol does not support custom HTTP bindings.
:ref:`HTTP binding traits <http-traits>` MUST be ignored if they are present.

The ``awsQuery`` protocol uses the following headers:

.. list-table::
    :header-rows: 1
    :widths: 20 20 80

    * - Header
      - Required
      - Description
    * - ``Content-Type``
      - true
      - This header uses the static values of ``application/x-www-form-urlencoded``
        for requests and ``text/xml`` for responses.
    * - ``Content-Length``
      - true
      - The standard ``Content-Length`` header defined by
        `RFC 7230 Section 3.3.2`_.


---------------------
Request serialization
---------------------

The ``awsQuery`` protocol serializes inputs in ``x-www-form-urlencoded``
request bodies. All keys and values MUST be encoded according to :rfc:`3986`.
Requests MUST have the following key value pairs added to the inputs in the
serialized body:

.. list-table::
    :header-rows: 1
    :widths: 30 70

    * - Key
      - Value
    * - ``Action``
      - The name of the operation.
    * - ``Version``
      - The value of the :ref:`"version" property of the service <service>`.

These, along with other input members, are serialized in the request body,
concatenated with the following rules:

* "&" is used to separate parameter key-value pairs.
* "=" is used to separate parameter names from values.
* "." is used to separate nested parameter name segments.

x-www-form-urlencoded shape serialization
-----------------------------------------

Simple shapes are serialized according to the following rules:

.. list-table::
    :header-rows: 1
    :widths: 25 75

    * - Smithy type
      - Request entity
    * - ``blob``
      - Text value that is base64 encoded.
    * - ``boolean``
      - Text value of either "true" or "false".
    * - ``byte``
      - Text value of the number.
    * - ``short``
      - Text value of the number.
    * - ``integer``
      - Text value of the number.
    * - ``long``
      - Text value of the number.
    * - ``float``
      - Text value of the number.
    * - ``double``
      - Text value of the number.
    * - ``bigDecimal``
      - Text value of the number, using scientific notation if an exponent is
        needed. Unfortunately, many parsers will either truncate the value or be
        unable to parse numbers that exceed the size of a double.
    * - ``bigInteger``
      - Text value of the number, using scientific notation if an exponent is
        needed. Unfortunately, many parsers will either truncate the value or be
        unable to parse numbers that exceed the size of a double.
    * - ``string``
      - UTF-8 value of the string.
    * - ``timestamp``
      - Text value of the timestamp. This protocol uses ``date-time`` as the
        default serialization. However, the :ref:`timestampFormat <timestampFormat-trait>`
        MAY be used to customize timestamp serialization.
    * - ``document``
      - Undefined. Document shapes are not supported in this protocol.

Aggregate shapes are serialized with additional segments for members appended
to the input's key.

.. list-table::
    :header-rows: 1
    :widths: 25 75

    * - Smithy type
      - Request entity
    * - ``list``
      - Each value provided in the list is serialized as a separate key with
        a "." separator, ``member``, a "." separator, and a ``1`` indexed
        incrementing counter appended to the container's key. The :ref:`xmlName-trait`
        can be used to serialize a property using a custom name instead of
        ``member``. The :ref:`xmlFlattened-trait` can be used to unwrap the
        values into a containing structure or union, with the key not
        containing the initial "." separator and ``member`` segment.
    * - ``set``
      - A set is serialized identically as a ``list`` shape, but only contains
        unique values.
    * - ``map``
      - Each key and value in each pair provided in the map is serialized as a
        separate key with a "." separator, ``entry``, a "." separator, a ``1``
        indexed incrementing counter, a "." separator, and a ``key`` or
        ``value`` segment appended to the container's key. The :ref:`xmlName-trait`
        can be used to serialize a property using custom names instead of
        ``member``, ``key``, or ``value``. The :ref:`xmlFlattened-trait` can be
        used to unwrap the values into a containing structure or union, with
        the key not containing the initial "." separator and ``entry`` segment.
    * - ``structure``
      - Each member value provided for the structure is serialized as a
        separate key with a "." separator and the member name appended to the
        container's key. The :ref:`xmlName-trait` can be used to serialize a
        property using a custom name.
    * - ``union``
      - A union is serialized identically as a ``structure`` shape, but only a
        single member can be set to a non-null value.


----------------------
Response serialization
----------------------

The ``awsQuery`` protocol serializes XML responses within an XML root node with
the name of the operation's output suffixed with "Response". A nested element,
with the name of the operation's output suffixed with "Result", contains the
contents of the successful response.

The value of the ``uri`` member of the :ref:`xmlNamespace trait <xmlNamespace-trait>`
is serialized in an ``xmlns`` attribute on the response's XML root node. The
following is a sample response to an operation named ``XmlTest``.

.. code-block:: xml

    <XmlTestResponse xmlns="https://example.com/">
        <XmlTestResult>
            <testValue>Hello!</testValue>
        </XmlTestResult>
    </XmlTestResponse>


XML shape serialization
-----------------------

.. list-table::
    :header-rows: 1
    :widths: 25 75

    * - Smithy type
      - XML entity
    * - ``blob``
      - XML text node with a value that is base64 encoded.
    * - ``boolean``
      - XML text node with a value either "true" or "false".
    * - ``byte``
      - XML text node with a value of the number.
    * - ``short``
      - XML text node with a value of the number.
    * - ``integer``
      - XML text node with a value of the number.
    * - ``long``
      - XML text node with a value of the number.
    * - ``float``
      - XML text node with a value of the number.
    * - ``double``
      - XML text node with a value of the number.
    * - ``bigDecimal``
      - XML text node with a value of the number, using scientific notation if
        an exponent is needed. Unfortunately, many XML parsers will either
        truncate the value or be unable to parse numbers that exceed the size
        of a double.
    * - ``bigInteger``
      - XML text node with a value of the number, using scientific notation if
        an exponent is needed. Unfortunately, many XML parsers will either
        truncate the value or be unable to parse numbers that exceed the size
        of a double.
    * - ``string``
      - XML text node with an XML-safe, UTF-8 value of the string.
    * - ``timestamp``
      - XML text node with a value of the timestamp. This protocol uses
        ``date-time`` as the default serialization. However, the
        :ref:`timestampFormat <timestampFormat-trait>` MAY be used to
        customize timestamp serialization.
    * - ``document``
      - Undefined. Document shapes are not recommended for use in XML based
        protocols.
    * - ``list``
      - XML element. Each value provided in the list is serialized as a nested
        XML element with the name ``member``. The :ref:`xmlName-trait` can be
        used to serialize a property using a custom name. The
        :ref:`xmlFlattened-trait` can be used to unwrap the values into a
        containing structure or union, with the value XML element using the
        structure or union member name.
    * - ``set``
      - XML element. A set is serialized identically as a ``list`` shape,
        but only contains unique values.
    * - ``map``
      - XML element. Each key-value pair provided in the map is serialized in
        a nested XML element with the name ``entry`` that contains nested
        elements ``key`` and ``value`` for the pair. The :ref:`xmlName-trait`
        can be used to serialize key or value properties using a custom name,
        it cannot be used to influence the ``entry`` name. The
        :ref:`xmlFlattened-trait` can be used to unwrap the entries into a
        containing structure or union, with the entry XML element using the
        structure or union member name.
    * - ``structure``
      - XML element. Each member value provided for the structure is
        serialized as a nested XML element where the element name is the
        same as the member name. The :ref:`xmlName-trait` can be used to
        serialize a property using a custom name. The :ref:`xmlAttribute-trait`
        can be used to serialize a property in an attribute of the containing
        element.
    * - ``union``
      - XML element. A union is serialized identically as a ``structure``
        shape, but only a single member can be set to a non-null value.

.. important::

    See :ref:`serializing-xml-shapes` for comprehensive documentation,
    including examples and behaviors when using multiple XML traits.


.. _awsQuery-errors:

-----------------------------
Operation error serialization
-----------------------------

Error responses in the ``awsQuery`` protocol are wrapped within an XML root
node named ``ErrorResponse``. A nested element, named ``Error``, contains the
serialized error structure members.

Serialized error shapes MUST also contain an additional child element ``Code``
that contains only the :token:`shape name <identifier>` of the error's
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


-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/awslabs/smithy/tree/main/smithy-aws-protocol-tests/model/awsQuery

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.

*TODO: Add event stream handling specifications.*

.. _`RFC 7230 Section 3.3.2`: https://tools.ietf.org/html/rfc7230#section-3.3.2
