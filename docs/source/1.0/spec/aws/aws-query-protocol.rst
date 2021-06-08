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

    The ``httpError`` trait can only be applied to :ref:`structure <structure>`
    shapes that also have the :ref:`error-trait`.
Value type
    ``structure``

The ``awsQueryError`` trait is a structure that supports the following members:

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
    operation errors using the ``aws.protocols#query`` protocol.

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

.. important::

    This protocol does not support document types.


.. |quoted shape name| replace:: ``awsQuery``
.. |name resolution text| replace:: The :ref:`xmlName-trait` can be used to serialize a property using a custom name
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
         - The :token:`member name <shape_id_member>`
       * - ``union`` member
         - The :token:`member name <shape_id_member>`


Examples
--------

.. important::

    These examples are non-exhaustive. See the :ref:`awsQuery-compliance-tests`
    for a suite of compliance tests for the ``awsQuery`` protocol.


Structures and Unions
=====================

|query aggregate text|

For example, given the following:

.. code-block:: smithy

    structure QueryStructuresInput {
        foo: String,

        @xmlName("Custom")
        bar: String,

        baz: MyStructure,
    }

    structure MyStructure {
        temp: String,
    }

The ``x-www-form-urlencoded`` serialization is:

.. code-block:: text

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

The ``x-www-form-urlencoded`` serialization is:

.. code-block:: text

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

The ``x-www-form-urlencoded`` serialization is:

.. code-block:: text

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

.. include:: aws-xml-serialization.rst.template


.. _awsQuery-errors:

-----------------------------
Operation error serialization
-----------------------------

Error response bodies in the ``awsQuery`` protocol are wrapped within an XML
root node named ``ErrorResponse``. A nested element, named ``Error``, contains
the serialized error structure members. :ref:`The HTTP response code is a
resolved value. <awsQuery-error-response-code>`

Serialized error shapes MUST also contain an additional child element ``Code``
that contains the :ref:`resolved error code value <awsQuery-error-code>`. This
is used to distinguish which specific error is serialized in the response.

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


.. _ec2Query-non-numeric-float-serialization:

------------------------------------------
Non-numeric float and double serialization
------------------------------------------

Smithy floats and doubles are defined by IEE754, which includes special values
for "not a number" and both positive and negative infinity. Unless otherwise
specified, the ``awsQuery`` treats those special values as strings with the
following values:

.. list-table::
    :header-rows: 1
    :widths: 50 50

    * - Special Value
      - String Value
    * - Not a number
      - ``NaN``
    * - positive infinity
      - ``Infinity``
    * - negative infinity
      - ``-Infinity``


.. _awsQuery-error-response-code:

Error HTTP response code resolution
-----------------------------------

The value of the HTTP response code for the error is resolved using the
following process:

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
2. The :token:`shape name <identifier>` of the error's :ref:`shape-id`.

.. _awsQuery-compliance-tests:

-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/awslabs/smithy/tree/main/smithy-aws-protocol-tests/model/awsQuery

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.

*TODO: Add event stream handling specifications.*

