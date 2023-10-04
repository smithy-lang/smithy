.. _rules-engine-parameters:

=======================
Rules engine parameters
=======================

:ref:`Rules engine parameters <rules-engine-endpoint-rule-set-parameter>` are a
set of named property values that are used by a rules engine implementation to
resolve a service rule set. The set of parameters are rule set specific, and
MUST be generated as a public type in the service’s package or namespace. The
rule set MAY define zero or more parameters.

Parameter names MUST start with a letter and be followed by one or more
alphabetical or numerical characters. Parameter names are case-sensitive when
used in a rule set and accompanying Smithy traits. Parameters MUST have
case-insensitively unique names. This restriction allows for parameter names to
be normalized to a more idiomatic language naming convention. For example, a
parameter with the name ``endpointId`` may be converted to ``EndpointId`` or
``endpoint_id``.

This following is the :rfc:`ABNF <5234>` grammar for rule set parameter names:

.. productionlist:: smithy
    identifier = ALPHA *(ALPHA / DIGIT)

Parameters declare their respective type using the ``type`` key. There are two
supported rule set parameter types: ``string`` and ``boolean``. The following
table provides the description of these types, and their Smithy compatible
types whose values can be bound to these parameters. Rule set parameters are
always considered nullable and have no default value associated with them.

.. list-table::
    :header-rows: 1
    :widths: 10 23 67

    * - Parameter type
      - Smithy type
      - Description
    * - ``string``
      - ``string``
      - UTF-8 encoded string.
    * - ``boolean``
      - ``boolean``
      - Boolean value type.


.. _rules-engine-parameters-implementation:

Parameter implementations
=========================

Implementations MUST always generate a parameter structure, even in the absence
of parameters. This provides for a stable API interface regardless of a
service’s rule set evolution over time. The addition of parameters to a rule
set MUST be a backwards-compatible.

Implementations MUST generate parameter types using their language idiomatic
interfaces to distinguish whether values are set or unset by users. For
example, the Go programming language would generate these types as pointer
values, whereas Rust would wrap types using the ``Option`` type.


.. _rules-engine-parameters-evolution:

Parameter evolution
===================

Services MAY add additional parameters to their rule set in subsequent
revisions. Services MUST NOT remove parameters, but MAY remove the usage of
those parameters from their rule set. Service teams SHOULD use the :ref:`deprecated
property <rules-engine-endpoint-rule-set-parameter-deprecated>` to provide a
description of the deprecation and relevant recourse. Implementations SHOULD
use the ``deprecated`` property value to generate language specific
documentation to indicate a parameter's deprecation. A service SHOULD provide
documentation for a parameter using the ``documentation`` property.


.. _rules-engine-parameters-binding-values:

------------------------
Binding parameter values
------------------------

Rules engine implementations bind values to parameters defined in rule sets,
utilizing information in the service model in addition to the rules.
`Rules engine parameter traits`_ are used to bind runtime values to rule sets.
Additionally, the rules engine contains :ref:`"built-ins" <rules-engine-parameters-built-ins>`
that implementations are responsible for sourcing the value of and binding it
to any applicable parameter. The rules engine has a set of included built-ins
that can be invoked without additional dependencies.

If a parameter has multiple values that can be bound to it, the most specific
value must be selected and provided to the implementation. The following is the
order of the most specific to least specific value locations:

#. `smithy.rules#staticContextParams trait`_
#. `smithy.rules#contextParam trait`_
#. `smithy.rules#clientContextParams trait`_
#. Built-in bindings
#. Built-in binding default values


.. _rules-engine-parameters-traits:

Rules engine parameter traits
-----------------------------

Rule set parameters MAY be bound to values from various locations in a client's
request flow using traits.

The examples in the following trait definitions are valid bindings into the
following rule set:

.. code-block:: json

    {
        "version": "1.0",
        "serviceId": "example",
        "parameters": {
            "linkId": {
                "type": "string",
                "documentation": "The identifier of the link to target."
            },
            "previewEndpoint": {
                "type": "boolean",
                "documentation": "Whether the client should target the service's preview endpoint."
            }
        },
        "rules": [
            // Abbreviated for clarity
        ]
    }


.. smithy-trait:: smithy.rules#clientContextParams
.. _smithy.rules#clientContextParams-trait:

``smithy.rules#clientContextParams`` trait
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Summary
    Defines one or more rule set parameters that MUST be generated as
    configurable client configuration parameters
Trait selector
    ``service``
Value type
    ``map`` of ``string`` containing a rule set parameter name to a
    ``clientContextParam`` structure.

