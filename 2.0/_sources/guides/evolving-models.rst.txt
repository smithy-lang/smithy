===============
Evolving Models
===============

This guide describes how to evolve Smithy models without breaking your
customers.

   .. note::

       This page does not include every possible backwards or forwards
       compatible change. For traits, the best place to look for compatibility
       is the specification for the trait or its model definition as defined by
       the :ref:`breaking changes property <trait-breaking-change-rules>`.

Updating services
=================

The following changes to operation shapes are backward-compatible:

#. Adding operations.
#. Adding resources.

The following changes are not backward-compatible:

#. Removing a resource or operation.
#. Renaming a shape that was already part of the service.


Updating operations
===================

The following changes to operation shapes are backward-compatible:

#. Adding a new error shape if the error shape is only encountered under new
   conditions that previously released clients/tools will not encounter.

The following changes are not backward-compatible:

#. Changing the shape targeted by the input or output of an operation. For
   example, it is not backward compatible to change an operation's input or
   output from targeting `smithy.api#Unit` to then later target a different
   shape.
#. Removing or renaming a resource or operation.
#. Removing an operation from a service or resource.
#. Removing a resource from a service.
#. Changing an operation from referencing an input/output structure to no
   longer referencing an input/output structure.
#. Renaming an error that is referenced by an operation.
#. The addition, removal, or modification of traits applied to an operation or
   members of the input/output of an operation MAY result in a breaking change
   if it changes the wire representation of an operation or breaks behavior
   that was previously relied upon by tooling.


Updating structures
===================

The following changes to structure shapes are backward-compatible:

#. Adding new optional members to a structure.
#. Removing the :ref:`required-trait` from a structure member and replacing
   it with the :ref:`default-trait` (assuming the member was not marked with
   the :ref:`clientOptional-trait`).
#. Removing the :ref:`required-trait` from a structure member when the
   structure is marked with the :ref:`input-trait`.
#. Removing the :ref:`required-trait` from a structure member that is
   marked with the :ref:`clientOptional-trait`.
#. Adding the :ref:`required-trait` to a member of a structure if the member
   is marked as ``clientOptional`` or the structure is marked with the ``input``
   trait.
#. Adding or removing the :ref:`input-trait` from a structure is generally
   backward incompatible.

   .. note::

       Many code generators automatically create dedicated synthetic input
       structures for each operation and treat the synthetic structure
       as if it is marked with the ``@input`` trait. Code generators that do
       this MAY ignore backward incompatible changes around adding or removing
       the ``@input`` trait.

The following changes to a structure are not backward-compatible:

#. Renaming a member.
#. Removing a member.
#. Changing the shape targeted by a member.
#. Adding the :ref:`required-trait` to a member that was not previously
   marked with the :ref:`clientOptional-trait`.
#. Removing the :ref:`default-trait` from a member.
#. Adding the :ref:`default-trait` to a member that was not previously marked
   with the :ref:`required-trait`.
#. Adding the :ref:`default-trait` to a member that was previously marked
   with the :ref:`clientOptional-trait`.
#. Removing the :ref:`clientOptional-trait` from a member that is marked as
   ``required``.
#. Adding or updating :ref:`constraint traits <constraint-traits>`
   that further restricts the allowed values of a member.


Booleans and API evolution
==========================

A boolean shape is often used to model state flags; however, consider whether
or not the state of a resource is actually binary. If other states can be
added in the future, it is often better to use a :ref:`enum shape <enum>`
or a :ref:`union shape <union>`.


Updating unions
===============

The following changes to union shapes are backward-compatible:

#. Adding a new member to a union. Unions in Smithy are considered "open";
   it is a backward-compatible change to add new members to a union. Smithy
   clients SHOULD anticipate and account for receiving unknown members for
   a union in a response from a service at runtime. Clients SHOULD NOT fail
   when receiving unknown members from a service.

The following changes are backward-incompatible:

#. Renaming a union member.
#. Removing a union member.
#. Changing the shape targeted by a union member.


Sparse lists and maps
=====================

The :ref:`sparse-trait` is used to influence code generation in various
programming languages. It is a backward-incompatible change for the ``sparse``
trait to be added or removed from a shape because it will affect types
generated by tooling that uses Smithy models.


Updating traits
===============

The following changes to trait definitions are backward compatible:

#. Relaxing the selector of a trait.
#. Removing a trait from the ``conflicts`` list.
#. Removing the ``structurallyExclusive`` property.
#. Marking a trait as deprecated.


Using Smithy Diff
=================

`Smithy Diff <https://github.com/smithy-lang/smithy/tree/main/smithy-diff>`_ is a
tool used to compare two Smithy models to check for backward compatibility
issues. Smithy Diff can be run via a Java library or via the Smithy CLI.
