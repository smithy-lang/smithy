# Smithy Model Format (SMF)

* **Authors**: Manuel Sugawara
* **Created**: 2026-05-08

## Abstract

This proposal defines a compact binary serialization format for Smithy models
optimized for fast loading. The format is a binary representation of the
Smithy JSON AST that eliminates parsing overhead, reduces allocations, and
enables selective loading of model subsets.

## Motivation

Smithy models are currently distributed and loaded using the JSON AST format.
While JSON is human-readable and widely supported, it has significant
performance costs for model loading:

1. **Parsing overhead**: A JSON tokenizer must handle whitespace, escape
   sequences, quoted strings, and structural characters. For the EC2 model
   (7.7MB JSON, 4,904 shapes), this is substantial work that produces no
   semantic value.

2. **Repeated string processing**: The string `"smithy.api#documentation"`
   appears 11,541 times in the EC2 model. Each occurrence must be read,
   allocated, and compared against known keys. Structural keys like `"traits"`,
   `"target"`, and `"members"` appear tens of thousands of times.

3. **Intermediate representation**: JSON loading first builds a `Node` tree,
   then walks it to construct shapes. This doubles the allocation cost.

4. **No selective loading**: A dynamic client that needs 5 operations from EC2
   must parse all 4,904 shapes. There is no way to skip irrelevant data.

5. **ShapeId re-parsing**: `ShapeId.from(string)` is called for every target
   reference, every trait name, and every shape key, even when the same
   ShapeId has already been parsed from an earlier occurrence of the same
   string.

Analysis of the AWS model corpus (424 service models, 250MB total JSON)
shows that:

- 21% of file bytes are repetitive JSON keys (96K occurrences, 9.7K unique)
- 28% is documentation text (incompressible but strippable)
- 48% is JSON syntax overhead (quotes, braces, colons, commas, whitespace)
- Only 9,742 unique strings serve as identifiers in the EC2 model, but they
  occur 96,079 times total

A format that interns strings into a symbol table and encodes the known
structure positionally can eliminate most of this overhead.

## Goals

1. Define a binary format that any language can implement a reader/writer for.
2. Optimize for loading speed: minimize parsing, allocations, and string
   comparisons during model construction.
3. Support full-model loading (codegen, validation) and selective loading
   (dynamic clients needing a subset of operations).
4. Maintain full fidelity with a fully resolved model, any assembled model
   is representable in SMF, and loading an SMF file produces an equivalent
   model without requiring trait merging or mixin application.
5. Keep the format simple enough that a complete reader is implementable in
   under 1,000 lines of code in any language.

## Non-goals

1. Human readability. A companion tool can convert SMF to/from JSON AST.
2. Minimal wire size. The format optimizes for loading speed, not compression.
   General-purpose compression (gzip, zstd) can be applied externally for
   distribution.
3. Streaming writes without buffering. The writer needs the full model to
   build the symbol table before writing.
4. Random-access mutation. SMF files are written once and read many times.

## Proposal

### Overview

SMF is a binary encoding of a **fully resolved** Smithy model that:

- Represents the model after all apply statements have been merged and all
  mixin members have been flattened into their target shapes. Apply statements
  are not preserved. Mixin relationships are preserved as metadata on each
  shape (the list of mixin shape IDs), but members introduced by mixins
  appear directly in the member list, the reader does not need to perform
  mixin application.
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
trait merging, mixin application, or forward reference resolution. It can
construct complete `Shape` objects directly from the binary data. Mixin
relationships are available as metadata for codegen and tooling that needs
them, but they do not affect the loading process.

### Tradeoffs

