# Smithy Model Format (SMF)

* **Authors**: Manuel Sugawara
* **Created**: 2026-05-08

## Abstract

This proposal defines a compact binary serialization format for fully resolved
Smithy models optimized for fast loading. The format avoids generic JSON
parsing overhead, reduces repeated string processing, and enables selective
loading of model subsets.

## Motivation

Smithy models are currently distributed and loaded using the JSON AST format.
While JSON is human-readable and widely supported, it has significant
performance costs for model loading:

1. **Generic parsing overhead**: A JSON tokenizer must handle whitespace,
   escape sequences, quoted strings, and structural characters before the
   model loader can interpret anything structurally meaningful.
2. **Repeated string processing**: Shape IDs, trait IDs, member names, and
   structural keys recur extensively across large models and model aggregates.
   Each occurrence must be decoded, allocated, and compared.
3. **No selective loading**: A dynamic client that needs a small subset of a
   large model still needs to parse the entire JSON document before it can
   construct the relevant shapes.
4. **Repeated identifier parsing**: Shape IDs are repeatedly reinterpreted
   from strings even though the same identifiers appear many times in a file.

A format that interns strings into a symbol table and encodes the known model
structure positionally can remove much of this work while preserving the same
assembled-model semantics.

## Goals

1. Define a binary format that any language can implement a reader/writer for.
2. Optimize for loading speed: minimize parsing, allocations, and string
   comparisons during model construction.
3. Support full-model loading (codegen, validation) and selective loading
   (dynamic clients needing a subset of operations).
4. Maintain full fidelity with a fully resolved model: any assembled model
   is representable in SMF, and loading an SMF file produces an equivalent
   model without requiring trait merging or mixin application.
5. Keep the format simple enough that a complete reader is compact and
   straightforward to implement in any language, without heavyweight parsing
   machinery.

## Non-goals

1. Human readability. A companion tool can convert SMF to/from JSON AST.
2. Minimal serialized size. SMF is a local, load-optimized representation, not
   a wire protocol; it optimizes for loading speed rather than the smallest
   possible file. General-purpose compression (gzip, zstd) can be applied
   externally for distribution, and measurements show gzipped SMF is not
   smaller than a gzipped compact model such as C2J (see *Alternatives and
   trade-offs*). The value of SMF is load speed, not size.
3. A composable build dependency. Because SMF is fully resolved, apply
   statements are merged away and cannot be re-targeted at shapes in another
   package. SMF is therefore not a substitute for the JSON AST as a package
   dependency that other packages apply traits onto. A team that needs both
   can distribute a sibling model (JSON AST or IDL) carrying the apply
   statements alongside the `.smf` file. See *Alternatives and trade-offs*.
4. Streaming writes without buffering. The writer needs the full model to
   build the symbol table before writing.
5. Random-access mutation. SMF files are written once and read many times.

## Proposal

### Overview

SMF is a binary encoding of a **fully resolved** Smithy model that:

- Represents the model after all apply statements have been merged and all
  mixin members have been flattened into their target shapes. Apply statements
  are not preserved. Mixin relationships are preserved as an explicit list of
  mixin shape IDs on each shape, but members introduced by mixins appear
  directly in the member list. Readers do not need to apply mixins to recover
  members or traits from the binary payload.
- Replaces all repeated strings (shape IDs, trait names, member names,
  structural keys) with integer references into a symbol table.
- Eliminates structural keys entirely, the format defines positional layouts
  for each shape type, so keys like `"type"`, `"traits"`, `"members"`, and
  `"target"` are never encoded.
- Encodes shape types as a single-byte enum instead of a string.
- Preserves member ordering (members are written and read sequentially).
- Length-prefixes each shape to enable skip-scanning.
- Includes a shape index for random-access selective loading.
- Uses a minimal self-describing encoding for dynamic trait values.

Because the model is fully resolved, the reader does not need to perform
trait merging or mixin flattening. Nor does it resolve forward references:
every reference is stored as an absolute shape ID (via the symbol table)
rather than a lexically-scoped name, so there are no unresolved names to
fix up — the reader maps a `SymRef` to a shape ID directly. It can construct
shape contents directly from the binary data. A full-model reader may still
perform a second pass to reconnect preserved mixin relationships after all
referenced shapes are available.

## Specification

### Notation

- All multi-byte integers are little-endian unless stated otherwise.
- `VarUInt` is an unsigned 32-bit integer encoded as LEB128 (7 payload bits
  per byte, most significant bit is the continuation flag: 1 = more bytes
  follow, 0 = final byte). The maximum encoded value is 2^32 - 1 (at most
  5 bytes). Encodings longer than 5 bytes are invalid. This type is used for
  all structural fields: counts, lengths, and symbol references.
- `VarInt` is a signed 64-bit integer encoded as zigzag + LEB128:
  encode as `(n << 1) ^ (n >> 63)`, then write as unsigned LEB128. The range
  is -2^63 to 2^63 - 1 (at most 10 bytes). This type is used exclusively
  within DynamicValue encodings for integer trait values.
