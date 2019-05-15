.. _validation:

================
Model Validation
================

This specification defines a customizable validation system for Smithy
models that can be used by API designers and organizations to ensure that
their APIs adhere to their own standards and best practices.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


Introduction
============

APIs require a great deal of care and discipline to ensure that they provide
a coherent interface to customers, particularly after an API is released and
new features are added. This specification defines metadata that is used to
validate a model against configurable validator definitions, ensuring that
developers adhere to an organization's API standards.

Tools like Checkstyle and Findbugs help to ensure that developers avoid common
bugs and pitfalls when writing code. This is a very powerful concept,
particularly for developers that are new to a programming language. This
concept is even more powerful when teams use the configurability of these
tools to communicate the coding standards of an organization and automate
their enforcement. This validation standard allows the same level of
conformity and rigor to be applied to Smithy models and API definitions.


.. _validator-definition:

Validators
==========

The ``validators`` metadata property contains an array of validator
objects that are used to constrain a model. Each object in the
``validators`` array supports the following properties:

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - **Required**. The name of the validator to apply. This name is used in
        implementations to find and configure the appropriate validator
        implementation. Validators only take effect if a Smithy processor
        implements the validator.
    * - id
      - ``string``
      - Defines a custom identifier for the validator.

        Multiple instances of a single validator can be configured for a model.
        Providing an ``id`` allows suppressions to suppress a specific instance
        of a validator.

        If ``id`` is not specified, it will default to the ``name`` property of
        the validator definition.
    * - message
      - ``string``
      - Provides a custom message to use when emitting validation events. The
        special ``{super}`` string can be added to a custom message to inject
        the original error message of the validation event into the custom
        message.
    * - severity
      - ``string``
      - Provides a custom :ref:`severity <severity-definition>` level to use
        when a validation event occurs. If no severity is provided, then the
        default severity of the validator is used.

        .. note::

              The severity of user-defined validators cannot be set to ERROR.
    * - namespaces
      - [ ``string`` ]
      - Provides a list of the namespaces that are targeted by the validator.
        The validator will ignore any validation events encountered that are
        not specific to the given namespaces.
    * - configuration
      - ``object``
      - Object that provides validator configuraton. The available properties
        are defined by each validator. Validators MAY require that specific
        configuration properties are provided.

The following Smithy document applies a custom validator named "SomeValidator":

.. code-block:: smithy

    metadata validators = [
      {
        // The name of the validator.
        name: "SomeValidator",
        // Uses a custom event ID for each validation event emitted.
        id: "CustomEventId",
        // Uses a custom message that also includes the default message.
        message: "My custom message name. {super}",
        // Applies the rule only to the following namespaces.
        namespaces: ["foo.baz", "bar.qux"],
        // The following properties are specific to the validator.
        configuration: {
          "someProperty": "foo",
        }
      }
    ]


.. _missing-validators:

Missing validators
------------------

If a Smithy implementation does not have an implementation for a specific
validator by name, the Smithy implementation MUST emit a WARNING validation
event with an event ID that is the concatenation of ``UnknownValidator.`` and
the name property of the validator that could not be found. For example, given
a custom validator that could not be found named ``Foo``, the implementation
MUST emit a validation event with an event ID of ``UnknownValidator.Foo`` and
a severity of WARNING.


.. _severity-definition:

Severity
========

When a model is in violation of a validator, a *validation event* is emitted.
This validation event contains metadata about the violation, including the
optional shape that was in violation, the source location of the violation,
the validator ID, and the severity of the violation. *Severity* is used
to define the importance or impact of a violation.

**ERROR**
    Indicates that something is structurally wrong with the model and cannot
    be suppressed.

    Validation events with a severity of ERROR are reserved for enforcing that
    models adhere to the Smithy specification. Validators cannot emit a
    validation event with a severity of ERROR.

**DANGER**
    Indicates that something is very likely wrong with the model. Unsuppressed
    DANGER validation events indicate that a model is invalid.

**WARNING**
    Indicates that something might be wrong with the model.

**NOTE**
    Informational message that does not imply anything is wrong with the model.


.. _suppression-definition:

Suppressions
============

The ``suppressions`` metadata property contains an array of
suppression objects. Suppressions are used to suppress specific validation
events.

.. note::

    Validation events with a severity of ``ERROR`` cannot be suppressed.

