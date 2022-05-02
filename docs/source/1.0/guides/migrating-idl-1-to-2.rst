=====================================
Smithy IDL 1.0 to 2.0 Migration Guide
=====================================

This guide describes how to migrate your models from Smithy IDL version 1.0
to version 2.0 without breaking your models or customers.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none

Update the model file version
=============================

For each model file you are upgrading, change the version from ``1`` or
``1.0`` to ``2.0``. In the IDL this is controlled with the
:ref:`version statement <smithy-version>`, and in the AST it is controlled
with the ``smithy`` :ref:`top level property <ast-top-level-properties>`. For
example, the following model:

.. code-block:: smithy

    $version: "1.0"

    namespace smithy.example

    string Foo

Should be updated to:

.. code-block:: smithy

    $version: "2.0"

    namespace smithy.example

    string Foo

An IDL model file may not have a version control statement at the top. In this
case, it uses version 1.0 by default. For example:

.. code-block:: smithy

   namespace smithy.example

   string Foo

Also needs to be updated to:

.. code-block:: smithy

    $version: "2.0"

    namespace smithy.example

    string Foo

Smithy's tooling is able to load both 1.0 model files and 2.0 model files into
a single combined model. Therefore when migrating, it may be wise to migrate
one file at a time.


Remove the box trait
====================

The ``box`` trait is removed in 2.0, so it must be removed from any shapes or
members that use it. Smithy structure members are considered boxed by default,
which can be changed using the :ref:`required-trait` or :ref:`default-trait`.
In effect, this means you can indiscriminately remove the ``box`` trait from
your models.

.. seealso::

    :ref:`structure-nullability`


Replace Primitive prelude shape targets
=======================================

The primitive shapes have been removed from the prelude, and so any member
targeting one of them must update to target its equivalent non-primitive
shape as well as add the :ref:`default-trait`.

.. list-table:
    :header-rows: 1
    :widths: 50 50

    * - Old target
      - New target
    * - ``PrimitiveBoolean``
      - ``Boolean``
    * - ``PrimitiveShort``
      - ``Short``
    * - ``PrimitiveInteger``
      - ``Integer``
    * - ``PrimitiveLong``
      - ``Long``
    * - ``PrimitiveFloat``
      - ``Float``
    * - ``PrimitiveDouble``
      - ``Double``

For example, the following model:

.. code-block:: smithy

    structure User {
        name: PrimitiveString
    }

Needs to be updated to:

.. code-block:: smithy

    structure User {
        @default
        name: String
    }


Add the default trait to streaming blobs
========================================

Members that target a blob shape with the :ref:`streaming-trait` have always
had an implicit default empty value. In IDL 2.0, that will become explicit.
Any such members that are not already marked with the :ref:`required-trait`
will now need to be marked with the :ref:`default-trait`.

For example, the following model:

.. code-block:: smithy

    $version: "1.0"

    namespace smithy.example

    structure OptionalStream {
        // This needs to be updated since it doesn't have the required or
        // default trait already.
        payload: StreamingBlob
    }

    structure RequiredStream {
        // This doesn't need to be updated because it already has the required
        // trait.
        @required
        payload: StreamingBlob
    }

    @streaming
    blob StreamingBlob

Needs to be updated to:

.. code-block:: smithy

    $version: "2.0"

    namespace smithy.example

    structure OptionalStream {
        @default
        payload: StreamingBlob
    }

    structure RequiredStream {
        @required
        payload: StreamingBlob
    }

    @streaming
    blob StreamingBlob


Optional migration steps
========================

The following steps are not required to update a model to be fully compatible
with 2.0, but instead are refactoring steps that can be taken to simplify a
your model.


Move operation inputs and outputs inline
----------------------------------------

The structures that define operation inputs and outputs very often use
boilerplate names and for readability are usually placed close to their parent
operation shapes to improve readability of the model. Smithy 2.0 introduced
:ref:`inline input and output <idl-inline-input-output>`, which allows you
to define those shapes as part of the definition of the operation rather than
separately. This improves readability and reduces the amount of boilerplate
needed to model an operation. For example, the following model:

