# Inline collection declarations

* **Author**: Manuel Sugawara
* **Created**: 2026-05-14

## Abstract

This proposal introduces syntactic sugar for declaring list and map
shapes inline within member definitions. Instead of requiring a
separate top-level shape definition, members can declare their
collection type directly using `[Target]` for lists and `{Key: Value}`
for maps. The assembler generates synthetic shapes with stable,
content-derived names that are grouped by structural equivalence.

## Motivation

Lists and maps are the most common shapes that exist solely to be
referenced by a single member. Unlike structures, they rarely carry
meaningful names, e.g., `ListOfStrings`, `StringToStringMap`, etc. are
boilerplate that adds noise without conveying intent. Requiring a
separate top-level definition forces authors to context-switch and
invent names for shapes that have no semantic identity beyond their
structure.

Consider a typical model today:

```smithy
list StringList {
    member: String
}

map TagMap {
    key: String
    value: String
}

map AccountIndex {
    key: AccountId
    value: Account
}

structure MyStructure {
    names: StringList
    tags: TagMap
    accounts: AccountIndex
}
```

With inline collections, this becomes:

```smithy
structure MyStructure {
    names: [String]
    tags: {String: String}
    accounts: {AccountId: Account}
}
```

The inline form is more readable, reduces boilerplate, and keeps the
collection definition co-located with its usage.

## Proposal

### Syntax

Two new forms are allowed in member target positions:

- **Lists**: `[Target]`, declares a list whose member targets `Target`.
- **Maps**: `{KeyTarget: ValueTarget}`, declares a map whose key targets
  `KeyTarget` and whose value targets `ValueTarget`.

```smithy
structure Foo {
    strings: [String]
    tags: {String: String}
    nested: {PersonId: [Account]}
}
```

These forms are only permitted in member target positions. They cannot
appear as top-level shape definitions.

#### Nesting

Inline collections can be nested. The inner collection produces its
own synthetic shape, and the outer collection references it:

```smithy
structure Foo {
    // A map from PersonId to a list of Account
    accounts: {PersonId: [Account]}
}
```

This desugars into two synthetic shapes: one list and one map.

Inline connections MUST NOT be nested more than 3 levels deep.

#### ABNF

The member target production is extended:

```
member_target = shape_id / inline_list / inline_map

inline_list = "[" [ws] member_target [ws] "]"

inline_map = "{" [ws] member_target [ws] ":" [ws] member_target [ws] "}"
```

### Synthetic shape naming

A predictable, content-derived naming convention enables the assembler
to recognize when two inline declarations refer to the same
collection type and reuse a single shape for both. This keeps the
model compact (one shape per unique collection type rather than one
per member) and ensures that the generated shape ID is stable across
refactorings: reordering members, moving them between structures, or
adding new usages of the same type does not change existing shape IDs.
The `_Synthetic` prefix makes these shapes easy to identify in
tooling and ensures they cannot collide with user-defined names.

Inline collections produce synthetic shapes with content-derived
names. The name uses a `_Synthetic` prefix followed by the collection
type and a token for each resolved target.

The name is derived from each target's **resolved** shape ID (not the
text as written), relative to the namespace of the declaring structure.
Each target is encoded as a token using these rules, applied per target
and decided purely from the resolved target's namespace and name, so a
name never changes based on what else exists in the model, and in
particular does not change if a shape is later added to the prelude:

- If the target is in the declaring structure's namespace and is itself a
  synthetic shape (its name starts with `_Synthetic`), the token is
  `_Name`. Nested inline collections hit this case. The short form is
  safe because the prelude never defines shapes whose names start with
  `_`.
- Otherwise, if the target's simple name starts with `_`, the token is
  the flattened-namespace form (see below). This keeps an
  underscore-prefixed user shape from fusing with the prelude or
  same-namespace markers.
- Otherwise, a prelude target (`smithy.api`) is `__Name`.
- Otherwise, a target in the declaring structure's namespace is `_Name`.
- Otherwise (any other namespace) the token is the flattened-namespace
  form `_seg1_seg2_Name`.

Lists are named `_SyntheticListOf<token>`; maps are named
`_SyntheticMapOf<keyToken>_To_<valueToken>`.

Examples (declared in namespace `smithy.example`):

| Inline syntax | Synthetic shape name |
|---------------|---------------------|
| `[String]` | `_SyntheticListOf__String` |
| `[Widget]` (same namespace) | `_SyntheticListOf_Widget` |
| `[com.foo#Account]` | `_SyntheticListOf_com_foo_Account` |
| `{String: String}` | `_SyntheticMapOf__String_To___String` |
| `{String: [Integer]}` | `_SyntheticMapOf__String_To___SyntheticListOf__Integer` |

