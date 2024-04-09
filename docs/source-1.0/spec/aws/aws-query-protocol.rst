.. _aws-query-protocol:

==================
AWS query protocol
==================

This document defines the ``aws.protocols#awsQuery`` protocol.


--------
Overview
--------

The AWS Query protocol uses HTTP and serializes HTTP requests using query
string parameters and responses using XML. Requests can be sent using GET
requests and placing parameters in the query string; however, sending
requests using POST with an ``application/x-www-form-urlencoded``
Content-Type is preferred because some proxies, clients, and servers have
limitations around the maximum amount of data that can be sent in a query
string.

A service is configured to use this protocol by applying the :ref:`aws.protocols#awsQuery-trait`.
The service MUST also define the :ref:`xmlnamespace-trait` which is used
to determine the XML namespace used in XML responses.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#awsQuery

    @awsQuery
    @xmlNamespace(uri: "http://foo.com")
    service MyService {
        version: "2020-02-05"
    }

.. important::

    * This protocol is deprecated and SHOULD NOT be used for any new service.
    * This protocol does not support document types.
    * This protocol does not support :ref:`HTTP binding traits <http-traits>`.
      HTTP binding traits MUST be ignored if they are present.
    * This protocol does not support any kind of streaming requests or
      responses, including event streams.


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
    * - :ref:`endpoint <endpoint-trait>`
      - Configures a custom operation endpoint.
    * - :ref:`hostLabel <hostLabel-trait>`
      - Binds a top-level operation input structure member to a label in
        the hostPrefix of an endpoint trait.
    * - :ref:`xmlAttribute <xmlAttribute-trait>`
      - Serializes an object property as an XML attribute rather than a nested
        XML element.
    * - :ref:`xmlFlattened <xmlFlattened-trait>`
      - By default, entries in lists, sets, and maps have values serialized in
        nested elements specific to their type. The ``xmlFlattened`` trait
        unwraps these elements into the containing structure.
    * - :ref:`xmlName <xmlName-trait>`
      - By default, the XML element names and form-urlencoded key segments used
        in serialized structures are the same as a structure member name. The
        ``xmlName`` trait changes these names to a custom value.  See
        :ref:`aws.protocols#awsQueryName-query-key-naming` for more information.
    * - :ref:`xmlNamespace <xmlNamespace-trait>`
      - Adds an xmlns namespace definition URI to XML element(s) generated
        for the targeted shape.
    * - :ref:`timestampFormat <timestampFormat-trait>`
      - Defines a custom timestamp serialization format.
    * - :ref:`awsQueryError <aws.protocols#awsQueryError-trait>`
      - Provides a :ref:`custom "Code" value <awsQuery-error-code>` for
        ``awsQuery`` errors and an :ref:`HTTP response code <awsQuery-error-response-code>`.
        The "Code" of an ``awsQuery`` error is used by clients to determine
        which type of error was encountered.
    * - :ref:`requestCompression <requestCompression-trait>`
      - Indicates that an operation supports compressing requests from clients
        to services.

.. |quoted shape name| replace:: ``awsQuery``
.. |name resolution text| replace:: The :ref:`xmlName-trait` can be used to serialize a property using a custom name
.. |query collection text| replace::
    Each value provided in the collection is serialized as a separate key with
    a "." separator, the string "member", a "." separator, and a "1" indexed
    incrementing counter appended to the container's key.
    |name resolution text| instead of "member". The :ref:`xmlFlattened-trait`
    can be used to unwrap the values into a containing structure or union,
    with the key not containing the initial "." separator and ``member``
    segment.
.. |query map text| replace::
    Each key and value in each pair provided in the map is serialized as a
    separate key with a "." separator, the string "entry", a "." separator,
    a "1" indexed incrementing counter, a "." separator, and the string
    "key" or "value" (for member keys or values, respectively) appended to
    the container's key. |name resolution text| instead of "member", "key",
    or "value". The :ref:`xmlFlattened-trait` can be used to unwrap the
    values into a containing structure or union, with the key not
    containing the initial "." separator and "entry" segment.
.. |query aggregate text| replace::
    Each member value provided for the shape is serialized as a separate key
    with a "." separator and the member name appended to the container's key.
    |name resolution text|. Members with null values are not serialized.
.. include:: aws-query-serialization.rst.template


