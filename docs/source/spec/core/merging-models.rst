.. _merging-models:

==============
Merging models
==============

Smithy models MAY be divided into multiple files so that they are easier to
maintain and evolve. Smithy tools MUST take the following steps to merge two
models together to form a composite model:

#. Assert that both models use a :ref:`version <smithy-version>` that is
   compatible with the tool versions specified.
#. Duplicate shape names, if found, MUST cause the model merge to fail.
#. Merge any conflicting :ref:`trait <traits>` definitions using
   :ref:`trait conflict resolution  <trait-conflict-resolution>`.
#. Merge the :ref:`metadata <metadata>` properties of both models using the
   :ref:`metadata merge rules <merging-metadata>`.