These names are stable: they are derived from resolved content, not
declaration order, so reordering members or adding new inline
collections does not change existing names. They are also
deterministic: the same resolved collection always produces the same
name.

The encoding is injective for all but pathological combinations of
namespaces and underscores (for example, `com.amazon#String` and
`com#amazon_String` both flatten to `com_amazon_String`). The assembler
keys deduplication on each collection's resolved element types, so
identical collections share one shape and a genuinely different
collection that maps to an already-used synthetic name is reported as a
validation error rather than silently reusing the first shape. A
user-defined shape that happens to use a `_Synthetic` name is likewise
reported.

The synthetic shapes are placed in the same namespace as the structure
that contains them.

### Grouping

Grouping is scoped to a single namespace. Within a namespace, all
members using the same inline collection type (e.g., `[String]`)
reference the same synthetic shape. Across namespaces, each namespace
produces its own synthetic shape independently:
`com.foo#_SyntheticListOf__String` and `com.bar#_SyntheticListOf__String`
are distinct shapes.

Explicit user-defined shapes are never grouped with synthetic shapes.
A `list MyList { member: String }` is a distinct shape from
`_SyntheticListOf__String` even though they are structurally equivalent.

### The `@synthetic` trait

Synthetic shapes carry the `smithy.api#synthetic` trait in the semantic
model and AST serialization. This trait identifies the shape as
assembler-generated and allows IDL serializers to reconstruct the
inline syntax when converting from AST back to IDL. It is not
user-applicable: only the assembler may attach it.

The trait is defined in the prelude so that any tooling that reads a
model's JSON AST can resolve it, including tooling that predates inline
collections or that reads a model serialized to an older IDL version.

### Traits on synthetic shapes

Because synthetic shapes are shared across all members that use the
same inline type, applying a trait to a synthetic shape would affect
every usage simultaneously. This creates action at a distance: adding
`@sparse` to `_SyntheticListOf__String` would make every `[String]`
member in the namespace sparse, which is almost never the intent. To
prevent this, traits cannot be applied to synthetic shapes.

No traits may be applied to synthetic shapes, either at definition
time or via `apply` statements. The `apply` statement MUST NOT target
a shape that has the `@synthetic` trait.

Traits that would normally apply to a collection shape (such as
`@sparse` or `@uniqueItems`) cannot be expressed using inline syntax.
Authors requiring these traits on the collection shape itself MUST use
explicit shape definitions.

Note that some traits like `@length` can also be applied to the
containing member. When applied to a member, the trait constrains only
that specific member usage, not the shared synthetic shape. For
example, `@length(min: 1) names: [String]` constrains the `names`
member but does not affect other members that reference the same
`_SyntheticListOf__String` shape.

### Referencing synthetic shapes

Synthetic shape names exist as valid shape IDs in the semantic model
and AST. However, since traits cannot be applied to synthetic shapes
and their members cannot be independently referenced, there is no
practical use case for referencing them directly in `apply` statements
(IDL or AST) or member target positions.

Synthetic shapes can be referenced in:

- Selectors (e.g., `[id = ns#_SyntheticListOf__String]`)
- Programmatic model access (Java API, JSON AST)

### Selectors

Synthetic shapes are normal shapes in the semantic model. They appear
in selector results and can be matched by type:

```
list                              // matches all lists, including synthetic
[trait|smithy.api#synthetic]      // matches only generated synthetic shapes
```

### AST serialization

In the JSON AST, synthetic shapes are serialized as normal shapes with
the `@synthetic` trait attached:

```json
{
    "smithy": "2.1",
    "shapes": {
        "com.example#_SyntheticListOf__String": {
            "type": "list",
            "member": {
                "target": "smithy.api#String"
            },
            "traits": {
                "smithy.api#synthetic": {}
            }
        },
        "com.example#MyStructure": {
            "type": "structure",
            "members": {
                "strings": {
                    "target": "com.example#_SyntheticListOf__String"
                }
            }
        }
    }
}
```

### IDL round-tripping

When converting from JSON AST to IDL 2.1, a serializer SHOULD
reconstruct inline syntax for shapes bearing the `@synthetic`
trait. When converting to IDL 2.0 or earlier, synthetic shapes MUST be
serialized as explicit top-level shape definitions.

## Alternatives considered

### Member-derived naming (`Foo~listMember`)

Names derived from the containing structure and member (e.g.,
`Foo~listMember` for a member named `listMember` in structure `Foo`)
using a character not valid in Smithy identifiers as separator.

This was rejected because:

- It prevents grouping. Each usage produces a unique shape even when
  structurally identical.
- Renaming a member changes the synthetic shape ID, which is a
  breaking change for anything referencing it.
- It creates many more shapes in the model than necessary.

### Illegal character separator (`Synthetic~ListOfString`)

Using a character not valid in Smithy identifiers (e.g., `~`) as part
of the synthetic name to guarantee no conflicts with user-defined
shapes.

This was rejected because:

- It requires extending the shape ID grammar to accommodate a new
  character.
- It adds complexity to parsers and tooling for a marginal benefit.
- The probability of a user-defined shape colliding with a
  `_Synthetic`-prefixed content-derived name is negligible in
  practice.

### Counter-based naming (`synthetic_1`, `synthetic_2`)

Sequential numbering of synthetic shapes in declaration order.

This was rejected because:

- Names are unstable, reordering declarations or adding new members
  renumbers existing shapes.
- Non-deterministic across serializations.
- Produces meaningless names in error messages and tooling.
- Breaks diffing, caching, and any tooling that indexes by shape ID.

### Allowing traits on synthetic shapes

Permitting traits like `@sparse`, `@uniqueItems`, or `@length` on
synthetic shapes, either via inline syntax or `apply`.

This was rejected because:

- It complicates grouping, traits on the shape would need to be part
  of the equivalence check.
- Applying traits to a shared/grouped shape affects all usages, which
  is rarely the intent.
- It introduces the new concept of "traits declared on a member that
  semantically apply to the target shape."
- The explicit shape definition path already handles these cases
  cleanly.
- Many common traits applied to lists and maps (such as `@length`,
  `@deprecated`, and `@documentation`) can also be applied to the
  containing member, reducing the need for shape-level trait
  application in practice.

### Square bracket syntax for maps (`[Key, Value]`)

Using `[String, String]` for maps.

This was rejected because:

- Ambiguous with list syntax, `[String]` is a list,
  `[String, String]` would be a map, creating a subtle and error-prone
  distinction.
- Does not convey key/value semantics; relies on positional
  convention.
- Forecloses future tuple or multi-type-parameter syntax.

### Curly braces with comma (`{Key, Value}`)

Using `{String, String}` for maps.

This was rejected because:

- Curly braces already denote structure/block bodies in Smithy,
  creating visual ambiguity.
- Does not convey key/value relationship.
- Resembles set literal syntax in many languages.

## Limitations

- Inline collections cannot carry shape-level traits. Authors needing
  `@sparse`, `@uniqueItems`, `@length` (on the collection), or other
  shape-level traits must use explicit shape definitions.
- Synthetic shapes cannot be targeted by `apply` statements.
- The inline syntax is only available in member target positions, not
  as top-level definitions.

## Migration

### Adopting inline syntax

Existing models using explicit list and map definitions can adopt
inline syntax incrementally. Replacing an explicit shape with inline
syntax changes the shape ID (from `ns#MyList` to
`ns#_SyntheticListOf__String`), which is a breaking change for:

- Other models referencing the shape by name
- Selectors targeting the shape by ID
- Code generators that derive type names from shape names

Authors should only migrate shapes that are not referenced externally.

### Outgrowing inline syntax

When a collection needs traits that cannot be expressed inline, the
migration path is:

1. Define an explicit named shape with the required traits.
2. Update the member to reference the explicit shape.

Since synthetic shapes cannot be referenced cross-model, this
migration is always local to the model that defines the inline
collection.

## FAQ

### Can inline collections be used in union members?

Yes. Inline collections can appear in any member target position,
including union members, structure members, and list/map member
targets (for nesting).

### Do synthetic shapes appear in code generation?

Synthetic shapes are normal shapes in the model. Code generators will
encounter them. However, most languages use idiomatic collection types
(e.g., `List<String>`, `Map<String, String>`) for lists and maps
regardless of the shape name, so the synthetic name is typically not
visible in generated code.

Code generators that do produce named types for collections can use
the synthetic name directly or transform it as needed for the target
language.

### What if two namespaces produce the same synthetic name?

Synthetic shapes are scoped to their
namespace. `com.foo#_SyntheticListOf__String` and
`com.bar#_SyntheticListOf__String` are distinct shapes, just as any two
shapes in different namespaces are distinct.

### Can a member target both an explicit shape and an inline collection?

A member targets exactly one shape. It either references an explicit
shape by name or uses inline syntax (which references the synthetic
shape). There is no way to "merge" the two.

### How does this interact with mixins?

If a mixin defines a member with an inline collection, the synthetic
shape is created in the mixin's namespace. Structures that apply the
mixin inherit the member, which continues to reference the same
synthetic shape.