- `String` is `VarUInt byteLength` followed by that many bytes of UTF-8.
- `SymRef` is a `VarUInt` symbol table index.
- Byte offsets are zero-indexed from the start of the relevant section.

### File Structure

```
┌────────────────────────────────┐
│ Header (8 bytes)               │
├────────────────────────────────┤
│ Symbol Table                   │
├────────────────────────────────┤
│ Trait Value Table              │
├────────────────────────────────┤
│ Cold Trait Value Table         │  (optional)
├────────────────────────────────┤
│ Shape Index (optional)         │
├────────────────────────────────┤
│ Metadata Section               │
├────────────────────────────────┤
│ Shapes Section                 │
├────────────────────────────────┤
│ CRC-32C (4 bytes, LE)          │
└────────────────────────────────┘
```

### Header

The header is exactly 8 bytes:

```
Offset  Size  Field
0       4     Magic number: 0x53 0x4D 0x46 0x00 (ASCII "SMF\0")
4       1     Format version (MUST be 0x01 for this specification)
5       1     Smithy version major (e.g., 2 for Smithy 2.x)
6       1     Smithy version minor (e.g., 0 for Smithy 2.0, 1 for Smithy 2.1)
7       1     Flags (bitfield):
                bit 0: has-metadata (1 if metadata section is non-empty)
                bit 1: has-shape-index (1 if shape index section is present)
                bit 2: has-trait-value-offsets
                bit 3: split-cold-traits
                bits 4-7: reserved, MUST be 0
```

A reader MUST reject files where the magic number does not match. A reader
MUST reject files where the format version is not supported. A reader SHOULD
ignore reserved bits and bytes. For format version `0x01`, the
`has-trait-value-offsets` flag MUST be set. The current writer emits Smithy
version `2.0`, and the current reader does not enforce the Smithy version
fields beyond decoding the file.

### Symbol Table

The symbol table maps integer IDs to strings. It consists of a reference to
an optional shared (pre-defined) symbol table followed by locally-defined
symbols.

```
VarUInt   sharedTableId
            0 = no shared table
            1 = smithy-core shared table (see below)
            Other values reserved for future shared tables.
VarUInt   sharedTableVersion (present only if sharedTableId != 0)
VarUInt   localSymbolCount
String[]  localSymbols (localSymbolCount length-prefixed UTF-8 strings)
```

Symbol IDs are assigned as follows:

- ID 0 is reserved and MUST NOT appear as a SymRef in the file. It indicates
  absence/null where contextually appropriate.
- IDs 1 through S (where S is the size of the shared table version selected by
  the file) correspond to entries in the shared symbol table.
- IDs S+1 through S+L (where L is localSymbolCount) correspond to the local
  symbols in order.

The **smithy-core shared table** (sharedTableId=1) is a well-known table
defined by this specification. Version 1 contains prelude shape IDs compiled
into the reader and writer. Only version 1 is currently defined.

The initial contents of the smithy-core shared table (version 1) are:

```
ID  String
1   smithy.api#Unit
2   smithy.api#Boolean
3   smithy.api#Byte
4   smithy.api#Short
5   smithy.api#Integer
6   smithy.api#Long
7   smithy.api#Float
8   smithy.api#Double
9   smithy.api#BigDecimal
10  smithy.api#BigInteger
11  smithy.api#String
12  smithy.api#Blob
13  smithy.api#Timestamp
14  smithy.api#Document
15  smithy.api#PrimitiveBoolean
16  smithy.api#PrimitiveByte
17  smithy.api#PrimitiveShort
18  smithy.api#PrimitiveInteger
19  smithy.api#PrimitiveLong
20  smithy.api#PrimitiveFloat
21  smithy.api#PrimitiveDouble
```

If future versions of the shared table add entries, they MUST do so by
appending new symbols, and readers will need to map each
`sharedTableVersion` to the corresponding prefix length when interpreting
local symbol IDs.

Implementations MUST include the shared table contents compiled into the
reader and writer. The shared table is never transmitted in the file.

Writers SHOULD assign local symbol IDs in descending frequency order (most
frequently referenced strings get the lowest IDs) to minimize VarUInt
encoding size.

### Trait Value Table

Trait values are deduplicated into one or two trait value tables and
referenced by index from shape trait encodings.

```
VarUInt     valueCount
VarUInt     dataByteLength
uint32LE[]  valueOffsets (valueCount entries, offsets relative to the first
             byte of the encoded value data block)
byte[]      valueData (concatenated DynamicValue encodings)
```

For format version `0x01`, the offset table is mandatory and the
`has-trait-value-offsets` flag is always set. The offset table allows readers,
especially selective readers, to jump directly to a referenced value without
scanning the entire value blob.

When `split-cold-traits` is clear, the file contains a single trait value
table.