| Decision | Benefit | Cost |
|----------|---------|------|
| Fully resolved model (no apply, mixins as metadata) | Reader builds shapes directly, no trait merging, mixin flattening, or forward reference resolution needed; mixin relationships preserved for codegen | Cannot round-trip to exact IDL file structure; loses apply provenance |
| Symbol table up front | Eliminates repeated strings; enables single-allocation ShapeId parsing | Writer must buffer full model; reader must allocate symbol array before processing shapes |
| Positional encoding (no keys) | Zero overhead for known structure | Format is version-sensitive; new fields require format version bump or flags |
| Length-prefixed shapes | Enables skip-scanning for selective loading | 1-5 extra bytes per shape |
| Shape index section | Enables selective loading; optional for when selective loading is needed | ~1-2% file size overhead |
| LEB128 variable integers | Fast on little-endian architectures (x86, ARM); compact | Slightly more complex than fixed-width integers |
| Trait values as Node-compatible encoding | Compatible with existing trait resolution pipeline | Still allocates Node objects for trait values |
| Member order preserved positionally | Correct semantics without relying on map implementation details | N/A |

---

## Alternatives Considered

### CBOR (RFC 8949)

CBOR is a schema-less binary encoding of JSON-like data. Every value carries
a type+length prefix byte. It has mature Java support and is an IETF standard.

**Pros:**

- Drop-in replacement for JSON with no schema changes, the JSON AST maps
  directly to CBOR.
- 2-3x faster parsing than JSON, 20-50% smaller output.
- Handles dynamic trait values naturally (same data model as JSON).
- Minimal implementation effort.

**Cons:**

- String deduplication is not part of core CBOR. "Packed CBOR"
  (draft-ietf-cbor-packed) adds a shared-item table mechanism, but it is
  still an IETF draft, not widely implemented in libraries, and works as a
  wrapping layer rather than a native feature. The reader must resolve
  references at arbitrary positions, adding indirection to every value access.
- No way to exploit the known structure. The decoder must handle any CBOR type
  at any position, preventing type-specialized fast paths.
- Structural keys (`"traits"`, `"target"`, `"members"`) are still encoded as
  strings (or packed references) on every shape, the format has no concept of
  positional fields.

CBOR would yield roughly a 2x improvement over JSON. The custom format achieves
~2-4x by eliminating the repeated-string problem entirely.

### Amazon Ion (binary, with shared symbol tables)

Ion is a richly-typed self-describing format with a key feature: symbol tables
that map repeated strings to integer IDs. Ion 1.1 adds macros that can encode
repeated structural patterns as compact templates.

**Pros:**

- Symbol tables solve the repeated-string problem directly. A shared symbol
  table containing Smithy's well-known strings eliminates most key overhead.
- Battle-tested Java implementation (ion-java).
- Text ↔ binary transcoding is built in, useful for debugging.
- Ion 1.1 macros could encode shape definitions as compact template
  invocations, approaching the density of a custom format.

**Cons:**

- Generality tax. Ion supports timestamps, decimals, s-expressions,
  annotations, clobs, blobs, and multiple integer encodings. Smithy needs 6
  value types. The reader must handle (or skip) features that will never
  appear.
- Ion structs are specified as unordered. Relying on field order for member
  semantics is fragile and implementation-dependent.
- The reader still produces a generic Ion value tree that must be walked to
  construct shapes, the same two-pass pattern as JSON (parse to tree, then
  interpret tree).
- ion-java is a substantial dependency (~50K LOC). A custom reader for the
  Smithy-specific subset is ~500-800 LOC.
- Ion 1.1 is still maturing. The macro system adds significant complexity to
  both the format and the implementation.

Ion with a shared symbol table would yield roughly a 3-4x improvement over
JSON. The custom format matches or exceeds this while being simpler, smaller,
and tailored to the exact access patterns of model loading.

### Custom Format (this proposal)

A purpose-built binary format that borrows the symbol table concept from Ion,
uses positional encoding for the known structure, and employs a minimal
self-describing encoding only for the dynamic parts (trait values, metadata).

**Pros:**

- Significant loading performance improvement (1.7x-3.6x across all model sizes). The reader is a tight loop: read symbol ID →
  switch on shape type byte → read fields positionally. No generic dispatch,
  no string comparisons, no intermediate tree.
- Minimal dependency. The reader is self-contained (~500-800 LOC) with no
  external library requirements.
- Member ordering is an explicit guarantee of the format, not an
  implementation detail of a map type.
- Tailored optimizations: annotation traits in 1 byte, pre-parsed ShapeIds,
  strippable documentation, selective loading via shape index.
