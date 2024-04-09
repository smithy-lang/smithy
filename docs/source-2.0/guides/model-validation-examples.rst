=========================
Model Validation Examples
=========================

:ref:`validator-definition` can be used to lint Smithy models to help avoid
common pitfalls and bugs as well as to enforce a common style for APIs
designed with Smithy.

This document provides a "cookbook" of examples for using,
suppressing, and modifying Smithy Validators.


Suppress a validator for a single shape or member
=================================================

Use the :ref:`@suppress <suppress-trait>` trait to suppress a validator on
a single shape or member. The example below shows the use of the suppress
trait to suppress the ``Foo`` and ``Bar`` validators.

.. code-block:: smithy

    // This trait cannot pass Foo or Bar validators
    @suppress(["Foo", "Bar"])
    structure exampleStructure {
        fieldA: String,
        fieldB: Integer
    }

.. tip::
    It is generally recommended to add a comment above the ``@suppress``
    trait to explain the reason for the suppression.


Suppress a validator for a namespace
====================================

Validators can be suppressed for an entire namespace rather than just for a
single shape. The following example demonstrates how to suppress the
``ShouldHaveUsedTimestamp`` validator for all shapes within the ``com.example
.weather`` namespace.

.. code-block:: smithy

    metadata suppressions = [
        {
            id: "ShouldHaveUsedTimestamp",
            namespace: "com.example.weather",
            reason: "Ignore `ShouldHaveUsedTimestamp` validator for `com.example.weather` namespace."
        },
    ]


Execute validator for all shapes matching selector
==================================================

We can use selectors to only run a validator for shapes matching that
selector. The following example demonstrates how to have the
``MissingPaginatedTrait`` built-in validator ignore any operations with the
``@internal`` trait. The ``MissingPaginatedTrait`` will run for only shapes
that are not operations with the ``@internal`` trait.

.. code-block:: smithy

    {
        name: "MissingPaginatedTrait",
        selector: ":not(operation[trait|internal])"
    }


Ignore built-in validator for multiple namespaces
=================================================

Selectors can be used to ignore a validator for specific namespaces such as
the root smithy namespace. This can be useful when you want a validator to
ignore imported shapes from another smithy package. The following example
shows how to have the ``MissingPaginatedTrait`` built-in validator ignore any
shapes in the ``smithy.*`` or ``com.example.*`` namespaces.

.. code-block:: smithy

    {
        name: "MissingPaginatedTrait",
        selector: ":not([id|namespace ^= 'com.example', 'smithy.'])"
    }


Ignore validator for specific shape id
======================================

Selectors can also be used to ignore one or more shapes based on the shape's
id. This example uses selectors to ignore a validator for a shape with a
specific name. In this case the ``MissingPaginatedTrait`` built-in validator
is set to ignore the shape with the name "IgnorableShape".

.. code-block:: smithy

    {
        name: "MissingPaginatedTrait",
        selector: ":not([id|name = 'IgnorableShape' i])"
    }


Set a custom severity for built-in validator
============================================

The following example demonstrates how to update the :ref:`severity
<severity-definition>` level of a built-in validator to a custom value. The
following example will cause validation events emitted by the
``MissingPaginatedTrait`` validator to be emitted at a ``WARNING`` level
instead of at the default ``ERROR`` severity.


.. code-block:: smithy

    {
        name: "MissingPaginatedTrait",
        severity: "WARNING""
    }


Common suffix on all operation inputs
=====================================

You may want to enforce a convention of all operation inputs ending with a
specific string such as "Request". The following example creates a custom
linter that checks that all operation input names end with "Request".

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "OperationInputName",
        configuration: {
            messageTemplate: """
                `@{id}` is bound as an input of `@{var|operation|id}` \
                but does not have a name ending with 'Request'.
                """,
            selector: "$operation(*) -[input]-> :not([id|name$=Request])"
        }
    }


Common suffix on all operation outputs
======================================

You may want to enforce a convention of all operation outputs ending with a
specific string such as "Response". The following example linter checks that
all operation output names end with "Response".

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "OperationOutputName",
        configuration: {
            messageTemplate: """
                `@{id}` is bound as an output of `@{var|operation|id}` \
                but does not have a name ending with 'Response'
                """,
            selector: "$operation(*) -[output]-> :not([id|name$=Response])"
        }
    }


