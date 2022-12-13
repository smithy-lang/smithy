.. _mixins:

------
Mixins
------

A mixin is a shape that has the :ref:`mixin-trait`. Adding a mixin to a shape
causes the members and traits of the mixin shape to be copied into the local
shape.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @mixin
    structure UserIdentifiersMixin {
        id: String
    }

    structure UserDetails with [UserIdentifiersMixin] {
        alias: String
    }

Multiple mixins can be applied:

.. code-block:: smithy

    @mixin
    structure UserIdentifiersMixin {
        id: String
    }

    @mixin
    structure AccessDetailsMixin {
        firstAccess: Timestamp
        lastAccess: Timestamp
    }

    structure UserDetails with [
        UserIdentifiersMixin
        AccessDetailsMixin
    ] {
        alias: String
    }

Mixins can be composed of other mixins:

.. code-block:: smithy

    @mixin
    structure MixinA {
        a: String
    }

    @mixin
    structure MixinB with [MixinA] {
        b: String
    }

    structure C with [MixinB] {
        c: String
    }

When a member is copied from a mixin into a target shape, the shape ID of the
copied member takes on the containing shape ID of the target shape. This
ensures that members defined via mixins are treated the same way as members
defined directly in a shape, and it allows members of a shape to be backward
compatibly refactored and moved into a mixin or for a shape to remove a mixin
and replace it with members defined directly in the shape.

The above `C` structure is equivalent to the following flattened structure
without mixins:

.. code-block:: smithy

    structure C {
        a: String
        b: String
        c: String
    }

Mixins be any shape type, but they MUST NOT be applied to a shape of a
different type. For example, a string mixin can be applied to a string
shape, but not to a blob shape.

.. code-block:: smithy

    @mixin
    @pattern("[a-zA-Z0-1]*")
    string AlphaNumericMixin

    @length(min: 8, max: 32)
    string Username with [AlphaNumericMixin]


Traits and mixins
=================

Shapes that use mixins inherit the traits applied to their mixins, except for
the :ref:`mixin-trait` and *mixin local traits*. Traits applied directly to a
shape take precedence over traits applied to its mixins.

For example, the definition of ``UserSummary`` in the following model:

.. code-block:: smithy

    /// Generic mixin documentation.
    @tags(["a"])
    @mixin
    structure UserInfoMixin {
        userId: String
    }

    structure UserSummary with [UserInfoMixin] {}

Is equivalent to the following flattened structure because it inherits the
traits of ``UserInfo``:

.. code-block:: smithy

    /// Generic mixin documentation.
    @tags(["a"])
    structure UserSummary {
        userId: String
    }

The definition of ``UserSummary`` in the following model:

.. code-block:: smithy

    /// Generic mixin documentation.
    @tags(["a"])
    @mixin
    structure UserInfoMixin {
        userId: String
    }

    /// Specific documentation
    @tags(["replaced-tags"])
    structure UserSummary with [UserInfoMixin] {}

Is equivalent to the following flattened structure because it inherits the
traits of ``UserInfo`` and traits applied to ``UserSummary`` take precedence
over traits it inherits:

.. code-block:: smithy

    /// Specific documentation
    @tags(["replaced-tags"])
    structure UserSummary {
        userId: String
    }

The order in which mixins are applied to a shape controls the inheritance
precedence of traits. For each mixin applied to a shape, traits applied
directly to the mixin override traits applied to any of its mixins. Traits
applied to mixins that come later in the list of mixins applied to a shape take
precedence over traits applied to mixins that come earlier in the list of
mixins. For example, the definition of `StructD` in the following model:

.. code-block:: smithy

    /// A
    @foo(1)
    @oneTrait
    @mixin
    structure StructA {}

    /// B
    @foo(2)
    @twoTrait
    @mixin
    structure StructB {}

    /// C
    @threeTrait
    @mixin
    structure StructC with [StructA, StructB] {}

    /// D
    @fourTrait
    structure StructD with [StructC] {}

Is equivalent to the following flattened structure:

.. code-block:: smithy

    // (1)
    /// D
    @fourTrait    // (2)
    @threeTrait   // (3)
    @foo(2)       // (4)
    @twoTrait     // (5)
    @oneTrait     // (6)
    structure StructD {}

1. The :ref:`documentation-trait` applied to ``StructD`` takes precedence over
   any inherited traits.
2. ``fourTrait`` is applied directly to ``StructD``.
3. ``threeTrait`` is applied to ``StructC``, ``StructC`` is a mixin of
   ``StructD``, and `StructD` inherits the resolved traits of each applied
   mixin.
