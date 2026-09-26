.. _smf:

===========================
Smithy Model Format (SMF)
===========================

.. warning::

    This feature is a developer preview and is subject to change. The binary
    format, APIs, and behavior may change in future releases without notice.

The Smithy Model Format (SMF) is a compact binary serialization of Smithy
models optimized for fast loading. It is a binary representation of the
:ref:`JSON AST <json-ast>` that eliminates parsing overhead, reduces
allocations, and enables selective loading of model subsets.

--------
Overview
--------

SMF is a binary encoding of a **fully resolved** Smithy model that:

* Represents the model after all apply statements have been merged and all
  mixin members have been flattened into their target shapes.
* Replaces all repeated strings (shape IDs, trait names, member names) with
  integer references into a symbol table.
* Eliminates structural keys entirely; the format defines positional layouts
  for each shape type.
* Encodes shape types as a single-byte enum instead of a string.
* Preserves member ordering (members are written and read sequentially).
* Length-prefixes each shape to enable skip-scanning.
* Includes a shape index for random-access selective loading.
* Uses a minimal self-describing encoding for dynamic trait values.

Because the model is fully resolved, the reader does not need to perform
trait merging or mixin application. It does not resolve forward references
either: every reference is stored as an absolute shape ID via the symbol
table rather than a lexically-scoped name, so there are no unresolved names to
fix up. It can construct complete shapes directly from the binary data.

.. _smf-file-extension:

File extension: ``.smf``

Media type: ``application/vnd.smithy.smf``


.. _smf-file-structure:

--------------
File structure
--------------

.. code-block:: none

    ┌────────────────────────────────┐
    │ Header (8 bytes)               │
    ├────────────────────────────────┤
    │ Symbol Table                   │
    ├────────────────────────────────┤
    │ Trait Value Table              │
    ├────────────────────────────────┤
    │ Cold Trait Value Table         │  (only if split-cold-traits)
    ├────────────────────────────────┤
    │ Shape Index (optional)         │
    ├────────────────────────────────┤
    │ Metadata Section               │
    ├────────────────────────────────┤
    │ Shapes Section                 │
    ├────────────────────────────────┤
    │ CRC-32C (4 bytes)              │
    └────────────────────────────────┘


.. _smf-notation:

--------
Notation
--------

* All multi-byte integers are little-endian unless stated otherwise.
* ``VarUInt`` is an unsigned 32-bit integer encoded as LEB128 (1-5 bytes).
* ``VarInt`` is a signed 64-bit integer encoded as zigzag + LEB128 (1-10 bytes).
* ``String`` is ``VarUInt byteLength`` followed by that many bytes of UTF-8.
* ``SymRef`` is a ``VarUInt`` symbol table index.
* Byte offsets are zero-indexed from the start of the relevant section.


.. _smf-header:

------
Header
------

The header is exactly 8 bytes:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Offset
      - Size
      - Field
    * - 0
      - 4
      - Magic number: ``0x53 0x4D 0x46 0x00`` (ASCII "SMF\\0")
    * - 4
      - 1
      - Format version (MUST be ``0x01`` for this specification)
    * - 5
      - 1
      - Smithy version major (e.g., 2 for Smithy 2.x)
    * - 6
      - 1
      - Smithy version minor (e.g., 0 for Smithy 2.0)
    * - 7
      - 1
      - Flags (bitfield): bit 0 = has-metadata, bit 1 = has-shape-index,
        bit 2 = has-trait-value-offsets, bit 3 = split-cold-traits,
        bits 4-7 reserved (MUST be 0)

A reader MUST reject files where the magic number does not match or the
format version is not supported.


.. _smf-symbol-table:

------------
Symbol table
------------

The symbol table maps integer IDs to strings:

.. code-block:: none

    VarUInt   sharedTableId (0 = none, 1 = smithy-core)
    VarUInt   sharedTableVersion (present only if sharedTableId != 0)
    VarUInt   localSymbolCount
    String[]  localSymbols

Symbol IDs are assigned as: ID 0 is reserved, IDs 1 through S correspond
to shared table entries, IDs S+1 through S+L correspond to local symbols.

Writers SHOULD assign local symbol IDs in descending frequency order to
minimize VarUInt encoding size.


.. _smf-trait-value-table:

-----------------
Trait value table
-----------------