When `split-cold-traits` is set, the file contains two trait value tables in
order:

1. the hot trait value table
2. the cold trait value table

Each trait application references the appropriate table based on whether it is
encoded in the hot or cold trait list for that shape.

The encoding of an individual trait reference is:

```
SymRef    traitId
VarUInt   traitValueRef (index into the trait value table)
```

This replaces inline `DynamicValue` encoding with a compact integer reference.

### Shape Index

The shape index maps shape IDs to byte offsets within the shapes section and
includes a neighbor list (dependency graph) for each shape. This enables
computing the transitive closure of any shape without parsing shape bodies.
Present only if the `has-shape-index` flag is set in the header. Full-model
reads can ignore this section; selective loading requires it. Current writers
always emit it.

The index uses fixed-size entries sorted by symbol reference to enable
O(log N) binary search for selective loading without scanning all entries.

```
VarUInt       entryCount
VarUInt       totalNeighborCount
byte[]        fixedEntryTable (entryCount × 15 bytes, sorted by symref)
uint32LE[]    neighborArray (totalNeighborCount × 4 bytes)
```

Each fixed-size entry (15 bytes):

```
Offset  Size  Field
0       4     symref (uint32 LE, symbol table index of the shape ID)
4       1     shapeType (same enum as Shape Type Enum)
5       4     byteOffset (uint32 LE, offset from first shape's first byte
                          in the shapes section)
9       4     neighborStart (uint32 LE, index into the flat neighbor array)
13      2     neighborCount (uint16 LE, number of neighbors)
```

The flat neighbor array stores all neighbor symrefs as packed uint32 LE
values. Each entry's neighbors are at
`neighborArray[neighborStart .. neighborStart + neighborCount]`.

Entries are sorted by `symref` in ascending order to enable binary search.
The `shapeType` byte enables the selective loading algorithm to classify
shapes (service, operation, structure, etc.) during closure computation
without parsing shape bodies.

The `neighborCount` field is a uint16, so a single shape MUST NOT have more
than 65,535 direct structural neighbors. This fixed 2-byte width keeps index
entries at 15 bytes for a compact, binary-searchable table and enables a fast
fixed-stride scan. No known model approaches this limit. A conforming writer
MUST fail rather than emit a shape whose neighbor count would overflow this
field; raising the limit requires a new format version.

The neighbor list contains all shape IDs that this shape directly references:
member targets, operation input/output/errors, resource lifecycle operations,
service operations/resources/errors, and identifiers/properties targets. It
does NOT include prelude shapes (which are assumed available) or shapes
referenced only within trait values.

**Selective loading algorithm:**

1. Read the symbol table, trait value table setup, and shape index.
2. Use the sorted fixed-size index entries directly (for example by binary
   search) or build a lookup from shape ID to index entry.
3. Starting from the desired root shapes (e.g., a service + operations),
   compute the transitive closure by following neighbor lists, this is pure
   integer set operations with no shape parsing.
4. Collect the byte offsets of all shapes in the closure.
5. Sort offsets and load only the selected shapes in ascending offset order.
   A reader may either skip-scan through the shapes section using
   `byteLength`, or jump directly to the selected offsets.
6. If trait applications on the loaded shapes reference non-prelude trait
   definition shapes, load those trait definition shapes in a follow-up pass.

This approach keeps closure computation in the index and avoids parsing
irrelevant shape bodies.

The neighbor lists exist specifically to support *selective* loading:
computing the transitive closure of an arbitrary subset (for example a
service plus one operation) as pure integer set operations, without parsing
any shape body. For a *full* read, neighbors are unnecessary — a writer could
instead emit shapes in reverse-topological order so a single forward pass
resolves every reference without a dependency graph. SMF stores neighbors
because the selective path cannot otherwise answer "which shapes does this
operation transitively reference" without reading bodies; reverse-topological
shape ordering remains available as a complementary optimization for the
full-read path.

### Metadata Section

Present only if the `has-metadata` flag is set in the header.

```
VarUInt         entryCount
MetadataEntry[] entries
```

Each MetadataEntry:

```
SymRef        key
DynamicValue  value
```

### Shapes Section

```
VarUInt   shapeCount
Shape[]   shapes
```

Each Shape:

```
SymRef    shapeId
VarUInt   byteLength (number of bytes that follow for this shape,
                      enabling skip-scanning)
byte      shapeType (see Shape Type Enum below)
TraitSet  traits
[payload] type-specific payload (see below)
```

The `byteLength` field counts all bytes after itself through the end of the
shape's type-specific payload. A reader that does not need this shape can
advance its position by `byteLength` bytes to reach the next shape.

#### Shape Type Enum