Common suffix on all error shapes
=================================

The following example creates a linter that checks that all error shapes end
with "Exception".

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "OperationErrorName",
        configuration: {
            messageTemplate: """
                `@{id}` is bound as an error but does not have a name ending with 'Exception'. \
                Perhaps you should rename this shape to `@{id|name}Exception`.
                """,
            selector: "operation -[error]-> :not([id|name$=Exception])"
        }
    }


Forbid prefix on shape members
==============================

This example checks that no member names begin with "is" or "Is". This
particular case is useful to prevent problems when using libraries such as
Jackson which changes behavior when object fields have "is" prefixes.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "ForbiddenIsPrefix",
        message: "This member starts with forbidden prefix 'is'",
        configuration: {
            selector: "[id|member ^='is','Is']"
        }
    }


Require integers to have a ``@range`` constraint
================================================

This example shows how to require all integers used in an operation input
have a range constraint with both a minimum and maximum value. The first
validator checks that the range trait exists on the shape, while the other
two validators check that both the maximum and minimum values of the range
are both filled out. This validation is split across three separate linters
to have clear, actionable error messages.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "RawIntegerWithoutRange",
        configuration: {
            messageTemplate: """
            This number shape in member `@{id}` of the operation input `@{var|structure}` \
            does not have a range constraint on both its minimum or maximum value. \
            Add the `@@range` trait to this integer shape and provide both minimum and maximum values. \
            For example, `@@range(min: 1, max: 500)`.
            """,
            selector: """
                operation -[input]-> $structure(*) > member
                :test(> number:not([trait|range|min]):not([trait|range|max]))
                """
        }
    },
    {
        name: "EmitEachSelector",
        id: "RawIntegerWithoutRangeMin",
        configuration: {
            messageTemplate: """
            This number shape in member `@{id}` of the operation input `@{var|structure}` \
            does not have a maximum range constraint. \
            Add a minimum value to the `@@range` trait on this shape. \
            For example, `@@range(>>> min: 1 <<<, max: 500)`.
            """,
            selector: """
                operation -[input]-> $structure(*) > member
                :test(> number[trait|range]:not([trait|range|min]))
                """
        }
    },
    {
        name: "EmitEachSelector",
        id: "RawIntegerWithoutRangeMax",
        configuration: {
            messageTemplate: """
            This number shape in member `@{id}` of the operation input `@{var|structure}` \
            does not have a maximum range constraint. \
            Add a maximum value to the `@@range` trait on this shape. \
            For example, `@@range(min: 1, >>> max: 500 <<<)`.
            """,
            selector: """
                operation -[input]-> $structure(*) > member
                :test(> number[trait|range]:not([trait|range|max]))
                """
        }
    }


Require lists to have an ``@length`` constraint
===============================================

This example shows how to require all List shapes in the ``com.example.weather``
namespace to apply the ``@length`` constraint trait with both a
minimum and maximum value. This validation is split across three separate
linters to have clear, actionable error messages.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "ListWithoutLengthConstraint",
        configuration: {
            messageTemplate: """
            List shape `@{id}` does not have a length constraint specified. \
            Add the `@@length` trait to the list shape. For example, `@@length(min: 1, max: 2)`.
            """,
            selector: "list:not([trait|length])"
        }
    },
    {
        name: "EmitEachSelector",
        id: "ListWithoutLengthConstraintMinimum",
        configuration: {
            messageTemplate: """
            List shape `@{id}` does not have a minimum length specified. \
            Add a `min` value to the `@@length` trait on the list shape. \
            For example, `@@length(>>> min: 1 <<<, max: 2)`.
            """,
            selector: "list[trait|length]:not([trait|length|min])"
        }
    },
    {
        name: "EmitEachSelector",
        id: "ListWithoutLengthConstraintMaximum",
        configuration: {
            messageTemplate: """
            List shape `@{id}` does not have a maximum length specified. \
            Add a `max` value to the `@@length` trait on the list shape. \
            For example, `@@length(min: 1, >>> max: 2 <<<)`.
            """,
            selector: "list[trait|length]:not([trait|length|max])"
        }
    }


