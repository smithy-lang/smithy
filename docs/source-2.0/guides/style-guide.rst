===========
Style Guide
===========

This document defines a style guide for Smithy models. Adhering to common
style guide makes models easier to read.


Model files
===========

Smithy models SHOULD be authored using the :ref:`Smithy IDL <idl>`.
Smithy models SHOULD resemble the following example:

.. code-block:: smithy

    $version: "2"
    metadata validators = []
    metadata suppressions = []

    namespace smithy.example.namespace

    /// This is the documentation
    @length(min: 1, max: 1000)
    string MyShape

    /// This is the documentation.
    integer AnotherShape

    /// Documentation about the structure.
    ///
    /// More descriptive documentation if needed...
    structure MyStructure {
        /// Documentation about the member.
        @required
        foo: String
    }

    // Example of creating custom traits.
    @trait(selector: "string")
    structure myTrait {}

    // Structures with no members place the braces on the same line.
    @mixin
    structure MyMixin {}

    // When using a single mixin, place "with" and the shape on the same line
    structure UsesMixin with [MyMixin] {
        foo: String
    }

    // When using multiple mixins, place each shape ID on its own line,
    // followed by a line that contains the opening brace.
    structure UsesMixin with [
        MyMixin
        SomeOtherMixin
    ] {
        foo: String
    }

* Each statement should appear on its own line.


File encoding
-------------

Smithy models are encoded in UTF-8.


New lines
---------

New lines are represented using ``Line Feed``, ``U+000A``.

All files SHOULD end with a new line.


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

Models are indented using four spaces.


Whitespace
----------

1. A single space appears after a comma (",") and after a colon (":").
2. Spaces do not occur before a comma (",") or colon (":").
3. Lines do not end with trailing spaces.
4. Members of an object are not horizontally aligned.


Commas
------

Omit commas everywhere except in traits or node values defined on a
single line.

Do:

.. code-block:: smithy

    $version: "2"
    metadata validators = [{
        name: "StandardOperationVerb"
        configuration: {
            verbs: ["Get", "Delete", "Create", "Update"]
            prefixes: ["Batch"]
        }
    }]

    namespace smithy.example.namespace

    /// Gets a resource by ID.
    @http(method: "GET", uri: "/message/{userId}")
    operation GetMessage {
        input: GetMessageInput
        output: GetMessageOutput
        errors: [
            ValidationError
            ResourceNotFoundError
        ]
    }

Do not:

.. code-block:: smithy

    $version: "2"
    metadata validators = [{
        name: "StandardOperationVerb",
        configuration: {
            verbs: ["Get" "Delete" "Create" "Update"],
            prefixes: ["Batch"],
        },
    },]

    namespace smithy.example.namespace

    /// Gets a resource by ID.
    @http(method: "GET" uri: "/message/{userId}")
    operation GetMessage {
        input: GetMessageInput,
        output: GetMessageOutput,
        errors: [
            ValidationError,
            ResourceNotFoundError,
        ],
    }


Naming
======


Shape names
-----------

* Shape names use a strict form of UpperCamelCase (e.g., "XmlRequest", "FooId").
* Numeric shapes should use descriptive names, including units of measurement
  (e.g., prefer "SizeInMb" over "Size").
* Enums should use a singular noun (e.g., prefer "Suit" over "Suits").
* Lists should use plural names (e.g., prefer "Users" over "UserList").
* Operations should follow the format of "VerbNoun" (e.g., "UpdateUser").
* Resources should use a singular noun (e.g., use "User" over "Users").
* Services should be named after the name of a service, omitting the word
  "Service" and branding when possible (e.g., prefer "S3" over
  "AmazonS3Service").


Member names
------------

Member names use a strict form of lowerCamelCase (e.g., "xmlRequest", "fooId").


Trait names
-----------

By convention, traits use lowerCamelCase (e.g., "xmlRequest", "fooId").

.. code-block:: smithy

    namespace smithy.example.namespace

    /// This is the documentation about the trait.
    ///
    /// This is more documentation.
    @trait(selector: "string")
    structure myTrait {}


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
