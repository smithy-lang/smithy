.. _xml-binding-traits:

==================
XML binding traits
==================

This document defines how to bind Smithy shapes to XML documents. Smithy
defines several traits that are used to influence the serialization of
shapes with XML based protocols.


.. _serializing-xml-shapes:

----------------------
Serializing XML shapes
----------------------

This document provides recommendations on how Smithy structures and
shapes within structures SHOULD be serialized with XML based protocols;
however, protocols MAY choose to deviate from these recommendations
if necessary.


.. _xml-structure-and-union-serialization:

Structure and union serialization
=================================

All XML serialization starts with a structure or union. The shape name of a
structure/union is used as the outermost XML element name. Members of a
structure/union are serialized as nested XML elements where the name of the
element is the same as the name of the member.

For example, given the following:

.. code-block:: smithy

    structure MyStructure {
        foo: String,
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure>
        <foo>example</foo>
    </MyStructure>


Custom XML element names
------------------------

Structure/union member element names can be changed using the
:ref:`xmlname-trait`.


XML attributes
--------------

The :ref:`xmlattribute-trait` is used to serialize a structure
member as an XML attribute.


``xmlName`` on structures and unions
------------------------------------

An ``xmlName`` trait applied to a structure or union changes the element name
of the serialized shape; however, it does not influence the serialization of
members that target it. Given the following:

.. tabs::

    .. code-tab:: smithy

        @xmlName("AStruct")
        structure A {
            b: B,
        }

        @xmlName("BStruct")
        structure B {
            hello: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#A": {
                    "type": "structure",
                    "members": {
                        "b": {
                            "target": "smithy.example#B"
                        }
                    },
                    "traits": {
                        "smithy.api#xmlName": "AStruct"
                    }
                },
                "smithy.example#B": {
                    "type": "structure",
                    "members": {
                        "hello": {
                            "target": "smithy.api#String"
                        }
                    },
                    "traits": {
                        "smithy.api#xmlName": "BStruct"
                    }
                }
            }
        }

The XML serialization of ``AStruct`` is:

.. code-block:: xml

    <AStruct>
        <b>
            <hello>value</hello>
        </b>
    </AStruct>


Simple type serialization
=========================

The following table defines how simple types are serialized in XML documents.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Shape
      - Serialization
    * - blob
      - Serialized as a `base64 encoded`_ string

        .. code-block:: smithy

            structure Struct {
                binary: Blob,
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
      - Serialized as `RFC 3339`_ date-time value.

        .. code-block:: smithy

              structure Struct {
                  date: Timestamp,
              }

        given a value of ``1578255206`` for ``date``:

        .. code-block:: xml

            <Struct>
                <date>2020-01-05T20:13:26Z</date>
            </Struct>

    * - document
      - .. warning::

            Document shapes are not recommended for use in XML based protocols.


.. _xml-list-and-set-serialization:

List and set serialization
==========================

List and set shapes use the same serialization semantics. List and set shapes
can be serialized as wrapped lists (the default behavior) or flattened lists.


Wrapped list serialization
--------------------------

A wrapped list or set is serialized in an XML element where each value is
serialized in a nested element named ``member``. For example, given the
following:

.. code-block:: smithy

    structure Foo {
        values: MyList
    }

    list MyList {
        member: String,
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

The :ref:`xmlname-trait` can be applied to the member of a list or set to
change the nested element name. For example, given the following:

.. tabs::

    .. code-tab:: smithy

        structure Foo {
            values: MyList
        }

        list MyList {
            @xmlName("Item")
            member: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Foo": {
                    "type": "structure",
                    "members": {
                        "values": {
                            "target": "smithy.example#MyList"
                        }
                    }
                },
                "smithy.example#MyList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#xmlName": "Item"
                        }
                    }
                }
            }
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
----------------------------

The :ref:`xmlflattened-trait` can be used to unwrap the values of list/set
into a containing structure/union. The name of the elements repeated within
the structure/union is based on the structure/union member name. For
example, given the following:

.. code-block:: smithy

    structure Foo {
        @xmlFlattened
        flat: MyList,
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
        flat: MySet,
    }

    set MySet {
        member: String
    }

The XML serialization of ``Choice`` is:

.. code-block:: xml

    <Choice>
        <Hi>example1</Hi>
        <Hi>example2</Hi>
        <Hi>example3</Hi>
    </Choice>

The ``xmlName`` trait applied to the member of a list/set has no effect when
serializing a flattened list into a structure/union. For example, given the
following:

.. code-block:: smithy

    union Choice {
        @xmlFlattened
        flat: MySet,
    }

    set MySet {
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
=================

Map shapes can be serialized as wrapped maps (the default behavior) or
flattened maps.


Wrapped map serialization
-------------------------

A wrapped map is serialized in an XML element where each value is
serialized in a nested element named ``entry`` that contains a nested
``key`` and ``value`` element. For example, given the following:

.. code-block:: smithy

    structure Foo {
        values: MyMap
    }

    map MyMap {
        key: String,
        value: String,
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
        key: String,

        @xmlName("Setting")
        value: String,
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
---------------------------

The :ref:`xmlFlattened-trait` can be used to flatten the members of map
into a containing structure/union. For example, given the following:

.. tabs::

    .. code-tab:: smithy

        structure Bar {
            @xmlFlattened
            flatMap: MyMap
        }

        map MyMap {
            key: String,
            value: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Bar": {
                    "type": "structure",
                    "members": {
                        "flatMap": {
                            "target": "smithy.example#MyMap",
                            "traits": {
                                "smithy.api#xmlFlattened": {}
                            }
                        }
                    }
                },
                "smithy.example#MyMap": {
                    "type": "map",
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    }
                }
            }
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
        flat: MyMap,
    }

    map MyMap {
        key: String,
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
        flat: MyMap,
    }

    map MyMap {
        @xmlName("Name")
        key: String,

        @xmlName("Setting")
        value: String,
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

----------------------
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

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @xmlAttribute
            foo: String,

            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#xmlAttribute": {}
                            }
                        },
                        "bar": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
        }

The XML serialization is:

.. code-block:: xml

    <MyStructure foo="example">
        <bar>example</bar>
    </MyStructure>

The serialized attribute name can be changed using the :ref:`xmlname-trait`.
Given the following:

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @xmlAttribute
            @xmlName("NotFoo")
            foo: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#xmlAttribute": {},
                                "smithy.api#xmlName": "NotFoo"
                            }
                        }
                    }
                }
            }
        }

The XML serialization is:

.. code-block:: xml

    <MyStructure NotFoo="example"/>


.. smithy-trait:: smithy.api#xmlFlattened
.. _xmlFlattened-trait:

----------------------
``xmlFlattened`` trait
----------------------

Summary
    Unwraps the values of a list or map into the containing structure.
Trait selector
    .. code-block:: none

        :is(structure, union) > :test(member > :test(collection, map))

    *Member of a structure or union that targets a list, set, or map*
Value type
    Annotation trait

Given the following:

.. tabs::

    .. code-tab:: smithy

        structure Foo {
            @xmlFlattened
            flat: MyList,

            nested: MyList,
        }

        list MyList {
            member: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Foo": {
                    "type": "structure",
                    "members": {
                        "flat": {
                            "target": "smithy.example#MyList",
                            "traits": {
                                "smithy.api#xmlFlattened": {}
                            }
                        },
                        "nested": {
                            "target": "smithy.example#MyList"
                        }
                    }
                },
                "smithy.example#MyList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
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

.. tabs::

    .. code-tab:: smithy

        structure Foo {
            @xmlFlattened
            flat: MyMap,

            notFlat: MyMap,
        }

        map MyMap {
            key: String
            value: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Foo": {
                    "type": "structure",
                    "members": {
                        "flat": {
                            "target": "smithy.example#MyMap",
                            "traits": {
                                "smithy.api#xmlFlattened": {}
                            }
                        },
                        "notFlat": {
                            "target": "smithy.example#MyMap"
                        }
                    }
                },
                "smithy.example#MyMap": {
                    "type": "map",
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    }
                }
            }
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

-----------------
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
        XmlName       :`XmlIdentifier` / (`XmlIdentifier` ":" `XmlIdentifier`)
        XmlIdentifier :(ALPHA / "_") *(ALPHA / DIGIT / "-" / "_")

By default, structure properties are serialized in attributes or nested
elements using the same name as the structure member name. Given the following:

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @xmlName("Foo")
            foo: String,

            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#xmlName": "Foo"
                            }
                        },
                        "bar": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
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
        foo: String,
    }

The XML serialization is:

.. code-block:: xml

    <AnotherStructure>
        <hello:foo>example</hello:foo>
    </AnotherStructure>


.. smithy-trait:: smithy.api#xmlNamespace
.. _xmlNamespace-trait:

----------------------
``xmlNamespace`` trait
----------------------

Summary
    Adds an `XML namespace`_ to an XML element.
Trait selector
    ``:is(service, member, simpleType, collection, map, structure, union)``

    *Service, simple types, list, map, set, structure, or union*
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

.. tabs::

    .. code-tab:: smithy

        @xmlNamespace(uri: "http://foo.com")
        structure MyStructure {
            foo: String,
            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String"
                        },
                        "bar": {
                            "target": "smithy.api#String"
                        }
                    },
                    "traits": {
                        "smithy.api#xmlNamespace": {
                            "uri": "http://foo.com"
                        }
                    }
                }
            }
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
        foo: String,

        @xmlName("baz:bar")
        bar: String,
    }

The XML serialization is:

.. code-block:: xml

    <MyStructure xmlns:baz="http://foo.com">
        <foo>example</foo>
        <baz:bar>example</baz:bar>
    </MyStructure>

.. _base64 encoded: https://tools.ietf.org/html/rfc4648#section-4
.. _RFC 3339: https://tools.ietf.org/html/rfc3339
.. _XML namespace: https://www.w3.org/TR/REC-xml-names/
.. _namespace prefix: https://www.w3.org/TR/REC-xml-names/#NT-Prefix