- Simpler than Ion for this use case, only the 6 JSON value types, no
  timestamps/decimals/annotations/s-expressions.

**Cons:**

- Design and implementation cost. The format must be specified, implemented,
  and maintained across languages.
- No existing tooling. A `smf dump` utility must be built for debugging
  (Ion provides `ion dump` for free).
- Less ecosystem leverage. Ion is used broadly within Amazon; a custom format
  is Smithy-specific.

### Rationale

The choice comes down to: how much of the loading hot path can you optimize?

| Format | Repeated strings | Structural keys | Intermediate tree | ShapeId re-parsing | Selective loading |
|--------|-----------------|-----------------|-------------------|-------------------|-------------------|
| CBOR | ✗ repeated (Packed CBOR draft exists but immature) | ✗ still encoded | ✗ still built | ✗ still re-parsed | ✗ not supported |
| Ion + shared symtab | ✓ interned | ✗ still encoded (as SIDs) | ✗ still built | ✓ once per symbol | ✗ not natively |
| Custom (SMF) | ✓ interned | ✓ eliminated | ✓ partially (structural only) | ✓ once per symbol | ✓ built-in |

The custom format is the only option that addresses all five sources of
overhead. The implementation cost is modest (a reader is under 1,000 LOC)
and the format is simple enough that other languages can implement it from
this specification without reverse-engineering a complex general-purpose
library.

Ion remains a reasonable pragmatic choice if implementation bandwidth is
constrained, it gets 60-70% of the benefit with zero format design work.
However, for the target use cases (dynamic client startup, codegen
performance), the remaining 30-40% matters.

---

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
│ Shape Index (optional)         │
├────────────────────────────────┤
│ Metadata Section               │
├────────────────────────────────┤
│ Shapes Section                 │
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
                bits 2-7: reserved, MUST be 0
```

A reader MUST reject files where the magic number does not match. A reader
MUST reject files where the format version is not supported. A reader SHOULD
ignore reserved bits and bytes.

Note: Documentation stripping is a writer concern. A writer that wishes to
produce a smaller file simply omits `smithy.api#documentation` traits during
serialization. This does not require a flag in the format; the resulting file
is simply a model without documentation traits.

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
- IDs 1 through S (where S is the size of the shared table) correspond to
  entries in the shared symbol table.
- IDs S+1 through S+L (where L is localSymbolCount) correspond to the local
  symbols in order.

The **smithy-core shared table** (sharedTableId=1) is a well-known table
defined by this specification. It contains commonly used Smithy strings that
appear across most models. Its contents are versioned; version N is always a
prefix of version N+1 (new entries are appended, never reordered or removed).

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

Note: Future versions of the shared table may expand to include commonly used
trait IDs and other well-known strings. New entries are always appended.

Implementations MUST include the shared table contents compiled into the
reader and writer. The shared table is never transmitted in the file.

Writers SHOULD assign local symbol IDs in descending frequency order (most
frequently referenced strings get the lowest IDs) to minimize VarUInt
encoding size.

### Trait Value Table

The trait value table deduplicates trait values that appear multiple times
across shapes. Each unique trait value is stored once in this table and
referenced by index from within shape trait encodings.

```
VarUInt         valueCount
DynamicValue[]  values (valueCount complete encoded values)
```

In the shapes section, each trait is encoded as:

```
SymRef    traitId
VarUInt   traitValueRef (index into the trait value table)
```

This replaces inline `DynamicValue` encoding with a compact integer reference.
Annotation traits (`{}`) that appear thousands of times are stored once and
referenced by a 1-2 byte VarUInt instead of being encoded inline each time.

### Shape Index

The shape index maps shape IDs to byte offsets within the shapes section and
includes a neighbor list (dependency graph) for each shape. This enables
computing the transitive closure of any shape without parsing shape bodies.
Present only if the `has-shape-index` flag is set in the header.

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

The neighbor list contains all shape IDs that this shape directly references:
member targets, operation input/output/errors, resource lifecycle operations,
service operations/resources/errors, and identifiers/properties targets. It
does NOT include prelude shapes (which are assumed available) or shapes
referenced only within trait values.