Each suppression object in the ``suppressions`` array supports the
following properties:

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Property
      - Type
      - Description
    * - ids
      - [ ``string`` ]
      - **Required**. An array of validator event IDs to suppress. One or more
        event IDs MUST be provided. A value of ``*`` MAY be provided in order
        to suppress all validation event IDs (e.g., ``["*"]``).
    * - shapes
      - [ ``string`` ]
      - A array of absolute :ref:`shape IDs <shape-id>` to suppress. An entire
        namespace can be suppressed by suffixing a namespace name with ``#``.
        For example, ``foo.baz#`` can be used to suppress all validation events
        on shapes in the "foo.baz" namespace.
    * - reason
      - ``string``
      - Provides a reason for the suppression.

One or more entries from the ``ids`` list and one or more entries from the
``shapes`` list (if provided) MUST match in order for a validation event to be
suppressed.

An example suppression for the "UnreferencedShape" validator:

.. code-block:: smithy

    metadata suppressions = [
      {
        // The list of rules to suppress.
        ids: ["UnreferencedShape"],
        // The optional list of shapes that are suppressed.
        shapes: ["foo.baz#SomeShape/members/someMemberName"],
        // The optional reason that the rule is suppressed.
        reason: "This shape is used for code generation."
      }
    ]

An example suppression that suppresses all validation events for all shapes
within a specific namespace:

.. code-block:: smithy

    metadata suppressions = [
      {
        ids: ["*"],
        shapes: ["smithy.testing#"],
        reason: "smithy.testing is used only for testing"
      }
    ]


Naming validators
=================


.. _AbbreviationName:

AbbreviationName
----------------

Validates that shape names and member names do not represent abbreviations
with all uppercase letters. For example, instead of using "XMLRequest" or
"instanceID", this validator recommends using "XmlRequest" and "instanceId".

Rationale
    Using a strict form of camelCase where abbreviations are written just
    like other words makes names more predictable and easier to work with
    in tooling. For example, a tool that generates code in Python might wish
    to represent camelCase words using snake_case; utilizing strict camel
    casing makes it easier to split words apart.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - allowedAbbreviations
         - [ ``string`` ]
         - A case-insensitive list of abbreviations to allow to be all capital
           letters. Defaults to an empty list.

Example:

.. code-block:: smithy

    metadata validators = [{name: "AbbreviationName"}]


.. _CamelCase:

CamelCase
---------

Validates that shape names and member names adhere to a consistent style of
camel casing. By default, this validator will ensure that shape names use
UpperCamelCase, and that member names use lowerCamelCase.

Rationale
    Utilizing a consistent camelCase style makes it easier to understand a
    model and can lead to consistent naming in code generated from Smithy
    models.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - shapeNames
         - ``string``
         - Specifies the camelCase style of shape names. Can be set to either
           "upper" (the default) or "lower".
       * - memberNames
         - ``string``
         - Specifies the camelCase style of member names. Can be set to either
           "upper" or "lower" (the default).

Example:

.. code-block:: smithy

    metadata validators = [{name: "CamelCase"}]


.. _ReservedWords:

ReservedWords
-------------

Validates that shape names and member names do not match a configured set of
reserved words.

Reserved words are compared in a case-insensitive manner via substring match
and support a leading and trailing wildcard character, "*". See
:ref:`wildcard evaluation <reserved-words-wildcards>` for more detail.

Rationale
    Tools that generate code from Smithy models SHOULD automatically convert
    reserved words into symbols that are safe to use in the targeted
    programming language. This validator can be used to warn about these
    conversions as well as to prevent sensitive words, like internal
    code-names, from appearing in public artifacts.

Default Severity
    ``DANGER``

Configuration
    A single key, ``reserved``, is **Required** in the configuration. Its
    value is a list of objects with the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 20 20 60

        * - Property
          - Type
          - Description
        * - words
          - [ ``string`` ]
          - **Required**. A list of words that shape or member names MUST not
            case-insensitively match. Supports only the leading and trailing
            wildcard character of "*".
        * - selector
          - ``string``
          - Specifies a selector of shapes to validate for this configuration.
            Defaults to validating all shapes, including member names.

            .. note::

                When evaluating member shapes, the *member name* will be
                evaluated instead of the shape name.
        * - reason
          - ``string``
          - A reason to display for why this set of words is reserved.

Example:

