---------------------------------
Serialization and Protocol traits
---------------------------------

.. smithy-trait:: smithy.api#protocolDefinition
.. _protocolDefinition-trait:

``protocolDefinition`` trait
============================

Summary
    A meta-trait that marks a trait as a protocol definition trait. Traits
    that are marked with this trait are applied to service shapes to
    define the protocols supported by a service. A client MUST understand
    at least one of the protocols in order to successfully communicate
    with the service.
Trait selector
    ``[trait|trait]``
Value type
    An object with the following properties:

    .. list-table::
       :header-rows: 1
       :widths: 10 23 67

       * - Property
         - Type
         - Description
       * - traits
         - [:ref:`shape-id`]
         - List of shape IDs that protocol implementations MUST understand
           in order to successfully use the protocol. Each shape MUST exist
           and MUST be a trait. Code generators SHOULD ensure that they
           support each listed trait.
       * - noInlineDocumentSupport
         - ``boolean``
         - If set to ``true``, indicates that this protocol does not support
           ``document`` type shapes. A service that uses such a protocol
           MUST NOT contain any ``document`` shapes in their service closure.

Smithy is protocol agnostic, which means it focuses on the interfaces and
abstractions that are provided to end-users rather than how the data is sent
over the wire. In Smithy, a *protocol* is a named set of rules that defines
the syntax and semantics of how a client and server communicate. This
includes the application layer protocol of a service (for example, HTTP)
and the serialization formats used in messages (for example, JSON). Traits
MAY be used to influence how messages are serialized (for example,
:ref:`jsonName-trait` and :ref:`xmlAttribute-trait`).

The following example defines a service that supports both the hypothetical
``jsonExample`` and ``xmlExample`` protocols.

.. code-block:: smithy

    /// An example JSON protocol.
    @protocolDefinition
    @trait(selector: "service")
    structure jsonExample {}

    /// An example XML protocol.
    @protocolDefinition
    @trait(selector: "service")
    structure xmlExample {}

    @jsonExample
    @xmlExample
    service WeatherService {
        version: "2017-02-11"
    }

Because protocol definitions are just specialized shapes, they can also
support configuration settings.

.. code-block:: smithy

    @protocolDefinition
    @trait(selector: "service")
    structure configurableExample {
        @required
        version: String
    }

    @configurableExample(version: "1.0")
    service WeatherService {
        version: "2017-02-11"
    }


.. smithy-trait:: smithy.api#jsonName
.. _jsonName-trait:

``jsonName`` trait
==================

Summary
    Allows a serialized object property name in a JSON document to differ from
    a structure or union member name used in the model.
Trait selector
    ``:is(structure, union) > member``

    *Any structure or union member*
Value type
    ``string``

Given the following structure definition,

.. code-block:: smithy

    structure MyStructure {
        @jsonName("Foo")
        foo: String

        bar: String
    }

and the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"

the JSON representation of the value would be serialized with the
following document:

.. code-block:: json

    {
        "Foo": "abc",
        "bar": "def"
    }

.. note::

    No two members of the same structure or union can use the
    same case-sensitive ``@jsonName``.


.. smithy-trait:: smithy.api#mediaType
.. _mediaType-trait:

``mediaType`` trait
===================

Summary
    Describes the contents of a blob or string shape using a design-time
    media type as defined by :rfc:`6838` (for example, ``application/json``).
Trait selector
    ``:is(blob, string)``

    *Any blob or string*
Value type
    ``string``

The following example defines a ``video/quicktime`` blob:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @mediaType("video/quicktime")
    blob VideoData

.. rubric:: Use cases

The primary function of the ``mediaType`` trait is to send open content
data over the wire inside of values that are isolated from the rest of
a payload using exact representations of customer provided data. While the
model does define the serialization format of values able to be stored in a
shape at design-time using a media type, models are not required to define
any kind of schema for the shape.

The ``mediaType`` trait can be used to aid tools in documentation,
validation, special-cased helpers to serialize and deserialize media type
contents in code, assigning a fixed Content-Type when using
:ref:`HTTP bindings <http-traits>`, etc.

.. rubric:: Comparisons to document types

