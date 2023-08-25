.. _constraint-traits:

=================
Constraint traits
=================

Constraint traits are used to constrain the values that can be provided
for a shape.


.. smithy-trait:: smithy.api#enum
.. _enum-trait:

--------------
``enum`` trait
--------------

Summary
    Constrains the acceptable values of a string to a fixed set.
Trait selector
    ``string``
Value type
    ``list`` of *enum definition* structures.

Smithy models SHOULD apply the enum trait when string shapes have a fixed
set of allowable values.

An *enum definition* is a structure that supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - value
      - string
      - **Required**. Defines the enum value that is sent over the wire. Values
        MUST NOT be empty. Values MUST be unique across all enum definitions in
        an ``enum`` trait.
    * - name
      - string
      - Defines a constant name that can be used in programming languages to
        reference an enum ``value``. A ``name`` is not required, though
        their use is strongly encouraged to help tools like code generators
        safely and reliably create symbols that represent specific values.

        Validation constraints:

        * Names MUST start with an upper or lower case ASCII Latin letter
          (``A-Z`` or ``a-z``), or the ASCII underscore (``_``) and be
          followed by zero or more upper or lower case ASCII Latin letters
          (``A-Z`` or ``a-z``), ASCII underscores (``_``), or ASCII digits
          (``0-9``). That is, names MUST match the following regular
          expression: ``^[a-zA-Z_]+[a-zA-Z_0-9]*$``.
        * The following stricter rules are recommended for consistency: Enum
          constant names SHOULD NOT contain any lowercase ASCII Latin letters
          (``a-z``) and SHOULD NOT start with an ASCII underscore (``_``).
          That is, enum names SHOULD match the following regular expression:
          ``^[A-Z]+[A-Z_0-9]*$``.
        * Names MUST be unique across all enum definitions of an ``enum``
          trait.
        * If any enum definition has a ``name``, then all entries in the
          ``enum`` trait MUST have a ``name``.
    * - documentation
      - string
      - Defines documentation about the enum value in the CommonMark_ format.
    * - tags
      - ``[string]``
      - Attaches a list of tags that allow the enum value to be categorized and
        grouped.
    * - deprecated
      - ``boolean``
      - Whether the enum value should be considered deprecated for consumers of
        the Smithy model.

.. note::

    Code generators MAY choose to represent enums as programming language
    constants. Those that do SHOULD use the enum definition's ``name`` property,
    if specified. Consumers that choose to represent enums as constants SHOULD
    ensure that unknown enum names returned from a service do not cause runtime
    failures.

The following example defines an enum of valid string values for ``MyString``.

.. tabs::

    .. code-tab:: smithy

        @enum([
            {
                value: "t2.nano",
                name: "T2_NANO",
                documentation: """
                    T2 instances are Burstable Performance
                    Instances that provide a baseline level of CPU
                    performance with the ability to burst above the
                    baseline.""",
                tags: ["ebsOnly"]
            },
            {
                value: "t2.micro",
                name: "T2_MICRO",
                documentation: """
                    T2 instances are Burstable Performance
                    Instances that provide a baseline level of CPU
                    performance with the ability to burst above the
                    baseline.""",
                tags: ["ebsOnly"]
            },
            {
                value: "m256.mega",
                name: "M256_MEGA",
                deprecated: true
            }
        ])
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#enum": [
                            {
                                "value": "t2.nano",
                                "name": "T2_NANO",
                                "documentation": "T2 instances are ...",
                                "tags": [
                                    "ebsOnly"
                                ]
                            },
                            {
                                "value": "t2.micro",
                                "name": "T2_MICRO",
                                "documentation": "T2 instances are ...",
                                "tags": [
                                    "ebsOnly"
                                ]
                            },
                            {
                                "value": "m256.mega",
                                "name": "M256_MEGA",
                                "deprecated": true
                            }
                        ]
                    }
                }
            }
        }


.. smithy-trait:: smithy.api#idRef
.. _idref-trait:

---------------
``idRef`` trait
---------------

Summary
    Indicates that a string value MUST contain a valid absolute
    :ref:`shape ID <shape-id>`.

    The ``idRef`` trait is used primarily when declaring
    :ref:`trait shapes <trait-shapes>` in a model. A trait shape
    that targets a string shape with the ``idRef`` trait indicates that when
    the defined trait is applied to a shape, the value of the trait MUST be
    a valid shape ID. The ``idRef`` trait can also be applied at any level of
    nesting on shapes referenced by trait shapes.
Trait selector
    ``:test(string, member > string)``

    *A string shape or a member that targets a string shape*
Value type
    ``structure``

The ``idRef`` trait is a structure that supports the following optional
members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - failWhenMissing
      - ``boolean``
      - When set to ``true``, the shape ID MUST target a shape that can be
        found in the model.
    * - selector
      - ``string``
      - Defines the :ref:`selector <selectors>` that the resolved shape,
        if found, MUST match.

        ``selector`` defaults to ``*`` when not defined.
    * - errorMessage
      - ``string``
      - Defines a custom error message to use when the shape ID cannot be
        found or does not match the ``selector``.

        A default message is generated when ``errorMessage`` is not defined.