.. code-block:: smithy

    metadata validators = [{
      id: "FooReservedWords"
      name: "ReservedWords",
      configuration: {
        reserved: [
          {
            words: [
              "Codename"
            ],
            reason: "This is the internal project name.",
          },
        ]
      }
    }]


.. _reserved-words-wildcards:

Wildcards in ReservedWords
~~~~~~~~~~~~~~~~~~~~~~~~~~

The ReservedWords validator allows leading and trailing wildcard characters to
be specified.

- Using both a leading and trailing wildcard indicates that shape or member
  names match when case-insensitively **containing** the word. The following
  table shows matches for a reserved word of ``*codename*``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - Create\ **Codename**\ Input
        - Match
      * - **Codename**\ Resource
        - Match
      * - Referenced\ **Codename**
        - Match
      * - **Codename**
        - Match

- Using a leading wildcard indicates that shape or member names match when
  case-insensitively **ending with** the word. The following table shows
  matches for a reserved word of ``*codename``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - CreateCodenameInput
        - No match
      * - CodenameResource
        - No match
      * - Referenced\ **Codename**
        - Match
      * - **Codename**
        - Match

- Using a trailing wildcard indicates that shape or member names match when
  case-insensitively **starting with** the word. The following table shows
  matches for a reserved word of ``codename*``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - CreateCodenameInput
        - No match
      * - **Codename**\ Resource
        - Match
      * - ReferencedCodename
        - No Match
      * - **Codename**
        - Match

- Using no wildcards indicates that shape or member names match when
  case-insensitively **the same as** the word. The following table shows
  matches for a reserved word of ``codename``:

  .. list-table::
      :header-rows: 1
      :widths: 75 25

      * - Example
        - Result
      * - CreateCodenameInput
        - No match
      * - CodenameResource
        - No match
      * - ReferencedCodename
        - No match
      * - **Codename**
        - Match



.. _StandardOperationVerb:

StandardOperationVerb
---------------------

Looks at each operation shape name and determines if the first word in the
operation shape name is one of the defined standard verbs or if it is a verb
that has better alternatives.

.. note::

    Operations names MUST use a verb as the first word in the shape name
    in order for this validator to properly function.

Rationale
    Using consistent verbs for operation shape names helps consumers of the
    API to more easily understand the semantics of an operation.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - verbs
         - [ ``string`` ]
         - The list of verbs that each operation shape name MUST start with.
       * - prefixes
         - [ ``string`` ]
         - A list of prefixes that MAY come before one of the valid verbs.
           Prefixes are often used to group families of operations under a
           common prefix (e.g., ``batch`` might be a common prefix in some
           organizations). Only a single prefix is honored.
       * - suggestAlternatives
         - ``object``
         - Used to recommend alternative verbs. Each key is the name of a verb
           that should be changed, and each value is a list of suggested
           verbs to use instead.

.. note::

    At least one ``verb`` or one ``suggestAlternatives`` key-value pair MUST
    be provided.

Example:

.. code-block:: smithy

    metadata validators = [{
      name: "StandardOperationVerb",
      configuration: {
        verbs: ["Register", "Deregister", "Associate"],
        prefixes: ["Batch"],
        suggestAlternatives: {
          "Make": ["Create"],
          "Transition": ["Update"],
        }
      }
    }]


.. _StutteredShapeName:

StutteredShapeName
------------------

Validators that :ref:`structure` member names and :ref:`union` member
names do not stutter their shape names.

As an example, if a structure named "Table" contained a member named
"TableName", then this validator would emit a WARNING event.

Rationale
    Repeating a shape name in the members of identifier of the shape is
    redundant.

Default severity
    ``WARNING``


Best practice validators
========================

.. _DeprecatedAuthSchemes:

DeprecatedAuthSchemes
---------------------

Validates that :ref:`auth schemes <auth-trait>` used are not in the
configured set of deprecated schemes. A validation event is emitted when one
of the deprecated auth schemes is found on a service shape.

Rationale
    As a service evolves, its authentication schemes might too. This validator
    can be used to inform consumers of a Smithy model that the auth scheme
    listed should be considered deprecated.

Default Severity
    ``WARNING``

Configuration
    .. list-table::
        :header-rows: 1
        :widths: 20 20 60

        * - Property
          - Type
          - Description
        * - schemes
          - [ ``string`` ]
          - **Required**. A list of deprecated auth scheme names.
        * - reason
          - ``string``
          - A reason to display for why these auth schemes are deprecated.

