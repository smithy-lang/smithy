.. _rules-engine-specification:

==========================
Rules engine specification
==========================

The Smithy rules engine provides service owners with a collection of traits
and components to define rule sets. Rule sets can be consumed by code
generators to provide the rule set's specific functionality.

A rule set defines zero or more *parameters* and one or more *rules*.
Parameters define the primary set of values that a rule set operates on. Rules
are composed of a set of *conditions*, which determine if a rule should be
selected, and a result. Conditions act on the defined parameters, and allow for
the modeling of statements.

When a rule’s conditions are evaluated successfully, the rule provides either a
result and its accompanying requirements or an error describing the unsupported
state. Modeled endpoint errors allow for more explicit descriptions to users,
such as providing errors when a service doesn't support a combination of
conditions.


.. smithy-trait:: smithy.rules#endpointRuleSet
.. _smithy.rules#endpointRuleSet-trait:

``smithy.rules#endpointRuleSet`` trait
======================================

Summary
    Defines a rule set for deriving service endpoints at runtime.
Trait selector
    ``service``
Value type
    ``document``

The content of the ``endpointRuleSet`` document has the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - version
      - ``string``
      - **Required**. The rule set schema version. This specification covers
        version 1.0 of the endpoint rule set.
    * - serviceId
      - ``string``
      - **Required**. An identifier for the corresponding service.
    * - parameters
      - ``map<string, parameter object>`` of `Parameter object`_
      - **Required**. A map of zero or more endpoint parameter names to
        their parameter configuration.
    * - rules
      - An ``array`` of one or more of the following types: `Endpoint rule
        object`_, `Error rule object`_, or `Tree rule object`_
      - **Required**. One or more endpoint rule definitions of any rule type.

A rule set defines endpoint parameters using the ``parameters`` property, and
maps zero or more parameter names to :ref:`parameter object <rules-engine-endpoint-rule-set-parameter>`
values.

Finally a rule set defines the ``rules`` property, which MUST contain one or
more of the following rule types: :ref:`endpoint rules, <rules-engine-endpoint-rule-set-endpoint-rule>`
:ref:`error rules, <rules-engine-endpoint-rule-set-error-rule>` or :ref:`tree rules. <rules-engine-endpoint-rule-set-error-rule>`
Rules are evaluated in order from their lowest array index position to highest.
If the list of rules is exhausted with none evaluated successfully, then an
implementation of the rule set engine MUST stop and return an error indicating
that rule exhaustion has occurred. Rule authors SHOULD use either an :ref:`endpoint rule <rules-engine-endpoint-rule-set-endpoint-rule>`
or :ref:`error rules, <rules-engine-endpoint-rule-set-error-rule>` with an
empty set of conditions to provide a more meaningful default or error depending
on the scenario.

.. _rules-engine-endpoint-rule-set-parameter:

----------------
Parameter object
----------------

A parameter object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. MUST be one of ``string`` or ``boolean``.
    * - builtIn
      - ``string``
      - Specifies a named built-in value that is sourced and provided to the
        endpoint provider by a caller.
    * - default
      - ``string`` or ``boolean``
      - Specifies the default value for the parameter if not set. Parameters
        with defaults MUST also be marked as ``required``. The type of the
        provided default MUST match ``type``.
    * - required
      - ``boolean``
      - Specifies that the parameter is required to be provided to the endpoint
        provider.
    * - documentation
      - ``string``
      - **Required**. Specifies a string that SHOULD be used to generate API
        reference documentation for the endpoint parameter.
    * - deprecated
      - `Deprecated object`_
      - Specifies whether an endpoint parameter has been deprecated.

The parameter typing is statically analyzed by the rules engine to validate
correct usage within the rule set. :ref:`Rules engine parameter traits <rules-engine-parameters-traits>`
allow values to be bound to parameters from other locations in generated
clients.

Parameters MAY be annotated with the ``builtIn`` property, which designates that
the parameter should be bound to a value determined by the built-in’s name. The
:ref:`rules engine contains built-ins <rules-engine-parameters-built-ins>` and
the set is extensible.

The ``required`` property is used to validate that a parameter value MUST be set
prior to evaluating the rule set's rules. If a ``required`` parameter is not set,
rule evaluation MUST NOT proceed, and an implementation MUST return an error to
the user.

The ``default`` property is used to set a default value on a property if a value
is not present. A parameter with the ``default`` property, MUST also be marked with
the ``required`` trait.

The following is an example of a parameter that is marked required.

.. code-block:: json

    {
        "parameters": {
            "linkId": {
                "type": "string",
                "documentation": "The identifier of the link to target.",
                "required": true
            }
        }
    }

