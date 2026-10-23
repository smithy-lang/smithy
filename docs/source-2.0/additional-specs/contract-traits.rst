.. _contract-traits:

===============
Contract traits
===============

Contract traits are used to further constrain the valid values and behaviors of a model.
Like constraint traits, contract traits are for validation only and SHOULD NOT
impact the types signatures of generated code.

--------------------------
Contract trait enforcement
--------------------------

Contract traits provide structured documentation of implicit API constraints,
and are useful for generating tests or applying static analysis to client or service code.

Contract traits SHOULD NOT be directly enforced by default when serializing or deserializing.
These traits often express contracts using higher-level constructs and simpler but less efficient expressions.
Services will usually check these contracts outside of service frameworks in more efficient ways.

.. smithy-trait:: smithy.contracts#conditions
.. _conditions-trait:

--------------------
``conditions`` trait
--------------------

Summary
    Restricts shape values to those which satisfy the given JMESPath expressions.
Trait selector
    ``:not(:test(service, operation, resource))``

    *Any shape other than services, operations, and resources*
Value type
    ``map``

The ``conditions`` trait is a map from condition names to ``Condition`` structures that contain
the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 23 67

    * - Property
      - Type
      - Description
    * - expression
      - ``string``
      - **Required**. JMESPath_ expression that must evaluate to true.
    * - documentation
      - ``string``
      - **Required**. Documentation about the condition defined using CommonMark_.

See the :ref:`JMESPath data model <jmespath-data-model>` for details on how Smithy types are mapped to JMESPath types.

.. code-block:: smithy

    @conditions({
        StartBeforeEnd: {
            documentation: "The start time must be strictly less than the end time",
            expression: "start < end"
        }
    })
    structure FetchLogsInput {
        @required
        start: Timestamp

        @required
        end: Timestamp
    }

    @conditions({
        NoKeywords: {
            documentation: "The name cannot contain either 'id' or 'name', as these are reserved keywords"
            expression: "!contains(@, 'id') && !contains(@, 'name')"
        }
    })
    string Name


.. smithy-trait:: smithy.contracts#contracts
.. _contracts-trait:

-------------------
``contracts`` trait
-------------------

Summary
    Constrains the calls of an operation to those which satisfy the given JMESPath
    expressions. Each expression must hold for every call.
Trait selector
    ``operation``
Value type
    ``map``

Where ``conditions`` restricts the *values* of a data shape, ``contracts``
constrains the *calls* of an operation. An operation is a procedure and has no
value; a contract is a predicate that must hold for every call, evaluated over
the operation's :ref:`call <jmespath-operation-call>`, the
``{input, output, error, before, after}`` object. The value type is the same
map of named ``Condition`` structures as ``conditions``.

Preconditions and postconditions are ordinary entries gated on the call outcome
(``output`` and ``error``). A call is materialized from an :ref:`examples-trait` value at build
time, with ``before`` and ``after`` supplied by the example's ``before`` and
``after`` members.

The following operation requires the document it publishes to be approved before
the call. The precondition reads the referenced document's state from the
``before`` snapshot using the ``resource`` and ``requires`` functions defined
below.

.. code-block:: smithy

    @contracts({
        DocumentApprovedOnSuccess: {
            documentation: "A successful publish requires the referenced document to be approved beforehand"
            expression: "requires(@, resource(before, input.document).status == 'APPROVED')"
        }
    })
    resource Document {
        identifiers: {documentId: String}
        properties: {status: String}
    }

    operation PublishDocument {
        input: PublishDocumentInput
        output: PublishDocumentOutput
    }

    @input
    @references([
        {resource: Document, name: "document", ids: {documentId: "documentId"}}
    ])
    structure PublishDocumentInput {
        @required
        documentId: String
    }

    @output
    structure PublishDocumentOutput {}

------------------
Contract functions
------------------

Contract expressions use the functions defined in the
:ref:`JMESPath data model <jmespath-functions>`: ``requires`` for necessary
preconditions and ``resource`` for resolving a resource against a ``before`` or
``after`` snapshot.

A named :ref:`references-trait` on the input structure projects a resource
handle reachable as ``input.<name>`` (``input.document`` above), so a
precondition over resource state can be written without constructing the handle
by hand.


.. _CommonMark: https://spec.commonmark.org/
.. _JMESPath: https://jmespath.org/