.. code-block:: smithy

    $version: "1.0"

    namespace smithy.example

    operation PutUser {
        input: PutUserInput,
        output: PutUserOutput
    }

    @input
    structure PutUserInput {
        email: String,
        id: String,
        username: String,
        description: String
    }

    @output
    structure PutUserOutput {}

can be updated to:

.. code-block::

    $version: "2.0"

    namespace smithy.example

    operation PutUser {
        input := {
            email: String
            id: String
            username: String
            description: String
        },
        output := {}
    }

.. seealso::

    the :ref:`inline input / output <idl-inline-input-output>` section of the
    spec for more details.


Abstract shared shape configuration with mixins
-----------------------------------------------

Models often have several shapes that refer to the same sets of members, or
which share a set of trait configurations. For example, resource instance
operations all require that the resource's identifiers be present in input.
With :ref:`mixins`, it is easy to simply share these member definitions without
having to copy and paste them. The following model:

.. code-block:: smithy

    $version: "1.0"

    namespace smithy.example

    resource User {
        identifiers: {
            email: String,
            id: String,
        },
        read: GetUser
    }

    operation GetUser {
        input: GetUserInput,
        output: GetUserOutput
    }

    @input
    structure GetUserInput {
        @required
        email: String,

        @required
        id: String,
    }

    @output
    structure GetUserOutput {
        @required
        email: String,

        @required
        id: String,

        description: String
    }

Can be updated to:

.. code-block:: smithy

    $version: "2.0"

    namespace smithy.example

    resource User {
        identifiers: {
            email: String
            id: String
            username: String
        },
        read: GetUser
    }

    @mixin
    structure UserIdentifiers {
        @required
        email: String

        @required
        id: String
    }

    operation GetUser {
        input := with [UserIdentifiers] {}
        output := with [UserIdentifiers] {
            description: String
        }
    }

Similarly, :ref:`mixins` can be useful if you have a shared set of traits
that otherwise have to be copied and pasted.

.. seealso::

    the :ref:`mixins section <mixins>` of the spec for more details on how they
    work.


Remove unsightly commas
-----------------------

Smithy IDL 2.0 removed the need to include commas when defining, lists, maps,
and shape properties. For example, the following model:

.. code-block:: smithy

    $version: "1.0"

    namespace smithy.example

    operation GetUser {
        input: GetUserInput,
        output: GetUserOutput,
        errors: [
            NotFoundError,
            AccessDeniedError,
        ],
    }

can be updated to:

.. code-block:: smithy

    $version: "1.0"

    namespace smithy.example

    operation GetUser {
        input: GetUserInput
        output: GetUserOutput
        errors: [
            NotFoundError
            AccessDeniedError
        ]
    }

Migrate trait-based string enums to enum shapes
-----------------------------------------------

Smithy IDL 2.0 introduced two new shape types: :ref:`enum` and :ref:`intEnum`.
While the latter is entirely new, the use case for the former was previously
handled by applying the :ref:`enum-trait` to a string shape. A major advantage
of using the enum shapes is that each enum value is now a :ref:`member`. This
means they can be individually targeted by traits, without having to have
special handling inside of Smithy itself. Their definitions in the IDL are now
also much more concise and readable. For example, the following model:

.. code-block::

    $version: "1.0"

    namespace smithy.example

    @enum([
        {
            name: "DIAMOND",
            value: "diamond"
        },
        {
            name: "CLUB",
            value: "club"
        },
        {
            name: "HEART",
            value: "heart"
        },
        {
            name: "SPADE",
            value: "spade"
        }
    ])
    string Suit

can be updated to:

.. code-block:: smithy

    $version: "2.0"

    namespace smithy.example

    enum Suit {
        @enumValue("diamond")
        DIAMOND

        @enumValue("club")
        CLUB

        @enumValue("heart")
        HEART

        @enumValue("spade")
        SPADE
    }
