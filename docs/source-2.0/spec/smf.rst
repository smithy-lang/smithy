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
trait merging, mixin application, or forward reference resolution. It can
construct complete shapes directly from the binary data.

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
        bits 2-7 reserved (MUST be 0)

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
    DynamicValue[]  values (valueCount encoded values)

In shapes, each trait is encoded as ``SymRef traitId`` + ``VarUInt valueRef``
(index into this table) instead of an inline ``DynamicValue``.


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
    VarUInt   traitCount
    Trait[]   traits
    [payload] type-specific payload


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

Tags 0x80-0xFF are reserved as length-prefixed extension tags for future
forward-compatible additions.


.. _smf-integrity:

------------------
Integrity checking
------------------

Every SMF file ends with a 4-byte CRC-32C (Castagnoli) checksum in
little-endian byte order, computed over all preceding bytes. A reader
MUST verify the checksum before processing data and reject the file if
it does not match.


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
