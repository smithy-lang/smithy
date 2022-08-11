======================
Type refinement traits
======================

Type refinement traits are traits that significantly refine, or change,
the type of a shape.


.. smithy-trait:: smithy.api#box
.. _box-trait:

-------------
``box`` trait
-------------

Summary
    Indicates that a shape is boxed. When a structure :ref:`member <member>` is
    marked with this trait or the shape targeted by a structure member is marked
    with the ``box`` trait, the member may or may not contain a value, and the
    member has no :ref:`default value <default-values>`.

    Boolean, byte, short, integer, long, float, and double shapes are only
    considered boxed if they are marked with the ``box`` trait. All other
    shapes are always considered boxed.
Trait selector
    .. code-block:: none

        :test(boolean, byte, short, integer, long, float, double,
              member > :test(boolean, byte, short, integer, long, float, double))

    *A boolean, byte, short, integer, long, float, double shape or a member that targets one of these shapes*
Value type
    Annotation trait.

The ``box`` trait is primarily used to influence code generation. For example,
in Java, this might mean the value provided as the member of an aggregate
shape can be set to null. In a language like Rust, this might mean the value
is wrapped in an `Option type`_.

.. tabs::

    .. code-tab:: smithy

        @box
        integer BoxedInteger

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#BoxedInteger": {
                    "type": "integer",
                    "traits": {
                        "smithy.api#box": {}
                    }
                }
            }
        }

The :ref:`prelude <prelude>` contains predefined simple shapes that can be
used in all Smithy models, including boxed and unboxed shapes.


.. smithy-trait:: smithy.api#error
.. _error-trait:

---------------
``error`` trait
---------------

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

.. tabs::

    .. code-tab:: smithy

        @error("client")
        structure ThrottlingError {}

Note that this structure is lacking the ``retryable`` trait that generically
lets clients know that the error is retryable.

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @retryable
        structure ThrottlingError {}

When using an HTTP-based protocol, it is recommended to add an
:ref:`httpError-trait` to use an appropriate HTTP status code with
the error.

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @retryable
        @httpError(429)
        structure ThrottlingError {}

The ``message`` member of an error structure is special-cased. It contains
the human-readable message that describes the error. If the ``message`` member
is not defined in the structure, code generated for the error may not provide
an idiomatic way to access the error message (e.g., an exception message
in Java).

.. tabs::

    .. code-tab:: smithy

        @error("client")
        @retryable
        @httpError(429)
        structure ThrottlingError {
            @required
            message: String,
        }


.. smithy-trait:: smithy.api#input
.. _input-trait:

---------------
``input`` trait
---------------

Summary
    Specializes a structure for use only as the input of a single operation.
Trait selector
    ``structure``
Value type
    Annotation trait.
Conflicts with
    :ref:`output-trait`, :ref:`error-trait`

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


.. smithy-trait:: smithy.api#output
.. _output-trait:

----------------
``output`` trait
----------------

Summary
    Specializes a structure for use only as the output of a single operation.
Trait selector
    ``structure``
Value type
    Annotation trait.
Conflicts with
    :ref:`input-trait`, :ref:`error-trait`

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

----------------
``sparse`` trait
----------------

Summary
    Indicates that lists and maps MAY contain ``null`` values. The ``sparse``
    trait has no effect on map keys; map keys are never allowed to be ``null``.
Trait selector
    ``:is(list, map)``
Value type
    Annotation trait.

The following example defines a :ref:`list <list>` shape that MAY contain
``null`` values:

.. tabs::

    .. code-tab:: smithy

        @sparse
        list SparseList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#SparseList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String",
                    },
                    "traits": {
                        "smithy.api#sparse": {}
                    }
                }
            }
        }

The following example defines a :ref:`map <map>` shape that MAY contain
``null`` values:

.. tabs::

    .. code-tab:: smithy

        @sparse
        map SparseMap {
            key: String,
            value: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#SparseMap": {
                    "type": "map",
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    },
                    "traits": {
                        "smithy.api#sparse": {}
                    }
                }
            }
        }

.. _Option type: https://doc.rust-lang.org/std/option/enum.Option.html