**Selective loading algorithm:**

1. Read the symbol table and shape index.
2. Build a lookup from shape ID to index entry.
3. Starting from the desired root shapes (e.g., a service + operations),
   compute the transitive closure by following neighbor lists, this is pure
   integer set operations with no shape parsing.
4. Collect the byte offsets of all shapes in the closure.
5. Sort offsets to enable a single sequential forward pass through the shapes
   section.
6. Scan the shapes section, parsing only shapes whose offsets are in the
   sorted set, skipping all others via `byteLength`.

This approach ensures that closure computation is O(closure_size) with no
I/O beyond the index, and shape reading is a single sequential pass with
no seeking.

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
VarUInt   traitCount
Trait[]   traits (traitCount entries)
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

Each trait:

```
SymRef        traitId (shape ID of the trait definition)
DynamicValue  value
```

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
VarUInt   traitCount
Trait[]   traits
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
VarUInt   memberTraitCount
Trait[]   memberTraits
VarUInt   mixinCount
SymRef[]  mixinIds
```

**Map shape** (0x10):

```
SymRef    keyTargetId
VarUInt   keyTraitCount
Trait[]   keyTraits
SymRef    valueTargetId
VarUInt   valueTraitCount
Trait[]   valueTraits
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

Tags 0x0B through 0xFF are reserved.

**Notes:**

- Tag 0x08 (empty object) is an optimization for annotation traits. A trait
  like `@required` has the value `{}` in the JSON AST. This tag encodes it in
  a single byte with no payload.
- Tag 0x03 (integer) uses zigzag-encoded VarInt with a range of -2^63 to
  2^63 - 1. JSON numbers that fit in this range and have no fractional part
  MUST use tag 0x03. JSON numbers with a fractional part or exponent that can
  be represented as IEEE 754 double without precision loss MUST use tag 0x04.
- Tag 0x09 (big integer) is for integer values that exceed the range of a
  64-bit signed integer. The value is encoded as a length-prefixed UTF-8
  string containing the base-10 representation of the integer.
- Tag 0x0A (big decimal) is for decimal values that require arbitrary
  precision. The value is encoded as a length-prefixed UTF-8 string
  containing the decimal number (e.g., "273.15").
  Both tags use string representation for simplicity since they are extremely
  rare in practice.
- Object keys (tag 0x07) are always inline strings, not symbol references.
  This keeps the dynamic value encoding self-contained and avoids coupling
  trait value internals to the symbol table.

### Selective Loading

Selective loading allows a reader to load only the shapes needed for a
specific use case (e.g., a dynamic client for a subset of operations).

The algorithm:

1. Read the header and symbol table.
2. Read the shape index.
3. Determine the initial set of needed shape IDs (e.g., the service shape
   and desired operation shapes).
4. Compute the transitive closure: for each needed shape, read it from the
   shapes section (using the index to locate it), extract all SymRef values
   that reference other shapes, and add those to the needed set. Repeat until
   no new shapes are discovered.
5. The resulting set of shapes constitutes the minimal model subset.

For streaming I/O (where seeking is not possible), the reader can perform
selective loading by scanning the shapes section sequentially, using the
`byteLength` field to skip shapes not in the needed set. This may require
multiple passes if forward references exist (a shape references another shape
that appears later in the file). Alternatively, the reader can perform a
single pass that reads all shapes but only fully parses (constructs builders
and Node values for) those in the needed set.

### Equivalence with JSON AST

An SMF file represents a fully resolved model. It is semantically equivalent
to the JSON AST produced by serializing an assembled `Model` (after trait
merging and mixin flattening) if and only if:

1. They have the same Smithy version (as encoded in the SMF header).
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
with mixin relationships intact.

### Prelude Handling

SMF files MUST NOT include prelude shapes (shapes in the `smithy.api`
namespace that are part of the Smithy specification). The reader is expected
to have the prelude built-in for the Smithy version declared in the header.

This keeps files small (avoids ~70 redundant shapes per file) and avoids
conflicts when merging SMF files with other model sources that also provide
the prelude.

