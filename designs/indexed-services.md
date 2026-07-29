# Indexed services

* **Author**: Jordon Phillips
* **Created**: 2026-07-29

## Abstract

This proposal introduces the `smithy.protocols#idx` and
`smithy.protocols#indexed` traits for assigning stable wire indexes to
structure and union members. It also introduces an IDL 2.1 shorthand for
applying `idx` to members.

Member indexes allow protocols to identify members by a compact integer
without relying on source declaration order. The indexes remain stable when a
model is reordered, transformed, or converted between the Smithy IDL and JSON
AST.

## Motivation

Smithy structure and union members have no canonical semantic order. Tooling
preserves declaration order when practical to reduce model churn, but model
authors and transformations can reorder members. Mixins can also introduce or
extract members, and JSON objects do not have a defined order.

Historically, protocols have not needed Smithy member order to be preserved.
Binary protocols can benefit from a stable order, however. When both endpoints
know a member's position, the protocol can identify it with a compact integer
or omit the identifier entirely. This reduces payload size and the work needed
to read and write the payload.

Declaration order cannot provide this guarantee because it is not semantic
model data. Stable ordering therefore needs to be represented by traits that
survive model transformations and serialization.

## Proposal

### The `idx` trait

The `idx` trait assigns a positive integer wire index to a member of a
non-mixin structure or union:

```smithy
$version: "2.0"

namespace smithy.protocols

@trait(
    selector: ":is(structure, union) :not([trait|mixin]) > member"
    breakingChanges: [{change: "update"}]
)
@range(min: 1, max: 65535)
integer idx
```

If any member of a structure or union has `idx`, every member of that shape
MUST have a unique index unless the member is explicitly exempt. Index values
MUST start at 1 and increase by 1 with no gaps.

The declaration order of members has no effect on their indexes. These two
structures are equivalent:

```smithy
$version: "2.0"

namespace smithy.example

use smithy.protocols#idx

structure Record {
    @idx(1)
    id: String

    @idx(2)
    value: String
}
```

```smithy
$version: "2.0"

namespace smithy.example

use smithy.protocols#idx

structure Record {
    @idx(2)
    value: String

    @idx(1)
    id: String
}
```

Members marked with `eventHeader` or `eventPayload` are exempt from the
requirement to have a member index. Members targeting a streaming blob or
union are also exempt.

#### Mixins

Mixins MUST NOT apply `idx` to their members. A shape that consumes a mixin
MUST assign a member index to each elided member in the context of the
consuming shape:

```smithy
$version: "2.0"

namespace smithy.example

use smithy.protocols#idx

@mixin
structure Identified {
    id: String
}

structure Record with [Identified] {
    @idx(2)
    value: String

    @idx(1)
    $id
}
```

Assigning member indexes at the point of consumption prevents independently
authored mixins from introducing conflicting indexes.

### The `indexed` trait

The `indexed` trait applies to a service:

```smithy
$version: "2.0"

namespace smithy.protocols

@trait(selector: "service")
structure indexed {}
```

Every member of every non-mixin structure and union in the closure of an
`indexed` service MUST have an `idx` trait unless one of the exemptions above
applies.

```smithy
$version: "2.0"

namespace smithy.example

use smithy.protocols#idx
use smithy.protocols#indexed

@indexed
service Example {
    version: "2026-01-01"
    operations: [GetRecord]
}

operation GetRecord {
    output: Record
}

structure Record {
    @idx(1)
    id: String

    @idx(2)
    value: String
}
```

Shapes outside the service closure are unaffected unless one of their members
uses `idx`. Applying `idx` to any member opts that shape into the same
completeness and ordering validation.

### IDL member index shorthand

Applying a trait to every member is verbose, so IDL 2.1 adds shorthand that
places the index and a period before an explicit or elided member:

```smithy
$version: "2.1"

namespace smithy.example

structure Record {
    1. id: String
    2. value: String
}
```

This example is equivalent to:

```smithy
$version: "2.1"

namespace smithy.example

use smithy.protocols#idx

structure Record {
    @idx(1)
    id: String

    @idx(2)
    value: String
}
```

The shorthand does not require a `use` statement.

#### Grammar updates

The shape member grammar is updated as follows:

```abnf
ShapeMember = TraitStatements [MemberIndex]
              (ExplicitShapeMember / ElidedShapeMember) [ValueAssignment]
MemberIndex = %x31-39 *(DIGIT) [`SP`] "." [`SP`]
```

The grammar accepts one or more ASCII digits, with a nonzero first digit.
Lossless syntax tools MAY preserve and format values of any magnitude. During
semantic model assembly, the value MUST fit in the Smithy `integer` type, so it
MUST NOT exceed 2147483647. The `idx` trait's range further requires a valid
member index to be no greater than 65535. Values between 65536 and 2147483647
parse successfully and then fail trait validation.

Only the shorthand is an IDL 2.1 feature. The `idx` and `indexed` traits can be
used explicitly in earlier IDL versions.

#### Explicit and shorthand applications