The ``clientContextParam`` structure has the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 23 67

    * - Property
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. The shape type used to generate the client
        configuration parameter. MUST be one of ``string`` or ``boolean``.
    * - documentation
      - ``string``
      - A description of the parameter that will be used to generate
        documentation for the client configurable.

Each parameter is identified using it’s name as specified in the rule set. It
is mapped to properties describing how the parameter should be configured on
the generated client. The type of a ``clientContextParam`` MUST be compatible
with the parameter type specified in the rule set. The client configuration
parameters SHOULD be configurable or overridable per operation invocation.

The following example specifies two parameters to be generated on clients as
configurable values:

.. code-block:: smithy

    @clientContextParams(
        linkId: {
            type: "string"
            documentation: "The identifier of the link to target."
        }
        previewEndpoint: {
            type: "boolean"
            documentation: "Whether the client should target the service's preview endpoint."
        }
    )
    service ExampleService {
        version: "2020-07-02"
        operations: [GetThing]
    }

.. smithy-trait:: smithy.rules#staticContextParams
.. _smithy.rules#staticContextParams-trait:

``smithy.rules#staticContextParams`` trait
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Summary
    Defines one or more rule set parameters that MUST be bound to the specified
    values.
Trait selector
    ``operation``
Value type
    ``map`` of ``string`` containing a rule set parameter name to a
    ``staticContextParam`` structure.

The ``staticContextParam`` structure has the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 23 67

    * - Property
      - Type
      - Description
    * - value
      - ``document``
      - **Required**. The static value to be set for the parameter. The type
        of the value MUST be either a ``string`` or ``boolean``.

Each parameter is identified using it’s name as specified in the rule set. The
type of a ``staticContextParam`` MUST be compatible with the parameter type
specified in the rule set.

The following example specifies two parameters to statically set for an
operation:

.. code-block:: smithy

    @staticContextParams(
        linkId: {
            value: "some value"
        }
        previewEndpoint: {
            value: true
        }
    )
    operation GetThing {}


.. smithy-trait:: smithy.rules#contextParam
.. _smithy.rules#contextParam-trait:

``smithy.rules#contextParam`` trait
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Summary
    Binds a top-level operation input structure member to a rule set parameter.
Trait selector
    ``structure > member``
Value type
    An ``object`` that supports the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 10 20 70

        * - Property
          - Type
          - Description
        * - name
          - ``string``
          - **Required**. The name of the context parameter to bind the
            member value to.


The following example specifies an operation with an input parameter ``buzz``
bound to the ``linkId`` rule set parameter:

.. code-block:: smithy

    operation GetThing {
        input := {
            fizz: String

            @contextParam(name: "linkId")
            buzz: String
        }
    }


.. important::

    If a member marked with the ``@contextParam`` trait is also marked as
    :ref:`required <required-trait>`, clients MUST NOT send requests if the
    parameter is unset, empty, or exclusively whitespace characters. This
    ensures that servers can reliably dispatch to operations based on these
    parameters.


.. _rules-engine-parameters-built-ins:

Rules engine built-ins
----------------------

:ref:`Rule set parameters <rules-engine-endpoint-rule-set-parameter>` MAY be
annotated with the ``builtIn`` property. When a parameter has this property,
the parameter’s value MUST be bound to the value retrieved from the identified
source, if present, UNLESS a more specific value supersedes it.

.. code-block:: json

    {
        "parameters": {
            "endpoint": {
                "type": "string",
                "builtIn": "SDK::Endpoint"
            }
        }
    }

The rules engine has a set of included built-ins that can be invoked without
additional dependencies, which are defined as follows:


.. _rules-engine-parameters-sdk-endpoint-built-in:

``SDK::Endpoint`` built-in
~~~~~~~~~~~~~~~~~~~~~~~~~~

Description
    A custom endpoint for a rule set.
Type
    ``string``


.. _rules-engine-parameters-sdk-adding-built-ins:

Adding built-ins through extensions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Extensions to the rules engine can provide additional built-ins. Code
generators MAY support these additional functions and SHOULD document which
extensions are supported. Additional built-ins MUST be namespaced, using
two colon ``:`` characters to separate namespace portions. This is utilized to
add the :ref:`AWS rules engine built-ins <rules-engine-aws-built-ins>`.

The rules engine is highly extensible through
``software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension``
`service providers`_. See the `Javadocs`_ for more information.

.. _Javadocs: https://smithy.io/javadoc/__smithy_version__/software/amazon/smithy/rulesengine/language/EndpointRuleSetExtension.html
.. _service providers: https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
