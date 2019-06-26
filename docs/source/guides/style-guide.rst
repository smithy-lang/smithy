===========
Style Guide
===========

This document defines a style guide for Smithy models. Adhering to common
style guide makes models easier to read.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


Model files
===========

Smithy models SHOULD be authored using the :ref:`Smithy IDL <smithy-language-specification>`.
Smithy models SHOULD resemble the following example:

.. code-block:: smithy

    $version: "0.1.0"

    metadata validators = []
    metadata suppressions = []

    namespace smithy.example.namespace

    @documentation("Documentation")
    string MyShape

    integer AnotherShape
    apply AnotherShape @documentation("Documentation")

    // Example of creating custom traits.
    trait myTrait {
      selector: "string",
      booleanTrait: true
    }


File encoding
-------------

Smithy models are encoded in UTF-8.


New lines
---------

New lines are represented using ``Line Feed``, ``U+000A``.

All files SHOULD end with a new line.


One namespace per file
----------------------

Source model files SHOULD include only a single namespace. Multiple namespaces
MAY appear when representing a model as a single build artifact.


Model file structure
====================

A model file consists of, in order:

1. License or copyright information, if present
2. Smithy version number
3. :ref:`Metadata <metadata>`, if present
4. Namespaces

Exactly one blank line separates each section that is present.


Formatting
==========


Indentation
-----------

Models are indented using two spaces.


Whitespace
----------

1. A single space appears after a comma (",") and after a colon (":").
2. Spaces do not occur before a comma (",") or colon (":").
3. Lines do not end with trailing spaces.
4. Members of an object are not horizontally aligned.


Trailing commas
---------------

Include trailing commas to limit diff noise.


Naming
======


Shape names
-----------

Shape names use a strict form of UpperCamelCase (e.g., "XmlRequest", "FooId").


Member names
------------

Member names use a strict form of lowerCamelCase (e.g., "xmlRequest", "fooId").


Abbreviations
-------------

Abbreviations are represented as normal words. For example, use
"XmlHttpRequest" instead of "XMLHTTPRequest". Even two-letter abbreviations
follow strict camelCasing: "fooId" is used instead of "fooID".


Namespace names
---------------

Namespace names should consist of lowercase letters, numbers, and dots.
Camel case words can be used to better control namespaces. For example,
aws.dynamoDB can be used instead of "aws.dynamodb" in order to better
influence how code is generated in languages that utilize namespaces
with uppercase characters.