The explicit `idx` trait and shorthand SHOULD NOT both be used on the same
member:

```smithy
@idx(1)
1. id: String
```

Both forms are ordinary applications of the same trait. Equal values are
treated as a redundant trait application. Different values produce the normal
conflicting-trait validation event.

#### List and map members

The grammar permits member index shorthand anywhere a shape member can occur,
including list and map members:

```smithy
list InvalidList {
    1. member: String
}

map InvalidMap {
    1. key: String
    2. value: String
}
```

These examples are syntactically valid but fail semantic validation because
the `idx` trait can only target members of non-mixin structures and unions.
Keeping this distinction avoids adding aggregate-shape-specific behavior to
the shape member grammar.

### Formatting

Formatters align periods using the widest member index in each shape:

```smithy
structure Indexed {
     1. first: String
     2. second: String
     3. third: String
     4. fourth: String
     5. fifth: String
     6. sixth: String
     7. seventh: String
     8. eighth: String
     9. ninth: String
    10. tenth: String
}
```

When formatting a file with a version requirement greater than or equal to
IDL 2.1 and less than 3.0, a formatter SHOULD render resolvable, valid `idx`
trait applications using the shorthand. A formatter MUST NOT add or upgrade a
version statement to enable the shorthand.

IDL serializers SHOULD emit the shorthand for member traits by default and
provide a way to retain the explicit trait form. Trait applications emitted as
`apply` statements remain explicit because the shorthand is part of an inline
member declaration.

### JSON AST representation

The JSON AST has no special representation for member indexes.

## Model evolution

Changing an existing member's `idx` value is backward-incompatible because it
changes the member's wire index. New members should be assigned the next
contiguous index so that existing member indexes remain stable.

## Alternatives considered

### Declaration order

Using declaration order would require every model representation and
transformation to preserve object ordering. JSON objects provide no such
guarantee, and mixin transformations can change where members appear.
Declaration order is therefore not sufficiently stable for wire semantics.

### Explicit traits only

The feature could use only explicit `idx` traits. This would avoid an IDL
grammar change, but indexed services apply the trait to nearly every member.
The shorthand makes the common form compact while preserving a normal trait
in the semantic model.

### Postfix member indexes

A postfix form such as `member: String = 1` would resemble Protobuf field
numbers, but it conflicts with Smithy's existing value assignment syntax.
Using a prefix keeps a member index distinct from the member's default value.

### Zero-based indexes

Zero-based indexes are common in programming APIs, but member indexes are not
collection offsets. More concretely, the `idx` trait started life as a
non-public trait and was already 1-indexed, so changing it would introduce
unnecessary churn.

## Prior art

| System | Syntax | Semantics |
| --- | --- | --- |
| [FlatBuffers](https://flatbuffers.dev/schema/#attributes) | `name:string (id: 0);` | IDs appear on every field, are contiguous from zero, and permit arbitrary source order. |
| [Cap'n Proto](https://capnproto.org/language.html#structs) | `name @0 :Text;` | Consecutive member indexes start at zero and remain stable when members are rearranged. |
| [FIDL](https://fuchsia.dev/fuchsia-src/reference/fidl/language/language#tables) | `1: locales vector<string>;` | Prefix member indexes identify table and union members. |
| [Bond](https://github.com/microsoft/bond/blob/master/doc/src/compiler.md#struct-definition) | `0: uint32 fieldName;` | Prefix values identify fields. |
| [Thrift](https://thrift.apache.org/docs/idl#field-id) | `1: optional string name` | Prefix field IDs identify members but need not declare an order. |
| [Protobuf](https://protobuf.dev/programming-guides/proto3/#assigning) | `string name = 1;` | Postfix field numbers are unique but not contiguous. |
| [Avro](https://avro.apache.org/docs/1.12.0/specification/#records) | Declaration order | Record fields are encoded in declaration order without explicit member indexes. |

## Limitations

* Member indexes cannot be reserved. Removing a non-final member requires
  reindexing later members to maintain contiguity. A separate reservation
  mechanism can be introduced in the future.
* The shorthand is only available to compatible IDL 2.x versions with a
  version requirement of 2.1 or later. Other model representations use the
  `idx` trait directly.
* Member indexes define stable ordering metadata but do not define a wire
  encoding. Each protocol decides how to use them.

## FAQ

### Does source declaration order matter?

No. Member indexes, rather than declaration order, define the stable order.
Members can be rearranged without changing their indexes.

### How are members added to an indexed shape?

A new member uses the next contiguous member index. Existing members retain
their indexes.

### Can a service adopt indexing incrementally?

Individual structures and unions can adopt indexing before the service does by
applying `idx` to their members. Applying `indexed` to the service requires all
non-exempt members of every non-mixin structure and union in its closure to be
indexed.

### How do member indexes interact with mixins?

A mixin does not assign member indexes. Each consuming structure or union
assigns indexes to its elided mixin members alongside its locally declared
members.

### Does the shorthand require importing `idx`?

No. The shorthand is equivalent to applying the absolute
`smithy.protocols#idx` trait.
