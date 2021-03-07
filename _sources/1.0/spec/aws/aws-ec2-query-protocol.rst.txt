.. _aws-ec2-query-protocol:

======================
AWS EC2 query protocol
======================

This specification defines the ``aws.protocols#ec2`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#ec2Query-trait:

--------------------------------
``aws.protocols#ec2Query`` trait
--------------------------------

Summary
    Adds support for an HTTP protocol that sends requests in the query string
    OR in a ``x-form-url-encoded`` body and responses in XML documents. This
    protocol is an Amazon EC2-specific extension of the ``awsQuery`` protocol.
Trait selector
    ``service [trait|xmlNamespace]``

    *Service shapes with the xmlNamespace trait*
Value type
    Annotation trait.

.. important::

    This protocol does not support document types.

.. important::

    This protocol only permits the :ref:`httpPayload-trait` to be applied to
    members that target structures, documents, strings, blobs, or unions.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#ec2Query

        @ec2Query
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
                        "aws.protocols#ec2Query": {}
                    }
                }
            }
        }


.. _aws.protocols#ec2QueryName-trait:

------------------------------------
``aws.protocols#ec2QueryName`` trait
------------------------------------

Summary
    Allows a serialized query key to differ from a structure member name when
    used in the model.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    ``string``

.. important::
    The ``aws.protocols#ec2QueryName`` MUST only apply when serializing
    operation inputs using the ``aws.protocols#ec2`` protocol.

Given the following structure definition:

.. tabs::

    .. code-tab:: smithy

        structure MyStruct {
            @ec2QueryName("foo")
            bar: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStruct": {
                    "type": "structure",
                    "members": {
                        "bar": {
                            "target": "smithy.api#String",
                            "traits": {
                                "aws.protocols#ec2QueryName": "foo"
                            }
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStruct``,

::

    "bar" = "baz"

the serialization of this structure as an input on the ``aws.protocols#ec2``
protocol is:

::

    MyStruct.foo=baz


.. _aws.protocols#ec2QueryName-query-key-naming:

Query key resolution
--------------------

The key component used to serialize a member in a request in ``ec2Query`` is
resolved using the following process:

1. Use the value of the :ref:`aws.protocols#ec2QueryName-trait` applied to the
   member, if present.
2. Use the value of the :ref:`xmlName trait <xmlName-trait>` applied to the
   member with the first letter capitalized, if present.
3. Use the default value for the member with the first letter capitalized, if
   present:

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


----------------
Supported traits
----------------

The ``aws.protocols#ec2Query`` protocol supports the following traits
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
    * - :ref:`ec2QueryName <xmlName-trait>`
      - By default, the form-urlencoded key segments used in serialized
        structures are the same as a structure member name. The ``ec2QueryName``
        changes the key segment name to a custom value. See
        :ref:`aws.protocols#ec2QueryName-query-key-naming` for more information.
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
        ``xmlName`` trait changes the these names to a custom value. See
        :ref:`aws.protocols#ec2QueryName-query-key-naming` for more information.
    * - :ref:`xmlNamespace <xmlNamespace-trait>`
      - Adds an xmlns namespace definition URI to XML element(s) generated
        for the targeted shape.
    * - :ref:`timestampFormat <timestampFormat-trait>`
      - Defines a custom timestamp serialization format.

.. important::

    This protocol does not support document types.


.. |quoted shape name| replace:: ``ec2Query``
.. |name resolution text| replace:: See :ref:`aws.protocols#ec2QueryName-query-key-naming`
   for how to serialize a property using a custom name
.. include:: aws-query-serialization.rst.template


Examples
--------

.. important::

    These examples are non-exhaustive. See the :ref:`ec2Query-compliance-tests`
    for a suite of compliance tests for the ``ec2Query`` protocol.


Structures and Unions
=====================

|query aggregate text|

For example, given the following:

.. code-block:: smithy

    structure Ec2QueryStructuresInput {
        foo: String,

        @ec2QueryName("A")
        HasQueryName: String,

        @ec2QueryName("B")
        @xmlName("IgnoreMe")
        HasQueryAndXmlName: String,

        @xmlName("c")
        UsesXmlName: String,

        baz: MyStructure,
    }

    structure MyStructure {
        temp: String,
    }

The ``x-www-form-urlencoded`` serialization is:

.. code-block:: text

    Action=Ec2QueryStructures
    &Version=2020-07-02
    &foo=bar
    &A=example0
    &B=example1
    &C=example2
    &baz.temp=example3


Collections
===========

|query collection text|

For example, given the following:

.. code-block:: smithy

    structure Ec2QueryListsInput {
        ListArg: StringList,
        ComplexListArg: GreetingList,

        @xmlFlattened
        FlattenedListArg: StringList,

        ListArgWithXmlNameMember: ListWithXmlName,

        // Notice that the xmlName on the targeted list member is ignored.
        @xmlFlattened
        @ec2QueryName("Hi")
        @xmlName("IgnoreMe")
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

    Action=Ec2QueryLists
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


----------------------
Response serialization
----------------------

The ``ec2Query`` protocol serializes XML responses within an XML root node with
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


.. _ec2Query-errors:

-----------------------------
Operation error serialization
-----------------------------

Error responses in the ``ec2Query`` protocol are wrapped within an XML root
node named ``Errors``. A nested element, named ``Error``, contains the
serialized error structure members.

Serialized error shapes MUST also contain an additional child element ``Code``
that contains only the :token:`shape name <identifier>` of the error's
:ref:`shape-id`. This can be used to distinguish which specific error has been
serialized in the response.

.. code-block:: xml

    <Errors>
        <Error>
            <Type>Sender</Type>
            <Code>InvalidGreeting</Code>
            <Message>Hi</Message>
            <AnotherSetting>setting</AnotherSetting>
        </Error>
        <RequestId>foo-id</RequestId>
    </Errors>


.. _ec2Query-compliance-tests:

-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/awslabs/smithy/tree/main/smithy-aws-protocol-tests/model/ec2Query

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.


*TODO: Add specifications, protocol examples, etc.*