.. _aws.protocols#awsQueryName-query-key-naming:

Query key resolution
--------------------

The key component used to serialize a member in a request in ``awsQuery`` is
resolved using the following process:

1. Use the value of the :ref:`xmlName <xmlName-trait>` trait applied to the
   member, if present.
2. Use the default value for the member:

   .. list-table::
       :header-rows: 1
       :widths: 50 50

       * - Member location
         - Default value
       * - ``list`` member
         - The string literal "member"
       * - ``set`` member
         - The string literal "member"
       * - ``map`` key
         - The string literal "key"
       * - ``map`` value
         - The string literal "value"
       * - ``structure`` member
         - The :token:`member name <smithy:ShapeIdMember>`
       * - ``union`` member
         - The :token:`member name <smithy:ShapeIdMember>`


Example requests
----------------

The following list of examples are non-exhaustive. See the
:ref:`awsQuery-compliance-tests` for a suite of compliance tests for the
``awsQuery`` protocol. Newlines have been to examples only for readability.


Structures and unions
=====================

|query aggregate text|

For example, given the following:

.. code-block:: smithy

    @input
    structure QueryStructuresInput {
        foo: String,

        @xmlName("Custom")
        bar: String,

        baz: MyStructure,
    }

    structure MyStructure {
        temp: String,
    }

The ``application/x-www-form-urlencoded`` serialization is:

.. code-block:: text

    POST / HTTP/1.1
    Content-Type: application/x-www-form-urlencoded
    Content-Length: ...
    Host: example.com

    Action=QueryStructures
    &Version=2020-07-02
    &foo=example1
    &Custom=example2
    &baz.temp=example3


Collections
===========

|query collection text|

For example, given the following:

.. code-block:: smithy

    @input
    structure QueryListsInput {
        ListArg: StringList,
        ComplexListArg: GreetingList,

        @xmlFlattened
        FlattenedListArg: StringList,

        ListArgWithXmlNameMember: ListWithXmlName,

        // Notice that the xmlName on the targeted list member is ignored.
        @xmlFlattened
        @xmlName("Hi")
        FlattenedListArgWithXmlName: ListWithXmlName,
    }

    list ListWithXmlName {
        @xmlName("item")
        member: String
    }

    list StringList {
        member: String
    }

    list GreetingList {
        member: GreetingStruct
    }

    structure GreetingStruct {
        hi: String,
    }

The ``application/x-www-form-urlencoded`` serialization is:

.. code-block:: text

    POST / HTTP/1.1
    Content-Type: application/x-www-form-urlencoded
    Content-Length: ...
    Host: example.com

    Action=QueryLists
    &Version=2020-07-02
    &ListArg.member.1=foo
    &ListArg.member.2=bar
    &ListArg.member.3=baz
    &ComplexListArg.member.1.hi=hello
    &ComplexListArg.member.2.hi=hola
    &FlattenedListArg.1=A
    &FlattenedListArg.2=B
    &ListArgWithXmlNameMember.item.1=A
    &ListArgWithXmlNameMember.item.2=B
    &Hi.1=A
    &Hi.2=B

Maps
====

|query map text|

For example, given the following:

.. code-block:: smithy

    @input
    structure QueryMapsInput {
        MapArg: StringMap,

        @xmlName("reNamed")
        RenamedMapArg: StringMap,

        ComplexMapArg: ComplexMap,

        MapWithXmlMemberName: MapWithXmlName,
    }

    map StringMap {
        key: String,
        value: String
    }

    map ComplexMap {
        key: String,
        value: GreetingStruct,
    }

    map MapWithXmlName {
        @xmlName("K")
        key: String,

        @xmlName("V")
        value: String
    }

    structure GreetingStruct {
        hi: String,
    }

The ``application/x-www-form-urlencoded`` serialization is:

.. code-block:: text

    POST / HTTP/1.1
    Content-Type: application/x-www-form-urlencoded
    Content-Length: ...
    Host: example.com

    Action=QueryMaps
    &Version=2020-07-02
    &MapArg.entry.1.key=bar
    &MapArg.entry.1.value=Bar
    &MapArg.entry.2.key=foo
    &MapArg.entry.2.value=Foo
    &reNamed.entry.1.key=foo
    &reNamed.entry.1.value=Foo
    &ComplexMapArg.entry.1.key=bar
    &ComplexMapArg.entry.1.value.hi=Bar
    &ComplexMapArg.entry.2.key=foo
    &ComplexMapArg.entry.2.value.hi=Foo
    &MapWithXmlMemberName.entry.1.K=bar
    &MapWithXmlMemberName.entry.1.V=Bar
    &MapWithXmlMemberName.entry.2.K=foo
    &MapWithXmlMemberName.entry.2.V=Foo