4. Because the `StructB` mixin applied to ``StructC`` comes after the
   ``StructA`` mixin in the list of mixins applied to ``StructC``, ``foo(2)``
   takes precedence over ``foo(1)``.
5. ``StructC`` inherits the resolved traits of ``StructB``.
6. ``StructC`` inherits the resolved traits of ``StructA``.


Mixin local traits
------------------

Sometimes it's necessary to apply traits to a mixin that are not copied onto
shapes that use the mixin. For example, if a mixin is an implementation detail
of a model, then it is recommended to apply the :ref:`private-trait` to the
mixin so that shapes outside of the namespace the mixin is defined within
cannot refer to the mixin. However, every shape that uses the mixin doesn't
necessarily need to be marked as private. The ``localTraits`` property of
the :ref:`mixin-trait` can be used to ensure that a list of traits applied to
the mixin are not copied onto shapes that use the mixin (note that this has
no effect on the traits applied to members contained within a mixin).

Consider the following model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @private
    @mixin(localTraits: [private])
    structure PrivateMixin {
        foo: String
    }

    structure PublicShape with [PrivateMixin] {}

``PublicShape`` is equivalent to the following flattened structure:

.. code-block:: smithy

    structure PublicShape {
        foo: String
    }

The ``PrivateMixin`` shape can only be referenced from the ``smithy.example``
namespace. Because the :ref:`private-trait` is present in the ``localTraits``
property of the :ref:`mixin-trait`, ``PublicShape`` is not marked with the
:ref:`private-trait` and can be referred to outside of ``smithy.example``.


Adding and replacing traits on copied members
---------------------------------------------

The members and traits applied to members of a mixin are copied onto the target
shape. It is sometimes necessary to provide a more specific trait value for a
copied member or to add traits only to a specific copy of a member. Traits can
be added on to these members like any other member. Additionally, traits can be
applied to these members in the JSON AST using the :ref:`apply type <ast-apply>`
and in the Smithy IDL using :ref:`apply statements <apply-statement>`.

.. note::

    Traits applied to shapes supersede any traits inherited from mixins.

For example:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @mixin
    structure MyMixin {
        /// Generic docs
        mixinMember: String
    }

    structure MyStruct with [MyMixin] {}
    apply MyStruct$mixinMember @documentation("Specific docs")

Alternatively, the member can be redefined if it targets the same shape:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @mixin
    structure MyMixin {
        /// Generic docs
        mixinMember: String
    }

    structure MyStruct with [MyMixin] {
        /// Specific docs
        mixinMember: String
    }


Mixins are an implementation detail of the model
================================================

Mixins are an implementation detail of models and are only intended to reduce
duplication in Smithy shape definitions. Mixins do not provide any kind of
runtime polymorphism for types generated from Smithy models. Smithy model
transformations like code generation or converting to other model formats
like OpenAPI SHOULD completely elide mixins by flattening the model.

It is a backward compatible change to remove a mixin from a shape as long as
equivalent traits and members of the mixin are applied directly to the shape.
Such a change should have no impact on generated artifacts like code or
OpenAPI models.


Mixins cannot be referenced other than as mixins to other shapes
================================================================

To ensure that mixins are not code generated, mixins MUST NOT be referenced
from any other shapes except to mix them into other shapes. Mixins MUST NOT be
used as operation input, output, or errors, and they MUST NOT be targeted by
members.

The following model is invalid because a structure member targets a mixin:

.. code-block:: smithy

    @mixin
    structure GreetingMixin {
        greeting: String
    }

    structure InvalidStructure {
        notValid: GreetingMixin // <- this is invalid
    }

The following model is invalid because an operation attempts to use a mixin
as input:

.. code-block:: smithy

    @mixin
    structure InputMixin {}

    operation InvalidOperation {
        input: InputMixin // <- this is invalid
    }


Mixins MUST NOT introduce cycles
================================

Mixins MUST NOT introduce circular references. The following model is invalid:

.. code-block:: smithy

    @mixin
    structure CycleA with [CycleB] {}

    @mixin
    structure CycleB with [CycleA] {}


Mixin members MUST NOT conflict
===============================

The list of mixins applied to a shape MUST NOT attempt to define members that
use the same member name with different targets. The following model is
invalid:

.. code-block:: smithy

    @mixin
    structure A1 {
        a: String
    }

    @mixin
    structure A2 {
        a: Integer
    }

    structure Invalid with [A1, A2] {}

