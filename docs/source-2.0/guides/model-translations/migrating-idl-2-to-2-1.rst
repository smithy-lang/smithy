=====================================
Smithy IDL 2.0 to 2.1 Migration Guide
=====================================

The ``smithy migrate`` command migrates Smithy IDL model files to version 2.1
in place:

.. code-block:: sh

    smithy migrate model/

Version 1.0 models undergo the transformations described in the
:doc:`IDL 1.0 to 2.1 migration guide <migrating-idl-1-to-2>` and are updated to
version 2.1.
For models declaring ``$version: "2"`` or ``$version: "2.0"``, only the version
declaration changes to ``$version: "2.1"`` by default. The inline collection
options below apply after the version declaration is upgraded.

The deprecated ``smithy upgrade-1-to-2`` command delegates to
``smithy migrate`` and supports the same options.

When model paths are provided, only those files or directories are modified.
With no model paths, all loaded local IDL files are migration targets,
including files from the configured ``sources`` and ``imports``.

Inferring inline collections
============================

Use ``--infer-inline-collections`` to replace references to list and map
shapes with :ref:`inline collection declarations <idl-inline-collections>`:

.. code-block:: sh

    smithy migrate --infer-inline-collections model/

The collection shape and all its members must have no traits. For example,
``names: StringList`` becomes ``names: [String]`` when ``StringList`` is a
trait-free list with a trait-free member targeting ``String``. Similarly,
``tags: TagMap`` becomes ``tags: {String: String}`` when ``TagMap`` is a
trait-free map with trait-free key and value members targeting ``String``.

Use ``--force-inline-collections`` to also migrate collections with traits
that can be applied to the referencing member:

.. code-block:: sh

    smithy migrate --force-inline-collections model/

These traits are copied onto each converted reference. An existing trait on
the referencing member takes precedence over the same trait on the collection,
replacing its entire value; individual trait properties are not merged.
Collection members must still have no traits. Collections with traits that
cannot be applied at a reference site, such as ``sparse`` and ``uniqueItems``,
remain named at that site. The force option includes inference behavior.

Both options validate the proposed edits against the entire model. References
stay named where inlining would invalidate member traits, nested trait
selectors, resource property bindings, or mixin targets. Collections marked
``private`` or ``suppress`` remain named, as do references that would expose
inaccessible elements across namespaces. An unsafe reference does not prevent
other eligible references or version declarations from being migrated.
Use ``--logging FINE`` or ``--debug`` to see why references remain named.

By default, both options keep named collection declarations and ``use`` statements so
other models can continue referencing the original shapes. References inside
named list and map declarations are migrated using the same rules, so their
``member``, ``key``, or ``value`` targets can change while the declarations
themselves remain available. If copying traits onto such a member gives it
traits, its containing collection is no longer eligible for future inlining.
Eligible nested collections are inlined up to three levels deep. Named
references are retained where further inlining would exceed this limit or
lose traits.

Inline collections have synthetic shape IDs, so converted references change
their target shape identity. Downstream code generators and ``smithy diff``
may report or respond to these target changes even though the original
collection declarations are retained.

Migration preserves surrounding source text. Copied traits use the model
serializer's IDL style, which can introduce multiple lines into a single-line
declaration. Run ``smithy format`` afterward to apply consistent formatting.

Removing orphaned collection declarations
=========================================

Add ``--remove-orphaned-shapes`` to either inline collection option to remove
named list and map declarations made unused by the migration:

.. code-block:: sh

    smithy migrate --infer-inline-collections --remove-orphaned-shapes model/

Only collections expanded by accepted migration edits are removal candidates.
Shapes that were already unused, and declarations outside the selected files,
are retained. References from any loaded model file, including shape IDs in
traits marked ``idRef``, prevent removal. Declarations are also retained if
removing them would invalidate the model. Nested collections can be removed
after the declarations that referenced them are removed.

Matching ``use`` and ``apply`` statements are removed from selected files along
with the declaration. Documentation on surviving declarations is preserved.
Use ``--logging FINE`` or ``--debug`` to see which collections are removed or
retained.

Models that are not loaded during migration may still reference removed shapes.
Include downstream models when checking references, or keep the original
declarations by omitting ``--remove-orphaned-shapes``.