Require strings to have a ``@pattern`` constraint
=================================================

This example shows how to require all strings used in an operation input to
have a ``@pattern`` constraint trait.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "RawStringWithoutPattern",
        namespace: ["com.example.weather"],
        configuration: {
            messageTemplate: """
            This String shape in member `@{id}` of the operation input `@{var|structure}` \
            does not have a pattern constraint. \
            Add the `@@pattern` trait to this string shape and provide a regex pattern. \
            For example, `@@pattern("^[\\S\\s]+$")`.
            """,
            selector: """
                operation -[input]-> $structure(*) > member
                :test(> string:not([trait|enum]):not([trait|pattern]))
                """
        }
    }


Require ``@externalDocumentation`` trait to provide a homepage entry
====================================================================

The following example shows how to enforce that all uses of the
:ref:`@externaldocumentation <externaldocumentation-trait>` include a
``Homepage`` entry.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "ExternalDocumentationMustIncludeHomePageValue",
        configuration: {
            messageTemplate: """
            @{id} has the `@@externalDocumentation` trait applied, but does not define a `"HomePage"` entry. \
            The following keys `@{trait|externalDocumentation|(keys)}` were defined for `@@externalDocumentation`, \
            but expected `"HomePage"` key.
            """,
            selector: "[trait|externalDocumentation]:not([trait|externalDocumentation|(keys) = Homepage])"
        }
    }


Limit shape name length
=======================

The following example limits the length of shape names within the ``com.example.weather``
namespace to between 3 and 80 characters.

.. code-block:: smithy

    {
        name: "EmitEachSelector"
        id: "ShapeNameLength",
        namespace: ["com.example.weather"],
        configuration: {
            messageTemplate: """
            Shape name @{id|name} is @{id|name|(length)} characters long.
            Shape names must be less than 60 characters and longer than 3 characters.
            """,
            selector: ":not([@id|name: @{(length)} <= 60 && @{(length)} >= 3])"
        }
    }


Limit nesting depth of input and output shape members
=====================================================

This example checks that resources are not deeply nested. In this case, it
will check that the nesting depth is less than 4.

.. code-block:: smithy

    {
        "name": "EmitEachSelector",
        "id": "LimitNestingDepthToFourLayers",
        "configuration": {
            messageTemplate: """
                `@{id}`, bound to operation `@{var|operation}` has a nesting depth >4. This is typically not advised.
                You should look for ways to reduce the nesting depth of this shape.
                """,
            selector: """
                $operation(*) -[input, output, error]->
                :test(> member > * > member > * > member > * member > *)
                """
        }
    }


Operations should have documentation
====================================

This example checks for documentation on all operation shapes.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "MissingOperationDocumentation",
        configuration: {
            messageTemplate: """
                Operation `@{id|name}` is missing documentation. Add the `@@documentation` \
                trait to this operation.
                """,
            selector: "operation :not([trait|documentation])"
        }
    }


Examples on all operations
==========================

This example checks for examples on all operation shapes.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "MissingOperationExamples",
        configuration: {
            messageTemplate: """
                Operation `@{id|name}` is missing examples. Add the `@@examples` \
                trait to this operation.
                """,
            selector: "operation :not([trait|examples])"
        }
    }


Operations should have common exception
=======================================

The following example shows how to check that all operations throw a common
exception.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "OperationErrorsIncludesCommonException",
        configuration: {
            messageTemplate: """
            Expected error `CommonException` is not bound to operation @{id|name} . Add \
            `CommonException` to the list of errors bound to this operation.
            """
            selector: "operation :not(:test(-[error]-> [id|name=CommonException]))",
        }
    }


.. tip::
    If you want an operation to throw multiple common errors, you likely want
    to use a validator that checks for a common mixin rather than a single
    error. See :ref:`Operations Should Use Common Mixin
    <operations-should-use-common-mixin>`


.. _operations-should-use-common-mixin:

Operations should use common mixin
==================================

The following example checks that an operation uses a common :ref:`mixin
<mixin-trait>`, ``CommonMixin``. This is useful when you want operations to
have a common set of errors added via a common mixin.

