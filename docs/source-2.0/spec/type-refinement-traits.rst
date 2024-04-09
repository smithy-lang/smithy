----------------------
Type refinement traits
----------------------

Type refinement traits are traits that significantly refine, or change, the
type of a shape.

.. smithy-trait:: smithy.api#default

.. _default-trait:

``default`` trait
=================

Summary
    Provides a structure member with a default value.
Trait selector
    ``:is(simpleType, list, map, structure > member :test(> :is(simpleType, list, map)))``

    A simple type, list, map, or a member of a structure that targets a simple type, list, or map.
Value type
    Document type.
See also
    * :ref:`structure-optionality`
    * :ref:`required-trait`
    * :ref:`clientOptional-trait`
    * :ref:`input-trait`

The ``@default`` trait assigns a default value to a structure member. The
following example defines a structure with a "language" member that has a
default value:

.. code-block:: smithy

    structure Message {
        @required
        title: String

        language: Language = "en"
    }

    enum Language {
        EN = "en"
    }

The above example uses syntactic sugar to apply the ``@default`` trait. The
``Message`` definition is exactly equivalent to:

.. code-block:: smithy

    structure Message {
        @required
        title: String

        @default("en")
        language: Language
    }

The ``@default`` trait can be added to root-level simple types, lists, or
maps. This can serve as a kind of template to enforce default values across
structure members in a model. Any structure member that targets a shape
marked with ``@default`` MUST also add a matching ``@default`` trait to the
member.

.. code-block:: smithy

    @default(0)
    integer ZeroValueInteger

    structure Message {
        zeroValueInteger: ZeroValueInteger = 0 // must be repeated and match the target.
    }

The ``@default`` trait on a structure member can be set to ``null`` to
explicitly indicate that the member has no default value or to override the default
value requirement of a targeted shape.

.. code-block:: smithy

    @default(0)
    integer ZeroValueInteger

    structure Message {
        zeroValueInteger: ZeroValueInteger = null // forces the member to be optional
    }

.. note::

    * The ``@default`` trait on root-level shapes has no impact when targeted by
      any other shape than a structure member.
    * The ``@default`` trait on root-level shapes cannot be set to ``null``.
    * The :ref:`clientOptional-trait` applied to a member marked with the
      ``default`` trait causes non-authoritative generators to ignore the
      ``default`` trait.


Default value constraints
-------------------------

The value of the ``@default`` trait MUST be compatible with the shape targeted
by the member and any applied constraint traits (for example, values for
numeric types MUST be numbers that fit within the targeted type and match any
:ref:`range <range-trait>` constraints, string types match any
:ref:`length <length-trait>` or :ref:`pattern <pattern-trait>` traits, etc).

The following shapes have restrictions on their default values:

* enum: can be set to any valid string *value* of the enum.
* intEnum: can be set to any valid integer *value* of the enum.
* document: can be set to ``null``, ```true``, ``false``, string, numbers,
  an empty list, or an empty map.
* list: can only be set to an empty list.
* map: can only be set to an empty map.
* structure: no default value.
* union: no default value.


Impact on API design
--------------------

The ``@default`` trait SHOULD NOT be used for partial updates or patch style
operations where it is necessary to differentiate between omitted values and
explicitly set values. Assigning default values is typically something that
occurs during deserialization, and as such, it is impossible for a server to
differentiate between whether a property was set to its default value or if a
property was omitted.


Updating default values
-----------------------

The default value of a root-level shape MUST NOT be changed nor can the default
trait be added or removed from an existing root-level shape. Changing the
default value of a root-level shape would cause any member reference to the
shape to break and could inadvertently impact code generated types for the
shape.

The default value of a member SHOULD NOT be changed. However, it MAY be
necessary in extreme cases to change a default value if changing the default
value addresses a customer-impacting issue or availability issue for a service.
Changing default values can result in parties disagreeing on the default value
of a member because they are using different versions of the same model.


Default value serialization
---------------------------

1. Implementations that ignore ``default`` traits do not assume a default
   value for a member. For example, non-authoritative implementations
   will ignore the ``default`` trait when a member is marked with the
   :ref:`clientOptional-trait`. These implementations would serialize any
   explicitly given value, even if it happens to be the default value.
2. All effective default values SHOULD be serialized. This ensures that
   messages are unambiguous and do not change during deserialization if the
   default value for a member changes after the message was serialized.
3. To avoid information disclosure, implementations MAY choose to not serialize
   a default value if the member is marked with the :ref:`internal-trait`.
4. A member marked ``@required`` MUST be serialized, including members that
   have a default.


.. smithy-trait:: smithy.api#addedDefault
.. _addedDefault-trait:

``addedDefault`` trait
======================

Summary
    Indicates that the :ref:`default-trait` was added to a structure member
    after initially publishing the member. This allows tooling to decide
    whether to ignore the ``@default`` trait if it will break backward
    compatibility in the tool.
Trait selector
    ``structure > member [trait|default]``

    *Member of a structure marked with the default trait*
Value type
    Annotation trait.
See also
    * :ref:`structure-optionality`
    * :ref:`default-trait`
    * :ref:`clientOptional-trait`
    * :ref:`input-trait`
    * :ref:`recommended-trait`


.. smithy-trait:: smithy.api#required
.. _required-trait:

``required`` trait
==================

Summary
    Marks a structure member as required, meaning a value for the member MUST
    be present.
Trait selector
    ``structure > member``

    *Member of a structure*
Value type
    Annotation trait.
See also
    * :ref:`structure-optionality`
    * :ref:`default-trait`
    * :ref:`clientOptional-trait`
    * :ref:`input-trait`
    * :ref:`recommended-trait`

The following example defines a structure with a required member.

.. code-block:: smithy

    structure MyStructure {
        @required
        foo: FooString
    }

.. important:: The required trait isn't just for inputs

    The required trait indicates that value MUST always be present for a
    member. It applies to all shapes, including inputs of operations, outputs
    of operations, and errors.


.. smithy-trait:: smithy.api#clientOptional
.. _clientOptional-trait:

``clientOptional`` trait
========================

Summary
    Requires that non-authoritative generators like clients treat a structure
    member as optional regardless of if the member is also marked with the
    :ref:`required-trait` or :ref:`default-trait`.
Trait selector
    ``structure > member``
Value type
    Annotation trait
See also
    * :ref:`structure-optionality`
    * :ref:`required-trait`
    * :ref:`default-trait`
    * :ref:`input-trait`

For cases when a service is unsure if a member will be required forever, the
member can be marked with the ``@clientOptional`` trait to ensure that
non-authoritative consumers of the model like clients treat the member as
optional. The ``@required`` trait can be backward compatibly removed from a
member marked as ``@clientOptional`` (and does not need to be replaced with
the ``@default`` trait). This causes the ``@required`` and ``@default`` traits
to function only as a server-side concern.

The ``@required`` trait on ``foo`` in the following structure is considered a
validation constraint rather than a type refinement trait:

.. code-block:: smithy

    structure Foo {
        @required
        @clientOptional
        foo: String
    }

.. note::

    Structure members in Smithy are automatically considered optional. For example,
    the following structure:

    .. code-block:: smithy

        structure Foo {
            baz: String
        }

    Is equivalent to the following structure:

    .. code-block:: smithy

        structure Foo {
            @clientOptional
            baz: String
        }


.. smithy-trait:: smithy.api#enumValue
.. _enumValue-trait:

``enumValue`` trait
===================

Summary
    Defines the value of an :ref:`enum <enum>` or :ref:`intEnum <intEnum>`.
    For enum shapes, a non-empty string value must be used. For intEnum
    shapes, an integer value must be used.
Trait selector
    ``:is(enum, intEnum) > member``
Value type
    ``string`` or ``integer``

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    enum Enum {
        @enumValue("foo")
        FOO
    }

    intEnum IntEnum {
        @enumValue(1)
        FOO
    }

The following enum definition uses syntactic sugar that is exactly equivalent:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    enum Enum {
        FOO = "foo"
    }

    intEnum IntEnum {
        FOO = 1
    }


.. smithy-trait:: smithy.api#error
.. _error-trait:

``error`` trait
===============

Summary
    Indicates that a structure shape represents an error. All shapes
    referenced by the :ref:`errors list of an operation <operation>`
    MUST be targeted with this trait.
Trait selector
    ``structure``
Value type
    ``string`` that MUST be set to "client" or "server" to indicate if the
    client or server is at fault for the error.
Conflicts with
    :ref:`trait <trait-shapes>`

The following structure defines a throttling error.

.. code-block:: smithy

    @error("client")
    structure ThrottlingError {}

Note that this structure is lacking the ``retryable`` trait that generically
lets clients know that the error is retryable.

.. code-block:: smithy

    @error("client")
    @retryable
    structure ThrottlingError {}

When using an HTTP-based protocol, it is recommended to add an
:ref:`httpError-trait` to use an appropriate HTTP status code with
the error.

.. code-block:: smithy

    @error("client")
    @retryable
    @httpError(429)
    structure ThrottlingError {}

The ``message`` member of an error structure is special-cased. It contains
the human-readable message that describes the error. If the ``message`` member
is not defined in the structure, code generated for the error may not provide
an idiomatic way to access the error message (e.g., an exception message
in Java).

.. code-block:: smithy

    @error("client")
    @retryable
    @httpError(429)
    structure ThrottlingError {
        @required
        message: String
    }


.. smithy-trait:: smithy.api#input
.. _input-trait:

``input`` trait
===============

Summary
    Specializes a structure for use only as the input of a single operation,
    providing relaxed backward compatibility requirements for structure
    members.
Trait selector
    ``structure``
Value type
    Annotation trait.
Conflicts with
    * :ref:`output-trait`
    * :ref:`error-trait`
See also
    * :ref:`structure-optionality`

The following example defines an ``@input`` structure:

.. code-block:: smithy

    @input
    structure SomeOperationInput {
        @required
        name: String
    }

``@input`` structure constraints
--------------------------------

Structure shapes marked with the ``@input`` trait MUST adhere to the
following constraints:

1. They can only be referenced in the model as an operation's input.
2. They cannot be used as the input of more than one operation.
3. They SHOULD have a shape name that starts with the name of the
   operation that targets it (if any). For example, the input shape of the
   ``GetSprocket`` operation SHOULD be named ``GetSprocketInput``,
   ``GetSprocketRequest``, or something similar.

These constraints allow tooling to specialize operation input shapes in
ways that would otherwise require complex model transformations.

Impact on backward compatibility
--------------------------------

Required members of a structure marked with the ``@input`` trait are implicitly
considered :ref:`clientOptional <clientOptional-trait>`. It is backward
compatible to remove the ``@required`` trait from top-level members of
structures marked with the ``@input`` trait, and the ``@required`` trait does
not need to be replaced with the ``@default`` trait (though this is allowed
as well). This gives service teams the ability to remove the ``@required``
trait from top-level input members and loosen requirements without risking
breaking previously generated clients.


.. smithy-trait:: smithy.api#output
.. _output-trait:

``output`` trait
================

Summary
    Specializes a structure for use only as the output of a single operation.
Trait selector
    ``structure``
Value type
    Annotation trait.
Conflicts with
    :ref:`input-trait`, :ref:`error-trait`

``@output`` structure constraints
---------------------------------

Structure shapes marked with the ``@output`` trait MUST adhere to the
following constraints:

1. They can only be referenced in the model as an operation's output.
2. They cannot be used as the output of more than one operation.
3. They SHOULD have a shape name that starts with the name of the
   operation that targets it (if any). For example, the output shape of the
   ``GetSprocket`` operation SHOULD be named ``GetSprocketOutput``.

These constraints allow tooling to specialize operation output shapes in
ways that would otherwise require complex model transformations.


.. smithy-trait:: smithy.api#sparse
.. _sparse-trait:

``sparse`` trait
================

Summary
    Indicates that lists and maps MAY contain ``null`` values. The ``sparse``
    trait has no effect on map keys; map keys are never allowed to be ``null``.
Trait selector
    ``:is(list, map)``
Value type
    Annotation trait.

The following example defines a :ref:`list <list>` shape that MAY contain
``null`` values:

.. code-block:: smithy

    @sparse
    list SparseList {
        member: String
    }

The following example defines a :ref:`map <map>` shape that MAY contain
``null`` values:

.. code-block:: smithy

    @sparse
    map SparseMap {
        key: String
        value: String
    }


.. smithy-trait:: smithy.api#mixin
.. _mixin-trait:

``mixin`` trait
===============

Summary
    Indicates that the targeted shape is a mixin.
Trait selector
    ``:not(member)``
Value type
    ``structure``

The mixin trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - ``localTraits``
      - [:ref:`shape-id`]
      - A list of shape IDs which MUST reference valid traits that are applied
        directly to the mixin. The traits in the list are not copied onto
        shapes that use the mixin. This only affects traits applied to the
        mixin container shape and has no impact on the members contained within
        a mixin.

        .. note::

            The ``mixin`` trait is considered implicitly present in this
            property and does not need to be explicitly added.

.. code-block:: smithy

    @mixin
    structure BaseUser {
        id: String
    }

    structure UserDetails with [BaseUser] {
        alias: String
        email: String
    }

.. seealso::

    See :doc:`mixins` for details on how mixins work.
