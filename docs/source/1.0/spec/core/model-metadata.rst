.. _metadata:

==============
Model metadata
==============

:dfn:`Metadata` is a schema-less extensibility mechanism that can be applied
to a model using a :ref:`metadata statement <metadata-statement>`.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. _metadata-statement:

------------------
Metadata statement
------------------

The metadata statement is used to attach arbitrary :ref:`metadata <metadata>`
to a model. Metadata statements MUST appear before a namespace statement
or any shapes are defined.

.. productionlist:: smithy
    metadata_statement:"metadata" `metadata_key` "=" `metadata_value`
    metadata_key:`text`
    metadata_value:`node_value`

.. code-block:: smithy
    :caption: Example

    metadata example.string1 = "hello there"
    metadata example.string2 = 'hello there'
    metadata example.bool1 = true
    metadata example.bool2 = false
    metadata example.number = 10
    metadata example.array = [10, true, "hello"]
    metadata example.object = {foo: "baz"}
    metadata example.null = null


.. _merging-metadata:

Merging metadata
================

When a conflict occurs between top-level metadata key-value pairs,
metadata is merged using the following logic:

1. If a metadata key is only present in one model, then the entry is valid
   and added to the merged model.
2. If both models contain the same key and both values are arrays, then
   the entry is valid; the values of both arrays are concatenated into a
   single array and added to the merged model.
3. If both models contain the same key and both values are exactly equal,
   then the conflict is ignored and the value is added to the merged model.
4. If both models contain the same key and the values do not both map to
   arrays, then the key is invalid and there is a metadata conflict error.

Given the following two Smithy models:

.. code-block:: smithy
    :caption: model-a.smithy

    metadata "foo" = ["baz", "bar"]
    metadata "qux" = "test"
    metadata "validConflict" = "hi!"

.. code-block:: smithy
    :caption: model-b.smithy

    metadata "foo" = ["lorem", "ipsum"]
    metadata "lorem" = "ipsum"
    metadata "validConflict" = "hi!"

Merging ``model-a.smithy`` and ``model-b.smithy`` produces the following
model:

.. code-block:: smithy

    metadata "foo" = ["baz", "bar", "lorem", "ipsum"]
    metadata "qux" = "test"
    metadata "lorem" = "ipsum"
    metadata "validConflict" = "hi!"