| Value | Shape Type   |
|-------|-------------|
| 0x00  | blob        |
| 0x01  | boolean     |
| 0x02  | string      |
| 0x03  | byte        |
| 0x04  | short       |
| 0x05  | integer     |
| 0x06  | long        |
| 0x07  | float       |
| 0x08  | double      |
| 0x09  | bigDecimal  |
| 0x0A  | bigInteger  |
| 0x0B  | timestamp   |
| 0x0C  | document    |
| 0x0D  | enum        |
| 0x0E  | intEnum     |
| 0x0F  | list        |
| 0x10  | map         |
| 0x11  | structure   |
| 0x12  | union       |
| 0x13  | operation   |
| 0x14  | resource    |
| 0x15  | service     |

Values 0x16-0xFF are reserved for future shape types.

#### Trait Encoding

`TraitSet` is encoded as follows.

When `split-cold-traits` is clear:

```
VarUInt   traitCount
TraitRef[] traits (traitCount entries)
```

When `split-cold-traits` is set:

```
VarUInt    hotTraitCount
TraitRef[] hotTraits (hotTraitCount entries)
VarUInt    coldTraitCount
TraitRef[] coldTraits (coldTraitCount entries)
```

Each `TraitRef` is:

```
SymRef   traitId (shape ID of the trait definition)
VarUInt  traitValueRef
```

Selective readers MAY skip cold traits entirely when `split-cold-traits` is
set.

#### Type-Specific Payloads

**Simple shapes** (0x00 through 0x0C):

```
VarUInt   mixinCount
SymRef[]  mixinIds (mixinCount entries; typically 0)
```

**Named-member shapes** (enum 0x0D, intEnum 0x0E, structure 0x11, union 0x12):

```
VarUInt   memberCount
Member[]  members (memberCount entries, in semantic order)
VarUInt   mixinCount
SymRef[]  mixinIds
```

Each Member:

```
SymRef    memberName
SymRef    targetId
TraitSet  traits
```

Members MUST be written in their semantic order. Since mixin members are
already flattened into the member list, all members (including those
originally introduced by mixins) appear directly. The `mixinIds` list
records which shapes were mixed in, but the reader does not need to
resolve them to construct the shape. Readers MUST process members in the
order they appear to preserve this ordering.

**List shape** (0x0F):

```
SymRef    memberTargetId
TraitSet  memberTraits
VarUInt   mixinCount
SymRef[]  mixinIds
```

**Map shape** (0x10):

```
SymRef    keyTargetId
TraitSet  keyTraits
SymRef    valueTargetId
TraitSet  valueTraits
VarUInt   mixinCount
SymRef[]  mixinIds
```

**Operation shape** (0x13):

```
byte      flags:
            bit 0: has-input
            bit 1: has-output
            bit 2: has-errors
            bits 3-7: reserved, MUST be 0
SymRef    inputId (present only if has-input)
SymRef    outputId (present only if has-output)
VarUInt   errorCount (present only if has-errors)
SymRef[]  errorIds (errorCount entries)
VarUInt   mixinCount
SymRef[]  mixinIds
```

**Resource shape** (0x14):

```
byte      lifecycleFlags:
            bit 0: has-put
            bit 1: has-create
            bit 2: has-read
            bit 3: has-update
            bit 4: has-delete
            bit 5: has-list
            bits 6-7: reserved, MUST be 0
SymRef    putId (if has-put)
SymRef    createId (if has-create)
SymRef    readId (if has-read)
SymRef    updateId (if has-update)
SymRef    deleteId (if has-delete)
SymRef    listId (if has-list)
VarUInt   identifierCount
Identifier[] identifiers
VarUInt   propertyCount
Property[]   properties
VarUInt   operationCount
SymRef[]  operationIds
VarUInt   collectionOperationCount
SymRef[]  collectionOperationIds
VarUInt   resourceCount
SymRef[]  resourceIds
VarUInt   mixinCount
SymRef[]  mixinIds
```

Each Identifier and Property:

```
SymRef    name
SymRef    targetId
```

**Service shape** (0x15):

```
String    version (length-prefixed UTF-8, inline, not a SymRef)
VarUInt   operationCount
SymRef[]  operationIds
VarUInt   resourceCount
SymRef[]  resourceIds
VarUInt   errorCount
SymRef[]  errorIds
VarUInt   renameCount
Rename[]  renames
VarUInt   mixinCount
SymRef[]  mixinIds
```

Each Rename:

```
SymRef    fromShapeId
String    toName (length-prefixed UTF-8, inline)
```

### Dynamic Value Encoding

Dynamic values represent arbitrary JSON-compatible data used for trait values
and metadata values. Each value is prefixed with a single-byte type tag.