To illustrate an example, a custom trait named ``integerRef`` is defined.
This trait can be attached to any shape, and the value of the trait MUST
contain a valid shape ID that targets an integer shape in the model.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait
        @idRef(failWhenMissing: true, selector: "integer")
        string integerRef

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#integerRef": {
                    "type": "string",
                    "traits": {
                        "smithy.api#trait": {},
                        "smithy.api#idRef": {
                            "failWhenMissing": true,
                            "selector": "integer"
                        }
                    }
                }
            }
        }

Given the following model,

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @integerRef(NotFound)
        string InvalidShape1

        @integerRef(String)
        string InvalidShape2

        @integerRef("invalid-shape-id!")
        string InvalidShape3

        @integerRef(Integer)
        string ValidShape

        @integerRef(MyShape)
        string ValidShape2

        integer MyShape

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#InvalidShape1": {
                    "type": "string",
                    "traits": {
                        "smithy.example#integerRef": "NotFound"
                    }
                },
                "smithy.example#InvalidShape2": {
                    "type": "string",
                    "traits": {
                        "smithy.example#integerRef": "String"
                    }
                },
                "smithy.example#InvalidShape3": {
                    "type": "string",
                    "traits": {
                        "smithy.example#integerRef": "invalid-shape-id!"
                    }
                },
                "smithy.example#ValidShape": {
                    "type": "string",
                    "traits": {
                        "smithy.example#integerRef": "Integer"
                    }
                },
                "smithy.example#ValidShape2": {
                    "type": "string",
                    "traits": {
                        "smithy.example#integerRef": "smithy.example#MyShape"
                    }
                },
                "smithy.example#MyShape": {
                    "type": "integer"
                }
            }
        }

- ``InvalidShape1`` is invalid because the "NotFound" shape cannot be
  found in the model.
- ``InvalidShape2`` is invalid because "smithy.api#String" targets a
  string which does not match the "integer" selector.
- ``InvalidShape3`` is invalid because "invalid-shape-id!" is not a
  syntactically correct absolute shape ID.
- ``ValidShape`` is valid because "smithy.api#Integer" targets an integer.
- ``ValidShape2`` is valid because "MyShape" is a relative ID that targets
  ``smithy.example#MyShape``.


.. smithy-trait:: smithy.api#length
.. _length-trait:

----------------
``length`` trait
----------------

Summary
    Constrains a shape to minimum and maximum number of elements or size.
Trait selector
    ``:test(collection, map, string, blob, member > :is(collection, map, string, blob))``

    *Any list, set, map, string, or blob; or a member that targets one of these shapes*
Value type
    ``structure``

The length trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - min
      - ``number``
      - Integer value that represents the minimum inclusive length of a shape.
    * - max
      - ``number``
      - Integer value that represents the maximum inclusive length of a shape.

At least one of min, max is required.

The following table describes what a length trait constrains when applied to
the corresponding shape:

===========  =====================================
Shape        Length constrains
===========  =====================================
list         The number of members
set          The number of members
map          The number of key-value pairs
string       The number of `Unicode scalar values <https://www.unicode.org/glossary/#unicode_scalar_value>`__
blob         The size of the blob in bytes
===========  =====================================

.. tabs::

    .. code-tab:: smithy

        @length(min: 1, max: 10)
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#length": {
                            "min": 1,
                            "max": 10
                        }
                    }
                }
            }
        }


.. smithy-trait:: smithy.api#pattern
.. _pattern-trait:

-----------------
``pattern`` trait
-----------------

Summary
    Restricts string shape values to a specified regular expression.
Trait selector
    ``:test(string, member > string)``

    *A string or a member that targets a string*
Value type
    ``string``

Smithy regular expressions MUST be valid regular expressions according to the
`ECMA 262 regular expression dialect`_. Patterns SHOULD avoid the use of
conditionals, directives, recursion, lookahead, look-behind, back-references,
and look-around in order to ensure maximum compatibility across programming
languages.

.. warning::

    Pattern values should be chosen with care, as regex evaluation can be
    expensive. Regular expressions SHOULD be tested against a range of
    potentially malformed inputs to ensure that the execution of the regular
    expression match does not lead to a potential denial of service. See
    `OWASP Regular expression Denial of Service`_ for more information.

.. important::

    The ``pattern`` trait does not implicitly add a leading ``^`` or trailing
    ``$`` to match an entire string. For example, ``@pattern("\\w+")`` matches
    both "hello" and "!hello!" because it requires that just part of the
    string matches the regular expression, whereas ``@pattern("^\\w+$")``
    requires that the entire string matches the regular expression.

.. note::

    Pattern values that contain ``\`` need to :ref:`escape it <string-escape-characters>`.
    For example, the regular expression ``^\w+$`` would be specified as
    ``@pattern("^\\w+$")``.

.. tabs::

    .. code-tab:: smithy

        @pattern("\\w+")
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#pattern": "\\w+"
                    }
                }
            }
        }


