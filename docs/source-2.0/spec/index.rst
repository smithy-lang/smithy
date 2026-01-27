.. _smithy-specification:

====================
Smithy specification
====================

This is the specification of Smithy |release|, an interface definition language
and set of tools used to build clients, servers, and other kinds of artifacts
through model transformations.

Conventions used in this document
=================================

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
document are to be interpreted as described in :rfc:`2119`.
This specification makes use of the Augmented Backus-Naur Form (ABNF)
:rfc:`5234` notation, including the *core rules* defined in Appendix B
of that document.

Please report technical errors and ambiguities in this specification to the
Smithy GitHub repository at https://github.com/smithy-lang/smithy.
This specification is open source; contributions are welcome.

Examples
========

Unless declared otherwise, example Smithy models given in this specification
are written using the :ref:`Smithy interface definition language (IDL) <idl>`
syntax. Complementary :ref:`JSON AST <json-ast>` examples are provided
alongside Smithy IDL examples where appropriate.

Table of contents
=================

.. toctree::
    :numbered:
    :maxdepth: 1

    model
    simple-types
    aggregate-types
    service-types
    mixins
    constraint-traits
    type-refinement-traits
    documentation-traits
    behavior-traits
    resource-traits
    authentication-traits
    protocol-traits
    streaming
    http-bindings
    endpoint-traits
    selectors
    model-validation
    idl
    json-ast
