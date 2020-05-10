.. _traits:

======
Traits
======

*Traits* are model components that can be attached to :doc:`shapes <index>`
to describe additional information about the shape; shapes provide the
structure and layout of an API, while traits provide refinement and style.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


-------------------------
Applying traits to shapes
-------------------------

Trait values immediately preceding a shape definition are applied to the
shape.

The following example applies the ``sensitive`` and ``documentation`` trait
to ``MyString``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @sensitive
        @documentation("Contains a string")
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#documentation": "Contains a string",
                        "smithy.api#sensitive": {}
                    }
                }
            }
        }

The shape ID of a trait is *resolved* against :token:`use_statement`\s and the
current namespace in exactly the same same way as
:ref:`other shape IDs <relative-shape-id>`.

.. important::

    Trait names are case-sensitive; it is invalid, for example, to refer to
    the :ref:`documentation-trait` as "Documentation").


Apply statement
===============

Traits can be applied to shapes outside of a shape's definition using the
``apply`` statement. This can be useful for allowing different teams within
the same organization to independently own different facets of a model.
For example, a service team could own the Smithy model that defines the
shapes and traits of the API, and a documentation team could own a Smithy
model that applies documentation traits to the shapes.

Apply statements are formed using the following grammar:

.. productionlist:: smithy
    apply_statement :"apply" `ws` `shape_id` `ws` `trait` `br`

The following example applies the :ref:`documentation-trait` and
:ref:`length-trait` to the ``smithy.example#MyString`` shape:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        apply MyString @documentation("This is my string!")
        apply MyString @length(min: 1, max: 10)

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#documentation": "This is my string!",
                        "smithy.api#length": {
                            "min": 1,
                            "max": 10
                        }
                    }
                }
            }
        }

Traits can be applied to members too:

.. code-block:: smithy

    namespace smithy.example

    apply MyStructure$foo @documentation("Structure member documentation")
    apply MyUnion$foo @documentation("Union member documentation")
    apply MyList$member @documentation("List member documentation")
    apply MySet$member @documentation("Set member documentation")
    apply MyMap$key @documentation("Map key documentation")
    apply MyMap$value @documentation("Map key documentation")


Scope of member traits
======================

Traits that target :ref:`members <member>` apply only in the context of
the member shape and do not affect the shape targeted by the member. Traits
applied to a member supersede traits applied to the shape referenced by the
member and do not conflict.


.. _trait-values:

------------
Trait values
------------

The value that can be provided for a trait depends on its type. A value for a
trait is defined in the IDL by enclosing the value in parenthesis. Trait values
can only appear immediately before a shape.

.. productionlist:: smithy
    trait_statements    : *(`ws` `trait`) `ws`
    trait               :"@" `shape_id` [`trait_body`]
    trait_body          :"(" `ws` `trait_body_value` `ws` ")"
    trait_body_value    :`trait_structure` / `node_value`
    trait_structure     :`trait_structure_kvp` *(`ws` `comma` `trait_structure_kvp`)
    trait_structure_kvp :`node_object_key` `ws` ":" `ws` `node_value`

The following example applies various traits to a structure shape and its
members.

.. code-block:: smithy

    @documentation("An animal in the animal kingdom")
    structure Animal {
        @required
        name: smithy.api#String,

        @length(min: 0)
        age: smithy.api#Integer,
    }


Structure, map, and union trait values
======================================

Traits that are a ``structure``, ``union``, or ``map`` are defined using
a special syntax that places key-value pairs inside of the trait
parenthesis. Wrapping braces, "{" and "}", are not permitted.

.. code-block:: smithy
    :caption: Example

    @structuredTrait(foo: "bar", baz: "bam")

Nested structure, map, and union values are defined using
:ref:`node value <node-values>` productions:

.. code-block:: smithy

    @structuredTrait(
        foo: {
            bar: "baz",
            qux: "true",
        }
    )

Omitting a value is allowed on ``list``, ``set``, ``map``, and ``structure``
traits if the shapes have no ``length`` constraints or ``required`` members.


Annotation traits
=================

A structure trait with no members is called an *annotation trait*. It's hard
to predict what information a trait needs to capture when modeling a domain;
a trait might start out as a simple annotation, but later might benefit
from additional information. By defining an annotation trait rather than a
boolean trait, the trait can safely add optional members over time as needed.

The following example defines an annotation trait named ``foo``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait
        structure foo {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#foo": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#trait": {}
                    }
                }
            }
        }

