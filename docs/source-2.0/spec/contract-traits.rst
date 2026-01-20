.. _contract-traits:

-----------------
Contract traits
-----------------

Contract traits are used to further constrain the valid values and behaviors of a model.
Like constraint traits, contract traits are for validation only and SHOULD NOT
impact the types signatures of generated code.


Contract trait enforcement
============================

Contract traits SHOULD NOT be directly enforced by default when serializing or deserializing.
These traits often express contracts using higher-level constructs and simpler but less efficient expressions.
Services will usually check these contracts outside of service frameworks in more efficient ways.

Contract traits are instead intended to be useful for generating tests
or applying static analysis to client or service code.

.. smithy-trait:: smithy.contracts#conditions
.. _conditions-trait:

``conditions`` trait
====================

Summary
    Restricts shape values to those which satisfy the given JMESPath expressions.
Trait selector
    ``:not(:test(service, operation, resource))``

    *Any shape other than services, operations, and resources*
Value type
    ``list``

The ``conditions`` trait is a list of ``Condition`` structures that contain
the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 23 67

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - The identifier of the conditions.
        The provided ``id`` MUST match Smithy's :token:`smithy:Identifier` ABNF.
        No two conditions on a single shape can share the same ID.
    * - expression
      - ``string``
      - JMESPath expression that must evaluate to true.
    * - description
      - ``string``
      - Description of the condition. Used in error messages when violated.

.. code-block:: smithy

    @conditions([
        {
            id: "StartBeforeEnd",
            description: "The start time must be strictly less than the end time",
            expression: "start < end"
        }
    ])
    structure FetchLogsInput {
        @required
        start: Timestamp

        @required
        end: Timestamp
    }

    @conditions([
        {
            id: "NoKeywords",
            expression: "!contains(@, 'id') && !contains(@, 'name')"
        }
    ])
    string Foo
