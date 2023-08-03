.. _constraint-traits:

-----------------
Constraint traits
-----------------

Constraint traits are used to constrain the values that can be provided
for a shape. Constraint traits are for validation only and SHOULD NOT
impact the types signatures of generated code.

.. seealso:: :ref:`required-trait`, a :doc:`type refinement trait <type-refinement-traits>`
       that also functions like a constraint.


Constraint trait enforcement
============================

Constraint traits SHOULD be enforced after deserializing input. For example,
when a server deserializes a request from a client, the server SHOULD enforce
any defined constraint traits and reject the request if appropriate.

Constraint traits SHOULD NOT be enforced when serializing shapes or when
deserializing output. For example, when returning a response from a server
to a client, failing to serialize a response due to a constraint trait
violation would prevent a client from observing a state change of the server
and would provide no real recourse for the client or server to recover.


.. smithy-trait:: smithy.api#idRef
.. _idref-trait:

``idRef`` trait
===============

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

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @trait
    @idRef(failWhenMissing: true, selector: "integer")
    string integerRef

Given the following model,

.. code-block:: smithy

    $version: "2"
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

- ``InvalidShape1`` is invalid because the "NotFound" shape cannot be
  found in the model.
- ``InvalidShape2`` is invalid because "smithy.api#String" targets a
  string which does not match the "integer" selector.
- ``InvalidShape3`` is invalid because "invalid-shape-id!" is not a
  syntactically correct absolute shape ID.
- ``ValidShape`` is valid because "smithy.api#Integer" targets an integer.
- ``ValidShape2`` is valid because "MyShape" is a relative ID that targets
  ``smithy.example#MyShape``.


.. _length-trait:

``length`` trait
================

Summary
    Constrains a shape to minimum and maximum number of elements or size.
Trait selector
    ``:test(list, map, string, blob, member > :is(list, map, string, blob))``

    *Any list, map, string, or blob; or a member that targets one of these shapes*
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
map          The number of key-value pairs
string       The number of `Unicode scalar values <https://www.unicode.org/glossary/#unicode_scalar_value>`__
blob         The size of the blob in bytes
===========  =====================================

.. code-block:: smithy

    @length(min: 1, max: 10)
    string MyString

    @length(min: 1)
    string NonEmptyString

    @length(max: 10)
    string StringLessThanOrEqualToTen


.. smithy-trait:: smithy.api#pattern
.. _pattern-trait:

``pattern`` trait
=================

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

.. code-block:: smithy

    @pattern("^[A-Za-z]+$")
    string Alphabetic

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

    .. code-block:: smithy

        @pattern("\\w+")
        string ContainsWords


.. smithy-trait:: smithy.api#private
.. _private-trait:

``private`` trait
=================

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

Consider the following model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @private
    string PrivateString

The following model is invalid because it attempts to refer to
``PrivateString`` from another namespace:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example.other

    list StringList {
        member: PrivateString
    }


.. smithy-trait:: smithy.api#range
.. _range-trait:

``range`` trait
===============

Summary
    Restricts allowed values of number shapes within an acceptable lower and
    upper bound.
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

.. code-block:: smithy

    @range(min: 1, max: 10)
    integer OneToTen

    @range(min: 1)
    integer GreaterThanOne

    @range(max: 10)
    integer LessThanTen


.. smithy-trait:: smithy.api#uniqueItems
.. _uniqueItems-trait:

``uniqueItems`` trait
=====================

Summary
    Requires the items in a :ref:`list <list>` to be unique
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
--------------

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


.. smithy-trait:: smithy.api#enum
.. _enum-trait:

``enum`` trait
==============

.. danger::
    This trait is deprecated. An :ref:`enum shape <enum>` should be used
    instead.

Summary
    Constrains the acceptable values of a string to a fixed set.
Trait selector
    ``string :not(enum)``
Value type
    ``list`` of *enum definition* structures.

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

    While the :ref:`changeStringEnumsToEnumShapes <changeStringEnumsToEnumShapes>`
    transform can be used to convert to an enum shape, it is recommended to use
    the :ref:`enum shape <enum>` instead.


.. _ECMA 262 regular expression dialect: https://www.ecma-international.org/ecma-262/8.0/index.html#sec-patterns
.. _CommonMark: https://spec.commonmark.org/
.. _OWASP Regular expression Denial of Service: https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS
