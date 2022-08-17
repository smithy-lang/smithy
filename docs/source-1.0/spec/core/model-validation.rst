.. _validation:

================
Model validation
================

Smithy provides a customizable validation system that can be used by
API designers and organizations to ensure that their APIs adhere to their
own standards and best practices.

------------
Introduction
------------

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

----------
Validators
----------

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
    * - selector
      - ``string``
      - A valid :ref:`selector <selectors>` that causes the validator to only
        validate shapes that match the selector. The validator will ignore any
        validation events encountered that do not satisfy the selector.
    * - configuration
      - ``object``
      - Object that provides validator configuration. The available properties
        are defined by each validator. Validators MAY require that specific
        configuration properties are provided.

The following Smithy document applies a custom validator named "SomeValidator":

.. code-block:: smithy

    $version: "1.0"

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

    namespace smithy.example

    // shapes are defined here...


.. _missing-validators:

Missing validators
==================

The actual implementation of a validator is defined in code and is
not defined in the Smithy model itself. If a Smithy implementation does not
have an implementation for a specific validator by name, the Smithy
implementation MUST emit a WARNING validation event with an event ID that is
the concatenation of ``UnknownValidator_`` and the ``name`` property of the
validator that could not be found. For example, given a custom validator
that could not be found named ``Foo``, the implementation MUST emit a
validation event with an event ID of ``UnknownValidator_Foo`` and a
severity of WARNING.


.. _severity-definition:

--------
Severity
--------

When a model is in violation of a validator, a *validation event* is emitted.
This validation event contains metadata about the violation, including the
optional shape that was in violation, the validator ID, and the severity of
the violation. *Severity* is used to define the importance or impact of
a violation.

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

------------
Suppressions
------------

Suppressions are used to suppress specific validation events.
Suppressions are created using the :ref:`suppress-trait` and
:ref:`suppressions metadata <suppressions-metadata>`.


.. _suppress-trait:

``suppress`` trait
=====================

Summary
    The suppress trait is used to suppress validation events(s) for a
    specific shape. Each value in the ``suppress`` trait is a
    validation event ID to suppress for the shape.
Trait selector
    ``*``
Value type
    ``[string]``

The following example suppresses the ``Foo`` and ``Bar`` validation events
for the ``smithy.example#MyString`` shape:

.. tabs::

    .. code-tab:: smithy

        $version: "1.0"

        namespace smithy.example

        @suppress(["Foo", "Bar"])
        string MyString


.. _suppressions-metadata:

Suppression metadata
====================

The ``suppressions`` metadata property contains an array of suppression objects
that are used to suppress validation events for the entire model or for an
entire namespace.

Each suppression object in the ``suppressions`` array supports the
following properties:

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The validation event ID to suppress.
    * - namespace
      - ``string``
      - **Required**. The validation event is only suppressed if it matches the
        supplied namespace. A value of ``*`` can be provided to match any namespace.
        ``*`` is useful for suppressing validation events that are not bound to any
        specific shape.
    * - reason
      - ``string``
      - Provides an optional reason for the suppression.

The following example suppresses all validation events on shapes
in the ``foo.baz`` namespace with an ID of ``UnreferencedShape``:

.. code-block:: smithy

    $version: "1.0"

    metadata suppressions = [
        {
            id: "UnreferencedShape",
            namespace: "foo.baz",
            reason: "This is a test namespace."
        }
    ]

The following example suppresses all validation events with an
ID of ``OverlyBroadValidator``:

.. code-block:: smithy

    $version: "1.0"

    metadata suppressions = [
        {
            id: "OverlyBroadValidator",
            namespace: "*"
        }
    ]


-------------------
Built-in validators
-------------------

Smithy provides built-in validators that can be used in any model in
the ``validators`` metadata property. Implementations MAY support
additional validators.


.. _EmitEachSelector:

EmitEachSelector
================

Emits a validation event for each shape that matches the given
:ref:`selector <selectors>`.

Rationale
    Detecting shapes that violate a validation rule using customizable
    validators allows organizations to create custom Smithy validators
    without needing to write code.

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
         - **Required**. A valid :ref:`selector <selectors>`. A validation
           event is emitted for each shape in the model that matches the
           ``selector``.
       * - :ref:`bindToTrait <emit-each-bind-to-trait>`
         - ``string``
         - An optional string that MUST be a valid :ref:`shape ID <shape-id>`
           that targets a :ref:`trait definition <trait-shapes>`.
           A validation event is only emitted for shapes that have this trait.
       * - :ref:`messageTemplate <emit-each-message-template>`
         - ``string``
         - A custom template that is expanded for each matching shape and
           assigned as the message for the emitted validation event.

The following example detects if a shape is missing documentation with the
following constraints:

- Shapes that have the documentation trait are excluded.
- Members that target shapes that have the documentation trait are excluded.
- Simple types are excluded.
- List and map members are excluded.

.. code-block:: smithy

    $version: "1.0"

    metadata validators = [{
        name: "EmitEachSelector",
        id: "MissingDocumentation",
        message: "This shape is missing documentation",
        configuration: {
            selector: """
                :not([trait|documentation])
                :not(simpleType)
                :not(member :test(< :test(list, map)))
                :not(member > [trait|documentation])"""
        }
    }]

The following example emits a validation event for each structure referenced as
input/output that has a shape name that does not case-insensitively end with
"Input"/"Output":

.. code-block:: smithy

    $version: "1.0"

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

    $version: "1.0"

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


.. _emit-each-bind-to-trait:

Binding events to traits
------------------------

The ``bindToTrait`` property contains a :ref:`shape ID <shape-id>` that MUST
reference a :ref:`trait definition <trait-shapes>` shape. When set, this
property causes the ``EmitEachSelector`` validator to only emit validation
events for shapes that have the referenced trait. The contextual location of
where the violation occurred in the model SHOULD point to the location where
the trait is applied to the matched shape.

Consider the following model:

.. code-block:: smithy

    metadata validators = [
        {
            name: "EmitEachSelector",
            id: "DocumentedString",
            configuration: {
                // matches all shapes
                selector: "*",
                // Only emitted for shapes with the documentation
                // trait, and each event points to where the
                // trait is defined.
                bindToTrait: documentation
            }
        }
    ]

    namespace smithy.example

    @documentation("Hello")
    string A // <-- Emits an event

    string B // <-- Does not emit an event

The ``DocumentedString`` validator will only emit an event for
``smithy.example#A`` because ``smithy.example#B`` does not have the
:ref:`documentation-trait`.


.. _emit-each-message-template:

Message templates
-----------------

A ``messageTemplate`` is used to create more granular error messages. The
template consists of literal spans and :token:`selector context value <selectors:SelectorContextValue>`
templates (for example, ``@{id}``). A selector context value MAY be escaped
by placing a ``@`` before a ``@`` character (for example, ``@@`` expands to
``@``). ``@`` characters in the message template that are not escaped MUST
form a valid ``selector_context_value`` production.

For each shaped matched by the ``selector`` of an ``EmitEachSelector``, a
:ref:`selector attribute <selector-attributes>` is created from the shape
along with all of the :ref:`selector variables <selector-variables>` that were
assigned when the shape was matched. Each ``selector_context_value`` in the
template is then expanded by retrieving nested properties from the shape
using a pipe-delimited path (for example, ``@{id|name}`` expands to the
name of the matching shape's :ref:`shape ID <id-attribute>`).

Consider the following model:

.. code-block:: smithy

    metadata validators = [
        {
            name: "EmitEachSelector",
            configuration: {
                selector: "[trait|documentation]",
                messageTemplate: """
                    This shape has a name of @{id|name} and a @@documentation \
                    trait of "@{trait|documentation}"."""
            }
        }
    ]

    namespace smithy.example

    @documentation("Hello")
    string A

    @documentation("Goodbye")
    string B

The above selector will emit two validation events:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Shape ID
      - Expanded message
    * - ``smithy.example#A``
      - This shape has a name of A and a @documentation trait of "Hello".
    * - ``smithy.example#B``
      - This shape has a name of B and a @documentation trait of "Goodbye".

:ref:`Selector variables <selector-variables>` can be used in the selector
to make message templates more descriptive. Consider the following example:

.. code-block:: smithy

    metadata validators = [
        {
            name: "EmitEachSelector",
            id: "UnstableTrait",
            configuration: {
                selector: """
                      $matches(-[trait]-> [trait|unstable])
                      ${matches}""",
                messageTemplate: "This shape applies traits(s) that are unstable: @{var|matches|id}"
            }
        }
    ]

    namespace smithy.example

    @trait
    @unstable
    structure doNotUseMe {}

    @doNotUseMe
    string A

The above selector will emit the following validation event:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Shape ID
      - Expanded message
    * - ``smithy.example#A``
      - This shape applies traits(s) that are unstable: [smithy.example#doNotUseMe]


Variable message formatting
---------------------------

Different types of variables expand to different kinds of strings in message
templates.

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Attribute
      - Expansion
    * - empty values
      - An empty value expands to nothingness [#comparison]_. Empty values are
        created when a selector context value attempts to access a variable
        or nested property that does not exist.

        Consider the following message template: ``Hello, @{foo}.``. Because
        ``foo`` is not a valid selector attribute, the message expands to:

        .. code-block:: none

            Hello, .
    * - :ref:`id <id-attribute>`
      - Expands to the absolute :ref:`shape ID <shape-id>` of a shape
        [#comparison]_.
    * - literal values
      - Literal values are created when descending into nested properties of
        an ``id``, ``service``, or projection attribute. A literal string is
        expanded to the contents of the string with no wrapping quotes.
        A literal integer is expanded to the string representation of the
        number. [#comparison]_
    * - :ref:`node <node-attribute>`
      - A JSON formatted string representation of a trait or nested property
        of a trait. The JSON is *not* pretty-printed, meaning there is no
        indentation or newlines inserted into the JSON output for formatting.
        For example, a template of ``@{trait|tags}`` applied to a shape with
        a :ref:`tags-trait` that contains "a" and "b" would expand to:

        .. code-block:: none

            ["a","b"]
    * - :ref:`projection <projection-attribute>`
      - Expands to a list that starts with ``[`` and ends with ``]``. Each
        shape in the projection is inserted into the list using variable
        message formatting. Subsequent shapes are separated from the previous
        shape by a comma followed by a space. If a variable projection
        (for example, ``@{var|foo}``) contains two shape IDs,
        ``smithy.example#A`` and ``smithy.example#B``, the attribute expands
        to:

        .. code-block:: none

            [smithy.example#A, smithy.example#B]
    * - :ref:`service <service-attribute>`
      - Expands to the absolute shape ID of a service shape [#comparison]_.
    * - :ref:`trait <trait-attribute>`
      -  Expands to nothingness [#comparison]_.

.. [#comparison] This is the same behavior that is used when the attribute is used in a :ref:`string comparison <string-comparators>`.


.. _EmitNoneSelector:

EmitNoneSelector
================

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

The following example detects if the model does not contain any constraint
traits.

.. code-block:: smithy

    $version: "1.0"

    metadata validators = [{
        name: "EmitNoneSelector",
        id: "MissingConstraintTraits",
        message: """
            No instances of the enum, pattern, length, or range trait
            could be found. Did you forget to apply these traits?""",
        configuration: {
            selector: ":is([trait|enum], [trait|pattern], [trait|length], [trait|range])",
        }
    }]