| Tag  | Type        | Payload |
|------|-------------|---------|
| 0x00 | null        | (none) |
| 0x01 | false       | (none) |
| 0x02 | true        | (none) |
| 0x03 | integer     | VarInt value |
| 0x04 | double      | 8 bytes, IEEE 754 double, little-endian |
| 0x05 | string      | String (VarUInt length + UTF-8 bytes) |
| 0x06 | list        | VarUInt count, then count DynamicValues |
| 0x07 | object      | VarUInt count, then count (String key, DynamicValue value) pairs |
| 0x08 | empty object| (none), equivalent to `{}` |
| 0x09 | big integer | String (base-10 integer as length-prefixed UTF-8) |
| 0x0A | big decimal | String (decimal number as length-prefixed UTF-8) |
| 0x0B | object (symref keys) | VarUInt count, then count (SymRef key, DynamicValue value) pairs |
| 0x0C | string (symref) | SymRef into the symbol table |

Tags 0x0D through 0xFF are reserved.

**Notes:**

- Tag 0x08 (empty object) is an optimization for annotation traits. A trait
  like `@required` has the value `{}` in the JSON AST. This tag encodes it in
  a single byte with no payload. A writer MUST use 0x08 for an empty object
  rather than 0x07/0x0B with a zero count; a reader treats 0x08 as an object
  with no members.
- Tag 0x03 (integer) uses zigzag-encoded VarInt with a range of -2^63 to
  2^63 - 1 and is used for integral numeric values that fit in that range.
  Tag 0x04 is used for `double` and `float` values. Fractional `BigDecimal`
  values are encoded using tag 0x0A.
- Tag 0x09 (big integer) is for integer values that exceed the range of a
  64-bit signed integer. The value is encoded as a length-prefixed UTF-8
  string containing the base-10 representation of the integer.
- Tag 0x0A (big decimal) is for decimal values that require arbitrary
  precision. The value is encoded as a length-prefixed UTF-8 string
  containing the decimal number (e.g., "273.15").
  Both tags use string representation for simplicity since they are extremely
  rare in practice.
- **Symbol-referenced keys and strings (tags 0x0B and 0x0C).** Trait-value
  object keys are drawn from a small, highly repeated vocabulary (member names
  such as `min`, `max`, `value`), and many string values (for example
  `enumValue`, `aws.protocols#ec2QueryName`) are equal to a member name that is
  already in the symbol table. A writer MAY encode an object whose keys are all
  interned using tag 0x0B (keys as `SymRef`), and a string value that is
  already an interned symbol using tag 0x0C (`SymRef`). This removes repeated
  string bytes and lets a reader share interned `String` instances. A writer
  that cannot or chooses not to intern falls back to tags 0x07 and 0x05
  respectively; both encodings are always valid, so a reader MUST support all
  four. A writer MUST NOT emit 0x0B/0x0C for a value that references a symbol
  not present in the file's symbol table.
- **Cold trait values are never symbol-referenced.** When `split-cold-traits`
  is set, values in the cold trait value table use only inline encodings
  (0x05/0x07), and their keys and strings are not interned into the symbol
  table. Cold values are decoded only on full reads and are dominated by unique
  strings (documentation), so interning them would bloat the symbol table that
  the selective hot path must still carry without benefiting that path. See
  *Writer Profiles* below.

#### Writer Profiles

The format serves two distinct goals that pull the encoding in different
directions; a writer picks per file based on the intended consumer.

- **Full-model / distribution profile** (`split-cold-traits` clear): optimizes
  for smallest full-model size. All object keys and eligible string values are
  interned and symbol-referenced. Every trait is in the single trait value
  table.
- **Dynamic-client profile** (`split-cold-traits` set): optimizes for selective
  loading speed. Only traits needed to execute a call are hot; everything else
  is cold. Only hot values are interned/symbol-referenced, keeping the symbol
  table lean for the selective reader. A selective reader decodes only hot
  traits of the requested closure.

Which traits are "hot" is a policy decision owned by the consumer runtime, not
by this format. A conforming producer for the dynamic-client profile SHOULD
classify as cold every trait the runtime does not consult at execution time
(documentation, examples, endpoint tests, and typically service-config and
introspection traits). This can be expressed either as a denylist of cold
traits or, more robustly, as an allowlist of execution-hot traits so that
newly introduced traits default to cold. Because a selective reader silently
omits cold traits, the hot set is a correctness-sensitive contract: a trait the
runtime needs but that is classified cold will be absent from a selective load.

### Selective Loading

Selective loading allows a reader to load only the shapes needed for a
specific use case (e.g., a dynamic client for a subset of operations).

The algorithm:

1. Read the header and symbol table.
2. Read the hot trait value table setup. If `split-cold-traits` is set, the
   selective reader may skip the cold trait value table entirely.
3. Read the shape index and compute the structural closure of the requested
   root shapes (for example, a service and one or more operations).
4. Collect the byte offsets of the selected shapes and load those shapes in
   ascending offset order.
5. If the loaded shapes reference non-prelude trait definition shapes, load
   those trait definition shapes in a follow-up pass.

When `split-cold-traits` is set, a selective reader MAY load only hot traits
for the selected shapes. In that case the result is intentionally a partial
model optimized for execution rather than a full-fidelity reconstruction of
the source model, and preserved relationship metadata such as mixin links may
also be omitted if the selective path does not reload them.

