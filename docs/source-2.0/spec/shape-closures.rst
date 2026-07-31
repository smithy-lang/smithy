.. _shape-closures:

==============
Shape closures
==============

A *shape closure* is a named set of shapes that consumers, such as code
generators, can refer to by id. Closures are declared in model metadata
and consumed by tools that need to operate on a discrete subset of the
model without anchoring that subset to a service.


.. _shape-vs-service-closure:

Shape closures vs. service closures
===================================

A :ref:`service closure <service-closure>` is the set of shapes connected
to a service through resources, operations, and members. The membership of
a service closure is implicit in the model. It is determined by traversing
neighbor relationships from a service shape, and any tool that wants to
operate on it just needs the service id.

A shape closure is intentionally decoupled from services. Its shapes are
specified by namespace or :ref:`selector <selectors>` and the result is
identified by an id that does not correspond to any shape in the model.

This makes shape closures the right tool when a service is not needed. For
example, a code generator may want to generate types without a service
or client wrapper. This is also useful for modeling events that aren't
part of an operation response.


.. _shapeClosures-metadata:

``shapeClosures`` metadata
==========================

Shape closures are declared with the ``shapeClosures`` metadata key. The
value is a list of ``ShapeClosure`` structures with the following members:

.. list-table::
    :header-rows: 1
    :widths: 15 15 70

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The namespaced identifier of the closure (for example,
        ``com.example#EventShapes``). The id is used by tools to refer to the
        closure. The id follows the same format as a shape id, MUST be unique
        across all closures in the model, and MUST NOT refer to a shape that
        exists in the model.

        This identifier is not intended to be semantically significant. The
        requirement for a namespace is intended to reduce the chance for naming
        collisions, not to suggest the namespace that any generated artifacts
        must be generated into.
    * - includeNamespaces
      - ``[string]``
      - Namespaces whose shapes are included in the closure.
    * - includeBySelector
      - ``string``
      - A :ref:`selector <selectors>` whose matched shapes are included
        in the closure.
    * - rename
      - map of :ref:`shape ID <shape-id>` to ``string``
      - Disambiguates shape name conflicts in the closure. Map keys are
        shape IDs contained in the closure, and map values are the
        disambiguated shape names to use in the context of the closure.
        Each given shape ID MUST reference a shape contained in the
        closure. Each given map value MUST match the
        :token:`smithy:Identifier` production used for shape IDs. Renaming
        a shape *does not* give the shape a new shape ID.

        * No renamed shape name can case-insensitively match any other
          renamed shape name or the name of a non-renamed shape contained
          in the closure.
        * Member shapes MAY NOT be renamed.
        * Service, resource, and operation shapes MAY NOT be renamed.
          Renaming shapes is intended for incidental naming conflicts,
          not for renaming the fundamental concepts of an API.
        * Shapes from other namespaces marked as :ref:`private <private-trait>`
          MAY be renamed.
        * A rename MUST use a name that is case-sensitively different
          from the original shape ID name.
    * - documentation
      - ``string``
      - Documentation for the closure in the CommonMark_ format.

A closure MUST define at least one of ``includeNamespaces`` or
``includeBySelector``.

The resolved closure is expanded transitively through directed neighbor
relationships from every shape matched by ``includeNamespaces`` or
``includeBySelector``.


Example
=======

.. code-block:: smithy

    $version: "2"

    metadata shapeClosures = [
        {
            id: "com.example#EventShapes"
            includeNamespaces: ["com.example"]
        }
    ]

    namespace com.example

    structure Event {
        message: String
    }

Tools that consume a closure use its id to look up the set of shapes to
operate on. For example, a code generator might generate types for every
shape in the named closure, or the
:ref:`includeClosures <includeClosures-transform>` build transform might
filter the model down to those shapes.

.. _CommonMark: https://spec.commonmark.org/