The following applications of the ``foo`` annotation trait are equivalent:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @foo
        string MyString1

        @foo()
        string MyString2

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString1": {
                    "type": "string",
                    "traits": {
                        "smithy.api#foo": {}
                    }
                },
                "smithy.example#MyString2": {
                    "type": "string",
                    "traits": {
                        "smithy.api#foo": {}
                    }
                }
            }
        }

A member can be safely added to an annotation trait if the member is not
marked as :ref:`required <required-trait>`. The applications of the ``foo``
trait in the previous example and the following example are all valid even
after adding a member to the ``foo`` trait:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait
        structure foo {
            baz: String,
        }

        @foo(baz: "bar")
        string MyString4

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#foo": {
                    "type": "structure",
                    "members": {
                        "baz": {
                            "target": "smithy.api#String"
                        }
                    },
                    "traits": {
                        "smithy.api#trait": {}
                    }
                },
                "smithy.example#MyString4": {
                    "type": "string",
                    "traits": {
                        "smithy.api#foo": {
                            "baz": "bar"
                        }
                    }
                }
            }
        }


List and set trait values
=========================

Traits that are a ``list`` or ``set`` shape are defined inside
of brackets (``[``) and (``]``) using a :token:`node_array` production.

.. code-block:: smithy
    :caption: Example

    @tags(["a", "b"])


Other trait values
==================

All other trait values MUST adhere to the JSON type mappings defined
in :ref:`trait-node-values` table.

The following example defines a string trait value:

.. code-block:: smithy

    @documentation("Hello")


.. _trait-shapes:

---------------------
Defining trait shapes
---------------------

A *trait shape* is a shape that is specialized to function as a trait.
Traits are defined inside of a namespace by applying ``smithy.api#trait``
to a shape. This trait can only be applied to simple types, ``list``,
``map``, ``set``, ``structure``, and ``union`` shapes.

The following example defines a trait named ``myTraitName`` in the
``smithy.example`` namespace:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait(selector: "*")
        structure myTraitName {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#myTraitName": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#trait": {
                            "selector": "*"
                        }
                    }
                }
            }
        }

.. tip::

    By convention, trait shape names SHOULD use a lowercase name so that they
    visually stand out from normal shapes.

After a trait is defined, it can be applied to any shape that matches its
selector. The following example applies the ``smithy.example#myTraitName``
trait to the ``MyString`` shape using a trait shape ID that is relative to
the current namespace:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @myTraitName
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#myTraitName": {}
                    }
                }
            }
        }

Built-in traits defined in the Smithy :ref:`prelude <prelude>` namespace,
``smithy.api``, are automatically available in every Smithy model and
namespace through relative shape IDs.

.. important::

    The only valid reference to a trait shape is through applying the trait
    to a shape. Members and references within a model MUST NOT refer to
    trait shapes.


.. _trait-shape-properties:

Trait shape properties
======================

The ``smithy.api#trait`` trait is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - selector
      - string
      - A valid :ref:`selector <selectors>` that defines where the trait
        can be applied. For example, a ``selector`` set to ``:test(list, map)``
        means that the trait can be applied to a :ref:`list` or :ref:`map`
        shape. This value defaults to ``*`` if not set, meaning the trait can
        be applied to any shape.
    * - conflicts
      - [string]
      - Defines the shape IDs of traits that MUST NOT be applied to the same
        shape as the trait being defined. This allows traits to be defined as
        mutually exclusive. Relative shape IDs that are not resolved in the IDL
        while parsing are assumed to refer to traits defined in the prelude
        namespace, ``smithy.api``. Conflict shape IDs MAY reference unknown
        trait shapes that are not defined in the model.
    * - structurallyExclusive
      - string
      - One of "member" or "target". When set to "member", only a single
        member of a structure can be marked with the trait. When set to
        "target", only a single member of a structure can target a shape
        marked with this trait.

The following example defines two custom traits: ``beta`` and
``structuredTrait``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        /// A trait that can be applied to a member.
        @trait(selector: "structure > member")
        structure beta {}

        /// A trait that has members.
        @trait(selector: "string", conflicts: [beta])
        structure structuredTrait {
            @required
            lorem: StringShape,

            @required
            ipsum: StringShape,

            dolor: StringShape,
        }

        // Apply the "beta" trait to the "foo" member.
        structure MyShape {
            @required
            @beta
            foo: StringShape,
        }

        // Apply the structuredTrait to the string.
        @structuredTrait(
            lorem: "This is a custom trait!",
            ipsum: "lorem and ipsum are both required values.")
        string StringShape

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#beta": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "structure",
                        "smithy.api#trait": {
                            "selector": "structure > member"
                        },
                        "smithy.api#documentation": "A trait that can be applied to a member."
                    }
                },
                "smithy.example#structuredTrait": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "structure",
                        "smithy.api#trait": {
                            "selector": "string",
                            "conflicts": [
                                "smithy.example#beta"
                            ]
                        },
                        "smithy.api#members": {
                            "lorem": {
                                "target": "StringShape",
                                "required": true
                            },
                            "dolor": {
                                "target": "StringShape"
                            }
                        },
                        "smithy.api#documentation": "A trait that has members."
                    }
                },
                "smithy.example#MyShape": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "structure",
                        "smithy.api#members": {
                            "beta": {
                                "target": "StringShape",
                                "required": true,
                                "beta": true
                            }
                        }
                    }
                },
                "smithy.example#StringShape": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "string",
                        "smithy.api#structuredTrait": {
                            "lorem": "This is a custom trait!",
                            "ipsum": "lorem and ipsum are both required values."
                        }
                    }
                }
            }
        }