.. note::
    Why must ``required`` be set when ``default`` is present?

    The ``required`` property exists as sole the determiner for rules code
    generation of "is the parameter always going to be set?" The ``default``
    property exists as a signal of where we can get the value from if
    it's missing. By forcing both to be set, code generators can simplify their
    handling of ``required``.

.. _rules-engine-endpoint-rule-set-parameter-deprecated:

-----------------
Deprecated object
-----------------

A deprecated object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - message
      - ``string``
      - Specifies an optional message that can be used in documentation to
        provide recourse options to a user.
    * - since
      - ``string``
      - A date string that indicates when the parameter field was deprecated.

The following is an example of a parameter that is marked as deprecated.

.. code-block:: json

    {
        "parameters": {
            "linkId": {
                "type": "string",
                "deprecated": {
                    "message": "This feature has been deprecated, and requests are now directed towards a global endpoint.",
                    "since": "2020-07-02"
                },
            }
        }
    }

.. _rules-engine-endpoint-rule-set-endpoint-rule:

--------------------
Endpoint rule object
--------------------

An endpoint rule object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. MUST be ``endpoint``.
    * - conditions
      - An ``array`` of `Condition object`_
      - **Required**. Zero or more conditions used to determine whether the
        endpoint rule should be selected.
    * - endpoint
      - `Endpoint object`_
      - **Required**. The endpoint to return if this rule is selected.
    * - documentation
      - ``string``
      - A description of the rule.

An endpoint rule MUST contain zero or more ``conditions``. If all
:ref:`condition <rules-engine-endpoint-rule-set-condition>` clauses evaluate
successfully, the endpoint rule is selected. If a condition fails, evaluation
of the rule MUST be terminated and evaluation proceeds to any subsequent rules.

The following example defines an endpoint rule object that checks a condition
and uses a parameter value in a url template:

.. code-block:: json

    {
        "documentation": "An endpoint rule description",
        "type": "endpoint",
        "conditions": [
            {
                "fn": "isValidHostLabel",
                "argv": [
                    {
                        "ref": "linkId"
                    }
                ]
            }
        ],
        "endpoint": {
            "url": "https://{linkId}.service.com"
        }
    }

.. _rules-engine-endpoint-rule-set-endpoint:

---------------
Endpoint object
---------------

An endpoint object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - url
      - ``string`` or `Reference object`_ or `Function object`_
      - **Required**. The endpoint url. This MUST specify a scheme and hostname
        and MAY contain port and base path components. A ``string`` value MAY
        be a `Template string`_. Any value for this property MUST resolve to a
        ``string``.
    * - properties
      - ``map<string, object>``
      - A map containing zero or more key value property pairs. Endpoint
        properties MAY be arbitrarily deep and contain other maps and arrays.
    * - headers
      - ``map<string, array>`` where the ``array`` value is one or more of
        following types: ``string`` or `Reference object`_ or `Function
        object`_
      - A map of transport header names to their respective values. A ``string``
        value in an array MAY be a template string.

An :ref:`endpoint rule <rules-engine-endpoint-rule-set-endpoint-rule>` uses an
endpoint object to define an endpoint selected based on successful evaluation
of rule conditions to that point.

An endpoint MAY return a set of endpoint properties using the ``properties``
field. This can be used to provide a grab-bag set of metadata associated with
an endpoint that an endpoint resolver implementation MAY use. For example, the
``authSchemes`` property is used to specify the priority ordered list of
authentication schemes and their configuration supported by the endpoint.
Properties MAY contain arbitrary nested maps and arrays of strings and
booleans.

.. note::
    To prevent ambiguity, the endpoint properties map MUST NOT contain
    reference or function objects. Properties MAY contain :ref:`template string <rules-engine-endpoint-rule-set-template-string>`

.. _rules-engine-endpoint-rule-set-endpoint-authschemes:

Endpoint ``authSchemes`` list property
--------------------------------------

The ``authSchemes`` property of an endpoint is used to specify the priority
ordered list of authentication schemes and their configuration supported by the
endpoint. The property is a list of configuration objects that MUST contain at
least a ``name`` property and MAY contain additional properties. Each
configuration object MUST have a unique value for its ``name`` property within
the list of configuration objects within a given ``authSchemes`` property.

If an ``authSchemes`` property is present on an `Endpoint object`_, clients
MUST resolve an authentication scheme to use via the following process:

#. Iterate through configuration objects in the ``authSchemes`` property.
#. If the ``name`` property in a configuration object contains a supported
   authentication scheme, resolve this scheme.
#. If the ``name`` is unknown or unsupported, ignore it and continue iterating.
#. If the list has been fully iterated and no scheme has been resolved, clients
   MUST return an error.

.. _rules-engine-standard-library-adding-authscheme-validators:

Adding ``authSchemes`` configuration validators
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Extensions to the rules engine can provide additional validators for
``authSchemes`` configuration objects. No validators are provided by default.