.. code-block:: smithy

    {
        name: "EmitEachSelector"
        id: "OperationShouldUseCommonMixin",
        namespace: ["com.example.weather"],
        configuration: {
            messageTemplate: """
            Operation `@{id|name}` does not use expected mixin `CommonMixin`. Add the \
            mixin to this operation. For example, `operation @{id|name} with [ CommonMixin ]`.
            """
            selector: "operation :not(-[mixin]-> [id = CommonMixin])"
        }
    }


Check that models do not use an internal name
=============================================

The following example shows how to prevent the use of internal codewords
within a smithy model. In this case we want to prevent our models from using
the word "spork" in any member, structure, resource, operation, or service
name or within any trait or comment.

.. code-block:: smithy

    {
        id: "DontUseInternalNamesValidator",
        name: "ReservedWords",
        configuration: {
            reserved: [
                {
                    words: ["*spork*"],
                    reason: """
                    Sporks are a secret type of silverware. We can't let the rest of the world know about them.
                    """
                }
            ]
        }
    }


Check that models use a trait at least once
===========================================

The built-in ``EmitNoneSelector`` can be used to ensure at least one instance
of a trait is found in a model. The following example checks that at least
one usage of the ``@length`` trait is found within models in the ``com.example
.weather`` namespace.

.. code-block:: smithy

    {
        "name": "EmitNoneSelector",
        "id": "NoInstancesOfLengthTrait",
        "message": "No instances of the length trait were found.",
        "namespace": ["com.example.weather"]
        "configuration": {
            "selector": "[trait|length]"
        }
    }

.. tip::
    It is usually concerning if a model does not use common constraint traits
    such as ``@range``, ``@pattern``, or ``@length`` at all.


Exceptions thrown from operations must have an ``@httpError`` trait
===================================================================

This example shows how to enforce that all errors bound to an operation have
an ``@httpError`` trait (and therefore ``httpError`` code) defined.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "ErrorHasHttpErrorTrait",
        configuration: {
            messageTemplate: """
            `@{id|name}` is bound as an error but does not have the `@@httpErrorTrait`. \
            Apply the `@@httpErrorTrait` to this shape.
            """
            selector: "operation -[error]-> :not([trait|httpError])"
        }
    }

.. note::
    This example only makes sense for services using an http protocol.


Prefix headers with X-
======================

Case insensitive check for "X-" prefix on all custom ``@httpHeaders`` used in your
model.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "CustomHeadersHaveXDashPrefix",
        configuration: {
            messageTemplate: """
                @{id|name} applies the `@@httpHeader` with a custom header value of `@{trait|httpHeader}` that does not begin with `x-`.
                Custom headers should be prefixed with `x-`.
                """
            selector: "[trait|httpHeader]:not([trait|httpHeader^='x-' i])"
        }
    }

.. note::
    This example only makes sense for services using an http protocol.

.. tip::
    If you prefer to not prefix custom headers with "X-" then you can flip the logic of this validator by changing the
    selector to ``[trait|httpHeader^="x-" i]``


Lifecycle operation naming
==========================

Checks that operation names match with the lifecycle (CRUD) operations they
are bound to. This example shows how to check that the operation shape bound
to the "create" lifecycle operation prefixed with "create" (case insensitive).

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "LifecycleCreateName",
        configuration: {
            messageTemplate: """
            Operation `@{id|name}` is bound to Lifecycle operation 'create' on resource @{var|resource}. \
            `Create` operation names should begin with 'Create'. Did you mean `Create@{id|name}`?
            """,
            selector: "$resource(*) -[create]-> :not([id|name^=Create i])"
        }
    }

Prefer binding operations to resource over binding directly to a service
========================================================================

While it is occasionally necessary to bind operations directly to a service,
in most cases it is preferable to bind operations to a resource over binding
to the service directly. The example below shows how to validate that
operations are bound to resources and not to the service.

.. code-block:: smithy

    {
        name: "EmitEachSelector",
        id: "PreferResourceBindingOverServiceBinding",
        severity: "WARNING",
        configuration: {
            messageTemplate: """
                Operation @{id|name} is bound directly to the service @{var|target|id|name}. \
                Consider binding this operation to a resource instead.
                """
            selector: "$target(service) ${target} > operation"
        }
    }