Deduplicates trait values that appear multiple times across shapes. Each
unique value is stored once and referenced by index.

.. code-block:: none

    VarUInt         valueCount
    VarUInt         dataByteLength
    uint32LE[]      valueOffsets (valueCount entries, relative to the start of
                    the value data block)
    byte[]          valueData (concatenated DynamicValue encodings)

The offset table is mandatory (the ``has-trait-value-offsets`` flag is always
set) and lets a reader, especially a selective reader, jump directly to a
referenced value without scanning the whole value blob.

In shapes, each trait is encoded as ``SymRef traitId`` + ``VarUInt valueRef``
(index into this table) instead of an inline ``DynamicValue``.

When ``split-cold-traits`` is set, the file contains two trait value tables in
order — hot then cold — each with the layout above. Each trait application
references the appropriate table based on whether it is encoded in the hot or
cold trait list of its shape, and a selective reader may skip the cold table
entirely.


.. _smf-shape-index:

-----------
Shape index
-----------

Present only if the ``has-shape-index`` flag is set. Uses fixed-size entries
sorted by symbol reference to enable O(log N) binary search for selective
loading.

.. code-block:: none

    VarUInt       entryCount
    VarUInt       totalNeighborCount
    byte[]        fixedEntryTable (entryCount × 15 bytes, sorted by symref)
    uint32LE[]    neighborArray (totalNeighborCount × 4 bytes)

Each fixed-size entry (15 bytes):