The rules engine is highly extensible through
``software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension``
`service providers`_. See the `Javadocs`_ for more information.


.. _rules-engine-endpoint-rule-set-error-rule:

-----------------
Error rule object
-----------------

An error rule object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. MUST be ``error``.
    * - conditions
      - An ``array`` of `Condition object`_
      - **Required**. Zero or more conditions used to determine whether the
        endpoint rule should be selected.
    * - error
      - ``string`` or `Reference object`_ or `Function object`_
      - **Required**. A descriptive message describing the error for consumption
        by the caller. A ``string`` value MAY be a `Template string`_. Any
        value for this property MUST resolve to a ``string``.
    * - documentation
      - ``string``
      - A description of the rule.

An error rule MUST contain zero or more ``conditions``. If all
:ref:`condition <rules-engine-endpoint-rule-set-condition>` clauses evaluate
successfully or zero conditions are defined, then the error rule MUST be
selected. If a condition fails evaluation, the rule MUST be terminated and
evaluation proceeds to any subsequent rules.

The following example defines an error rule object that checks a condition:

.. code-block:: json

    {
        "documentation": "An error rule description.",
        "type": "error",
        "conditions": [
            {
                "fn": "not",
                "argv": [
                    {
                        "fn": "isValidHostLabel",
                        "argv": [
                            {
                                "ref": "linkId"
                            }
                        ]
                    }
                ]
            }
        ],
        "error": "{linkId} must be a valid HTTP host label."
    }


.. note::
    In production rule sets, rather than using ``not``, it's more common to see
    rules where the error rule has no conditions and is only matched after all
    other rules have failed to match.


.. _rules-engine-endpoint-rule-set-tree-rule:

----------------
Tree rule object
----------------

A tree rule object contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property name
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. MUST be ``endpoint``.
    * - conditions
      - An ``array`` of `Condition object`_
      - **Required**. Zero or more conditions used to determine whether the
        endpoint rule should be selected.
    * - rules
      - An ``array`` of one or more of the following types: `Endpoint rule
        object`_, `Error rule object`_, or `Tree rule object`_
      - **Required**. One or more endpoint rule definitions of any rule type.
    * - documentation
      - ``string``
      - A description of the rule.

A tree rule MUST contain one or more ``conditions``. If all
:ref:`condition <rules-engine-endpoint-rule-set-condition>` clauses evaluate
successfully, the tree rule is selected. If a condition fails, evaluation of
the rule MUST be terminated and evaluation proceeds to any subsequent rules.

A tree rule MUST have one or more subordinate rules specified using the ``rules``
property. A tree rule is equivalent to the following logical expression:

.. code-block::

    treeCondition1 && ... && treeConditionN && ( subRule1 || ... || subRuleN )

Tree rules are considered terminal branches of the rule set. If a tree rule’s
subordinate rules are exhausted with none evaluated successfully, then an
implementation of the rules engine MUST stop and return an error indicating
that rule exhaustion has occurred.

Rule authors SHOULD use either an `Endpoint rule object`_ or `Error rule object`_
with an empty set of conditions to provide a more meaningful default or error,
depending on the scenario.

The following example is an abbreviated example of a tree rule that consists
of a tree, endpoint, and error rule.

.. code-block:: json

    {
        "conditions": [
            {
                "fn": "isValidHostLabel",
                "argv": [
                    {
                        "ref": "linkId"
                    }
                ]
            }
        ],
        "type": "tree",
        "rules": [
            {
                "type": "tree",
                "conditions": [
                    // Abbreviated for clarity
                ],
                "rules": [
                    // Abbreviated for clarity
                ]
            },
            {
                "type": "endpoint",
                "conditions": [
                    // Abbreviated for clarity
                ],
                "endpoint": {
                    "url": "{linkId}.service.com"
                }
            },
            {
                "type": "error",
                "conditions": [
                    // Abbreviated for clarity
                ],
                "error": "An error message."
            }
        ]
    }


.. _rules-engine-endpoint-rule-set-condition:

----------------
Condition object
----------------

A condition is specified using an object containing the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 50 40

    * - Property
      - Type
      - Description
    * - fn
      - ``string``
      - **Required**. The name of the function to be executed.
    * - argv
      - An ``array`` of one or more of the following types: ``string``,
        ``bool``, ``array``, `Reference object`_, or `Function object`_
      - **Required**. The arguments for the function.
    * - assign
      - ``string``
      - The destination variable to assign the functions result to.

Conditions are defined within rule objects as requirements for continuing to
evaluate the rules within. Conditions are evaluated in-order by their
positional index in the array, starting from zero. Conditions represent the
logical expression ``condition1 && ... && conditionN`` where ``condition1``
through ``conditionN`` are each condition objects.

