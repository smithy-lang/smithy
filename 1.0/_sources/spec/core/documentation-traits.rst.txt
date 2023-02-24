.. _documentation-traits:

====================
Documentation traits
====================

Documentation traits are used to document and describe shapes in the model
in a way that does not materially affect the semantics of the model.

.. smithy-trait:: smithy.api#deprecated
.. _deprecated-trait:

--------------------
``deprecated`` trait
--------------------

Summary
    Marks a shape or member as deprecated.
Trait selector
    ``*``
Value type
    ``structure``

The ``deprecated`` trait is a structure that supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - message
      - ``string``
      - Provides a plain text message for a deprecated shape or member.
    * - since
      - ``string``
      - Provides a plain text date or version for when a shape or member was
        deprecated.

.. tabs::

    .. code-tab:: smithy

        @deprecated
        string SomeString

        @deprecated(message: "This shape is no longer used.", since: "1.3")
        string OtherString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#SomeString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#deprecated": {}
                    }
                },
                "smithy.example#OtherString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#deprecated": {
                            "message": "This shape is no longer used.",
                            "since": "1.3"
                        }
                    }
                }
            }
        }


.. smithy-trait:: smithy.api#documentation
.. _documentation-trait:

-----------------------
``documentation`` trait
-----------------------

Summary
    Adds documentation to a shape or member using the CommonMark_ format.
Trait selector
    ``*``
Value type
    ``string``

.. tabs::

    .. code-tab:: smithy

        @documentation("This *is* documentation about the shape.")
        string MyString


.. rubric:: Effective documentation

The *effective documentation trait* of a shape is resolved using the following
process:

#. Use the ``documentation`` trait of the shape, if present.
#. If the shape is a :ref:`member`, then use the ``documentation`` trait of
   the shape targeted by the member, if present.

For example, given the following model,

.. tabs::

    .. code-tab:: smithy

        structure Foo {
            @documentation("Member documentation")
            baz: Baz,

            bar: Baz,

            qux: String,
        }

        @documentation("Shape documentation")
        string Baz

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Foo": {
                    "type": "structure",
                    "members": {
                        "baz": {
                            "target": "smithy.example#Baz",
                            "traits": {
                                "smithy.api#documentation": "Member documentation"
                            }
                        },
                        "bar": {
                            "target": "smithy.example#Baz"
                        },
                        "qux": {
                            "target": "smithy.api#String"
                        }
                    }
                },
                "smithy.example#Baz": {
                    "type": "string",
                    "traits": {
                        "smithy.api#documentation": "Shape documentation"
                    }
                }
            }
        }

the effective documentation of ``Foo$baz`` resolves to "Member documentation",
``Foo$bar`` resolves to "Shape documentation", ``Foo$qux`` is not documented,
``Baz`` resolves to "Shape documentation", and ``Foo`` is not documented.


.. smithy-trait:: smithy.api#examples
.. _examples-trait:

------------------
``examples`` trait
------------------

Summary
    Provides example inputs and outputs for operations.
Trait selector
    ``operation``
Value type
    ``list`` of example structures

Each ``example`` trait value is a structure with the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - title
      - ``string``
      - **Required**. A short title that defines the example.
    * - documentation
      - ``string``
      - A longer description of the example in the CommonMark_ format.
    * - input
      - ``document``
      - Provides example input parameters for the operation. Each key is
        the name of a top-level input structure member, and each value is the
        value of the member.
    * - output
      - ``document``
      - Provides example output parameters for the operation. Each key is
        the name of a top-level output structure member, and each value is the
        value of the member.
    * - error
      - :ref:`examples-ErrorExample-structure`
      - Provides an error shape ID and example error parameters for the
        operation.

The values provided for the ``input`` and ``output`` members MUST be
compatible with the shapes and constraints of the corresponding structure.
These values use the same semantics and format as
:ref:`custom trait values <trait-node-values>`.

A value for ``output`` or ``error`` SHOULD be provided. However, both
MUST NOT be defined for the same example.

.. tabs::

    .. code-tab:: smithy

        @readonly
        operation MyOperation {
            input: MyOperationInput,
            output: MyOperationOutput,
            errors: [MyOperationError]
        }

        apply MyOperation @examples([
            {
                title: "Invoke MyOperation",
                input: {
                    tags: ["foo", "baz", "bar"],
                },
                output: {
                    status: "PENDING",
                }
            },
            {
                title: "Another example for MyOperation",
                input: {
                    foo: "baz",
                },
                output: {
                    status: "PENDING",
                }
            },
            {
                title: "Error example for MyOperation",
                input: {
                    foo: "!",
                },
                error: {
                    shapeId: MyOperationError,
                    content: {
                        message: "Invalid 'foo'. Special character not allowed.",
                    }
                }
            },
        ])


.. _examples-ErrorExample-structure:

``ErrorExample`` structure
==========================

