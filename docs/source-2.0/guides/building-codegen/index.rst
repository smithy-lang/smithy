--------------------------------
Creating a Smithy Code Generator
--------------------------------

This guide describes how to structure a new Smithy code generator so that
the generator can be used to build generic clients for any web service
modeled with Smithy, how to build shape code generators that are reusable
in a client and server context, and how to ensure a proper separation
between generic code generation and AWS-specific code generation.

.. note::

   This is a living document. Important content might be missing, and
   the guidance provided here may change over time.


Smithy's Java reference implementation
======================================

This guide is tailored to `Smithy's Java reference implementation`_ and
Gradle_ as a build tool, but much of the guidance is applicable to
implementations in other languages as well. The reference implementation
includes various abstractions that code generators can use to reduce the
development effort needed to build a new code generator.


Pluggable codegen
=================

Just like Smithy models, Smithy code generators need to be pluggable and
extensible. The code generator needs to be able to react to traits found
in the model and influence the generated code and related artifacts like
dependency graphs. For example, if the :ref:`aws.auth#sigv4-trait` is found
on a service, a code generator should look for a codegen plugin that adds
support for signing requests using AWS SigV4. Codegen plugins need to be
able to influence the dependencies of a client, the client configuration
options exposed by the client, the interceptors used by a client, and how
the client serializes and deserializes shapes.


Goals of this guide
===================

1. Define requirements of a Smithy code generator and recommendations
   on how to meet those requirements.
2. Define a recommended project layout and deliverables.
3. Define clear boundaries between generic code generation and AWS-specific
   code generation to avoid coupling.
4. Increase consistency across implementations, making it easier to
   contribute changes to multiple generators.


Non-Goals of this guide
=======================

1. Remove all ambiguity on how to build a Smithy code generator. Each
   codegen project is unique because each target language is unique.
2. Force specific implementation details. This guide is non-normative.
   You're free to implement a code generator in any language using any
   tooling you want.
3. Document the Smithy specification. This is supplementary content to
   help guide code generators and is not intended to restate what is
   already defined by the specification.


Tenets for Smithy code generators
=================================

These are the tenets of Smithy code generators
(unless you know better ones):

1. **Smithy implementations adhere to the spec**. The Smithy spec and model
   are the contract between clients, servers, and other implementations.
   A Smithy client written in any programming language should be able to
   connect to a Smithy server written in any programming language without
   either having to care about the programming language used to implement
   the other.
2. **The code Smithy generates is familiar to developers**. Language idioms
   and developer experience factor in to how developers and companies
   choose between Smithy and alternatives.
3. **Components not monoliths**. We write modular components that
   developers can compose together to meet their requirements. Our
   components have clear boundaries: adding a dependency on an AWS protocol
   does not require a client to use AWS credentials; Smithy code generators
   do not depend on AWS SDK libraries.
4. **Developers trust the code Smithy generates**. Generated code is valid
   without the need to manually edit or further transform it, it is
   readable and easy to understand, and it does the right thing by
   default. We avoid breaking changes to generated code outside of major
   version bumps.
5. **Our code is maintainable because we limit public interfaces**. We
   limit the dependencies we take on. We don't expose overly open
   interfaces that hinder our ability to evolve the code base.
6. **No implementation stands alone**. Test cases, protocol tests, code
   fixes, and missing abstractions have a greater impact if every Smithy
   implementation can use them rather than just a single implementation.
7. **Service teams don't need to know the details of every code
   generator that exists or will ever exist**. When modeling a service,
   service teams only need to consider if the model is a valid Smithy
   model; the constraints of any particular programming language should
   not be a concern when modeling a service. Smithy is meant to work
   with any number of languages, and it is an untenable task to attempt
   to bubble up every constraint, reserved word, or other limitations to
   modelers.


Navigation
==========

.. toctree::
    :maxdepth: 1

    overview-and-concepts
    mapping-shapes-to-languages
    creating-codegen-repo
    configuring-the-generator
    implementing-the-generator
    making-codegen-pluggable
    generating-code
    decoupling-codegen-with-symbols
    using-the-semantic-model

.. TODO: Testing doc topics:
.. integration testing, protocol tests, using examples as tests.

.. TODO: client topics:
.. Generating a client interface, configuration, interceptors,
.. observability, Smithy reference architecture, paginators,
.. waiters, endpoint resolution

.. _Smithy's Java reference implementation: https://github.com/smithy-lang/smithy
.. _Gradle: https://gradle.org