If a condition returns ``None`` or ``False``, the condition does not match. A
condition that does not match MUST immediately terminate the evaluation of the
rule. Processing starts at the next immediate rule to be evaluated. In order
for a rule's right-hand-side to be considered, all conditions MUST match.
Conditions MAY use references defined in previous conditions in the same rule.

A condition object MAY assign the output result of a function to a named
variable using the ``assign`` property. Variables created using ``assign``
follow the same rules as parameter name identifiers. Variables are scoped to
the rule they are defined in, with variables created in a tree rule being
visible to all subordinate rules and their children. For :ref:`endpoint rules <rules-engine-endpoint-rule-set-endpoint-rule>`
or :ref:`error rules <rules-engine-endpoint-rule-set-error-rule>`, the variable
will fall out of scope once evaluation of the rule has completed. A condition
may also reference a variable declared by a prior condition statement within
the same rule. Assigned variables are similar to :ref:`parameters <rules-engine-endpoint-rule-set-parameter>`
and can be referenced in string templates or passed as values to other
functions. Variable names MUST NOT overlap with existing parameter names or
variable declarations in scope.

The following example shows using the :ref:`stringEquals function <rules-engine-standard-library-stringEquals>`
to compare two arguments as a prerequisite to an endpoint object:

.. code-block:: json

    {
        "type": "endpoint",
        "conditions": [
            {
                "documentation": "Use the base endpoint if using the default link",
                "fn": "stringEquals",
                "argv": [
                    {
                        "ref": "linkId"
                    },
                    "default"
                ]
            }
        ],
        "endpoint": {
            "url": "https://service.com"
        }
    }

The following example shows the invocation of the :ref:`parseUrl <rules-engine-standard-library-parseURL>`
function, taking a single argument and assigning the result to the ``url``
variable. The ``url`` variable is then used as part of a template string in an
endpoint object.

.. code-block:: json

    {
        "type": "endpoint",
        "conditions": [
            {
                "fn": "parseURL",
                "argv": [
                    {
                        "ref": "Endpoint"
                    }
                ],
                "assign": "url"
            }
        ],
        "endpoint": {
            "url": "https://service.com/{url#path}"
        }
    }


.. _rules-engine-endpoint-rule-set-function:

---------------
Function object
---------------

A function is specified using an object containing the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 50 40

    * - Property
      - Type
      - Description
    * - fn
      - ``string``
      - **Required**. The name of the function to be executed.
    * - argv
      - An ``array`` of one or more of the following types: ``string``,
        ``bool``, ``array``, `Reference object`_, or `Function object`_
      - **Required**. The arguments for the function.

All functions have specific type signatures associated with their definition.
Static analysis validates that all arguments passed to a function are of the
correct type.

The :ref:`rules engine contains functions <rules-engine-standard-library>` and
the set is extensible.


.. _rules-engine-endpoint-rule-set-reference:

----------------
Reference object
----------------

A reference is an object containing the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 50 40

    * - Property
      - Type
      - Description
    * - ref
      - ``string``
      - **Required**. The name of the parameter or variable.

References allow for parameter and variable assignments, created by a condition
object, to be used in subsequent rules and conditions. A parameter MUST be
checked with an ``isSet`` condition object to determine whether the parameter
is set and can be safely used. The rules engine statically verifies references
are valid at time-of-use.


.. _rules-engine-endpoint-rule-set-template-string:

---------------
Template string
---------------

String values in rules can use the ``{parameterName}`` syntax for defining
automatic templating of the named parameter or variable values into a string.
For example, if the string ``{parameterName}.foobar.baz`` is defined in the
rule set and ``parameterName`` is bound to the value ``foo``, the resolved
string value would be ``foo.foobar.baz``.

Template string parameters follow the same de-referencing rules as outlined
in reference object. The rules engine statically verifies referenced
parameters in template strings.


.. _rules-engine-endpoint-rule-set-template-string-shorthand:

Template shorthand
------------------

Template parameters also support a shorthand syntax for accessing nested
properties or array arguments using the ``#`` character. This syntax is
syntactic sugar for using the long-form ``getAttr`` function. For example,
``{parameterName#foo}``, with ``parameterName`` being a variable containing a
JSON object, and ``foo`` being a property defined on that object.

The following two expressions are equivalent:

.. code-block:: json

    {
        "fn": "stringEquals",
        "argv": [
            "linkId",
            {
                "fn": "getAttr",
                "argv": [
                    {
                        "ref": "partResult"
                    },
                    [
                        "name"
                    ]
                ]
            }
        ]
    }



.. code-block:: json

    {
        "fn": "stringEquals",
        "argv": [
            "linkId",
            "{partResult#name}"
        ]
    }

.. _Javadocs: https://smithy.io/javadoc/__smithy_version__/software/amazon/smithy/rulesengine/language/EndpointRuleSetExtension.html
.. _service providers: https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