Example:

.. code-block:: smithy

    metadata validators = [{
      id: "DeprecateFooScheme"
      name: "DeprecatedAuthSchemes",
      configuration: {
        schemes: [
          "foo"
        ],
        reason: "Please migrate to the foo2 scheme.",
      }
    }]


.. _DeprecatedProtocols:

DeprecatedProtocols
-------------------

Validates that :ref:`protocols <protocols-trait>` used are not in the
configured set of deprecated protocols. A validation event is emitted when one
of the deprecated protocols is found on a service shape.

Rationale
    As a service evolves, its protocols might too. This validator can be used
    to inform consumers of a Smithy model that the protocol listed should be
    considered deprecated.

Default Severity
    ``WARNING``

Configuration
    .. list-table::
        :header-rows: 1
        :widths: 20 20 60

        * - Property
          - Type
          - Description
        * - protocols
          - [ ``string`` ]
          - **Required**. A list of deprecated protocol names.
        * - reason
          - ``string``
          - A reason to display for why these protocols are deprecated.

Example:

.. code-block:: smithy

    metadata validators = [{
      id: "DeprecateFooProtocol"
      name: "DeprecatedProtocols",
      configuration: {
        protocols: [
          "foo"
        ],
        reason: "Please migrate to the bar protocol.",
      }
    }]


.. _InputOutputStructureReuse:

InputOutputStructureReuse
-------------------------

Detects when a structure is used as both input and output or if a structure
is referenced as the input or output for multiple operations.

Rationale
    1. Using the same structure for both input and output can lead to
       backward-compatibility problems in the future if the members or traits
       used in input needs to diverge from those used in output. It is always
       better to use structures that are exclusively used as input or exclusively
       used as output.
    2. Referencing the same input or output structure from multiple operations
       can lead to backward-compatibility problems in the future if the
       inputs or outputs of the operations ever need to diverge. By using the
       same structure, you are unnecessarily tying the interfaces of these
       operations together.

Default severity
    ``DANGER``


.. _MissingPaginatedTrait:

MissingPaginatedTrait
---------------------

Checks for operations that look like they should be paginated but do not
have the :ref:`paginated-trait`.

Rationale
    Paginating operations that can return potentially unbounded lists of
    data helps to maintain a predictable SLA and helps to prevent operational
    issues in the future.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - verbsRequirePagination
         - [``string``]
         - Defines the case-insensitive operation verb prefixes for operations
           that MUST be paginated. A ``DANGER`` event is emitted for any
           operation that has a shape name that starts with one of these verbs.
           Defaults to ``["list", "search"]``.
       * - verbsSuggestPagination
         - [``string``]
         - Defines the case-insensitive operation verb prefixes for operations
           that SHOULD be paginated. A ``WARNING`` event is emitted when an
           operation is found that matches one of these prefixes, the operation
           has output, and the output contains at least one top-level member
           that targets a :ref:`list`. Defaults to ``["describe", "get"]``
       * - inputMembersRequirePagination
         - [``string``]
         - Defines the case-insensitive operation input member names that
           indicate that an operation MUST be paginated. A ``DANGER`` event
           is emitted if an operation is found to have an input member name
           that case-insensitively matches one of these member names.
           Defaults to ``["maxResults", "pageSize", "limit", "nextToken", "pageToken", "token"]``
       * - outputMembersRequirePagination
         - [``string``]
         - Defines the case-insensitive operation output member names that
           indicate that an operation MUST be paginated. A ``DANGER`` event
           is emitted if an operation is found to have an output member name
           that case-insensitively matches one of these member names.
           Defaults to ``["nextToken", "pageToken", "token", "marker", "nextPage"]``.

Example:

.. code-block:: smithy

   metadata validators = [{name: "MissingPaginatedTrait"}]


Modeling validators
===================


.. _ShouldHaveUsedTimestamp:

ShouldHaveUsedTimestamp
-----------------------

Looks for shapes that likely represent time, but that do not use a
timestamp shape.

The ShouldHaveUsedTimestamp validator checks the following names:

* string shape names
* short, integer, long, float, and double shape names
* structure member names
* union member names

The ShouldHaveUsedTimestamp validator checks each of the above names to see if
they likely represent a time value. If a name does look like a time value,
the shape or targeted shape MUST be a timestamp shape.

A name is assumed to represent a time value if it:

* Begins or ends with the word "time"
* Begins or ends with the word "date"
* Ends with the word "at"
* Ends with the word "on"
* Contains the exact string "timestamp" or "Timestamp"

For the purpose of this validator, words are matched case insensitively. Words
are separated by either an underscore character, or by mixed case characters.
For example, "FooBar", "fooBar", "foo_bar", "Foo_Bar", and "FOO_BAR" all
contain the same two words, "foo" and "bar".

Rationale
    Smithy tooling can convert timestamp shapes into idiomatic language types
    that make them easier to work with in client tooling.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - additionalPatterns
         - [ ``string`` ]
         - A list of regular expression patterns that identify names that
           represent time.


.. _UnreferencedShape:

UnreferencedShape
-----------------

Looks for shapes that are not connected to from any service shape within
the model.

Rationale
    Unreferenced shapes are good candidates for removal from a model.

Default severity
    ``NOTE``


Misc validators
===============

.. _EmitEachSelector:

EmitEachSelector
----------------

Emits a validation event for each shape that matches the given
:ref:`selector <selectors>`.

Rationale
    Detecting shapes that violate a validation rule using customizable
    validators allows organizations to create custom Smithy validators without
    needing to write code.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - selector
         - ``string``
         - **Required**. A valid :ref:`selector <selectors>`. Each shape in
           the model that is returned from the selector with emit a validation
           event.

Example:

The following example detects if a shape is missing documentation with the
following constraints:

- Shapes that have the documentation trait are excluded.
- Members that target shapes that have the documentation trait are excluded.
- Simple types are excluded.
- List and map members are excluded.

.. code-block:: smithy

    metadata validators = [{
      name: "EmitEachSelector",
      id: "MissingDocumentation",
      message: "This shape is missing documentation"
      configuration: {
        selector: "
            :not([trait|documentation])
            :not(simpleType)
            :not(member:of(:each(list, map)))
            :not(:test(member > [trait|documentation]))"
      }
    }]

The following example emits a validation event for each structure referenced as
input/output that has a shape name that does not case-insensitively end with
"Input"/"Output":

.. code-block:: smithy

    metadata validators = [
      {
        name: "EmitEachSelector",
        id: "OperationInputName",
        message: "This shape is referenced as input but the name does not end with 'Input'",
        configuration: {
          selector: "operation -[input]-> :not([id|name$=Input i])"
        }
      },
      {
        name: "EmitEachSelector",
        id: "OperationOutputName",
        message: "This shape is referenced as output but the name does not end with 'Output'",
        configuration: {
          selector: "operation -[output]-> :not([id|name$=Output i])"
        }
      }
    ]

The following example emits a validation event for each operation referenced
as lifecycle 'read' or 'delete' that has a shape name that does not start with
"Get" or "Delete":

.. code-block:: smithy

    metadata validators = [
      {
        name: "EmitEachSelector",
        id: "LifecycleGetName",
        message: "Lifecycle 'read' operation shape names should start with 'Get'",
        configuration: {
          selector: "operation [read]-> :not([id|name^=Get i])"
        }
      },
      {
        name: "EmitEachSelector",
        id: "LifecycleDeleteName",
        message: "Lifecycle 'delete' operation shape names should start with 'Delete'",
        configuration: {
          selector: "operation -[delete]-> :not([id|name^=Delete i])"
        }
      }
    ]


.. _EmitNoneSelector:

EmitNoneSelector
----------------

Emits a validation event if no shape in the model matches the given
:ref:`selector <selectors>`.

Rationale
    Detecting the omission of a specific trait, pattern, or other requirement
    can help developers to remember to apply constraint traits, documentation,
    etc.

Default severity
    ``DANGER``

Configuration
    .. list-table::
       :header-rows: 1
       :widths: 20 20 60

       * - Property
         - Type
         - Description
       * - selector
         - ``string``
         - **Required**. A valid :ref:`selector <selectors>`. If no shape
           in the model is returned by the selector, then a validation event
           is emitted.

Example:

The following example detects if the model does not contain any constraint
traits.

.. code-block:: smithy

    metadata validators = [{
      name: "EmitNoneSelector",
      id: "MissingConstraintTraits",
      message: "No instances of the enum, pattern, length, or range trait "
          "could be found. Did you forget to apply these traits?",
      configuration: {
        selector: ":each([trait|enum], [trait|pattern], [trait|length], [trait|range])",
      }
    }]
