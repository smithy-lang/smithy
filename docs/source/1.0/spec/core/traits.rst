.. _non-aws-traits:

======
Traits
======

*Traits* are model components that can be attached to :ref:`shapes <shapes>`
to describe additional information about the shape; shapes provide the
structure and layout of an API, while traits provide refinement and style.

.. seealso::

    The :ref:`traits` specification.

.. contents:: Table of contents
    :depth: 3
    :local:
    :backlinks: none


.. smithy-trait:: smithy.api#trait

.. _trait-trait:

---------------
``trait`` trait
---------------

Summary
    Marks a shape as a :ref:`trait <traits>`.
Trait selector
    ``:is(simpleType, list, map, set, structure, union)``

    This trait can only be applied to simple types, ``list``, ``map``, ``set``,
    ``structure``, and ``union`` shapes.
Value type
    ``structure``

The ``trait`` trait is a structure that supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - selector
      - ``string``
      - A valid :ref:`selector <selectors>` that defines where the trait
        can be applied. For example, a ``selector`` set to ``:test(list, map)``
        means that the trait can be applied to a :ref:`list <list>` or
        :ref:`map <map>` shape. This value defaults to ``*`` if not set,
        meaning the trait can be applied to any shape.
    * - conflicts
      - [``string``]
      - Defines the shape IDs of traits that MUST NOT be applied to the same
        shape as the trait being defined. This allows traits to be defined as
        mutually exclusive. Provided shape IDs MAY target unknown traits
        that are not defined in the model.
    * - structurallyExclusive
      - ``string``
      - One of "member" or "target". When set to "member", only a single
        member of a structure can be marked with the trait. When set to
        "target", only a single member of a structure can target a shape
        marked with this trait.

.. seealso::

    :ref:`Defining traits <defining-traits>`.


.. include:: traits/auth-traits.rst.template

.. include:: traits/behavior-traits.rst.template

.. include:: traits/constraint-traits.rst.template

.. include:: traits/documentation-traits.rst.template

.. include:: traits/endpoint-traits.rst.template

.. include:: traits/http-traits.rst.template

.. include:: traits/protocol-traits.rst.template

.. include:: traits/resource-traits.rst.template

.. include:: traits/stream-traits.rst.template

.. include:: traits/type-refinement-traits.rst.template

.. include:: traits/xml-traits.rst.template