.. _trait-node-values:

-----------------
Trait node values
-----------------

The value provided for a trait MUST be compatible with the ``shape`` defined
for the trait. The following table defines each shape type that is available
to target from trait shapes and how values for those shapes are defined
in JSON and :token:`node <node_value>` values.

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Smithy type
      - JSON type
      - Description
    * - blob
      - string
      - A ``string`` value that is base64 encoded.
    * - boolean
      - boolean
      - Can be set to ``true`` or ``false``.
    * - byte
      - number
      - The value MUST fall within the range of -128 to 127
    * - short
      - number
      - The value MUST fall within the range of -32,768 to 32,767
    * - integer
      - number
      - The value MUST fall within the range of -2^31 to (2^31)-1.
    * - long
      - number
      - The value MUST fall within the range of -2^63 to (2^63)-1.
    * - float
      - number
      - A normal JSON number.
    * - double
      - number
      - A normal JSON number.
    * - bigDecimal
      - string | number
      - bigDecimal values can be serialized as strings to avoid rounding
        issues when parsing a Smithy model in various languages.
    * - bigInteger
      - string | number
      - bigInteger values can be serialized as strings to avoid truncation
        issues when parsing a Smithy model in various languages.
    * - string
      - string
      - The provided value SHOULD be compatible with the ``mediaType`` of the
        string shape if present; however, this is not validated by Smithy.
    * - timestamp
      - number | string
      - If a number is provided, it represents Unix epoch seconds with optional
        millisecond precision. If a string is provided, it MUST be a valid
        :rfc:`3339` string with optional millisecond precision and no
        UTC offset (for example, ``1990-12-31T23:59:60Z``).
    * - list and set
      - array
      - Each value in the array MUST be compatible with the referenced member.
    * - map
      - object
      - Each key MUST be compatible with the ``key`` member of the map, and
        each value MUST be compatible with the ``value`` member of the map.
    * - structure
      - object
      - All members marked as required MUST be provided in a corresponding
        key-value pair. Each key MUST correspond to a single member name of
        the structure. Each value MUST be compatible with the member that
        corresponds to the member name.
    * - union
      - object
      - The object MUST contain a single single key-value pair. The key MUST be
        one of the member names of the union shape, and the value MUST be
        compatible with the corresponding shape.

Trait values MUST be compatible with any constraint traits found related to the
shape being validated.


.. _trait-conflict-resolution:

-------------------------
Trait conflict resolution
-------------------------

Trait conflict resolution is used when the same trait is applied multiple
times to a shape. Duplicate traits applied to shapes are allowed in the
following cases:

1. If the trait is a ``list`` or ``set``, then the conflicting trait values
   are concatenated into a single trait value.
2. If both values are exactly equal, then the conflict is ignored.

All other instances of trait collisions are prohibited.

The following model definition is **valid** because the ``length`` trait is
duplicated on the ``MyList`` shape with the same values:

.. code-block:: smithy

    namespace smithy.example

    @length(min: 0, max: 10)
    list MyList {
        member: String
    }

    apply MyList @length(min: 0, max: 10)

The following model definition is **valid** because the ``tags`` trait is
a :ref:`list` shape:

.. code-block:: smithy

    namespace smithy.example

    @tags(["foo", "baz", "bar"])
    string MyString

    // This is a valid trait collision on a list trait, tags.
    // tags becomes ["foo", "baz", "bar", "bar", "qux"]
    apply MyString @tags(["bar", "qux"])

The following model definition is **invalid** because the ``length`` trait is
duplicated on the ``MyList`` shape with different values:

.. code-block:: smithy

    namespace smithy.example

    @length(min: 0, max: 10)
    list MyList {
        member: String
    }

    apply MyList @length(min: 10, max: 20)