The following model is also invalid, but not specifically because of mixins.
This model is invalid because the member name ``a`` and ``A`` case
insensitively conflict.

.. code-block:: smithy

    @mixin
    structure A1 {
        a: String
    }

    @mixin
    structure A2 {
        A: Integer
    }

    structure Invalid with [A1, A2] {}

Members that are mixed into shapes MAY be redefined if and only if each
redefined member targets the same shape. Traits applied to redefined members
supersede any traits inherited from mixins.

.. code-block:: smithy

    @mixin
    structure A1 {
        @private
        a: String
    }

    @mixin
    structure A2 {
        @required
        a: String
    }

    structure Valid with [A1, A2] {}


Member ordering
===============

The order of structure and union members is important for languages like C
that require a stable ABI. Mixins provide a deterministic member ordering.
Members inherited from mixins come before members defined directly in the
shape.

Members are ordered in a kind of depth-first, preorder traversal of mixins
that are applied to a structure or union. To resolve the member order of a
shape, iterate over each mixin applied to the shape in the order in which they
are applied, from left to right. For each mixin, iterate over the mixins
applied to the mixin in the order in which mixins are applied. When the
evaluated shape has no mixins, the members of that shape are added to the
resolved list of ordered members. After evaluating all the mixins of a shape,
the members of the shape are added onto the resolved list of ordered members.
This process continues until all mixins and the members of the starting shape
are added to the ordered list.

Given the following model:

.. code-block:: smithy

    @mixin
    structure FilteredByNameMixin {
        nameFilter: String
    }

    @mixin
    structure PaginatedInputMixin {
        nextToken: String
        pageSize: Integer
    }

    structure ListSomethingInput with [
        PaginatedInputMixin
        FilteredByNameMixin
    ] {
        sizeFilter: Integer
    }

The members are ordered as follows:

1. ``nextToken``
2. ``pageSize``
3. ``nameFilter``
4. ``sizeFilter``


Mixins on shapes with non-member properties
===========================================

Some shapes don't have members, but do have other properties. Adding a mixin
to such a shape merges the properties of each mixin into the local shape. Only
certain properties may be defined in the mixin shapes. See the sections below
for which properties are permitted for each shape type.

Scalar properties defined in the local shape are kept, and non-scalar
properties are merged. When merging map properties, the values for local keys
are kept. The ordering of merged lists / sets follows the same ordering as
members.

Service mixins
--------------

Service shapes with the :ref:`mixin-trait` may define any property. For
example, in the following model:

.. code-block:: smithy

    operation OperationA {}

    @mixin
    service A {
        version: "A"
        operations: [OperationA]
    }

    operation OperationB {}

    @mixin
    service B with [A] {
        version: "B"
        rename: {
            "smithy.example#OperationA": "OperA"
            "smithy.example#OperationB": "OperB"
        }
        operations: [OperationB]
    }

    operation OperationC {}

    service C with [B] {
        version: "C"
        rename: {
            "smithy.example#OperationA": "OpA"
            "smithy.example#OperationC": "OpC"
        }
        operations: [OperationC]
    }

The flattened equivalent of ``C`` with no mixins is:

.. code-block:: smithy

    operation OperationA {}

    operation OperationB {}

    operation OperationC {}

    service C {
        version: "C"
        rename: {
            "smithy.example#OperationA": "OpA"
            "smithy.example#OperationB": "OperB"
            "smithy.example#OperationC": "OpC"
        }
        operations: [OperationA, OperationB, OperationC]
    }


Resource mixins
---------------

Resource shapes with the :ref:`mixin-trait` MAY NOT define any properties. This
is because every property of a resource shape is intrinsically tied to its set
of identifiers. Changing these identifiers would invalidate every other
property of a given resource. For example:

.. code-block:: smithy

    @mixin
    @internal
    resource MixinResource {}

    resource MixedResource with [MixinResource] {}


Operation mixins
----------------

Operation shapes with the :ref:`mixin-trait` MAY NOT define an ``input`` or
``output`` shape other than the :ref:`unit-type`. This is because allowing
input and output shapes to be shared goes against the goal of the
:ref:`input-trait` and :ref:`output-trait`.

Operation shapes with the :ref:`mixin-trait` MAY define errors.

.. code-block:: smithy

    @mixin
    operation ValidatedOperation {
        errors: [ValidationError]
    }

    @error("client")
    structure ValidationError {}

    operation GetUsername with [ValidatedOperation] {
        input := {
            id: String
        }
        output := {
            name: String
        }
        error: [NotFoundError]
    }

    @error("client")
    structure NotFoundError {}