----------------------
Response serialization
----------------------

The ``awsQuery`` protocol serializes XML responses within an XML root node with
the name of the operation's output suffixed with "Response". A nested element,
with the name of the operation's output suffixed with "Result", contains the
contents of the successful response. A nested element named "ResponseMetadata"
contains a nested element named "RequestId" that contains a unique identifier
for the associated request.

The value of the ``uri`` member of the :ref:`xmlNamespace trait <xmlNamespace-trait>`
is serialized in an ``xmlns`` attribute on the response's XML root node. The
following is a sample response to an operation named ``XmlTest``.

.. code-block:: xml

    HTTP/1.1 200 OK
    Date: Mon, 27 Jul 2009 12:28:53 GMT
    Content-Type: text/xml
    Content-Length: ...

    <XmlTestResponse xmlns="https://example.com/">
        <XmlTestResult>
            <testValue>Hello!</testValue>
        </XmlTestResult>
        <ResponseMetadata>
            <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
        </ResponseMetadata>
    </XmlTestResponse>

XML shape serialization
-----------------------

.. include:: aws-xml-serialization.rst.template


.. _awsQuery-errors:

-----------------------------
Operation error serialization
-----------------------------

Error response bodies in the ``awsQuery`` protocol are wrapped within an XML
root node named ``ErrorResponse``. A nested element, named ``Error``, contains
the serialized error structure members. :ref:`The HTTP response code is a
resolved value. <awsQuery-error-response-code>`

.. code-block:: xml
    :caption: AWS Query error example

    HTTP/1.1 400 Bad Request
    Date: Mon, 27 Jul 2009 12:28:53 GMT
    Content-Type: text/xml
    Content-Length: ...

    <ErrorResponse>
        <Error>
            <Type>Sender</Type>
            <Code>InvalidGreeting</Code>
            <Message>Hi</Message>
            <AnotherSetting>setting</AnotherSetting>
        </Error>
        <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
    </ErrorResponse>

Error responses contain the following nested elements:

* ``Error``: A container for the encountered error.
* ``Type``: One of "Sender" or "Receiver"; whomever is at fault from
  the service perspective.
* ``Code``: The :ref:`resolved error code value <awsQuery-error-code>`
  that is used to distinguish which specific error is serialized in
  the response.
* ``RequestId``: Contains a unique identifier for the associated request.

In the above example, ``Message``, and ``AnotherSetting`` are additional,
hypothetical members of the serialized error structure.


.. _awsQuery-non-numeric-float-serialization:
.. include:: non-numeric-floats.rst.template


.. _awsQuery-error-response-code:

Error HTTP response status code resolution
------------------------------------------

The status code of an error is ``400``, ``500``, or a custom status code
defined by the :ref:`aws.protocols#awsQueryError-trait`. The status code
is determined through the following process:

1. Use the value of the ``httpResponseCode`` member of the :ref:`aws.protocols#awsQueryError-trait`
   applied to the error structure, if present.
2. Use the value ``400`` if the value of the :ref:`error-trait` is ``"client"``.
3. Use the value ``500``.


.. _awsQuery-error-code:

Error "Code" resolution
-----------------------

The value of the "Code" element serialized in the error is resolved using the
following process:

1. Use the value of the ``code`` member of the :ref:`aws.protocols#awsQueryError-trait`
   applied to the error structure, if present.
2. The :token:`shape name <smithy:Identifier>` of the error's :ref:`shape-id`.


.. smithy-trait:: aws.protocols#awsQuery
.. _aws.protocols#awsQuery-trait:

--------------------------------
``aws.protocols#awsQuery`` trait
--------------------------------

Summary
    Adds support for the awsQuery protocol to a service. The service MUST have
    an :ref:`xmlnamespace-trait`.
Trait selector
    ``service [trait|xmlNamespace]``

    *Service shapes with the xmlNamespace trait*