.. smithy-trait:: smithy.api#private
.. _private-trait:

-----------------
``private`` trait
-----------------

Summary
    Prevents models defined in a different namespace from referencing the
    targeted shape.
Trait selector
    ``*``
Value type
    Annotation trait

Shapes marked as ``private`` cannot be accessed outside of the namespace in
which the shape is defined. The ``private`` trait is meant only to control
access from within the model itself and SHOULD NOT influence code-generation
of the targeted shape.


.. smithy-trait:: smithy.api#range
.. _range-trait:

---------------
``range`` trait
---------------

Summary
    Restricts allowed values of byte, short, integer, long, float, double,
    bigDecimal, and bigInteger shapes within an acceptable lower and upper
    bound.
Trait selector
    ``:test(number, member > number)``

    *A number or a member that targets a number*
Value type
    ``structure``

The range trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - min
      - ``bigDecimal``
      - Specifies the allowed inclusive minimum value.
    * - max
      - ``bigDecimal``
      - Specifies the allowed inclusive maximum value.

At least one of ``min`` or ``max`` is required. ``min`` and ``max`` accept both
integers and real numbers. Real numbers may only be applied to float, double,
or bigDecimal shapes. ``min`` and ``max`` MUST fall within the allowable range
of the targeted numeric shape to which it is applied.

.. tabs::

    .. code-tab:: smithy

        @range(min: 1, max: 10)
        integer MyInt

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyInt": {
                    "type": "integer",
                    "traits": {
                        "smithy.api#range": {
                            "min": 1,
                            "max": 10
                        }
                    }
                }
            }
        }


.. smithy-trait:: smithy.api#required
.. _required-trait:

------------------
``required`` trait
------------------

Summary
    Marks a structure member as required, meaning a value for the member MUST
    be present and not set to ``null``.
Trait selector
    ``structure > member``

    *Member of a structure*
Value type
    Annotation trait.

The required trait applies to structure data, operation input, output, and
errors. When a member that is part of the input of an operation is marked as
required, a client MUST provide a value for the member when calling the
operation. When a member that is part of the output of an operation or an
error is marked as required, a service MUST provide a value for the member
in a response.

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @required
            foo: FooString,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.example#FooString",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                }
            }
        }

.. seealso::

   :ref:`recommended-trait`


.. smithy-trait:: smithy.api#uniqueItems
.. _uniqueItems-trait:

---------------------
``uniqueItems`` trait
---------------------

Summary
    Indicates that the items in a :ref:`list <list>` MUST be unique
    based on :ref:`value-equality`.
Trait selector
    ``list :not(> member ~> :is(float, double, document))``

    *A list that does not transitively contain floats, doubles, or documents*
Conflicts with
    * :ref:`sparse-trait`
Value type
    Annotation trait.


.. code-block:: smithy

    @uniqueItems
    list MyList {
        member: String
    }


.. _value-equality:

Value equality
==============

Two values are considered equal if:

* They are the same type.
* Both are strings and are the same codepoint-for-codepoint.
* Both are blobs and are the same byte-for-byte.
* Both are booleans and are both true or are both false.
* Both are timestamps and refer to the same instant in time.
* Both are lists and have an equal value item-for-item. Note that
  sets, a deprecated data type, are treated exactly like lists.
* Both are maps, have the same number of entries, and each key value
  pair in one map has an equal key value pair in the other map. The
  order of entries does not matter.
* Both are numbers of the same type and have the same mathematical value.
* Both are structures of the same type and have the same members with
  equal values.
* Both are unions of the same type, are set to the same member, and the
  set members have the same value.

.. note::

    Floats, doubles, and documents are not allowed in ``@uniqueItems`` lists
    because they only allow for partial equivalence and require special care
    to determine if two values are considered equal.


.. _precedence:

----------------
Trait precedence
----------------

Some constraint traits can be applied to shapes as well as members.
Constraint traits applied to members take precedence over constraint
traits applied to the shape targeted by members.

In the below example, the ``range`` trait applied to ``numberOfItems``
takes precedence over the one applied to ``PositiveInteger``. The resolved
minimum will be ``7``, and the maximum ``12``.

.. tabs::

    .. code-tab:: smithy

        structure ShoppingCart {
            @range(min: 7, max:12)
            numberOfItems: PositiveInteger
        }

        @range(min: 1)
        integer PositiveInteger

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.example#PositiveInteger",
                            "traits": {
                                "smithy.api#range": {
                                    "min": 7,
                                    "max": 12
                                }
                            }
                        }
                    }
                },
                "smithy.example#PositiveInteger": {
                    "type": "integer",
                    "traits": {
                        "smithy.api#range": {
                            "min": 1
                        }
                    }
                }
            }
        }

.. _ECMA 262 regular expression dialect: https://www.ecma-international.org/ecma-262/8.0/index.html#sec-patterns
.. _CommonMark: https://spec.commonmark.org/
.. _OWASP Regular expression Denial of Service: https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS
.. _JSON Schema "Instance Equality": https://json-schema.org/draft/2020-12/json-schema-core.html#rfc.section.4.2.2