The serialization format of a shape marked with the ``@mediaType`` trait is
an important part of its contract. In contrast, document types are
serialized in a protocol-agnostic way and can only express data types as
granular as the JSON-type system. Design-time media types are preferred over
document types when the exact bytes of a value are required for an
application to function.


.. smithy-trait:: smithy.api#timestampFormat
.. _timestampFormat-trait:

``timestampFormat`` trait
=========================

Summary
    Defines a custom timestamp serialization format.
Trait selector
    ``:test(timestamp, member > timestamp)``

    *timestamp or member that targets a timestamp*
Value type
    ``string``

By default, the serialization format of a timestamp is implicitly determined by
the :ref:`protocol <protocolDefinition-trait>` of a service; however, the
serialization format can be explicitly configured in some protocols to
override the default format using the ``timestampFormat`` trait.

.. rubric:: Timestamp formats

Smithy defines the following built-in timestamp formats:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Format
      - Description
    * - date-time
      - Date time as defined by the ``date-time`` production in
        :rfc:`3339#section-5.6`
        with optional fractional precision but no UTC offset (for example,
        ``1985-04-12T23:20:50.52Z``).
        *However*, offsets are parsed gracefully, but the datetime is normalized
        to an offset of zero by converting to UTC.
    * - http-date
      - An HTTP date as defined by the ``IMF-fixdate`` production in
        :rfc:`7231#section-7.1.1.1` (for example,
        ``Tue, 29 Apr 2014 18:30:38 GMT``).
    * - epoch-seconds
      - Also known as Unix time, the number of seconds that have elapsed since
        00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970,
        with optional fractional precision (for example, ``1515531081.1234``).

.. rubric:: Resolving timestamp formats

The following steps are taken to determine the serialization format of a
:ref:`member <member>` that targets a timestamp:

1. Use the ``timestampFormat`` trait of the member, if present.
2. Use the ``timestampFormat`` trait of the shape, if present.
3. Use the default format of the protocol.

.. important::

    This trait SHOULD NOT be used unless the intended serialization format of
    a timestamp differs from the default protocol format. Using this trait too
    liberally can cause other tooling to improperly interpret the timestamp.


.. _xml-bindings:

XML bindings
============

Defines how to bind Smithy shapes to XML documents.


.. _xml-structure-and-union-serialization:

Structure and union serialization
---------------------------------

All XML serialization starts with a structure or union. The shape name of a
structure/union is used as the outermost XML element name. Members of a
structure/union are serialized as nested XML elements where the name of the
element is the same as the name of the member.

For example, given the following:

.. code-block:: smithy

    structure MyStructure {
        foo: String
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure>
        <foo>example</foo>
    </MyStructure>


Custom XML element names
~~~~~~~~~~~~~~~~~~~~~~~~

Structure/union member element names can be changed using the
:ref:`xmlname-trait`.


XML attributes
~~~~~~~~~~~~~~

The :ref:`xmlattribute-trait` is used to serialize a structure
member as an XML attribute.


``xmlName`` on structures and unions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

An ``xmlName`` trait applied to a structure or union changes the element name
of the serialized shape; however, it does not influence the serialization of
members that target it. Given the following:

.. code-block:: smithy

    @xmlName("AStruct")
    structure A {
        b: B
    }

    @xmlName("BStruct")
    structure B {
        hello: String
    }

The XML serialization of ``AStruct`` is:

.. code-block:: xml

    <AStruct>
        <b>
            <hello>value</hello>
        </b>
    </AStruct>


Simple type serialization
-------------------------

The following table defines how simple types are serialized in XML documents.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Shape
      - Serialization
    * - blob
      - Serialized as a :rfc:`base64 encoded <4648#section-4>` string

        .. code-block:: smithy

            structure Struct {
                binary: Blob
            }

        given a value of ``value`` for ``binary``:

        .. code-block:: xml

            <Struct>
                <binary>dmFsdWU=</binary>
            </Struct>

    * - boolean
      - Serialized as "``true``" or "``false``"
    * - string
      - Serialized as an XML-safe UTF-8 string
    * - byte
      - Serialized as the string value of the number
    * - short
      - Serialized as the string value of the number
    * - integer
      - Serialized as the string value of the number
    * - long
      - Serialized as the string value of the number
    * - float
      - Serialized as the string value of the number using scientific
        notation if an exponent is needed.
    * - double
      - Serialized as the string value of the number using scientific
        notation if an exponent is needed.
    * - bigInteger
      - Serialized as the string value of the number using scientific
        notation if an exponent is needed.
    * - bigDecimal
      - Serialized as the string value of the number using scientific
        notation if an exponent is needed.
    * - timestamp
      - Serialized as :rfc:`3339` date-time value.

        .. code-block:: smithy

              structure Struct {
                  date: Timestamp
              }

        given a value of ``1578255206`` for ``date``:

        .. code-block:: xml

            <Struct>
                <date>2020-01-05T20:13:26Z</date>
            </Struct>

    * - document
      - .. warning::

            Document shapes are not recommended for use in XML based protocols.


.. _xml-list-serialization:

List serialization
------------------

List shapes use the same serialization semantics. List shapes
can be serialized as wrapped lists (the default behavior) or flattened lists.


Wrapped list serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~

A wrapped list is serialized in an XML element where each value is
serialized in a nested element named ``member``. For example, given the
following:

.. code-block:: smithy

    structure Foo {
        values: MyList
    }

    list MyList {
        member: String
    }

The XML serialization of ``Foo`` is:

.. code-block:: xml

    <Foo>
        <values>
            <member>example1</member>
            <member>example2</member>
            <member>example3</member>
        </values>
    </Foo>

The :ref:`xmlname-trait` can be applied to the member of a list to
change the nested element name. For example, given the following:

.. code-block:: smithy

    structure Foo {
        values: MyList
    }

    list MyList {
        @xmlName("Item")
        member: String
    }

The XML serialization of ``Foo`` is:

.. code-block:: xml

    <Foo>
        <values>
            <Item>example1</Item>
            <Item>example2</Item>
            <Item>example3</Item>
        </values>
    </Foo>


Flattened list serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The :ref:`xmlflattened-trait` can be used to unwrap the values of list
into a containing structure/union. The name of the elements repeated within
the structure/union is based on the structure/union member name. For
example, given the following:

.. code-block:: smithy

    structure Foo {
        @xmlFlattened
        flat: MyList
    }

The XML serialization of ``Foo`` is:

.. code-block:: xml

    <Foo>
        <flat>example1</flat>
        <flat>example2</flat>
        <flat>example3</flat>
    </Foo>

The ``xmlName`` trait applied to the structure/union member is used to change
the name of the repeated XML element. For example, given the following:

.. code-block:: smithy

    union Choice {
        @xmlFlattened
        @xmlName("Hi")
        flat: MyList
    }

    list MyList {
        member: String
    }

The XML serialization of ``Choice`` is:

.. code-block:: xml

    <Choice>
        <Hi>example1</Hi>
        <Hi>example2</Hi>
        <Hi>example3</Hi>
    </Choice>

The ``xmlName`` trait applied to the member of a list has no effect when
serializing a flattened list into a structure/union. For example, given the
following:

.. code-block:: smithy

    union Choice {
        @xmlFlattened
        flat: MyList
    }

    list MyList {
        @xmlName("Hi")
        member: String
    }

The XML serialization of ``Choice`` is:

.. code-block:: xml

    <Choice>
        <flat>example1</flat>
        <flat>example2</flat>
        <flat>example3</flat>
    </Choice>


.. _xml-map-serialization:

Map serialization
-----------------

Map shapes can be serialized as wrapped maps (the default behavior) or
flattened maps.


Wrapped map serialization
~~~~~~~~~~~~~~~~~~~~~~~~~

A wrapped map is serialized in an XML element where each value is
serialized in a nested element named ``entry`` that contains a nested
``key`` and ``value`` element. For example, given the following:

.. code-block:: smithy

    structure Foo {
        values: MyMap
    }

    map MyMap {
        key: String
        value: String
    }

The XML serialization of ``Foo`` is:

.. code-block:: xml

    <Foo>
        <values>
            <entry>
                <key>example-key1</key>
                <value>example1</value>
            </entry>
            <entry>
                <key>example-key2</key>
                <value>example2</value>
            </entry>
        </values>
    </Foo>

The :ref:`xmlname-trait` can be applied to the key and value members of a map
to change the nested element names.  For example, given the following:

.. code-block:: smithy

    structure Foo {
        values: MyMap
    }

    map MyMap {
        @xmlName("Name")
        key: String

        @xmlName("Setting")
        value: String
    }

The XML serialization of ``Foo`` is:

.. code-block:: xml

    <Foo>
        <values>
            <entry>
                <Name>example-key1</Name>
                <Setting>example1</Setting>
            </entry>
            <entry>
                <Name>example-key2</Name>
                <Setting>example2</Setting>
            </entry>
        </values>
    </Foo>


Flattened map serialization
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The :ref:`xmlFlattened-trait` can be used to flatten the members of map
into a containing structure/union. For example, given the following:

.. code-block:: smithy

    structure Bar {
        @xmlFlattened
        flatMap: MyMap
    }

    map MyMap {
        key: String
        value: String
    }

The XML serialization of ``Bar`` is:

.. code-block:: xml

    <Bar>
        <flatMap>
            <key>example-key1</key>
            <value>example1</value>
        </flatMap>
        <flatMap>
            <key>example-key2</key>
            <value>example2</value>
        </flatMap>
        <flatMap>
            <key>example-key3</key>
            <value>example3</value>
        </flatMap>
    </Bar>

The ``xmlName`` trait applied to the structure/union member is used to change
the name of the repeated XML element. For example, given the following:

.. code-block:: smithy

    union Choice {
        @xmlFlattened
        @xmlName("Hi")
        flat: MyMap
    }

    map MyMap {
        key: String
        value: String
    }

The XML serialization of ``Choice`` is:

.. code-block:: xml

    <Choice>
        <Hi>
            <key>example-key1</key>
            <value>example1</value>
        </Hi>
        <Hi>
            <key>example-key1</key>
            <value>example1</value>
        </Hi>
        <Hi>
            <key>example-key1</key>
            <value>example1</value>
        </Hi>
    </Choice>

Unlike flattened lists and sets, flattened maps *do* honor ``xmlName``
traits applied to the key or value members of the map. For example, given
the following:

.. code-block:: smithy

    union Choice {
        @xmlFlattened
        @xmlName("Hi")
        flat: MyMap
    }

    map MyMap {
        @xmlName("Name")
        key: String

        @xmlName("Setting")
        value: String
    }

The XML serialization of ``Choice`` is:

.. code-block:: xml

    <Choice>
        <Hi>
            <Name>example-key1</Name>
            <Setting>example1</Setting>
        </Hi>
        <Hi>
            <Name>example-key2</Name>
            <Setting>example2</Setting>
        </Hi>
        <Hi>
            <Name>example-key3</Name>
            <Setting>example3</Setting>
        </Hi>
    </Choice>


.. smithy-trait:: smithy.api#xmlAttribute
.. _xmlAttribute-trait:

``xmlAttribute`` trait
----------------------

Summary
    Serializes an object property as an XML attribute rather than a nested
    XML element.
Trait selector
    .. code-block:: none

        structure > :test(member > :test(boolean, number, string, timestamp))

    *Structure members that target boolean, number, string, or timestamp*
Value type
    Annotation trait
Conflicts with
    :ref:`xmlNamespace-trait`

By default, the serialized XML attribute name is the same as the structure
member name. For example, given the following:

.. code-block:: smithy

    structure MyStructure {
        @xmlAttribute
        foo: String

        bar: String
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure foo="example">
        <bar>example</bar>
    </MyStructure>

The serialized attribute name can be changed using the :ref:`xmlname-trait`.
Given the following:

.. code-block:: smithy

    structure MyStructure {
        @xmlAttribute
        @xmlName("NotFoo")
        foo: String
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure NotFoo="example"/>


.. smithy-trait:: smithy.api#xmlFlattened
.. _xmlFlattened-trait:

``xmlFlattened`` trait
----------------------

Summary
    Unwraps the values of a list or map into the containing structure.
Trait selector
    .. code-block:: none

        :is(structure, union) > :test(member > :test(list, map))

    *Member of a structure or union that targets a list or map*
Value type
    Annotation trait

Given the following:

.. code-block:: smithy

    structure Foo {
        @xmlFlattened
        flat: MyList

        nested: MyList
    }

    list MyList {
        member: String
    }

The XML serialization of ``Foo`` is:

.. code-block:: xml

    <Foo>
        <flat>example1</flat>
        <flat>example2</flat>
        <flat>example3</flat>
        <nested>
            <member>example1</member>
            <member>example2</member>
            <member>example3</member>
        </nested>
    </Foo>

Maps can be flattened into structures too. Given the following:

.. code-block:: smithy

    structure Foo {
        @xmlFlattened
        flat: MyMap

        notFlat: MyMap
    }

    map MyMap {
        key: String
        value: String
    }

The XML serialization is:

.. code-block:: xml

    <Foo>
        <flat>
            <key>example-key1</key>
            <value>example1</value>
        </flat>
        <flat>
            <key>example-key2</key>
            <value>example2</value>
        </flat>
        <notFlat>
            <entry>
                <key>example-key1</key>
                <value>example1</value>
            </entry>
            <entry>
                <key>example-key2</key>
                <value>example2</value>
            </entry>
        </notFlat>
    </Foo>


.. smithy-trait:: smithy.api#xmlName
.. _xmlName-trait:

``xmlName`` trait
-----------------

Summary
    Changes the serialized element or attribute name of a structure, union,
    or member.
Trait selector
    ``:is(structure, union, member)``

    *A structure, union, or member*
Value type
    ``string`` value that MUST adhere to the :token:`smithy:XmlName` ABNF production:

    .. productionlist:: smithy
        XmlName       :`XmlIdentifier` / (`XmlIdentifier`` ":" `XmlIdentifier``)
        XmlIdentifier :(ALPHA / "_") *(ALPHA / DIGIT / "-" / "_")

By default, structure properties are serialized in attributes or nested
elements using the same name as the structure member name. Given the following:

.. code-block:: smithy

    structure MyStructure {
        @xmlName("Foo")
        foo: String

        bar: String
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure>
        <Foo>example</Foo>
        <bar>example</bar>
    </MyStructure>

A namespace prefix can be inserted before the element name. Given the
following

.. code-block:: smithy

    structure AnotherStructure {
        @xmlName("hello:foo")
        foo: String
    }

The XML serialization is:

.. code-block:: xml

    <AnotherStructure>
        <hello:foo>example</hello:foo>
    </AnotherStructure>


.. smithy-trait:: smithy.api#xmlNamespace
.. _xmlNamespace-trait:

``xmlNamespace`` trait
----------------------

Summary
    Adds an `XML namespace`_ to an XML element.
Trait selector
    ``:is(service, member, simpleType, list, map, structure, union)``

    *Service, simple types, list, map, structure, or union*
Value type
    ``structure``
Conflicts with
    :ref:`xmlAttribute-trait`

The ``xmlNamespace`` trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - uri
      - ``string`` value containing a valid URI
      - **Required**. The namespace URI for scoping this XML element.
    * - prefix
      - ``string`` value
      - The `namespace prefix`_ for elements from this namespace. Values
        provides for ``prefix`` property MUST adhere to the
        :token:`smithy:XmlIdentifier` production.

Given the following:

.. code-block:: smithy

    @xmlNamespace(uri: "http://foo.com")
    structure MyStructure {
        foo: String
        bar: String
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure xmlns="http://foo.com">
        <foo>example</foo>
        <bar>example</bar>
    </MyStructure>

Given the following:

.. code-block:: smithy

    @xmlNamespace(uri: "http://foo.com", prefix: "baz")
    structure MyStructure {
        foo: String

        @xmlName("baz:bar")
        bar: String
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure xmlns:baz="http://foo.com">
        <foo>example</foo>
        <baz:bar>example</baz:bar>
    </MyStructure>

.. _base64 encoded: https://tools.ietf.org/html/rfc4648#section-4
.. _XML namespace: https://www.w3.org/TR/REC-xml-names/
.. _namespace prefix: https://www.w3.org/TR/REC-xml-names/#NT-Prefix


See also
========

* :doc:`http-bindings`
