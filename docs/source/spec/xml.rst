==========
XML traits
==========

Protocols that serialize messages using XML SHOULD honor the ``xmlAttribute``,
``xmlFlattened``, ``xmlName``, and ``xmlNamespace`` traits when serializing
payloads.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _xmlAttribute-trait:

----------------------
``xmlAttribute`` trait
----------------------

Summary
    Moves a serialized object property to an attribute of the enclosing
    structure.
Trait selector
    .. code-block:: css

        :test(
            member:of(structure) > :test(
                boolean, number, string, timestamp
            )
        )

    *Structure members that target boolean, number, string, or timestamp*
Value type
    Annotation trait
Conflicts with
    :ref:`xmlNamespace-trait`

Given the following structure definition,

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @xmlAttribute
            foo: String,

            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyStructure": {
                        "type": "structure",
                        "members": {
                            "foo": {
                                "target": "String",
                                "xmlAttribute": true
                            },
                            "bar": {
                                "target": "String"
                            }
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"

the XML representation of the value would be serialized with the
following document:

.. code-block:: xml

    <MyStructure foo="abc">
        <bar>def</bar>
    </MyStructure>


.. _xmlFlattened-trait:

----------------------
``xmlFlattened`` trait
----------------------

Summary
    Moves serialized collection members from their collection element to that
    of the collection's container.
Trait selector
    ``:test(map, collection, member:of(structure) > :test(map, collection))``

    *Any map, list, or set or any structure member that targets a map, list, or set*
Value type
    Annotation trait

Given the following list definition:

.. tabs::

    .. code-tab:: smithy

        @xmlFlattened
        list MyList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyList": {
                        "type": "list",
                        "member": {
                            "target": "String"
                        },
                        "xmlFlattened": true
                    }
                }
            }
        }

and the following values provided for ``MyList``,

::

    "foo", "bar", "baz"

the XML representation of the value would be serialized with the
following document:

.. code-block:: xml

    <member>foo</member>
    <member>bar</member>
    <member>baz</member>

Given the following definition:

.. tabs::

    .. code-tab:: smithy

        @xmlFlattened
        map MyMap {
            key: String
            value: String
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyMap": {
                        "type": "map",
                        "key": {
                            "target": "String"
                        },
                        "value": {
                            "target": "String"
                        },
                        "xmlFlattened": true,
                        "xmlName": "MyMapEntry"
                    }
                }
            }
        }

and the following values provided for ``MyMap``:

.. code-block:: json

    {
        "foo": "bar",
        "bar": "baz"
    }

the XML representation of the value would be serialized with the following
document:

.. code-block:: xml

    <MyMapEntry>
        <key>foo</key>
        <value>bar</value>
    </MyMapEntry>
    <MyMapEntry>
        <key>bar</key>
        <value>baz</value>
    </MyMapEntry>


.. _xmlName-trait:

-----------------
``xmlName`` trait
-----------------

Summary
    Allows a serialized object property name to differ from a structure member
    name used in the model.
Trait selector
    ``*``
Value type
    ``string`` value

Given the following structure definition,

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @xmlName("Foo")
            foo: String,

            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyStructure": {
                        "type": "structure",
                        "members": {
                            "foo": {
                                "target": "String",
                                "xmlName": "Foo"
                            },
                            "bar": {
                                "target": "String"
                            }
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"

the XML representation of the value would be serialized with the
following document:

.. code-block:: xml

    <MyStructure>
        <Foo>abc</Foo>
        <bar>def</bar>
    </MyStructure>

.. note::

    Values for the ``xmlName`` trait must start with a letter (lower/upper
    case) or ``_``, followed by letters (lower/upper case), digits, ``_``, or
    ``-``. Values for an ``xmlName`` adhere to the following ABNF.

.. productionlist:: smithy
    xml_identifier :(ALPHA / "_")
                   :*(ALPHA / DIGIT / "-" / "_")
    xml_name       :xml_identifier / (xml_identifier ":" xml_identifier)


.. _xmlNamespace-trait:

----------------------
``xmlNamespace`` trait
----------------------

Summary
    Adds an xmlns namespace definition URI to an XML element.
Trait selector
    ``*``
Value type
    ``object`` value
Conflicts with
    :ref:`xmlAttribute-trait`

The ``xmlNamespace`` trait is an object that contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - uri
      - ``string`` value containing a valid URI
      - **Required**. The namespace URI for scoping this XML element.
    * - prefix
      - ``string`` value
      - The prefix for elements from this namespace.

Given the following structure definition,

.. tabs::

    .. code-tab:: smithy

        @xmlNamespace(uri: "http://foo.com")
        structure MyStructure {
            foo: String,
            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyStructure": {
                        "type": "structure",
                        "members": {
                            "foo": {
                                "target": "String"
                            },
                            "bar": {
                                "target": "String"
                            }
                        },
                        "xmlNamespace": {
                            "uri": "http://foo.com"
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"

the XML representation of the value would be serialized with the
following document:

.. code-block:: xml

    <MyStructure xmlns="http//foo.com">
        <foo>abc</foo>
        <bar>def</bar>
    </MyStructure>

Given the following definition with a prefix:

.. tabs::

    .. code-tab:: smithy

        @xmlNamespace(uri: "http://foo.com", prefix: "bar")
        structure MyStructure {
            foo: String,
            @xmlName("baz:bar")
            bar: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyStructure": {
                        "type": "structure",
                        "members": {
                            "foo": {
                                "target": "String"
                            },
                            "bar": {
                                "target": "String",
                                "xmlName": "baz:bar"
                            }
                        },
                        "xmlNamespace": {
                            "uri": "http://foo.com",
                            "prefix": "baz"
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"

the XML representation of the value would be serialized with the
following document:

.. code-block:: xml

    <MyStructure xmlns:baz="http//foo.com">
        <foo>abc</foo>
        <baz:bar>def</baz:bar>
    </MyStructure>

.. note::

    Values for the ``prefix`` option must start with a letter (lower/upper
    case) or ``_``, followed by letters (lower/upper case), digits, ``_``, or
    ``-``. Values for ``prefix`` adhere to the following ABNF.

.. productionlist:: smithy
    xml_prefix      :(ALPHA / "_")
                    :*(ALPHA / DIGIT / "-" / "_")

.. _xml-examples:

--------------------
Combining XML Traits
--------------------

Note that many of the XML payload serialization traits can be combined to
influence the overall structure of the payload.

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @xmlAttribute
            foo: String,

            @xmlName("Bar")
            bar: String,

            baz: MyList
        }

        @xmlFlattened
        list MyList {
            @xmlName("Item")
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyStructure": {
                        "type": "structure",
                        "members": {
                            "foo": {
                                "target": "String",
                                "xmlAttribute": true,
                            },
                            "bar": {
                                "target": "String",
                                "xmlName": "Bar"
                            },
                            "baz": {
                                "target": "MyList"
                            }
                        }
                    },
                    "MyList": {
                        "type": "list",
                        "member": {
                            "target": "String",
                            "xmlName": "Item"
                        },
                        "xmlFlattened": true
                    }
                }
            }
        }

Providing the following values provided for ``MyStructure``,

::

    "foo" = "abc"
    "bar" = "def"
    "baz" = ["ggg", "hhh", "iii"]


the XML representation of the value would be serialized with the
following document:

.. code-block:: xml

    <MyStructure foo="abc">
        <Bar>def</Bar>
        <Item>ggg</Item>
        <Item>hhh</Item>
        <Item>iii</Item>
    </MyStructure>