.. code-block:: none

    Offset  Size  Field
    0       4     symref (uint32 LE)
    4       1     shapeType
    5       4     byteOffset (uint32 LE, from first shape's first byte)
    9       4     neighborStart (uint32 LE, index into neighbor array)
    13      2     neighborCount (uint16 LE)

The flat neighbor array stores all neighbor symrefs as packed uint32 LE
values. The ``shapeType`` byte enables the selective loading algorithm to
classify shapes during closure computation without parsing shape bodies.

The ``neighborCount`` field is a uint16, so a single shape MUST NOT have more
than 65,535 direct structural neighbors. This fixed 2-byte width keeps index
entries at 15 bytes for a compact, binary-searchable table. No known model
approaches this limit. A conforming writer MUST fail rather than emit a shape
whose neighbor count would overflow this field; raising the limit requires a
new format version.


.. _smf-shapes-section:

--------------
Shapes section
--------------

.. code-block:: none

    VarUInt   shapeCount
    Shape[]   shapes

Each Shape:

.. code-block:: none

    SymRef    shapeId
    VarUInt   byteLength (bytes that follow for this shape)
    byte      shapeType
    TraitSet  traits
    [payload] type-specific payload

``TraitSet`` is a single trait list when ``split-cold-traits`` is clear:

.. code-block:: none

    VarUInt   traitCount
    Trait[]   traits          (each: SymRef traitId + VarUInt traitValueRef)

and two lists — hot then cold — when ``split-cold-traits`` is set:

.. code-block:: none

    VarUInt   hotTraitCount
    Trait[]   hotTraits
    VarUInt   coldTraitCount
    Trait[]   coldTraits

Members carry their own ``TraitSet`` in the same form. A selective reader may
skip the cold traits.


.. _smf-dynamic-values:

-----------------------
Dynamic value encoding
-----------------------

Dynamic values encode trait values and metadata:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Tag
      - Type
      - Payload
    * - 0x00
      - null
      - (none)
    * - 0x01
      - false
      - (none)
    * - 0x02
      - true
      - (none)
    * - 0x03
      - integer
      - VarInt value
    * - 0x04
      - double
      - 8 bytes IEEE 754, little-endian
    * - 0x05
      - string
      - String (VarUInt length + UTF-8 bytes)
    * - 0x06
      - list
      - VarUInt count, then count DynamicValues
    * - 0x07
      - object
      - VarUInt count, then count (String key, DynamicValue value) pairs
    * - 0x08
      - empty object
      - (none), equivalent to ``{}``
    * - 0x09
      - big integer
      - String (base-10 representation)
    * - 0x0A
      - big decimal
      - String (decimal number)
    * - 0x0B
      - object (symref keys)
      - VarUInt count, then count (SymRef key, DynamicValue value) pairs
    * - 0x0C
      - string (symref)
      - SymRef into the symbol table

Tags 0x0B and 0x0C let a writer reference already-interned object keys and
string values by symbol instead of repeating their bytes. They are optional:
0x07/0x05 remain valid inline forms and a reader MUST support all four. Cold
trait values (when ``split-cold-traits`` is set) always use the inline forms
and are not interned. See :ref:`smf-writer-profiles`.

The empty object ``{}`` is always encoded with the single-byte ``0x08`` tag
rather than ``0x07``/``0x0B`` with a zero count; a reader treats ``0x08`` as an
object with no members.

Tags 0x0D through 0xFF are reserved for future versions of this specification.

.. _smf-writer-profiles:

Writer profiles
---------------

A writer chooses an encoding profile per file based on the intended consumer:

- **Full-model / distribution** (``split-cold-traits`` clear): smallest
  full-model size; all eligible keys/strings are symbol-referenced; one trait
  value table.
- **Dynamic-client** (``split-cold-traits`` set): fastest selective loading;
  only execution-hot traits are hot and interned, everything else is cold and
  inline. A selective reader decodes only hot traits of the requested closure.

Which traits are hot is a policy owned by the consumer runtime. For the
dynamic-client profile, classify as cold every trait not consulted at
execution time (documentation, examples, endpoint tests, and typically
service-config/introspection traits). An allowlist of execution-hot traits is
more robust than a cold denylist because new traits default to cold. Because a
selective reader silently omits cold traits, the hot set is a
correctness-sensitive contract.


.. _smf-integrity:

------------------
Integrity checking
------------------

Every SMF file ends with a 4-byte CRC-32C (Castagnoli) checksum in
little-endian byte order, computed over all preceding bytes. When
verification is enabled, a reader MUST reject the file if the checksum
does not match.

CRC verification is intended primarily as an *install-time* integrity gate
rather than a per-load cost. The recommended lifecycle is:

- **Install/import time (once):** when a model is first added to a local
  store, the reader SHOULD verify the CRC-32C. Integrity matters here because
  the bytes come from an external source, and the one-time cost is irrelevant.
- **Load time (every use):** once a file is verified and trusted on local
  disk, repeated loads SHOULD skip verification and parse directly. This is
  where the loading-speed advantage is realized.

This split matters because CRC cost is asymmetric: a hardware CRC32C intrinsic
(JVM, native readers) makes full-file verification nearly free, but a pure
interpreted reader with a software CRC loop can spend one to two orders of
magnitude more on verification than on the selective load it protects.
Readers control verification per call, so no format change is needed. Skipping
verification at load time is safe only when the file was verified at install
time and the local store is trusted, which MUST be a deliberate choice.


.. _smf-selective-loading:

-----------------
Selective loading
-----------------

The shape index enables loading only the shapes needed for a specific
use case (e.g., a dynamic client for a subset of operations):

1. Read the header and symbol table.
2. Read the shape index.
3. Determine the initial set of needed shape IDs.
4. Compute the transitive closure by following neighbor lists.
5. Collect byte offsets of all shapes in the closure.
6. Scan the shapes section, parsing only shapes whose offsets are in the
   closure, skipping all others via ``byteLength``.

For service-level selective loading (service + specific operations), the
closure algorithm skips operation and resource neighbors of the service
shape, following only error structure neighbors. The shape type byte in
the index enables this classification without parsing shape bodies.


.. _smf-prelude:

----------------
Prelude handling
----------------

SMF files MUST NOT include prelude shapes. The reader has the prelude
built-in for the Smithy version declared in the header. New prelude traits
(and potentially shapes) may be added to Smithy without a minor version
bump; an SMF file referencing an unknown prelude definition will fail to
load, the same behavior as JSON AST and IDL files.


.. _smf-equivalence:

------------------------------
Equivalence with JSON AST
------------------------------

An SMF file is semantically equivalent to the JSON AST produced by
serializing an assembled model if they have the same Smithy version,
metadata entries, and shapes (same types, traits, members in the same
order, and same targets).

SMF does not preserve apply statements (traits are merged). SMF does
preserve mixin relationships and the ``@mixin`` trait.

Because apply statements are merged away, an SMF file cannot be consumed as a
composable build dependency the way the JSON AST can (where one package's
apply statement may target a shape in another package). SMF is intended for
fully-resolved consumers such as dynamic clients and codegen. A team that
needs dependency composition can distribute a sibling model (JSON AST or IDL)
carrying the apply statements alongside the ``.smf`` file.
