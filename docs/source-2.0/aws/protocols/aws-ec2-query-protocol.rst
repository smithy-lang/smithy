.. _aws-ec2-query-protocol:

======================
AWS EC2 query protocol
======================

This specification defines the ``aws.protocols#ec2`` protocol.


.. smithy-trait:: aws.protocols#ec2Query
.. _aws.protocols#ec2Query-trait:

--------------------------------
``aws.protocols#ec2Query`` trait
--------------------------------

Summary
    Adds support for an HTTP protocol that sends requests in a
    ``application/x-www-form-url-encoded`` body and responses in XML
    documents. This protocol is an Amazon EC2-specific extension
    of the ``awsQuery`` protocol.
Trait selector
    ``service [trait|xmlNamespace]``

    *Service shapes with the xmlNamespace trait*
Value type
    Annotation trait.

.. important::

    * This protocol does not support document types.
    * This protocol does not support :ref:`HTTP binding traits <http-traits>`.
      HTTP binding traits MUST be ignored if they are present.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.protocols#ec2Query

    @ec2Query
    service MyService {
        version: "2020-02-05"
    }


.. smithy-trait:: aws.protocols#ec2QueryName
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
    The ``aws.protocols#ec2QueryName`` trait is only used when serializing
    operation inputs using the ``aws.protocols#ec2`` protocol.

Given the following structure definition:

.. code-block:: smithy

    $version: "2"

    use aws.protocols#ec2QueryName

    structure MyStruct {
        @ec2QueryName("foo")
        bar: String
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
3. Use the default value for the member, if present:

   .. list-table::
       :header-rows: 1
       :widths: 50 50

       * - Member location
         - Default value
       * - ``structure`` member
         - The :token:`member name <smithy:ShapeIdMember>` capitalized
       * - ``union`` member
         - The :token:`member name <smithy:ShapeIdMember>` capitalized


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
    * - :ref:`requestCompression <requestCompression-trait>`
      - Indicates that an operation supports compressing requests from clients
        to services.

.. important::

    This protocol does not support document types.


.. |quoted shape name| replace:: ``ec2Query``
.. |name resolution text| replace:: See :ref:`aws.protocols#ec2QueryName-query-key-naming`
   for how to serialize a property using a custom name
.. |query list text| replace::
    Each value provided in the list is serialized as a separate key with
    a "." separator and a "1" indexed incrementing counter appended to the
    container's key.
.. |query map text| replace::
    Map serialization is currently undefined for this protocol.
.. |query aggregate text| replace::
    Each member value provided for the shape is serialized as a separate key
    with a "." separator and the member name appended to the container's key.
    |name resolution text|. Members with null values are not serialized.
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

    @input
    structure Ec2QueryStructuresInput {
        foo: String

        @ec2QueryName("A")
        HasQueryName: String

        @ec2QueryName("B")
        @xmlName("IgnoreMe")
        HasQueryAndXmlName: String

        @xmlName("c")
        UsesXmlName: String

        baz: MyStructure
    }

    structure MyStructure {
        temp: String
    }

The ``x-www-form-urlencoded`` serialization is:

.. code-block:: text

    Action=Ec2QueryStructures
    &Version=2020-07-02
    &Foo=bar
    &A=example0
    &B=example1
    &C=example2
    &Baz.Temp=example3


Lists
=====

|query list text|

For example, given the following:

.. code-block:: smithy

    @input
    structure Ec2QueryListsInput {
        ListArg: StringList
        ComplexListArg: GreetingList

        @ec2QueryName("Renamed")
        @xmlName("IgnoreMe")
        RenamedListArg: StringList
    }

    list StringList {
        member: String
    }

    list GreetingList {
        member: GreetingStruct
    }

    structure GreetingStruct {
        hi: String
    }

The ``application/x-www-form-urlencoded`` serialization is:

.. code-block:: text

    Action=Ec2QueryLists
    &Version=2020-07-02
    &ListArg.1=foo
    &ListArg.2=bar
    &ListArg.3=baz
    &ComplexListArg.1.Hi=hello
    &ComplexListArg.2.Hi=hola
    &Renamed.1=A
    &Renamed.2=B


----------------------
Response serialization
----------------------

The ``ec2Query`` protocol serializes XML responses within an XML root node with
the name of the operation's output suffixed with "Response", which contains the
contents of the successful response.

The value of the ``uri`` member of the :ref:`xmlNamespace trait <xmlNamespace-trait>`
is serialized in an ``xmlns`` attribute on the response's XML root node. The
following is a sample response to an operation named ``XmlTest``.

.. code-block:: xml

    <XmlTestResponse xmlns="https://example.com/">
        <testValue>Hello!</testValue>
    </XmlTestResponse>

XML shape serialization
-----------------------

.. include:: aws-xml-serialization.rst.template


.. _ec2Query-errors:

-----------------------------
Operation error serialization
-----------------------------

Error responses in the ``ec2Query`` protocol are wrapped within an XML root
node named ``Response``. Inside this, there is an ``Errors`` tag containing
the actual error, and a ``RequestId`` tag holding the request ID. Nested inside
of the ``Errors`` tag is an ``Error`` tag which contains the serialized error
structure members.

Serialized error shapes MUST also contain an additional child element ``Code``
that contains only the :token:`shape name <smithy:Identifier>` of the error's
:ref:`shape-id`. This can be used to distinguish which specific error has been
serialized in the response.

.. code-block:: xml

    <Response>
        <Errors>
            <Error>
                <Code>InvalidGreeting</Code>
                <Message>Hi</Message>
                <AnotherSetting>setting</AnotherSetting>
            </Error>
        </Errors>
        <RequestId>foo-id</RequestId>
    </Response>

* ``Code``: The :token:`shape name <smithy:Identifier>` of the error's
  :ref:`shape-id`.
* ``RequestId``: Contains a unique identifier for the associated request.

In the above example, ``Message``, and ``AnotherSetting`` are additional,
hypothetical members of the serialized error structure.


.. _ec2Query-non-numeric-float-serialization:
.. include:: non-numeric-floats.rst.template


.. _ec2Query-compliance-tests:

-------------------------
Protocol compliance tests
-------------------------

A full compliance test suite is provided and SHALL be considered a normative
reference: https://github.com/awslabs/smithy/tree/main/smithy-aws-protocol-tests/model/ec2Query

These compliance tests define a model that is used to define test cases and
the expected serialized HTTP requests and responses for each case.


*TODO: Add specifications, protocol examples, etc.*

.. include:: error-rename.rst.template