The ``ErrorExample`` structure defines an error example using the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - shapeId
      - :ref:`shape-id`
      - The shape ID of the error in this example. This shape ID MUST be of
        a structure shape with the error trait. The structure shape MUST be
        bound as an error to the operation this example trait is applied to.
    * - content
      - ``document``
      - Provides example error parameters for the operation. Each key is
        the name of a top-level error structure member, and each value is the
        value of the member.


.. smithy-trait:: smithy.api#externalDocumentation
.. _externalDocumentation-trait:

-------------------------------
``externalDocumentation`` trait
-------------------------------

Summary
    Provides named links to external documentation for a shape.
Trait selector
    ``*``
Value type
    ``map`` of ``string`` containing a name to ``string`` containing a valid
    URL.

.. tabs::

    .. code-tab:: smithy

        @externalDocumentation(
            "Homepage": "https://www.example.com/",
            "API Reference": "https://www.example.com/api-ref",
        )
        service MyService {
            version: "2006-03-01",
        }


.. smithy-trait:: smithy.api#internal
.. _internal-trait:

------------------
``internal`` trait
------------------

Summary
    Shapes marked with the internal trait are meant only for internal use.
    Tooling can use the ``internal`` trait to filter out shapes from models
    that are not intended for external customers.
Trait selector
    ``*``
Value type
    Annotation trait

As an example, a service team may wish to use a version of a model that
includes features that are only available to internal customers within the
same company, whereas clients for external customers could be built from a
filtered version of the model.

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            foo: String,

            @internal
            bar: String,
        }


.. smithy-trait:: smithy.api#recommended
.. _recommended-trait:

---------------------
``recommended`` trait
---------------------

Summary
    Indicates that a structure member SHOULD be set. This trait is useful when
    the majority of use cases for a structure benefit from providing a value
    for a member, but the member is not actually :ref:`required <required-trait>`
    or cannot be made required backward compatibly.
Trait selector
    ``structure > member``
Value type
    Structure with the following members:

    .. list-table::
        :header-rows: 1
        :widths: 10 10 80

        * - Property
          - Type
          - Description
        * - reason
          - ``string``
          - Provides a reason why the member is recommended.
Conflicts with
   :ref:`required-trait`

.. code-block:: smithy

    @input
    structure PutContentsInput {
        @required
        contents: String,

        @recommended(reason: "Validation will reject contents if they are invalid.")
        validateContents: Boolean,
    }


.. smithy-trait:: smithy.api#sensitive
.. _sensitive-trait:

-------------------
``sensitive`` trait
-------------------

Summary
    Indicates that the data stored in the shape is sensitive and MUST be
    handled with care.
Trait selector
    ``:not(:is(service, operation, resource, member))``

    *Any shape that is not a service, operation, resource, or member.*
Value type
    Annotation trait

Sensitive data MUST NOT be exposed in things like exception messages or log
output. Application of this trait SHOULD NOT affect wire logging
(i.e., logging of all data transmitted to and from servers or clients).

.. tabs::

    .. code-tab:: smithy

        @sensitive
        string MyString


.. smithy-trait:: smithy.api#since
.. _since-trait:

---------------
``since`` trait
---------------

Summary
    Defines the version or date in which a shape or member was added to
    the model.
Trait selector
    ``*``
Value type
    ``string`` representing the date it was added.


.. smithy-trait:: smithy.api#tags
.. _tags-trait:

--------------
``tags`` trait
--------------

Summary
    Tags a shape with arbitrary tag names that can be used to filter and group
    shapes in the model.
Trait selector
    ``*``
Value type
    ``[string]``

Tools can use these tags to filter shapes that should not be visible for a
particular consumer of a model. The string values that can be provided to the
tags trait are arbitrary and up to the model author.

.. tabs::

    .. code-tab:: smithy

        @tags(["experimental", "public"])
        string SomeStructure {}


.. smithy-trait:: smithy.api#title
.. _title-trait:

---------------
``title`` trait
---------------

Summary
    Defines a proper name for a service or resource shape. This title can be
    used in automatically generated documentation and other contexts to
    provide a user friendly name for services and resources.
Trait selector
    ``:is(service, resource)``

    *Any service or resource*
Value type
    ``string``

.. tabs::

    .. code-tab:: smithy

        namespace acme.example

        @title("ACME Simple Image Service")
        service MySimpleImageService {
            version: "2006-03-01",
        }


.. smithy-trait:: smithy.api#unstable
.. _unstable-trait:

------------------
``unstable`` trait
------------------

Summary
    Indicates a shape is unstable and MAY change in the future. This trait can
    be applied to trait shapes to indicate that a trait is unstable or
    experimental. If possible, code generators SHOULD use this trait to warn
    when code generated from unstable features are used.
Trait selector
    ``*``

Value type
    Annotation trait

.. tabs::

    .. code-tab:: smithy

        @unstable
        string MyString


.. _CommonMark: https://spec.commonmark.org/
