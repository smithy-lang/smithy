.. _metadata:

==============
Model metadata
==============

:dfn:`Metadata` is a schema-less extensibility mechanism that can be applied
to a model using a :ref:`metadata statement <metadata-statement>`. For
example, metadata is used to define :ref:`validators <validation>` and
:ref:`suppressions <suppression-definition>` that are applied to the entire
model.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. _metadata-statement:

------------------
Metadata statement
------------------

Metadata statements MUST appear before any namespace statement or any shapes
are defined. Metadata is defined by the following ABNF:

.. productionlist:: smithy
    metadata_section   :*(`metadata_statement`)
    metadata_statement :"metadata" `ws` `node_object_key` `ws` "=" `ws` `node_value` `br`

.. code-block:: smithy
    :caption: Example

    metadata exampleString = "hello there"
    metadata "example.string2" = 'hello there'
    metadata bool1 = true
    metadata bool2 = false
    metadata number = 10
    metadata array = [10, true, "hello"]
    metadata object = {foo: "baz"}
    metadata null = null


.. _merging-metadata:

----------------
Merging metadata
----------------

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