**Forward compatibility:** New prelude traits (and potentially shapes) may
be added to Smithy without a minor version bump. An SMF file that references
a prelude shape or trait not known to an older reader will fail to load, but
this is the same behavior as JSON AST and IDL files that reference unknown
prelude definitions. SMF does not introduce any new compatibility constraints
beyond what already exists.

When a new Smithy version adds prelude definitions:

1. The shared symbol table MAY be extended with a new version that includes
   the new prelude shape IDs (appended, never reordered).
2. A reader with the updated prelude and shared symbol table handles the
   new shapes transparently.
3. A reader without the updated prelude will fail when encountering a
   reference to an unknown prelude shape; the same failure mode as loading
   a JSON AST file with unknown prelude references.

This ensures that:
- New readers can always read old files (old prelude is a subset of new).
- Old readers fail explicitly when encountering unknown shapes, regardless
  of format (SMF, JSON, or IDL).
- The shared symbol table version in the file tells the reader whether its
  compiled-in table is sufficient.

### File Extension and Media Type

- File extension: `.smf`
- Media type: `application/vnd.smithy.smf`

### Versioning

The format version byte in the header identifies the version of this
specification. A reader MUST reject files with an unsupported format version.

Future versions of this specification MAY:

- Add new shape type enum values (readers SHOULD skip unknown shape types
  using the byteLength field and emit a warning).
- Add new dynamic value type tags (readers MUST reject unknown tags as they
  cannot determine the payload length). To mitigate this, tags in the range
  0x80-0xFF are reserved as **length-prefixed extension tags**: their payload
  is always `VarUInt byteLength` followed by `byteLength` bytes of
  type-specific data. A reader that encounters an unknown tag in this range
  can skip it without understanding the contents. Tags in 0x0B-0x7F remain
  non-self-describing and require a format version bump to add.
- Add new entries to the shared symbol table (always appended, never
  reordered).