Value type
    Annotation trait.
See
    `Protocol tests <https://github.com/smithy-lang/smithy/tree/__smithy_version__/smithy-aws-protocol-tests/model/awsQuery>`_

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#awsQuery

        @awsQuery
        @xmlNamespace(uri: "http://foo.com")
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
                        "aws.protocols#awsQuery": {},
                        "smithy.api#xmlNamespace": {
                            "uri": "example.com"
                        }
                    }
                }
            }
        }


.. smithy-trait:: aws.protocols#awsQueryError
.. _aws.protocols#awsQueryError-trait:

-------------------------------------
``aws.protocols#awsQueryError`` trait
-------------------------------------

Summary
    Provides a :ref:`custom "Code" value <awsQuery-error-code>` for
    ``awsQuery`` errors and an :ref:`HTTP response code <awsQuery-error-response-code>`.
    The "Code" of an ``awsQuery`` error is used by clients to determine which
    type of error was encountered.
Trait selector
    ``structure [trait|error]``

    The ``awsQueryError`` trait can only be applied to :ref:`structure <structure>`
    shapes that also have the :ref:`error-trait`.
Value type
    ``structure`` that supports the following members:

    .. list-table::
        :header-rows: 1
        :widths: 10 25 65

        * - Property
          - Type
          - Description
        * - code
          - ``string``
          - **Required** The value used to distinguish this error shape during
            client deserialization.
        * - httpResponseCode
          - ``integer``
          - **Required** The HTTP response code used on a response that contains
            this error shape.

.. important::

    The ``aws.protocols#awsQueryError`` trait is only used when serializing
    operation errors using the ``aws.protocols#query`` protocol. Unless
    explicitly stated in other Smithy protocol specification, this trait has
    no impact on other Smithy protocols.

The following example defines an error that uses a custom "Code" of
"InvalidThing" and an HTTP status code of 400.

.. tabs::

    .. code-tab:: smithy

        use aws.protocols#awsQueryError

        @awsQueryError(
            code: "InvalidThing",
            httpResponseCode: 400,
        )
        @error("client")
        structure InvalidThingException {
            message: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#InvalidThingException": {
                    "type": "structure",
                    "members": {
                        "message": {
                            "target": "smithy.api#String"
                        }
                    },
                    "traits": {
                        "aws.protocols#awsQueryError": {
                            "code": "InvalidThing",
                            "httpResponseCode": 400
                        },
                        "smithy.api#error": "client"
                    }
                }
            }
        }


.. smithy-trait:: aws.protocols#awsQueryCompatible
.. _aws.protocols#awsQueryCompatible-trait:

------------------------------------------
``aws.protocols#awsQueryCompatible`` trait
------------------------------------------

Summary
    When using the :ref:`awsQuery <aws.protocols#awsQuery-trait>` protocol,
    custom ``Code`` and ``HTTP response code`` values can be defined for an error response via
    the :ref:`awsQueryError <aws.protocols#awsQueryError-trait>` trait.

    The ``awsQueryCompatible`` trait allows services to backward compatibly migrate from ``awsQuery`` to
    :ref:`awsJson1_0 <aws.protocols#awsJson1_0-trait>` without removing values defined in the ``awsQueryError`` trait.

    This trait adds the ``x-amzn-query-error`` header in the form of ``Code;Fault`` to error responses.
    ``Code`` is the value defined in the :ref:`awsQueryError <aws.protocols#awsQueryError-trait>`,
    and ``Fault`` is one of ``Sender`` or ``Receiver``.

Trait selector
    ``service [trait|awsJson1_0]``

Value type
    Annotation trait

.. code-block:: smithy

    $version: "1"
    use aws.protocols#awsQueryCompatible
    use aws.protocols#awsQueryError
    use aws.protocols#awsJson1_0

    @awsQueryCompatible
    @awsJson1_0
    service MyService {
        version: "2020-02-05"
    }

    @awsQueryError(
        code: "InvalidThing",
        httpResponseCode: 400,
    )
    @error("client")
    structure InvalidThingException {
        message: String
    }


.. _awsQuery-compliance-tests:

-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/smithy-lang/smithy/tree/main/smithy-aws-protocol-tests/model/awsQuery

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.

.. include:: error-rename.rst.template