### Equivalence with JSON AST

An SMF file represents a fully resolved model. It is semantically equivalent
to the JSON AST produced by serializing an assembled `Model` (after trait
merging and mixin flattening) if and only if:

1. The SMF header's Smithy version declaration is consistent with the model
   semantics. Current writers emit `2.0`.
2. They have the same metadata entries (key-value pairs, compared by value
   equality).
3. They have the same set of shapes, where each shape has the same type, the
   same traits (by trait ID and value equality), and the same type-specific
   properties (members in the same order, same targets, etc.).

Note that SMF does not preserve:

- Apply statements (traits are merged onto their target shapes).

SMF does preserve:

- Mixin relationships (the list of mixin shape IDs on each shape).
- The `@mixin` trait on shapes that are mixins.
- All traits, including those originally introduced by mixins (merged onto
  the target shape's members).

A conforming writer MUST serialize from a fully assembled model. A conforming
reader produces a model equivalent to one loaded from the flattened JSON AST,
with mixin relationships intact. This equivalence applies to full-model reads;
selective loading may intentionally omit unrelated shapes, cold traits, and
preserved relationship metadata not needed for execution.

### Prelude Handling

SMF files SHOULD NOT include prelude shapes (shapes in the `smithy.api`
namespace that are part of the Smithy specification). The reader is expected
to have the prelude built-in for the Smithy version declared in the header.

This keeps files small and avoids conflicts when merging SMF files with other
model sources that also provide the prelude.

**Forward compatibility:** New prelude traits (and potentially shapes) may
be added to Smithy without a minor version bump. An SMF file that references
a prelude shape or trait not known to an older reader will fail to load, but
this is the same behavior as JSON AST and IDL files that reference unknown
prelude definitions. SMF does not introduce any new compatibility constraints
beyond what already exists.

When a new Smithy version adds prelude definitions, the shared symbol table
may eventually need a new version. Because only shared-table version 1 exists
today, compatibility is straightforward. If future versions add entries, the
reader must treat each version as a distinct prefix length when resolving
local symbol IDs, and older readers will still fail explicitly when they
encounter unknown prelude references.

### File Extension and Media Type

- File extension: `.smf`
- Media type: `application/vnd.smithy.smf`

### Versioning

The format version byte in the header identifies the version of this
specification. A reader MUST reject files with an unsupported format version.

Future versions of this specification MAY:

- Add new entries to the shared symbol table (always appended, never
  reordered).
- Add new flag bits to the header (readers SHOULD ignore unknown flags).
- Define new shape types or dynamic value tags, but doing so requires a new
  format version.

Future versions MUST NOT:

- Reorder or remove entries from the shared symbol table.
- Change the encoding of existing shape types or dynamic value tags.
- Change the meaning of existing header flags.

### Integrity Checking

Every SMF file MUST end with a 4-byte CRC-32C (Castagnoli) checksum computed
over all preceding bytes (from the first byte of the header through the last
byte of the shapes section). The checksum is stored in little-endian byte
order.

```
┌────────────────────────────────┐
│ Header + Symbol Table +        │
│ Trait Value Tables +           │
│ Shape Index + Metadata +       │
│ Shapes Section                 │  ← CRC-32C computed over these bytes
├────────────────────────────────┤
│ CRC-32C (4 bytes, LE)          │
└────────────────────────────────┘
```

A conforming writer MUST compute and append the CRC-32C after writing all
sections. Readers SHOULD verify the CRC-32C before processing any data and
MUST reject the file with a clear error if verification is enabled and the
checksum does not match. Implementations MAY expose an opt-out for trusted
inputs or benchmarking.

CRC-32C is chosen over CRC-32 because it has hardware acceleration on modern
x86 (SSE 4.2) and ARM (CRC extension) processors, making verification
effectively free on the critical loading path.

#### Integrity checking lifecycle

CRC verification is intended primarily as an **install-time** integrity gate,
not a per-load cost on the hot path. The recommended lifecycle is:

- **At install/import time (once):** when a model is first added to a local
  store (for example, an AWS CLI-style `add-model` step that drops an `.smf`
  file into a models directory), the reader SHOULD verify the CRC-32C. The
  file is accepted from an external source at this point, so integrity matters
  and the one-time cost is irrelevant.
- **At load time (every invocation):** once a file has been verified and is
  trusted on local disk, repeated loads SHOULD skip CRC verification and go
  straight to (selective) parsing. This is where the format's loading-speed
  advantage is realized.

This split matters because CRC cost is highly asymmetric across
implementations. On the JVM (and native readers) the hardware CRC32C
intrinsic makes full-file verification nearly free, so verifying on every load
is cheap. In a pure interpreted reader (for example a pure-Python reader with
a software CRC loop) full-file verification can cost **one to two orders of
magnitude more than the entire selective load it protects**, so verifying on
every invocation would erase the format's advantage.

No format change is required to support this: readers already control
verification per call (the reference implementation exposes a `verifyCrc`
flag on its read entry points and on the selective-load request). Hot-path
callers pass a value that skips verification; the install/import path verifies
once. Skipping verification at load time is safe **only** if the file was
verified at install time and the local store is trusted against post-install
corruption or tampering — the same trust model used by package managers and
by botocore's cached service models. This MUST be a deliberate, documented
choice rather than an accidental default.


## Alternatives and trade-offs

### Design trade-offs

| Decision | Benefit | Cost |
|----------|---------|------|
| Fully resolved model (no apply, mixins as relationships) | Reader builds shapes directly — no trait merging, mixin flattening, or forward-reference resolution; mixin relationships preserved for codegen | Cannot round-trip to exact IDL file structure; loses apply provenance; not usable as a composable build dependency (see below) |
| Symbol table up front | Eliminates repeated strings; enables single-allocation shape ID parsing | Writer must buffer the full model; reader must allocate the symbol array before processing shapes |
| Positional encoding (no keys) | Zero overhead for known structure | Format is version-sensitive; new fields require a format-version bump or flags |
| Length-prefixed shapes | Enables skip-scanning for selective loading | 1–5 extra bytes per shape |
| Shape index section | Enables selective loading | ~1–2% file-size overhead |
| VarUInt/VarInt for structural fields | Most counts, lengths, and symrefs are small, so variable-length ints are markedly smaller than fixed-width; keeps the symbol-dense body compact | Slightly more parsing work than fixed-width ints |
| Fixed-width `uint32` for the index and trait-value offset tables | Enables random access and batch parsing (e.g. one `struct.iter_unpack` over the whole index in interpreted readers) exactly where seeking matters | Larger than varints for these specific tables |
| Trait values as `Node`-compatible encoding | Compatible with the existing trait-resolution pipeline | Still allocates `Node` objects for trait values |
| Member order preserved positionally | Correct semantics without relying on map implementation details | None |

The integer-encoding split is deliberate: **variable-length** ints are used
for the body, where the overwhelming majority of counts/lengths/symrefs are
small and fixed-width encoding would inflate the symbol-dense payload; and
**fixed-width** ints are used for the shape index and trait-value offset
tables, where random access and batch parsing matter more than a few bytes.
So "not optimizing for minimal size" does not imply fixed-width everywhere —
each section uses the encoding that best serves its access pattern.

### Not a composable build dependency

SMF encodes a fully assembled model, so an apply statement in package A that
targets a shape in package B has already been merged into B's shape by the
time the model is serialized. There is no representation of a *dangling* apply
in SMF, and therefore no way for a consumer to compose an SMF model with
another package's applies the way the JSON AST allows. This is an accepted
limitation for the target use cases (dynamic client, codegen of a complete
model). A team that needs both fast loading *and* dependency composition can
distribute a sibling model — the JSON AST or IDL carrying the apply
statements — alongside the `.smf` file, and use SMF only for the
fully-resolved consumers. Adding optional dangling-apply support would be a
future format change, not part of this preview.

### CBOR (RFC 8949)

CBOR is a schema-less binary encoding of JSON-like data with mature Java
support. It is roughly a 2× improvement over JSON, but string deduplication is
not part of core CBOR (Packed CBOR is still an IETF draft), structural keys are
still encoded on every shape, and the decoder must handle any type at any
position, preventing type-specialized fast paths. It also has no notion of
selective loading. SMF eliminates the repeated-string and structural-key
overhead entirely and adds a selective-loading index.

### Amazon Ion (with shared symbol tables)

Ion's shared symbol tables solve the repeated-string problem directly, and
`ion-java` is battle-tested. Ion with a shared symbol table would get roughly
60–70% of the benefit with no format-design work, and remains a reasonable
pragmatic choice if implementation bandwidth is constrained. SMF is preferred
here because: Ion structs are unordered (member ordering would be fragile);
the reader still walks a generic Ion value tree rather than constructing shapes
positionally; Ion has no Smithy-specific selective-loading index; and a goal of
this work is a reader with no third-party dependency (important for the Python
consumer targeted as a C2J replacement). The earlier "generality tax" argument
— that a reader must handle Ion features that never appear — is weak, since a
library does the parsing; the real justifications are positional shape layout,
the selective index, and zero-dependency readers.

### Custom format (this proposal)

A purpose-built format that borrows the symbol-table concept, uses positional
encoding for the known structure, adds a Smithy-specific selective-loading
index, and uses a minimal self-describing encoding only for the dynamic parts
(trait values, metadata). It is the only option that addresses repeated
strings, structural keys, the intermediate tree, shape-ID re-parsing, and
selective loading together. The cost is design, implementation, and
maintenance across languages, plus the need for an `smf`-to-JSON tool for
debugging.

### Prefix-varint instead of LEB128 (evaluated, rejected)

We evaluated replacing LEB128 with a prefix-varint (the Sparrowhawk encoding,
where the trailing-zero count of the first byte gives the total width, so the
value can be read with one word load and no per-byte branch). It is not a good
fit for SMF's data, so we keep LEB128.

Both encodings produce identical byte sizes. The difference is decode time as a
function of value magnitude. LEB128 is cheapest for small values (a one-byte
value is a single loop iteration) and gets more expensive as values widen.
Prefix-varint has near-constant cost regardless of magnitude. Measured decode
of 5,000,000 values on the JVM:

| Value distribution | LEB128 | prefix-varint |
|---|---|---|
| Model-like (70% 1-byte, 25% 2-3 byte, 5% wide) | 30.4 ms | 32.8 ms (+8%) |
| All 1-byte | 17.5 ms | 32.4 ms (+85%) |
| Medium (~3 byte) | 27.1 ms | 32.9 ms (+22%) |
| Wide (~4-5 byte) | 37.2 ms | 32.5 ms (-13%) |

Prefix-varint only wins when most values are wide (4-5 bytes). SMF's structural
values are the opposite: symrefs, counts, lengths, and member indices are
overwhelmingly small, in large part because the writer assigns the lowest
symbol IDs to the most frequent strings. A pure-Python decode of the model-like
distribution showed the same direction, with prefix-varint about 53% slower.
Sparrowhawk benefits from prefix-varint because RPC payloads carry more wide
values and its decode loop is sensitive to continuation-bit branch
misprediction; SMF sits in the small-value regime where LEB128 is already
optimal.

## Open questions

1. **Source locations.** SMF does not encode source location (file, line,
   column); shapes loaded from SMF report no location. For the target use
   cases (dynamic client, codegen) this is acceptable and is treated as a
   non-goal for the preview. An optional source-map section could be added
   later if validation-error UX on SMF-loaded models becomes important; that
   would also require the error renderer to show byte offsets alongside parsed
   values.
2. **Shared symbol table governance.** The shared table is defined by this
   specification (version 1 = prelude shape IDs). The process for adding
   entries in future versions, and whether AWS-specific traits belong in a
   separate shared table (`sharedTableId=2`), is left open.
3. **Selective loading and trait references.** The closure algorithm follows
   structural references (targets, inputs, outputs, errors) but does not
   inspect trait values for shape IDs. Traits such as `@idRef`/`@references`
   may contain shape IDs that are not followed. This is acceptable for the
   dynamic-client use case and is a documented limitation.

Resolved during design: integrity checking (a mandatory CRC-32C was added; see
*Integrity Checking*), and symbol-referenced object keys / string values
(added as DynamicValue tags `0x0B`/`0x0C`; see *Dynamic Value Encoding*).

## Appendix A: size estimates

Approximate sizes for the EC2 model (~4,900 shapes). The comparison includes
JSON *without* documentation so that stripped SMF is compared like-for-like,
and gzipped variants because models are typically distributed compressed:

| Representation | Size |
|---|---|
| Pretty-printed JSON AST | ~7,700 KB |
| Minified JSON AST (with docs) | ~4,900 KB |
| Minified JSON AST (no docs) | ~1,900 KB |
| SMF (full, with docs) | ~2,560 KB |
| SMF (split cold traits / no hot docs) | ~2,540 KB |
| Minified JSON (no docs) + gzip | ~500 KB |
| SMF + gzip | ~630 KB |

Gzipped SMF is *not* smaller than a gzipped compact JSON model; SMF's symbol
interning overlaps with what a general compressor already does. The takeaway
is that SMF's advantage is **load speed, not size** — when the smallest
distributable file is the priority, a compact model plus a general compressor
wins.

## Appendix B: benchmark results

JMH, single-threaded, warmed up, Apple M1 Pro / JDK 25. Four loading paths:
JSON via the assembler, SMF via the assembler, SMF direct (`SmfReader.read`),
and SMF selective (service + one operation). Representative figures — STS and
DynamoDB are the common/small cases, S3 is mid, EC2 is the extreme upper bound:

| Path | STS | DynamoDB | S3 | EC2 |
|---|---|---|---|---|
| SMF direct vs JSON | ~2.3× | ~2.6× | ~3.0× | ~5.0× |
| SMF selective vs JSON | ~2.7× | ~4.1× | ~5.3× | ~30× |
| SMF via assembler vs JSON | ~1.0× | ~1.0× | ~1.1× | ~2.0× |

The direct and selective paths are the primary targets (codegen/tooling and
dynamic clients respectively). The assembler-integrated path preserves full
compatibility with the loading pipeline and so retains its per-shape overhead;
its speedup is smaller and it is not the primary path.

The selective path additionally benefits from the dynamic-client writer
profile (split cold traits): decoding only execution-hot traits of the
requested closure yields a further ~1.5–3.7× on the common models over a
flat file, largest on models carrying many non-execution traits.