- Add new flag bits to the header (readers SHOULD ignore unknown flags).

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
│ Shape Index + Metadata +       │
│ Shapes Section                 │  ← CRC-32C computed over these bytes
├────────────────────────────────┤
│ CRC-32C (4 bytes, LE)          │
└────────────────────────────────┘
```

A conforming writer MUST compute and append the CRC-32C after writing all
sections. A conforming reader MUST verify the CRC-32C before processing any
data and MUST reject the file with a clear error if the checksum does not
match.

CRC-32C is chosen over CRC-32 because it has hardware acceleration on modern
x86 (SSE 4.2) and ARM (CRC extension) processors, making verification
effectively free on the critical loading path.

## Open Questions

1. **Source locations**: The format does not encode source location information
   (file, line, column). Validation errors on SMF-loaded models will not have
   useful locations. Is this acceptable for the target use cases (dynamic
   client, codegen), or should an optional source map section be defined?

2. **Integrity checking**: ~~The format has no checksum or CRC. A corrupted file
   may produce silently incorrect models. Should a trailing checksum (e.g.,
   CRC-32C) be added to the header or footer?~~
   **Resolved**: A CRC-32C checksum is mandatory. See the Integrity Checking
   section below.

3. **Shared symbol table governance**: The shared table contents are defined by
   this specification. What is the process for adding new entries in future
   versions? Should AWS-specific traits (aws.api#service, aws.protocols#*)
   be included, or kept in a separate shared table (sharedTableId=2)?

4. **DynamicValue object keys**: Object keys in trait values are always inline
   strings. Traits like `@examples` and `@paginated` have structured values
   with repeated keys. Should a future version allow SymRef keys in objects
   (e.g., a new tag 0x0C for "symbol-keyed object")?

5. **Selective loading and trait references**: The selective loading closure
   algorithm follows structural SymRefs (targets, inputs, outputs, errors)
   but does not inspect trait values for shape ID references. Traits like
   `@idRef` or `@references` may contain shape IDs that are not followed.
   This is acceptable for the dynamic client use case but should be
   documented as a known limitation.

## Appendix A: Size Estimates

Based on the EC2 model (4,904 shapes, 7.7MB pretty-printed JSON):

| Representation | Size |
|---|---|
| Pretty-printed JSON AST | 7,683 KB |
| Minified JSON AST | 4,905 KB |
| SMF (full, with docs) | ~2,940 KB |
| SMF (stripped, no docs) | ~755 KB |
| Minified JSON + gzip | 599 KB |
| SMF (stripped) + gzip | ~350 KB |

## Appendix B: Loading Performance Model

For a model with N shapes, M total members, T total trait applications, and
U unique strings:

| Operation | JSON AST | SMF |
|---|---|---|
| String allocations for keys | O(N + M + T) per occurrence | O(U) total (symbol table only) |
| ShapeId parsing | O(N + M + T) per reference | O(U) total (pre-parsed from symbol table) |
| Key-based dispatch | String comparison per field | Byte switch per shape type |
| Trait value construction | Via intermediate Node tree | Direct Node construction from bytes |
| Skip a shape | Not possible | Read VarUInt, advance position |

Real-world speedup via the direct `SmfReader.read()` path is 3.5-5.5x across
all model sizes (see Appendix D). Selective loading of a single operation
achieves 4-23x depending on model size. The assembler-integrated path
(which preserves compatibility with the full loading pipeline) achieves
1.2-2.2x.

## Appendix C: Selective Loading Cost

For a dynamic client loading K operations from a model with N shapes:

| Step | Cost |
|---|---|
| Read symbol table | O(U), one-time, ~290KB for EC2 |
| Read shape index | O(N), one-time, ~50KB for EC2 |
| Compute closure | O(K × avg_refs), typically 50-200 shapes |
| Parse needed shapes | O(closure_size), typically <100KB of data |
| Skip irrelevant shapes | O(1) per shape (read byteLength, advance) |

## Appendix D: Benchmark Results

Benchmarks on representative AWS service models (JMH, single-threaded,
warmed up, Apple M1 Pro arm64, JDK 25).

Four loading paths are compared:
- **JSON (assembler)**: Load JSON AST via `Model.assembler().addImport(file)`
- **SMF (assembler)**: Load SMF via `Model.assembler().addImport(file)`
- **SMF (direct)**: Load SMF via `SmfReader.read(byte[])`, no assembler overhead
- **SMF (selective)**: Load service + 1 operation closure via `SmfReader.readSelective`

| Model | JSON (assembler) | SMF (assembler) | SMF (direct) | SMF (selective, 1 op) |
|---|---|---|---|---|
| EC2 (4,904 shapes) | 56.8 ms | 26.3 ms | 10.3 ms | 2.5 ms |
| S3 (1,021 shapes) | 12.8 ms | 10.8 ms | 3.7 ms | 2.2 ms |
| DynamoDB (394 shapes) | 7.6 ms | 6.0 ms | 2.2 ms | 1.4 ms |
| STS (49 shapes) | 2.2 ms | 2.2 ms | 0.6 ms | 0.5 ms |

**Speedup vs JSON (assembler):**

| Path | EC2 | S3 | DynamoDB | STS |
|---|---|---|---|---|
| SMF (assembler) | 2.2x | 1.2x | 1.3x | 1.0x |
| SMF (direct) | **5.5x** | **3.5x** | **3.5x** | **3.6x** |
| SMF (selective) | **22.7x** | **5.9x** | **5.5x** | **4.3x** |

The direct path (`SmfReader.read`) is the primary target for codegen and
tooling that needs the full model. The selective path is the primary target
for dynamic clients that need a single operation at startup.

**Key tradeoff: SMF assumes the model is valid.** The SMF loading path
bypasses the model assembler's trait resolution and validation pipeline.
Shapes are loaded as fully-formed objects and placed directly into the model.
This is safe because:

1. An SMF file is produced from a fully assembled and validated model. The
   writer only accepts a `Model` that has already passed validation.
2. Re-validating on every load is redundant work for models that were
   validated at publish time.
3. The model assembler's validation can still be run explicitly after loading
   if needed (e.g., during development or when combining SMF with other model
   sources).

If an SMF file is loaded alongside other model sources (e.g., `.smithy` or
`.json` files), the assembler still validates the combined model. The SMF
shapes participate in validation, they are just not individually re-parsed
through the trait resolution pipeline.
