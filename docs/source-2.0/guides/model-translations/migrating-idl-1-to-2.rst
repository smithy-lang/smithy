=====================================
Smithy IDL 1.0 to 2.0 Migration Guide
=====================================

This guide describes how to migrate your models from Smithy IDL version 1.0
to version 2.0 without breaking your models or customers.

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

    $version: "2"
    namespace smithy.example

    string Foo

An IDL model file may not have a version control statement at the top. In this
case, it uses version 1.0 by default. For example:

.. code-block:: smithy

   namespace smithy.example

   string Foo

Also needs to be updated to:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    string Foo

Smithy's tooling is able to load both 1.0 model files and 2.0 model files into
a single combined model. Therefore when migrating, it may be wise to migrate
one file at a time.


Replace the box trait
=====================

The ``box`` trait is removed in 2.0. Any shape marked with the ``box`` trait
needs to be updated.


Boxed root-level shapes
-----------------------

For non-member, root level shapes, simply remove the ``box`` trait. Root level
shapes in Smithy 2.0 have no default values unless you explicitly assign them
one.

This 1.0 model:

.. code-block:: smithy

    $version: "1.0"
    namespace smithy.example

    @box // < remove this
    boolean MyBoolean

Becomes this 2.0 model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    boolean MyBoolean


Converting primitive root-level shapes from 1.0
-----------------------------------------------

Some shapes in Smithy 1.0 had default zero values: boolean, byte, short,
integer, long, float, and double. If you defined any of these shapes and did
not mark them with the ``@box`` trait, add the ``@default`` trait to them
set to ``false`` for booleans and ``0`` for numbers. Any member that targets
them also need to repeat this default value on the member.

This 1.0 model:

.. code-block:: smithy

    $version: "1.0"
    namespace smithy.example

    boolean MyPrimitiveBoolean

    integer MyPrimitiveInteger

    structure Foo {
        myBoolean: MyPrimitiveBoolean,
        myInteger: MyPrimitiveInteger
    }

Becomes this 2.0 model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @default(false)
    boolean MyPrimitiveBoolean

    @default(0)
    integer MyPrimitiveInteger

    structure Foo {
        myBoolean: MyPrimitiveBoolean = false
        myInteger: MyPrimitiveInteger = 0
    }


Boxed members
-------------

If a member is marked with the box trait, replace the trait with a
``@default(null)`` trait to have the same effect of overriding the
default value of the target shape.

This 1.0 model:

.. code-block:: smithy

    $version: "1.0"
    namespace smithy.example

    structure MyStructure {
        @box // change this to = null below
        foo: PrimitiveBoolean
    }

Becomes this 2.0 model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    structure MyStructure {
        foo: PrimitiveBoolean = null
    }

.. seealso::

    :ref:`structure-optionality`


Convert set shapes to list shapes
=================================

The set shape was deprecated for IDL 2.0. Each set shape must be replaced by a
list shape with the :ref:`uniqueItems-trait`.

For example, the following set:

.. code-block:: smithy

    $version: "1.0"
    namespace smithy.example

    set StringSet {
        member: String
    }

Needs to be updated to:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @uniqueItems
    list StringSet {
        member: String
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

    $version: "2"
    namespace smithy.example

    structure OptionalStream {
        payload: StreamingBlob = ""
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

.. code-block:: smithy

    $version: "2"
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

    $version: "2"
    namespace smithy.example

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


Use the target elision syntax sugar to reduce boilerplate
---------------------------------------------------------

Resource shapes contain a set of identifiers, but when writing structures that
contain those identifiers you have to duplicate those definitions entirely. In
IDL 2.0, you can use the target elision syntax with a structure bound to a
resource. For example:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    resource User {
        identifiers: {
            id: String
            email: String
        }
    }

    // The `for` syntax here determines which resource should be checked.
    structure UserDetails for User {
        // With this syntax, the target is automatically inferred from the
        // resource.
        $id

        // Uncomment this to include an email member. Unlike with mixins, you
        // must opt in to the members that you want to include. This allows you
        // to have partial views of a resource, such as in a create operation
        // that does not bind all of the identifiers.
        // $email

        address: String
    }

This syntax can also be used with mixins to more succinctly add additional
traits to included members.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @mixin
    structure UserIdentifiers {
        id: String
        email: String
    }

    structure UserDetails with [UserIdentifiers] {
        @required
        $id

        @required
        $email
    }


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

    $version: "2"
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

.. code-block:: smithy

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

    $version: "2"
    namespace smithy.example

    enum Suit {
        DIAMOND = "diamond"
        CLUB = "club"
        HEART = "heart"
        SPADE = "spade"
    }
